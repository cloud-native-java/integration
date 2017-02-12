package integration;

import cnj.CloudFoundryService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.PushApplicationRequest;
import org.cloudfoundry.operations.applications.SetEnvironmentVariableApplicationRequest;
import org.cloudfoundry.operations.applications.StartApplicationRequest;
import org.cloudfoundry.operations.services.BindServiceInstanceRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.rest.client.DataFlowTemplate;
import org.springframework.cloud.dataflow.rest.client.TaskOperations;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = DataFlowIT.Config.class)
public class DataFlowIT {

	private Log log = LogFactory.getLog(getClass());
	@Autowired
	private CloudFoundryOperations cloudFoundryOperations;
	private File serviceDefinitionsManifest;
	@Autowired
	private CloudFoundryService cloudFoundryService;
	private String appName = "cfdf";

	@Before
	public void deploy() throws Throwable {
		deployServiceDefinitions();
		deployDataFlowServer();
	}

	@Test
	public void deployTasksAndStreams() throws Exception {
		DataFlowTemplate df = this.dataFlowTemplate(this.appName);
		appDefinitions()
				.parallelStream()
				.forEach(u -> {
					log.info("importing " + u);
					df.appRegistryOperations().importFromResource(u, true);
					log.info("imported " + u);
				});

		Arrays.asList(deployStreams(df), deployTasks(df))
				.parallelStream()
				.map(r -> r)
				.forEach(Runnable::run);
		log.info("deployed tasks and streams.");
	}

	private void deployServiceDefinitions() {
		File projectFolder = new File(new File("."), "../dataflow/server-definitions");
		serviceDefinitionsManifest = new File(projectFolder, "manifest.yml");
		this.cloudFoundryService.pushApplicationUsingManifest(serviceDefinitionsManifest);
	}

	private void deployDataFlowServer() throws Throwable {

		// deploy the DF server
		String serverRedis = "cfdf-redis", serverMysql = "cfdf-mysql", serverRabbit = "cfdf-rabbit";
		Stream.of("rediscloud 100mb " + serverRedis,
				"cloudamqp lemur " + serverRabbit,
				"p-mysql 100mb " + serverMysql)
				.parallel()
				.map(x -> x.split(" "))
				.forEach(tpl -> this.cloudFoundryService.createServiceIfMissing(tpl[0], tpl[1], tpl[2]));

		String urlForServerJarDistribution = this.serverJarUrl();
		File cfdfJar = new File(System.getProperty("user.home"), "cfdf.jar");
		Assert.assertTrue(cfdfJar.getParentFile().exists() || cfdfJar.getParentFile().mkdirs());

		Path targetFile = cfdfJar.toPath();
		if (!cfdfJar.exists()) {
			URI uri = URI.create(urlForServerJarDistribution);
			try (InputStream inputStream = uri.toURL().openStream()) {
				java.nio.file.Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
			}
			this.log.info("downloaded Data Flow server to " + targetFile.toFile().getAbsolutePath() + ".");
		}
		log.info("Data Flow Server jar lives at " + cfdfJar.getAbsolutePath());

		int twoG = 1024 * 2;
		this.cloudFoundryOperations.applications()
				.push(PushApplicationRequest
						.builder()
						.application(targetFile)
						.buildpack("https://github.com/cloudfoundry/java-buildpack.git")
						.noStart(true)
						.name(appName)
						.host("cfdf-" + UUID.randomUUID().toString())
						.memory(twoG)
						.diskQuota(twoG)
						.build())
				.block();
		log.info("pushed (but didn't start) the Data Flow server");

		Map<String, String> env = new ConcurrentHashMap<>();

		// CF authentication
		env.put("SPRING_CLOUD_DEPLOYER_CLOUDFOUNDRY_ORG", System.getenv("CF_ORG"));
		env.put("SPRING_CLOUD_DEPLOYER_CLOUDFOUNDRY_SPACE", System.getenv("CF_SPACE"));
		env.put("SPRING_CLOUD_DEPLOYER_CLOUDFOUNDRY_USERNAME", System.getenv("CF_USER"));
		env.put("SPRING_CLOUD_DEPLOYER_CLOUDFOUNDRY_PASSWORD", System.getenv("CF_PASSWORD"));

		env.put("SPRING_CLOUD_DEPLOYER_CLOUDFOUNDRY_STREAM_SERVICES", serverRabbit);
		env.put("SPRING_CLOUD_DEPLOYER_CLOUDFOUNDRY_TASK_SERVICES", serverMysql);

		env.put("SPRING_CLOUD_DEPLOYER_CLOUDFOUNDRY_SKIP_SSL_VALIDATION", "false");
		env.put("SPRING_CLOUD_DEPLOYER_CLOUDFOUNDRY_URL", "https://api.run.pivotal.io");
		env.put("SPRING_CLOUD_DEPLOYER_CLOUDFOUNDRY_DOMAIN", "cfapps.io");

		env.put("MAVEN_REMOTE_REPOSITORIES_LR_URL", "https://cloudnativejava.artifactoryonline.com/cloudnativejava/libs-release");
		env.put("MAVEN_REMOTE_REPOSITORIES_LS_URL", "https://cloudnativejava.artifactoryonline.com/cloudnativejava/libs-snapshot");
		env.put("MAVEN_REMOTE_REPOSITORIES_PR_URL", "https://cloudnativejava.artifactoryonline.com/cloudnativejava/plugins-release");
		env.put("MAVEN_REMOTE_REPOSITORIES_PS_URL", "https://cloudnativejava.artifactoryonline.com/cloudnativejava/plugins-snapshot");

		env.put("SPRING_CLOUD_DEPLOYER_CLOUDFOUNDRY_STREAM_INSTANCES", "1");

		env.entrySet()
				.forEach(e -> {
					this.cloudFoundryOperations
							.applications()
							.setEnvironmentVariable(
									SetEnvironmentVariableApplicationRequest
											.builder()
											.name(appName)
											.variableName(e.getKey())
											.variableValue(e.getValue())
											.build())
							.block();

					log.info("set environment variable for " + appName + ": " + e.getKey() + '=' + e.getValue());
				});

		log.info("set all " + env.size() + " environment variables.");

		// bind the relevant services to DF
		Stream.of(serverMysql, serverRedis)
				.forEach(svc -> {

					this.cloudFoundryOperations.services()
							.bind(BindServiceInstanceRequest
									.builder()
									.applicationName(appName)
									.serviceInstanceName(svc)
									.build())
							.block();
					log.info("binding " + svc + " to " + appName);
				});

		// start
		this.cloudFoundryOperations
				.applications()
				.start(StartApplicationRequest
						.builder()
						.stagingTimeout(Duration.ofMinutes(10))
						.startupTimeout(Duration.ofMinutes(10))
						.name(appName)
						.build())
				.block();

		log.info("started the Spring Cloud Data Flow Cloud Foundry server.");
	}

	private Runnable deployStreams(DataFlowTemplate df) {
		return () -> {
			Map<String, String> streams = new HashMap<>();
			streams.put("ttl", "time | brackets | log");

			log.info("going to deploy " + streams.size() + " new stream(s).");
			streams.entrySet()
					.parallelStream()
					.forEach(stream -> {
						String streamName = stream.getKey();

						StreamSupport.stream(Spliterators.spliteratorUnknownSize(df.streamOperations().list().iterator(),
								Spliterator.ORDERED), false)
								.filter(sdr -> sdr.getName().equals(streamName)).forEach(tdr -> {
							log.info("deploying stream " + streamName);
							df.streamOperations().destroy(streamName);
						});

						df.streamOperations()
								.createStream(streamName, stream.getValue(), true);
					});
		};
	}

	private Runnable deployTasks(DataFlowTemplate df) {
		return () -> {
			Map<String, String> tasks = new HashMap<>();
			tasks.put("my-simple-task", "simple-task");

			log.info("going to deploy " + tasks.size() + " new task(s).");
			tasks.entrySet()
					.parallelStream()
					.forEach(task -> {

						String taskName = task.getKey();

						StreamSupport.stream(Spliterators.spliteratorUnknownSize(df.taskOperations().list().iterator(),
								Spliterator.ORDERED), false)
								.filter(tdr -> tdr.getName().equals(taskName)).forEach(tdr -> {
							log.info("destroying task " + taskName);
							df.taskOperations().destroy(taskName);
						});

						log.info("deploying task " + taskName);
						TaskOperations to = df.taskOperations();
						to.create(taskName, task.getValue());
						to.launch(taskName,
								Collections.emptyMap(),
								Collections.singletonList(System.currentTimeMillis() + ""));
					});
		};
	}

	private DataFlowTemplate dataFlowTemplate(String cfDfServerName) throws Exception {
		String urlForApplication = this.cloudFoundryService.urlForApplication(cfDfServerName);
		log.info("attempting to create a DataFlowTemplate using the following API endpoint " + urlForApplication);
		return Optional.ofNullable(urlForApplication)
				.map(u -> new DataFlowTemplate(URI.create(u), new RestTemplate()))
				.orElseThrow(() -> new RuntimeException(
						"can't find a URI for the Spring Cloud Data Flow server!"));
	}

	private String serverJarUrl() {
		String serverJarVersion = "1.1.0.BUILD-SNAPSHOT";
		String serverJarUrl = "http://repo.spring.io/${server_jar_url_prefix}/org/springframework/cloud/" +
				"spring-cloud-dataflow-server-cloudfoundry/${server_jar_version}" +
				"/spring-cloud-dataflow-server-cloudfoundry-${server_jar_version}.jar";
		String prefix = serverJarVersion.toUpperCase().contains("RELEASE") ? "release" : "snapshot";
		return serverJarUrl
				.replace("${server_jar_url_prefix}", prefix)
				.replace("${server_jar_version}", serverJarVersion);
	}

	private List<String> appDefinitions() {
		List<String> apps = new ArrayList<>();
		apps.add("http://repo.spring.io/libs-release-local/org/springframework/cloud/task/app/spring-cloud-task-app-descriptor/Addison.RELEASE/spring-cloud-task-app-descriptor-Addison.RELEASE.task-apps-maven");
		apps.add("http://repo.spring.io/libs-release/org/springframework/cloud/stream/app/spring-cloud-stream-app-descriptor/Avogadro.SR1/spring-cloud-stream-app-descriptor-Avogadro.SR1.stream-apps-rabbit-maven");

		Optional.ofNullable(this.cloudFoundryService.urlForApplication("server-definitions"))
				.map(x -> x + "/dataflow-example-apps.properties")
				.ifPresent(apps::add);

		apps.forEach(x -> log.info("registering: " + x));
		return apps;
	}

	@SpringBootApplication
	public static class Config {
	}
}
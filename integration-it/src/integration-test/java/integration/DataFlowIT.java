package integration;

import cnj.CloudFoundryService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.PushApplicationRequest;
import org.cloudfoundry.operations.applications.SetEnvironmentVariableApplicationRequest;
import org.cloudfoundry.operations.applications.StartApplicationRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.rest.client.DataFlowTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = DataFlowIT.Config.class)
public class DataFlowIT {

	@SpringBootApplication
	public static class Config {
	}

	private Log log = LogFactory.getLog(getClass());

	@Autowired
	private CloudFoundryOperations cloudFoundryOperations;

	@Autowired
	private CloudFoundryService cloudFoundryService;

	@Before
	public void before() throws Throwable {

		String serverRedis = "cfdf-redis", serverMysql = "cfdf-mysql", serverRabbit = "cfdf-rabbit";
		String appName = "cfdf";

		this.cloudFoundryService.destroyApplicationIfExists(appName);

		Stream.of("rediscloud 100mb " + serverRedis,
				"cloudamqp lemur " + serverRabbit,
				"p-mysql 100mb " + serverMysql)
				.parallel()
				.map(x -> x.split(" "))
				.forEach(tpl -> this.cloudFoundryService.createServiceIfMissing(tpl[0], tpl[1], tpl[2]));

		String urlForServerJarDistribution = this.serverJarUrl();
		Path targetFile = Files.createTempFile("cfdf", ".jar");
		URI uri = URI.create(urlForServerJarDistribution);
		try (InputStream inputStream = uri.toURL().openStream()) {
			java.nio.file.Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
		}

		this.log.info("downloaded Data Flow server to " +
				targetFile.toFile().getAbsolutePath() + ".");

		int twoG = 1024 * 2;
		this.cloudFoundryOperations.applications()
				.push(PushApplicationRequest
						.builder()
						.application(targetFile)
						.noStart(true)
						.name(appName)
						.memory(twoG)
						.diskQuota(twoG)
						.build())
				.block();


		Map<String, String> env = new ConcurrentHashMap<>();

		// CF authentication
		env.put("SPRING_CLOUD_DEPLOYER_CLOUDFOUNDRY_SKIP_SSL_VALIDATION", "false");
		env.put("SPRING_CLOUD_DEPLOYER_CLOUDFOUNDRY_URL", "https://api.run.pivotal.io");
		env.put("SPRING_CLOUD_DEPLOYER_CLOUDFOUNDRY_ORG", System.getenv("CF_ORG"));
		env.put("SPRING_CLOUD_DEPLOYER_CLOUDFOUNDRY_SPACE", System.getenv("CF_SPACE"));
		env.put("SPRING_CLOUD_DEPLOYER_CLOUDFOUNDRY_DOMAIN", "cfapps.io");
		env.put("SPRING_CLOUD_DEPLOYER_CLOUDFOUNDRY_STREAM_SERVICES", serverRabbit);
		env.put("SPRING_CLOUD_DEPLOYER_CLOUDFOUNDRY_TASK_SERVICES", serverMysql);
		env.put("SPRING_CLOUD_DEPLOYER_CLOUDFOUNDRY_USERNAME", System.getenv("CF_USER"));
		env.put("SPRING_CLOUD_DEPLOYER_CLOUDFOUNDRY_PASSWORD", System.getenv("CF_PASSWORD"));
		env.put("MAVEN_REMOTE_REPOSITORIES_LR_URL", "https://cloudnativejava.artifactoryonline.com/cloudnativejava/libs-release");
		env.put("MAVEN_REMOTE_REPOSITORIES_LS_URL", "https://cloudnativejava.artifactoryonline.com/cloudnativejava/libs-snapshot");
		env.put("MAVEN_REMOTE_REPOSITORIES_PR_URL", "https://cloudnativejava.artifactoryonline.com/cloudnativejava/plugins-release");
		env.put("MAVEN_REMOTE_REPOSITORIES_PS_URL", "https://cloudnativejava.artifactoryonline.com/cloudnativejava/plugins-snapshot");

		env.put("SPRING_CLOUD_DEPLOYER_CLOUDFOUNDRY_STREAM_INSTANCES", "1");

		env.entrySet()
				.parallelStream()
				.forEach((e) ->
						this.cloudFoundryOperations
								.applications()
								.setEnvironmentVariable(SetEnvironmentVariableApplicationRequest
										.builder()
										.name(appName)
										.variableName(e.getKey())
										.variableValue(e.getValue())
										.build())
								.block());

		this.cloudFoundryOperations
				.applications()
				.start(StartApplicationRequest.builder().name(appName).build())
				.block();

	}

	private DataFlowTemplate dataFlowTemplate(
			String cfDfServerName) throws Exception {
		return
				Optional.ofNullable(this.cloudFoundryService.urlForApplication(cfDfServerName))
						.map(u -> new DataFlowTemplate(URI.create(u), new RestTemplate()))
						.orElseThrow(() -> new RuntimeException(
								"can't find a URI for the Spring Cloud Data Flow server!"));
	}

	@Test
	public void deployStreamsAndTasksToDataFlowServer()
			throws Throwable {

//		this.dataFlowTemplate.streamOperations().list().forEach(System.out::println );

		/*

    [ -f ${server_jar} ] || wget -O ${server_jar} "${server_jar_url}"
    [ -f ${server_jar} ] && echo "cached ${server_jar} locally."

    cf push $app_name -m 2G -k 2G --no-start -p ${server_jar}


    cf bind-service $app_name $server_redis
    cf bind-service $app_name $server_mysql

    cf set-env $app_name SPRING_CLOUD_DEPLOYER_CLOUDFOUNDRY_SKIP_SSL_VALIDATION false
    cf set-env $app_name SPRING_CLOUD_DEPLOYER_CLOUDFOUNDRY_URL https://api.run.pivotal.io
    cf set-env $app_name SPRING_CLOUD_DEPLOYER_CLOUDFOUNDRY_ORG $CF_ORG
    cf set-env $app_name SPRING_CLOUD_DEPLOYER_CLOUDFOUNDRY_SPACE $CF_SPACE
    cf set-env $app_name SPRING_CLOUD_DEPLOYER_CLOUDFOUNDRY_DOMAIN cfapps.io
    cf set-env $app_name SPRING_CLOUD_DEPLOYER_CLOUDFOUNDRY_STREAM_SERVICES $server_rabbit
    cf set-env $app_name SPRING_CLOUD_DEPLOYER_CLOUDFOUNDRY_TASK_SERVICES $server_mysql
    cf set-env $app_name SPRING_CLOUD_DEPLOYER_CLOUDFOUNDRY_USERNAME $CF_USER
    cf set-env $app_name SPRING_CLOUD_DEPLOYER_CLOUDFOUNDRY_PASSWORD $CF_PASSWORD

    cf set-env $app_name MAVEN_REMOTE_REPOSITORIES_LR_URL https://cloudnativejava.artifactoryonline.com/cloudnativejava/libs-release
    cf set-env $app_name MAVEN_REMOTE_REPOSITORIES_LS_URL https://cloudnativejava.artifactoryonline.com/cloudnativejava/libs-snapshot
    cf set-env $app_name MAVEN_REMOTE_REPOSITORIES_PR_URL https://cloudnativejava.artifactoryonline.com/cloudnativejava/plugins-release
    cf set-env $app_name MAVEN_REMOTE_REPOSITORIES_PS_URL https://cloudnativejava.artifactoryonline.com/cloudnativejava/plugins-snapshot

    cf set-env $app_name SPRING_CLOUD_DEPLOYER_CLOUDFOUNDRY_STREAM_INSTANCES 1

    cf start $app_name*/


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

}

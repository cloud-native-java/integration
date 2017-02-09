package demo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.client.lib.rest.CloudControllerClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.rest.client.DataFlowTemplate;
import org.springframework.cloud.dataflow.rest.client.TaskOperations;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ClientConfiguration.class)
public class DataFlowIntegrationTest {

	@Autowired
	private CloudFoundryHelper helper;

	@Autowired
	private DataFlowTemplate df;

	@Autowired
	private CloudControllerClient cf;

	private Log log = LogFactory.getLog(getClass());

	List<String> appDefinitions() {
		List<String> apps = new ArrayList<>();
		apps.add("http://repo.spring.io/libs-release-local/org/springframework/cloud/task/app/spring-cloud-task-app-descriptor/Addison.RELEASE/spring-cloud-task-app-descriptor-Addison.RELEASE.task-apps-maven");
		apps.add("http://repo.spring.io/libs-release/org/springframework/cloud/stream/app/spring-cloud-stream-app-descriptor/Avogadro.SR1/spring-cloud-stream-app-descriptor-Avogadro.SR1.stream-apps-rabbit-maven");
		this.helper
				.uriFor("dataflow-server-definitions")
				.map(x -> x + "/dataflow-example-apps.properties")
				.ifPresent(apps::add);
		return apps;
	}



	@Test
	public void dataFlowIsFlowing() throws Exception {

		appDefinitions()
				.parallelStream()
				.forEach(u -> {
					log.info("importing " + u);
					df.appRegistryOperations().importFromResource(u, true);
				});

		log.info("destroying all existing streams");
		df.streamOperations().destroyAll();

		Map<String, String> streams = new HashMap<>();
		streams.put("ttl", "time | brackets | log");
		log.info("going to deploy " + streams.size() + " new stream(s).");
		streams.entrySet().parallelStream()
				.forEach(stream -> df.streamOperations()
						.createStream(stream.getKey(), stream.getValue(), true));

		Map<String, String> tasks = new HashMap<>();
		tasks.put("my-simple-task", "simple-task");
		log.info("going to deploy " + tasks.size() + " new task(s).");
		tasks.entrySet().parallelStream()
				.forEach(task -> {
					TaskOperations to = df.taskOperations();
					to.create(task.getKey(), task.getValue());
					to.launch(task.getKey(),
							Collections.emptyMap(),
							Collections.singletonList(System.currentTimeMillis() + ""));
				});
	}
}
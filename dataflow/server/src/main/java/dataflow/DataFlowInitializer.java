package dataflow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.dataflow.rest.client.DataFlowTemplate;
import org.springframework.cloud.dataflow.rest.client.TaskOperations;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.stream.Stream;

@Component
class DataFlowInitializer {

	private final URI baseUri;

	@Autowired
	public DataFlowInitializer(@Value("${server.port}") int port) {
		this.baseUri = URI.create("http://localhost:" + port);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void deployOnStart(ApplicationReadyEvent e) throws Exception {

		// <1>
		DataFlowTemplate df = new DataFlowTemplate(baseUri, new RestTemplate());

		// <2>
		Stream.of(
				"http://bit.ly/stream-applications-rabbit-maven",
				new URL(baseUri.toURL(), "app.properties").toString()
		)
				.parallel()
				.forEach(u -> df.appRegistryOperations().importFromResource(u, true));


		TaskOperations taskOperations = df.taskOperations();
		Stream.of("batch-task", "simple-task")
				.forEach(tn -> {
					String name = "my-" + tn;
					taskOperations.create(name, tn); // <3>
					Map<String, String> properties = Collections.singletonMap("simple-batch-task.input", System.getenv("HOME") + "Desktop/in.csv");
					List<String> arguments = Arrays.asList("input=in", "output=out");
					taskOperations.launch(name, properties, arguments); // <4>
				});


		// <3>
		Map<String, String> streams = new HashMap<>();
		streams.put("bracket-time", "time | brackets | log");
		streams.entrySet().parallelStream()
				.forEach(stream -> df.streamOperations()
						.createStream(stream.getKey(), stream.getValue(), true));
	}
}
package dataflow;


import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.dataflow.rest.client.DataFlowTemplate;
import org.springframework.context.event.EventListener;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

//@Component
// TODO
public class DataFlowInitializer {

	@Value("${server.port}")
	private int port;

	@EventListener(ApplicationReadyEvent.class)
	public void applicationIsReady(ApplicationReadyEvent e) throws Exception {

		URI baseUri = URI.create ("http://localhost:" + port);
		DataFlowTemplate dataFlowTemplate = new DataFlowTemplate(baseUri, new RestTemplate());

		// Import event stream apps //TODO show how to deploy custom apps defined locally
		dataFlowTemplate.appRegistryOperations()
				.importFromResource(new URL(baseUri.toURL(), "app.properties").toString(), false);

		// Import RabbitMQ stream apps
		dataFlowTemplate.appRegistryOperations()
				.importFromResource("http://bit.ly/stream-applications-rabbit-maven", false);

		// Deploy a set of event stream definitions
		List<StreamApp> streams = Arrays.asList(
				new StreamApp("account-stream",
						"account-web: account-web | account-worker: account-worker"),
				new StreamApp("order-stream",
						"order-web: order-web | order-worker: order-worker"),
				new StreamApp("payment-stream",
						"payment-web: payment-web | payment-worker: payment-worker"),
				new StreamApp("warehouse-stream",
						"warehouse-web: warehouse-web | warehouse-worker: warehouse-worker"));

		// Deploy the streams in parallel
		streams.parallelStream()
				.forEach(stream -> dataFlowTemplate.streamOperations()
						.createStream(stream.getName(), stream.getDefinition(), true));

	}
}

@Data
@AllArgsConstructor
class StreamApp {
	private String name;
	private String definition;
}
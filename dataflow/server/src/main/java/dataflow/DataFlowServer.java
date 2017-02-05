package dataflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.dataflow.server.EnableDataFlowServer;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

// <1>
@EnableDataFlowServer
@SpringBootApplication
public class DataFlowServer {

	public static void main(String[] args) {
		SpringApplication.run(DataFlowServer.class, args);
	}
}

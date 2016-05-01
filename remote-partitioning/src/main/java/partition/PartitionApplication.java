package partition;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.integration.annotation.IntegrationComponentScan;

@EnableBatchProcessing
@IntegrationComponentScan
@SpringBootApplication
public class PartitionApplication {

	public static void main(String args[]) {
		SpringApplication.run(PartitionApplication.class, args);
	}
}
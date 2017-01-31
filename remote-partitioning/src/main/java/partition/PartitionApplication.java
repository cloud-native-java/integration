package partition;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.support.PeriodicTrigger;

import javax.sql.DataSource;

@EnableBatchProcessing
@IntegrationComponentScan
@SpringBootApplication
public class PartitionApplication {

	public static void main(String args[]) {
		SpringApplication.run(PartitionApplication.class, args);
	}

	@Bean
	JdbcTemplate template(DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}

	@Bean(name = PollerMetadata.DEFAULT_POLLER)
	PollerMetadata defaultPoller() {
		PollerMetadata pollerMetadata = new PollerMetadata();
		pollerMetadata.setTrigger(new PeriodicTrigger(10));
		return pollerMetadata;
	}
}
package processing;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.io.File;

@EnableBatchProcessing
@SpringBootApplication
public class BatchApplication {

	@Bean
	RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Bean
	JdbcTemplate jdbcTemplate(DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}

	// <1>
	@Bean
	CommandLineRunner run(
			JobLauncher launcher,
			Job job,
			@Value("${user.home}") String home) {
		return args ->
				launcher.run(job,
						new JobParametersBuilder()
								.addString("input", path(home, "in.csv"))
								.addString("output", path(home, "out.csv"))
								.toJobParameters());
	}

	public static void main(String[] args) {
		SpringApplication.run(BatchApplication.class, args);
	}

	private String path(String home, String fileName) {
		return new File(home, fileName).getAbsolutePath();
	}
}
package task;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.annotation.Bean;

import java.util.Map;

@EnableTask  // <1>
@EnableBatchProcessing
@SpringBootApplication
public class BatchTaskExample {

	private Log log = LogFactory.getLog(getClass());

	@Bean
	Step tasklet(StepBuilderFactory sbf) {
		return sbf
				.get("tasklet")
				.tasklet((contribution, chunkContext) -> {
					log.info("Hello, world. " +
							"Here are the parameters: ");
					chunkContext
						.getStepContext()
						.getJobParameters()
						.entrySet()
						.forEach(this::log);
					return RepeatStatus.FINISHED;
				})
				.build();
	}

	@Bean
	Job hello(JobBuilderFactory jbf) {
		Step step = this.tasklet(null);
		return jbf
				.get("hello")
				.start(step)
				.build();
	}

	private void log(Map.Entry<String, Object> e) {
		log.info(e.getKey() + ':' + e.getValue());
	}

	public static void main(String[] args) {
		SpringApplication.run(BatchTaskExample.class, args);
	}
}

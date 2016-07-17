package task;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.annotation.Bean;

@EnableTask
// <1>
@EnableBatchProcessing
// <2>
@SpringBootApplication
public class BatchTaskExample {

	@Bean
	Job hello(JobBuilderFactory jobBuilderFactory,
			StepBuilderFactory stepBuilderFactory) {

		// @formatter:off

		Log log = LogFactory.getLog(getClass());

		TaskletStep step = stepBuilderFactory
				.get("one")
				.tasklet((stepContribution, chunkContext) -> { // <3>
							log.info("Hello, world");
							log.info("parameters: ");
							chunkContext
									.getStepContext()
									.getJobParameters()
									.entrySet()
									.forEach(
											e -> log.info(e.getKey() + ':'
													+ e.getValue()));
							return RepeatStatus.FINISHED;
						}).build();

		return jobBuilderFactory.get("hello").start(step).build();

		// @formatter:on
	}

	public static void main(String[] args) {
		SpringApplication.run(BatchTaskExample.class, args);
	}
}

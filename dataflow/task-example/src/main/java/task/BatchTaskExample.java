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

@EnableTask  // <1>
@EnableBatchProcessing
@SpringBootApplication
public class BatchTaskExample {

	private Log log = LogFactory.getLog(getClass());

	@Bean
	Step tasklet(StepBuilderFactory sbf, BatchTaskProperties btp) {
		return sbf
				.get("tasklet")
				.tasklet((contribution, chunkContext) -> {
					log.info("input = " + btp.getInput());
					return RepeatStatus.FINISHED;
				})
				.build();
	}

	@Bean
	Job hello(JobBuilderFactory jbf) {
		Step step = this.tasklet(null, null);
		return jbf
				.get("batch-task")
				.start(step)
				.build();
	}

	public static void main(String[] args) {
		SpringApplication.run(BatchTaskExample.class, args);
	}
}

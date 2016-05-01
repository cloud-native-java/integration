package partition;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StopWatch;

@Configuration
@Profile("!worker")
class JobConfiguration {

	@Bean
	Job job(JobBuilderFactory jobBuilderFactory, @Qualifier(PartitionConfiguration.STEP_1) Step step) throws Exception {

		JobExecutionListener jobExecutionListener = new JobExecutionListener() {

			private volatile StopWatch stopWatch = new StopWatch();

			private Log log = LogFactory.getLog(getClass());

			@Override
			public void beforeJob(JobExecution jobExecution) {
				this.stopWatch.start();
			}

			@Override
			public void afterJob(JobExecution jobExecution) {
				this.stopWatch.stop();
				log.info("job finished in " + this.stopWatch.prettyPrint());
			}
		};

		return jobBuilderFactory
				.get("job")
				.listener(jobExecutionListener)
				.incrementer(new RunIdIncrementer())
				.start(step)
				.build();
	}
}

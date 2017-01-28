package processing;

import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.web.client.HttpStatusCodeException;
import processing.email.InvalidEmailException;

import java.util.Map;
import java.util.concurrent.Executors;

@Configuration
class BatchConfiguration {

	@Bean
	Job etl(JobBuilderFactory jbf,
	        StepBuilderFactory sbf,
	        JdbcTemplate template,
	        Step1Configuration step1Configuration,
	        Step2Configuration step2Configuration) throws Exception {

		Step setup = sbf.get("clean-contact-table")
				.tasklet((contribution, chunkContext) -> {
					template.update("delete from PEOPLE");
					return RepeatStatus.FINISHED;
				})
				.build();

		Step s1 = sbf.get("file-db")
				.<Person, Person>chunk(10)
				.faultTolerant()
					.skip(InvalidEmailException.class)
					.skipPolicy((Throwable t, int skipCount) -> {
						LogFactory.getLog(getClass()).info("skipping ");
						return t.getClass().isAssignableFrom(InvalidEmailException.class);
					})
					.retry(HttpStatusCodeException.class)
					.retryLimit(2)
				.reader(step1Configuration.fileReader(null))
				.processor(step1Configuration.emailValidatingProcessor(null))
				.writer(step1Configuration.jdbcWriter(null))
				.taskExecutor(new ConcurrentTaskExecutor(Executors.newFixedThreadPool(10)))
				.build();

		Step s2 = sbf.get("db-file")
				.<Map<Integer, Integer>, Map<Integer, Integer>>chunk(1000)
				.reader(step2Configuration.jdbcReader(null))
				.writer(step2Configuration.fileWriter(null))
				.build();

		return jbf.get("etl")
				.incrementer(new RunIdIncrementer())
				.start(setup)
				.next(s1)
				.next(s2)
				.build();
	}
}
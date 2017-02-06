package processing;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.HttpStatusCodeException;
import processing.email.InvalidEmailException;

import java.util.Map;

@Configuration
class BatchConfiguration {

	// <1>
	@Bean
	Job etl(JobBuilderFactory jbf,
	        StepBuilderFactory sbf,
	        Step1Configuration step1, // <2>
	        Step2Configuration step2,
	        Step3Configuration step3) throws Exception {

		Step setup = sbf.get("clean-contact-table")
				.tasklet(step1.tasklet(null)) // <3>
				.build();

		Step s2 = sbf.get("file-db")
				.<Person, Person>chunk(1000) // <4>
				.faultTolerant()  // <5>
				.skip(InvalidEmailException.class)
				.retry(HttpStatusCodeException.class)
				.retryLimit(2)
				.reader(step2.fileReader(null)) // <6>
				.processor(step2.emailValidatingProcessor(null)) // <7>
				.writer(step2.jdbcWriter(null)) // <8>
				.build();

		// <9>
		Step s3 = sbf.get("db-file")
				.<Map<Integer, Integer>, Map<Integer, Integer>>chunk(100)
				.reader(step3.jdbcReader(null))
				.writer(step3.fileWriter(null))
				.build();

		return jbf.get("etl")
				.incrementer(new RunIdIncrementer()) // <10>
				.start(setup)  // <11>
				.next(s2)
				.next(s3)
				.build(); // <12>
	}
}
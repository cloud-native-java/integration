package partition;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@Profile(Profiles.LEADER_PROFILE)
class JobConfiguration {

	@Bean
	Step stagingStep(StepBuilderFactory sbf,
	                 JdbcTemplate jdbcTemplate) {
		return sbf
				.get("staging")
				.tasklet((contribution, chunkContext) -> {
					jdbcTemplate.execute("truncate NEW_PEOPLE");
					return RepeatStatus.FINISHED;
				})
				.build();
	}

	@Bean
	Job job(JobBuilderFactory jbf, PartitionStepConfiguration psc) throws Exception {
		Step partitionStep = psc.partitionStep(null,
				null, null, null);

		return jbf
				.get("job")
				.incrementer(new RunIdIncrementer())
				.start(stagingStep(null, null))
				.start(partitionStep)
				.build();
	}
}

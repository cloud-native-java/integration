package partition;

import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Date;

@Configuration
@Profile(Profiles.LEADER_PROFILE)
class JobConfiguration {

	@Bean
	Job job(JobBuilderFactory jbf,
	        StepBuilderFactory sbf,
	        JdbcTemplate jdbcTemplate,
	        PartitionStepConfiguration pc)
			throws Exception {

		Step partitionStep = pc.partitionStep(null,
				null, null, null);

		return jbf.get("job")
				.incrementer(parameters -> {
					JobParameters params = parameters == null ? new JobParameters() : parameters;
					return new JobParametersBuilder(params)
							.addDate("date", new Date())
							.toJobParameters();
				})
				.start(
						sbf.get("announce")
								.tasklet((StepContribution contribution, ChunkContext chunkContext) -> {
									jdbcTemplate.execute("truncate NEW_PEOPLE");
									return RepeatStatus.FINISHED;
								})
								.build())
				.start(partitionStep)
				.build();
	}
}

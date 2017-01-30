package partition;

import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.Date;

@Configuration
@Profile(PartitionConfiguration.LEADER_PROFILE)
class JobConfiguration {

	@Bean
	JdbcTemplate template(DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}

	@Bean
	Job job(JobBuilderFactory jbf,
	        StepBuilderFactory sbf,
	        JdbcTemplate jdbcTemplate,
	        @Qualifier(PartitionConfiguration.STEP_1) Step step)
			throws Exception {


		return jbf.get("job")
				.incrementer(parameters -> {
					JobParameters params = (parameters == null) ? new JobParameters() : parameters;
					return new JobParametersBuilder(params)
							.addDate("date", new Date())
							.toJobParameters();
				})
				.start(
						sbf.get("announce")
								.tasklet((StepContribution contribution, ChunkContext chunkContext) -> {
									LogFactory.getLog(getClass()).info(String.format("starting partitioned job @ %s",
											Instant.now().toString()));
									jdbcTemplate.execute("truncate NEW_PERSON");
									return RepeatStatus.FINISHED;
								})
								.build())
				.start(step)
				.build();
	}
}

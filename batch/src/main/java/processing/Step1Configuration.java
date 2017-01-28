package processing;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
class Step1Configuration {

	@Bean
	Tasklet tasklet(JdbcTemplate jdbcTemplate) {

		Log log = LogFactory.getLog(getClass());

		return (contribution, chunkContext) -> { // <1>
			log.info("starting the ETL job.");
			jdbcTemplate.update("delete from PEOPLE");
			return RepeatStatus.FINISHED;
		};
	}
}
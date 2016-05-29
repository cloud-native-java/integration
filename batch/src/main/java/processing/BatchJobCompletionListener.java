package processing;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.batch.JobExecutionEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class BatchJobCompletionListener {

	private Log log = LogFactory.getLog(getClass());

	private final JdbcTemplate template;

	@EventListener(JobExecutionEvent.class)
	public void jobExecuted(JobExecutionEvent executionEvent) {
		this.template.query("select * from customer", rs -> {
			this.log.info(String.format("id={}, email={}, full_name={}",
					rs.getLong("id"),
					rs.getString("email"),
					rs.getString("full_name")));
		});
	}

	@Autowired
	public BatchJobCompletionListener(DataSource dataSource) {
		this.template = new JdbcTemplate(dataSource);
	}
}

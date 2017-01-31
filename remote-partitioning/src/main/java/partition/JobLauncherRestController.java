package partition;

import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.stream.Collectors;

import static org.springframework.batch.core.ExitStatus.COMPLETED;

@RestController
@Profile(PartitionConfiguration.LEADER_PROFILE)
public class JobLauncherRestController {

	private final Job job;
	private final JobLauncher jobLauncher;

	public JobLauncherRestController(JobConfiguration job, JobLauncher jobLauncher)
			throws Exception {
		this.job = job.job(null, null, null, null);
		this.jobLauncher = jobLauncher;
	}

	@RequestMapping(value = "/migrate",
			method = {RequestMethod.POST, RequestMethod.GET})
	ResponseEntity<?> start() throws JobExecutionException {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start("migration-job");
		try {
			JobExecution execution = this.jobLauncher.run(this.job,
					new JobParametersBuilder()
							.addDate("date", new Date())
							.toJobParameters());
			if (execution.getExitStatus().equals(COMPLETED)) {
				return ResponseEntity.ok(COMPLETED);
			}
			return ResponseEntity
					.badRequest()
					.body(execution
							.getAllFailureExceptions()
							.stream().map(Object::toString)
							.collect(Collectors.joining(", ")));
		}
		finally {
			stopWatch.stop();
			LogFactory.getLog(getClass()).info(stopWatch.prettyPrint());
		}
	}
}

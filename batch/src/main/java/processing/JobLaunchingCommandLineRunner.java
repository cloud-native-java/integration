package processing;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class JobLaunchingCommandLineRunner
		implements CommandLineRunner {

	private Log log = LogFactory.getLog(getClass());

	private final JobLauncher launcher;
	private final Job job;
	private final Resource resource;

	@Autowired
	public JobLaunchingCommandLineRunner(JobLauncher launcher, Job job,
	                                     @Value("${file:data.csv}") Resource resource) {
		this.launcher = launcher;
		this.resource = resource;
		this.job = job;
	}

	@Override
	public void run(String... args) throws Exception {

		JobParameters jobParameters = new JobParametersBuilder()
				.addString("file", resource.getFile().getAbsolutePath())
				.toJobParameters();
		JobExecution jobExecution = launcher.run(job, jobParameters);
		log.error("jobExecution: " + jobExecution.toString());
	}

}

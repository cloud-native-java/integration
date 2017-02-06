package edabatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.integration.launch.JobLaunchRequest;
import org.springframework.batch.integration.launch.JobLaunchingGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.channel.MessageChannels;
import org.springframework.integration.dsl.file.Files;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Configuration
public class IntegrationConfiguration {

	private Log log = LogFactory.getLog(getClass());

	@Bean
	MessageChannel invalid() {
		return MessageChannels.direct().get();
	}

	@Bean
	MessageChannel completed() {
		return MessageChannels.direct().get();
	}

	// <1>
	@Bean
	IntegrationFlow etlFlow(
			@Value("${input-directory:${HOME}/Desktop/in}") File directory,
			JobLauncher launcher, Job job) {
		// @formatter:off
		return IntegrationFlows
				.from(Files.inboundAdapter(directory).autoCreateDirectory(true),
						consumer -> consumer.poller(poller -> poller
								.fixedRate(1000)))
				.handle(File.class,
						(file, headers) -> {

							// <2>
							JobParameters parameters = new JobParametersBuilder()
									.addParameter(
											"file",
											new JobParameter(file
													.getAbsolutePath()))
									.toJobParameters();

							return MessageBuilder
									.withPayload(
											new JobLaunchRequest(job,
													parameters))
									.setHeader(FileHeaders.ORIGINAL_FILE,
											file.getAbsolutePath())
									.copyHeadersIfAbsent(headers).build();
						})
				// <3>
				.handle(new JobLaunchingGateway(launcher))
				.routeToRecipients(
						spec -> spec.recipient(invalid(),
								ms -> !jobFinished(ms.getPayload()))
								.recipient(completed(),
										ms -> jobFinished(ms.getPayload())))
				.get();
		// @formatter:on
	}

	// <4>
	private boolean jobFinished(Object payload) {
		return JobExecution.class.cast(payload).getExitStatus()
				.equals(ExitStatus.COMPLETED);
	}

	@Bean
	IntegrationFlow finishedJobsFlow(
			@Value("${completed-directory:${HOME}/Desktop/completed}") File finished,
			JdbcTemplate jdbcTemplate) {
		// @formatter:off
		return IntegrationFlows
				.from(completed())
				.handle(JobExecution.class,
						(je, headers) -> {
							String ogFileName = String.class.cast(headers
									.get(FileHeaders.ORIGINAL_FILE));
							File file = new File(ogFileName);
							mv(file, finished);
							List<Contact> contacts = jdbcTemplate.query(
									"select * from CONTACT",
									(rs, i) -> new Contact(
											rs.getBoolean("valid_email"),
											rs.getString("full_name"),
											rs.getString("email"),
											rs.getLong("id")));
							contacts.forEach(log::info);
							return null;
						}).get();
		// @formatter:on
	}

	@Bean
	IntegrationFlow invalidFileFlow(
			@Value("${error-directory:${HOME}/Desktop/errors}") File errors) {
		// @formatter:off
		return IntegrationFlows
				.from(this.invalid())
				.handle(JobExecution.class,
						(je, headers) -> {
							String ogFileName = String.class.cast(headers
									.get(FileHeaders.ORIGINAL_FILE));
							File file = new File(ogFileName);
							mv(file, errors);
							return null;
						}).get();
		// @formatter:on
	}

	private void mv(File in, File out) {
		try {
			Assert.isTrue(out.exists() || out.mkdirs());
			File target = new File(out, in.getName());
			java.nio.file.Files.copy(in.toPath(), target.toPath(), REPLACE_EXISTING);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}

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
import org.springframework.util.StreamUtils;

import java.io.*;
import java.util.List;

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
	IntegrationFlow etlFlow(@Value("${input-directory:${HOME}/Desktop/in}") File directory,
	                        JobLauncher launcher,
	                        Job job) {
		// @formatter:off
		return IntegrationFlows
			.from(Files.inboundAdapter(directory)
					.autoCreateDirectory(true),
				consumer -> consumer.poller(
					poller -> poller.fixedRate(1000)))
			.handle(File.class, (file, headers) -> {

				// <2>
				JobParameters parameters = new JobParametersBuilder()
					.addParameter( "file",
						new JobParameter(file.getAbsolutePath()))
					.toJobParameters();

				return MessageBuilder.withPayload(
						new JobLaunchRequest(job, parameters))
					.setHeader(FileHeaders.ORIGINAL_FILE,
						file.getAbsolutePath())
					.copyHeadersIfAbsent(headers)
					.build();
			})
			// <3>
			.handle(new JobLaunchingGateway(launcher))
			.routeToRecipients(spec -> spec
				.recipient(invalid(),
					ms -> !jobFinished(ms.getPayload()))
				.recipient(completed(),
					ms -> jobFinished(ms.getPayload()))
			)
			.get();
		// @formatter:on
	}

	// <4>
	private boolean jobFinished(Object payload) {
		return JobExecution.class.cast(payload)
				.getExitStatus().equals(ExitStatus.COMPLETED);
	}

	@Bean
	IntegrationFlow finishedJobsFlow(
			@Value("${completed-directory:${HOME}/Desktop/completed}") File finished,
			JdbcTemplate template) {
		// @formatter:off
		return IntegrationFlows
			.from(completed())
			.handle(JobExecution.class, (je, headers) -> {
				String ogFileName = String.class.cast(
					headers.get(FileHeaders.ORIGINAL_FILE));
				File file = new File(ogFileName);
				mv(file, finished);
				List<Contact> contacts = template.query(
					"select * from CONTACT", (rs, i) -> new Contact(
							rs.getString("full_name"),
							rs.getString("email"),
							rs.getLong("id")
					));
				contacts.forEach(log::info);
				return null;
			})
			.get();
		// @formatter:on
	}

	@Bean
	IntegrationFlow invalidFileFlow(
			@Value("${error-directory:${HOME}/Desktop/errors}") File errors) {
		// @formatter:off
		return IntegrationFlows
			.from(this.invalid())
			.handle(JobExecution.class, (je, headers) -> {
				String ogFileName = String.class.cast(
						headers.get(FileHeaders.ORIGINAL_FILE));
				File file = new File(ogFileName);
				mv(file, errors);
				return null;
			})
			.get();
		// @formatter:on
	}

	private void mv(File in, File out) {
		// @formatter:off
		try {
			Assert.isTrue(out.exists() || out.mkdirs());
			try (InputStream inStream = new BufferedInputStream(
					  new FileInputStream(in));
			     OutputStream outStream = new BufferedOutputStream(
					  new FileOutputStream(out))) {
				StreamUtils.copy(inStream, outStream);
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		// @formatter:on
	}
}

package processing;

import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.integration.launch.JobLaunchRequest;
import org.springframework.batch.integration.launch.JobLaunchingGateway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.channel.MessageChannels;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

import java.io.*;
import java.util.List;

@Configuration
@IntegrationComponentScan
public class IntegrationConfiguration {

	@Bean
	MessageChannel invalidFiles() {
		return MessageChannels.direct().get();
	}

	@Bean
	MessageChannel finishedJobs() {
		return MessageChannels.direct().get();
	}

	@Bean
	MessageSource<File> fileMessageSource(@Value("${input-directory:${HOME}/Desktop/in}") File directory) {
		FileReadingMessageSource fileReadingMessageSource = new FileReadingMessageSource();
		fileReadingMessageSource.setDirectory(directory);
		return fileReadingMessageSource;
	}

	@Bean
	IntegrationFlow etlFlow(MessageSource<File> files, JobLauncher launcher, Job job) {

		return IntegrationFlows
				.from(files, consumerSpec -> consumerSpec.poller(pollerSpec -> pollerSpec.fixedRate(1000)))
				.handle(File.class, (file, headers) -> {
					JobParameters parameters = new JobParametersBuilder()
							.addParameter("file", new JobParameter(file.getAbsolutePath()))
							.toJobParameters();
					return MessageBuilder.withPayload(new JobLaunchRequest(job, parameters))
							.setHeader(FileHeaders.ORIGINAL_FILE, file.getAbsolutePath())
							.copyHeadersIfAbsent(headers)
							.build();
				})
				.handle(new JobLaunchingGateway(launcher))
				.routeToRecipients(spec -> spec
						.recipient(invalidFiles(),
								ms -> !JobExecution.class.cast(ms.getPayload()).getExitStatus().equals(ExitStatus.COMPLETED))
						.recipient(finishedJobs(),
								ms -> JobExecution.class.cast(ms.getPayload()).getExitStatus().equals(ExitStatus.COMPLETED))
				)
				.get();

	}

	@Bean
	IntegrationFlow finishedJobsFlow(JdbcTemplate template, @Value("${input-directory:${HOME}/Desktop/finished}") File finished) {
		return IntegrationFlows.from(finishedJobs())
				.handle(JobExecution.class, (je, headers) -> {
					String fileNameForOriginalFile = String.class.cast(
							headers.get(FileHeaders.ORIGINAL_FILE));
					File file = new File(fileNameForOriginalFile);
					mv(file, finished);
					List<Contact> contacts = template.query(
							"select * from CONTACT", (rs, i) -> new Contact(
									rs.getString("full_name"),
									rs.getString("email"),
									rs.getLong("id")
							));
					contacts.forEach(System.out::println);
					return null;
				})
				.get();
	}

	private void mv(File in, File out) {
		Assert.isTrue(out.exists() || out.mkdirs());
		File dest = new File(out, in.getName());
		try (BufferedInputStream bin = new BufferedInputStream(new FileInputStream(in));
		     BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(dest))) {
			byte[] buffer = new byte[1024];
			while (bin.read(buffer) != -1) {
				bout.write(buffer);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Bean
	IntegrationFlow invalidFileFlow(@Value("${input-directory:${HOME}/Desktop/errors}") File errors) {
		return IntegrationFlows.from(this.invalidFiles())
				.handle(JobExecution.class, (je, headers) -> {
					String fileNameForOriginalFile = String.class.cast(
							headers.get(FileHeaders.ORIGINAL_FILE));
					File file = new File(fileNameForOriginalFile);
					System.err.println("ERROR processing " + fileNameForOriginalFile + ". Relocating.");
					mv(file, errors);
					return null;
				})
				.get();
	}
}

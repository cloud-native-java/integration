package processing;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.sql.DataSource;
import java.util.Map;

@EnableBatchProcessing
@SpringBootApplication
public class BatchApplication {

	private Log log = LogFactory.getLog(getClass());

	public static void main(String[] args) {
		SpringApplication.run(BatchApplication.class, args);
	}

	@Bean
	JdbcTemplate jdbcTemplate(DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}

	@Bean
	RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Bean
	Job job(JobBuilderFactory jobBuilderFactory,
	        StepBuilderFactory stepBuilderFactory,
	        JdbcTemplate template,
	        ItemReader<Contact> fileReader,
	        ItemProcessor<Contact, Contact> emailProcessor,
	        ItemWriter<Contact> jdbcWriter) {

		Step setup = stepBuilderFactory
				.get("clean-contact-table")
				.tasklet((contribution, chunkContext) -> {
					template.update("delete from CONTACT");
					return RepeatStatus.FINISHED;
				})
				.build();

		Step fileToJdbc = stepBuilderFactory.get("file-to-jdbc-fileToJdbc")
				.<Contact, Contact>chunk(5)  // <1>
				.reader(fileReader)
				.processor(emailProcessor)
				.writer(jdbcWriter)
				.faultTolerant()
				.skip(InvalidEmailException.class)  // <2>
				.skipPolicy((Throwable t, int skipCount) -> {
					LogFactory.getLog(getClass()).info("skipping ");
					return t.getClass().isAssignableFrom(InvalidEmailException.class);
				})
				.retry(HttpStatusCodeException.class) // <3>
				.retryLimit(2)
				.build();

		Job job = jobBuilderFactory.get("etl")  // <4>
				.start(setup)
				.next(fileToJdbc)
				.build();

		return job;
	}

	// <5>
	@Bean
	@StepScope
	FlatFileItemReader<Contact> fileReader(
			@Value("file://#{jobParameters['file']}") Resource pathToFile,
			DefaultLineMapper<Contact> lm) {

		FlatFileItemReader<Contact> itemReader = new FlatFileItemReader<>();
		itemReader.setResource(pathToFile);
		itemReader.setLineMapper(lm);
		return itemReader;
	}


	public static class InvalidEmailException extends Exception {
		public InvalidEmailException(String email) {
			super(String.format("the email %s isn't valid", email));
		}
	}

	// <6>
	@Bean
	ItemProcessor<Contact, Contact> validatingProcessor(RestTemplate restTemplate,
	                                                    @Value("${emailvalidator.uri}") String uri,
	                                                    @Value("${mashape.key}") String mashapeKey) {
		return item -> {

			UriComponents emailValidatedUri = UriComponentsBuilder.fromHttpUrl(uri)
					.buildAndExpand(item.getEmail());

			RequestEntity<Void> requestEntity = RequestEntity.get(emailValidatedUri.toUri())
					.header("X-Mashape-Key", mashapeKey)
					.build();

			ParameterizedTypeReference<Map<String, Boolean>> ptr =
					new ParameterizedTypeReference<Map<String, Boolean>>() {
					};

			log.info("attempting to validate email " + item.getEmail());
			ResponseEntity<Map<String, Boolean>> responseEntity = restTemplate.exchange(requestEntity, ptr);

			boolean valid = responseEntity.getBody().get("isValid");
			log.info("email validated? " + valid);

			if (!valid) throw new InvalidEmailException(item.getEmail());

			item.setValidEmail(valid);
			return item;
		};
	}

	// <7>
	@Bean
	JdbcBatchItemWriter<Contact> jdbcWriter(DataSource dataSource) {

		JdbcBatchItemWriter<Contact> batchItemWriter = new JdbcBatchItemWriter<>();
		batchItemWriter.setItemSqlParameterSourceProvider(
				new BeanPropertyItemSqlParameterSourceProvider<>());
		batchItemWriter.setDataSource(dataSource);
		batchItemWriter.setSql("insert into CONTACT( full_name, email, valid_email ) values ( :fullName, :email, :validEmail )");
		return batchItemWriter;
	}

	// <8>
	@Bean
	DefaultLineMapper<Contact> lineMapper() {

		DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
		tokenizer.setNames("fullName,email".split(","));

		BeanWrapperFieldSetMapper<Contact> mapper = new BeanWrapperFieldSetMapper<>();
		mapper.setTargetType(Contact.class);

		DefaultLineMapper<Contact> defaultLineMapper = new DefaultLineMapper<>();
		defaultLineMapper.setFieldSetMapper(mapper);
		defaultLineMapper.setLineTokenizer(tokenizer);
		return defaultLineMapper;
	}
}
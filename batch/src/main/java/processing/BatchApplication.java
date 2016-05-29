package processing;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;

import javax.sql.DataSource;

@EnableBatchProcessing
@SpringBootApplication
public class BatchApplication {

	public static void main(String[] args) {
		SpringApplication.run(BatchApplication.class, args);
	}

	@Bean
	Job job(JobBuilderFactory jobBuilderFactory,
	        StepBuilderFactory stepBuilderFactory,
	        ItemReader<Contact> fileReader,
	        ItemWriter<Contact> jdbcWriter) {

		Step step = stepBuilderFactory.get("file-to-jdbc-step") // <1>
				.<Contact, Contact>chunk(5)
				.reader(fileReader)
				.writer(jdbcWriter)
				.build();

		Job job = jobBuilderFactory.get("etl")  // <2>
				.start(step)
				.build();

		return job;
	}

	// <3>
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

	// <4>
	@Bean
	JdbcBatchItemWriter<Contact> jdbcWriter(DataSource dataSource) {

		JdbcBatchItemWriter<Contact> batchItemWriter = new JdbcBatchItemWriter<>();
		batchItemWriter.setItemSqlParameterSourceProvider(
				new BeanPropertyItemSqlParameterSourceProvider<>());
		batchItemWriter.setDataSource(dataSource);
		batchItemWriter.setSql("insert into CONTACT( full_name, email) values ( :fullName, :email )");
		return batchItemWriter;
	}

	// <5>
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
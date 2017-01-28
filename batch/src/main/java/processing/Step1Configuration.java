package processing;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import processing.email.EmailValidationService;
import processing.email.InvalidEmailException;

import javax.sql.DataSource;


@Configuration
class Step1Configuration {

	private final Log log = LogFactory.getLog(getClass());

	@Bean
	@StepScope
	FlatFileItemReader<Person> fileReader(
			@Value("file://#{jobParameters['input']}") Resource in) throws Exception {

		return new FlatFileItemReaderBuilder<Person>()
				.name("file-reader")
				.resource(in)
				.saveState(false)
				.targetType(Person.class)
				.delimited().delimiter(",").names(new String[]{"firstName", "age", "email"})
				.build();
	}

	@Bean
	ItemProcessor<Person, Person> emailValidatingProcessor(
			EmailValidationService emailValidator) {
		return item -> {
			String email = item.getEmail();
			log.debug(String.format("validating %s", email));
			if (!emailValidator.isEmailValid(email)) {
				log.info(String.format("email %s is invalid", email));
				throw new InvalidEmailException(email);
			}
			return item;
		};
	}

	@Bean
	JdbcBatchItemWriter<Person> jdbcWriter(DataSource ds) {
		return new JdbcBatchItemWriterBuilder<Person>()
				.dataSource(ds)
				.sql("insert into PEOPLE( AGE, FIRST_NAME, EMAIL) values (:age, :firstName, :email)")
				.beanMapped()
				.build();
	}
}

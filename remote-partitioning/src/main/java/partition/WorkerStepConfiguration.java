package partition;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.support.MySqlPagingQueryProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.Collections;


@Configuration
class WorkerStepConfiguration {

	private Log log = LogFactory.getLog(getClass());

	@Value("${partition.chunk-size}")
	private int chunk;

	@Bean
	@StepScope
	JdbcPagingItemReader<Person> reader(DataSource dataSource,
	                                    @Value("#{stepExecutionContext['minValue']}") Long minValue,
	                                    @Value("#{stepExecutionContext['maxValue']}") Long maxValue) {

		log.info("reading " + minValue + " to " + maxValue);

		MySqlPagingQueryProvider queryProvider = new MySqlPagingQueryProvider();
		queryProvider.setSelectClause("id as id, email as email, age as age, first_name as firstName");
		queryProvider.setFromClause("from PEOPLE");
		queryProvider.setWhereClause("where id >= " + minValue + " and id <= " + maxValue);
		queryProvider.setSortKeys(Collections.singletonMap("id", Order.ASCENDING));

		JdbcPagingItemReader<Person> reader = new JdbcPagingItemReader<>();
		reader.setDataSource(dataSource);
		reader.setFetchSize(this.chunk);
		reader.setQueryProvider(queryProvider);
		reader.setRowMapper((rs, i) -> new Person(
				rs.getInt("id"),
				rs.getInt("age"),
				rs.getString("firstName"),
				rs.getString("email")));
		return reader;
	}

	@Bean
	@StepScope
	JdbcBatchItemWriter<Person> writer(DataSource dataSource) {
		JdbcBatchItemWriter<Person> writer = new JdbcBatchItemWriter<>();
		writer.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>());
		writer.setSql("INSERT INTO NEW_PEOPLE(id,age,first_name,email) VALUES(:id, :age, :firstName, :email )");
		writer.setDataSource(dataSource);
		return writer;
	}


	@Bean
	Step workerStep(StepBuilderFactory stepBuilderFactory) {
		return stepBuilderFactory
				.get("workerStep")
				.<Person, Person>chunk(this.chunk)
				.reader(reader(null, null, null))
				.writer(writer(null))
				.build();
	}

}

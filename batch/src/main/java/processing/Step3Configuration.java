package processing;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.Map;

@Configuration
class Step3Configuration {

	// <1>
	@Bean
	JdbcCursorItemReader<Map<Integer, Integer>> jdbcReader(DataSource dataSource) {
		return new JdbcCursorItemReaderBuilder<Map<Integer, Integer>>()
				.dataSource(dataSource)
				.name("jdbc-reader")
				.sql("select COUNT(age) c, age a from PEOPLE group by age")
				.rowMapper((rs, i) -> Collections.singletonMap(
						rs.getInt("a"), rs.getInt("c")))
				.build();
	}

	//<2>
	@Bean
	@StepScope
	FlatFileItemWriter<Map<Integer, Integer>> fileWriter(
			@Value("file://#{jobParameters['output']}") Resource out) {

		DelimitedLineAggregator<Map<Integer, Integer>> aggregator =
				new DelimitedLineAggregator<Map<Integer, Integer>>() {
					{
						// <3>
						setDelimiter(",");
						setFieldExtractor(ageAndCount -> {
							Map.Entry<Integer, Integer> next = ageAndCount.entrySet().iterator().next();
							return new Object[]{next.getKey(), next.getValue()};
						});
					}
				};

		return new FlatFileItemWriterBuilder<Map<Integer, Integer>>()
				.name("file-writer")
				.resource(out)
				.lineAggregator(aggregator)
				.build();
	}
}

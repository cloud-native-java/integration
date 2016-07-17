package partition;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.integration.partition.MessageChannelPartitionHandler;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.support.MySqlPagingQueryProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.Aggregator;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.Payloads;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.scheduling.support.PeriodicTrigger;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class PartitionConfiguration {

	public static final String STEP_1 = "step1";

	public static final String WORKER_STEP = "workerStep";

	private Log log = LogFactory.getLog(getClass());

	@Value("${partition.chunk-size}")
	private int chunk;

	@Value("${partition.grid-size:2}")
	private int gridSize;

	@Bean
	MessagingTemplate messagingTemplate(PartitionChannels master) {
		MessagingTemplate messagingTemplate = new MessagingTemplate(
				master.masterRequests());
		messagingTemplate.setReceiveTimeout(60 * 1000 * 60);
		return messagingTemplate;
	}

	@MessageEndpoint
	public static class ReplyAggregatingMessageEndpoint {

		@Autowired
		private MessageChannelPartitionHandler partitionHandler;

		@Aggregator(inputChannel = PartitionChannels.Partition.MASTER_REPLIES, outputChannel = PartitionChannels.Partition.MASTER_REPLIES_AGGREGATED, sendTimeout = "3600000", sendPartialResultsOnExpiry = "true")
		public List<?> aggregate(@Payloads List<?> messages) {
			return this.partitionHandler.aggregate(messages);
		}
	}

	@Bean
	MessageChannelPartitionHandler partitionHandler(
			MessagingTemplate messagingTemplate, JobExplorer jobExplorer,
			PartitionChannels master) throws Exception {
		MessageChannelPartitionHandler partitionHandler = new MessageChannelPartitionHandler();
		partitionHandler.setReplyChannel(master.masterRequestsAggregated());
		partitionHandler.setMessagingOperations(messagingTemplate);
		partitionHandler.setJobExplorer(jobExplorer);
		partitionHandler.setStepName(WORKER_STEP);
		partitionHandler.setPollInterval(5000L);
		partitionHandler.setGridSize(this.gridSize);
		return partitionHandler;
	}

	@Bean
	@StepScope
	JdbcPagingItemReader<Customer> pagingItemReader(DataSource dataSource,
			@Value("#{stepExecutionContext['minValue']}") Long minValue,
			@Value("#{stepExecutionContext['maxValue']}") Long maxValue) {

		log.info("reading " + minValue + " to " + maxValue);

		MySqlPagingQueryProvider queryProvider = new MySqlPagingQueryProvider();
		queryProvider.setSelectClause("id, firstName, lastName, birthdate");
		queryProvider.setFromClause("from CUSTOMER");
		queryProvider.setWhereClause("where id >= " + minValue + " and id <= "
				+ maxValue);
		queryProvider.setSortKeys(Collections.singletonMap("id",
				Order.ASCENDING));

		JdbcPagingItemReader<Customer> reader = new JdbcPagingItemReader<>();
		reader.setDataSource(dataSource);
		reader.setFetchSize(this.chunk);
		reader.setQueryProvider(queryProvider);
		reader.setRowMapper((rs, i) -> new Customer(rs.getLong("id"), rs
				.getString("firstName"), rs.getString("lastName"), rs
				.getDate("birthdate")));
		return reader;
	}

	@Bean
	@StepScope
	JdbcBatchItemWriter<Customer> customerItemWriter(DataSource dataSource) {
		JdbcBatchItemWriter<Customer> writer = new JdbcBatchItemWriter<>();
		writer.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>());
		writer.setSql("INSERT INTO NEW_CUSTOMER VALUES "
				+ " (:id, :firstName, :lastName, :birthdate)");
		writer.setDataSource(dataSource);
		return writer;
	}

	@Bean
	Partitioner columnRangePartitioner(JdbcOperations jdbcTemplate,
			@Value("${partition.table:CUSTOMER}") String table,
			@Value("${partition.column:ID}") String column) {
		return gridSize -> {

			Map<String, ExecutionContext> result = new HashMap<>();

			int min = jdbcTemplate.queryForObject("SELECT MIN(" + column
					+ ") from " + table, Integer.class);
			int max = jdbcTemplate.queryForObject("SELECT MAX(" + column
					+ ") from " + table, Integer.class);
			int targetSize = (max - min) / gridSize + 1;
			int number = 0;
			int start = min;
			int end = start + targetSize - 1;

			while (start <= max) {
				ExecutionContext value = new ExecutionContext();
				result.put("partition" + number, value);
				if (end >= max) {
					end = max;
				}
				value.putInt("minValue", start);
				value.putInt("maxValue", end);
				start += targetSize;
				end += targetSize;
				number++;
			}

			return result;
		};
	}

	@Bean(name = STEP_1)
	Step step1(StepBuilderFactory stepBuilderFactory, Partitioner partitioner,
			PartitionHandler partitionHandler, @Qualifier(WORKER_STEP) Step step)
			throws Exception {
		return stepBuilderFactory.get(STEP_1)
				.partitioner(step.getName(), partitioner).step(step)
				.partitionHandler(partitionHandler).build();
	}

	@Bean(name = WORKER_STEP)
	Step workerStep(StepBuilderFactory stepBuilderFactory) {
		return stepBuilderFactory.get(WORKER_STEP)
				.<Customer, Customer> chunk(this.chunk)
				.reader(pagingItemReader(null, null, null))
				.writer(customerItemWriter(null)).build();
	}

	@Bean(name = PollerMetadata.DEFAULT_POLLER)
	PollerMetadata defaultPoller() {
		PollerMetadata pollerMetadata = new PollerMetadata();
		pollerMetadata.setTrigger(new PeriodicTrigger(10));
		return pollerMetadata;
	}
}

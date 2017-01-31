package partition;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.integration.partition.MessageChannelPartitionHandler;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.Aggregator;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.Payloads;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.jdbc.core.JdbcOperations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
class PartitionStepConfiguration {

	@Value("${partition.grid-size:4}")
	private int gridSize;

	@Bean
	MessagingTemplate messagingTemplate(PartitionLeaderChannels channels) {
		MessagingTemplate messagingTemplate = new MessagingTemplate(
				channels.leaderRequestsChannel());
		messagingTemplate.setReceiveTimeout(60 * 1000 * 60);
		return messagingTemplate;
	}

	@Bean
	MessageChannelPartitionHandler partitionHandler(
			MessagingTemplate messagingTemplate,
			JobExplorer jobExplorer,
			PartitionLeaderChannels master) throws Exception {
		MessageChannelPartitionHandler partitionHandler = new MessageChannelPartitionHandler();
		partitionHandler.setReplyChannel(master.leaderRequestsAggregatedChannel());
		partitionHandler.setMessagingOperations(messagingTemplate);
		partitionHandler.setJobExplorer(jobExplorer);
		partitionHandler.setStepName("workerStep");
		partitionHandler.setPollInterval(5_000L);
		partitionHandler.setGridSize(this.gridSize);
		return partitionHandler;
	}

	@Bean
	Partitioner partitioner(JdbcOperations jdbcTemplate,
	                        @Value("${partition.table:PEOPLE}") String table,
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

	@Bean
	Step partitionStep(StepBuilderFactory sbf,
	                   Partitioner partitioner,
	                   PartitionHandler partitionHandler,
	                   WorkerStepConfiguration workerStepConfiguration) throws Exception {
		Step step = workerStepConfiguration.workerStep(null);
		return sbf.get("partitionStep")
				.partitioner(step.getName(), partitioner)
				.step(step)
				.partitionHandler(partitionHandler)
				.build();
	}

	@MessageEndpoint
	public static class ReplyAggregatingMessageEndpoint {

		@Autowired
		private MessageChannelPartitionHandler partitionHandler;

		@Aggregator(inputChannel = PartitionLeaderChannels.LEADER_REPLIES,
				outputChannel = PartitionLeaderChannels.LEADER_REPLIES_AGGREGATED,
				sendTimeout = "3600000", sendPartialResultsOnExpiry = "true")
		public List<?> aggregate(@Payloads List<?> messages) {
			return this.partitionHandler.aggregate(messages);
		}
	}
}

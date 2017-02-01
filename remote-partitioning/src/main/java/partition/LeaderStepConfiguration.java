package partition;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.integration.partition.MessageChannelPartitionHandler;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.Map;

@Configuration
class LeaderStepConfiguration {

	// <1>
	@Bean
	Step stagingStep(StepBuilderFactory sbf,
	                 JdbcTemplate jdbc) {
		return sbf
				.get("staging")
				.tasklet((contribution, chunkContext) -> {
					jdbc.execute("truncate NEW_PEOPLE");
					return RepeatStatus.FINISHED;
				})
				.build();
	}

	// <2>
	@Bean
	Step partitionStep(StepBuilderFactory sbf,
	                   Partitioner p,
	                   PartitionHandler ph,
	                   WorkerStepConfiguration wsc) {
		Step workerStep = wsc.workerStep(null);
		return sbf.get("partitionStep")
				.partitioner(workerStep.getName(), p)
				.partitionHandler(ph)
				.build();
	}

	// <3>
	@Bean
	MessageChannelPartitionHandler partitionHandler(
			@Value("${partition.grid-size:4}") int gridSize,
			MessagingTemplate messagingTemplate,
			JobExplorer jobExplorer) {
		MessageChannelPartitionHandler partitionHandler = new MessageChannelPartitionHandler();
		partitionHandler.setMessagingOperations(messagingTemplate);
		partitionHandler.setJobExplorer(jobExplorer);
		partitionHandler.setStepName("workerStep");
		partitionHandler.setGridSize(gridSize);
		return partitionHandler;
	}

	// <4>
	@Bean
	MessagingTemplate messagingTemplate(LeaderChannels channels) {
		return new MessagingTemplate(channels.leaderRequestsChannel());
	}

	// <5>
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
}

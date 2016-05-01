package partition;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.dsl.channel.MessageChannels;

@Configuration
@EnableBinding(PartitionChannels.Partition.class)
class PartitionChannels {

	@Autowired
	private Partition partition;

	@Bean(name = Partition.MASTER_REPLIES_AGGREGATED)
	QueueChannel masterRequestsAggregated() {
		return MessageChannels.queue().get();
	}

	DirectChannel masterRequests() {
		return partition.masterRequests();
	}

	DirectChannel masterReplies() {
		return partition.masterReplies();
	}


	public interface Partition {
		String MASTER_REPLIES = "masterReplies";
		String MASTER_REQUESTS = "masterRequests";
		String MASTER_REPLIES_AGGREGATED = "masterRepliesAggregated";

		@Output(MASTER_REQUESTS)
		DirectChannel masterRequests();

		@Input(MASTER_REPLIES)
		DirectChannel masterReplies();
	}
}

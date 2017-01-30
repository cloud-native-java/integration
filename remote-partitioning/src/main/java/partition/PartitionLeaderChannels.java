package partition;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.dsl.channel.MessageChannels;
import org.springframework.messaging.MessageChannel;

@Configuration
@EnableBinding(PartitionLeaderChannels.PartitionLeader.class)
class PartitionLeaderChannels {

	private final PartitionLeader partitionLeader;

	@Autowired
	public PartitionLeaderChannels(PartitionLeader partitionLeader) {
		this.partitionLeader = partitionLeader;
	}

	@Bean(name = PartitionLeader.MASTER_REPLIES_AGGREGATED)
	public QueueChannel masterRequestsAggregatedChannel() {
		return MessageChannels.queue().get();
	}

	public MessageChannel masterRequestsChannel() {
		return partitionLeader.masterRequests();
	}

	public MessageChannel masterRepliesChannel() {
		return partitionLeader.masterReplies();
	}

	public interface PartitionLeader {
		String MASTER_REPLIES = "masterReplies";
		String MASTER_REQUESTS = "masterRequests";
		String MASTER_REPLIES_AGGREGATED = "masterRepliesAggregated";

		@Output(MASTER_REQUESTS)
		MessageChannel masterRequests();

		@Input(MASTER_REPLIES)
		MessageChannel masterReplies();
	}
}

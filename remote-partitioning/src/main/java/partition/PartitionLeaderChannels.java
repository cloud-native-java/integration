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

	public static final String LEADER_REPLIES = "leaderReplies";
	public static final String LEADER_REQUESTS = "leaderRequests";
	public static final String LEADER_REPLIES_AGGREGATED = "leaderRepliesAggregated";

	private final PartitionLeader partitionLeader;

	@Autowired
	public PartitionLeaderChannels(PartitionLeader partitionLeader) {
		this.partitionLeader = partitionLeader;
	}

	@Bean(name = PartitionLeaderChannels.LEADER_REPLIES_AGGREGATED)
	public QueueChannel leaderRequestsAggregatedChannel() {
		return MessageChannels.queue().get();
	}

	public MessageChannel leaderRequestsChannel() {
		return partitionLeader.leaderRequests();
	}

	public MessageChannel leaderRepliesChannel() {
		return partitionLeader.leaderReplies();
	}

	public interface PartitionLeader {

		@Output(PartitionLeaderChannels.LEADER_REQUESTS)
		MessageChannel leaderRequests();

		@Input(PartitionLeaderChannels.LEADER_REPLIES)
		MessageChannel leaderReplies();
	}
}

package partition.workers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.channel.DirectChannel;


@Configuration
@EnableBinding(PartitionWorkerChannels.PartitionWorker.class)
@Profile(PartitionWorkerConfiguration.WORKER_PROFILE)
class PartitionWorkerChannels {

	@Autowired
	private PartitionWorker channels;

	DirectChannel workerRequests() {
		return channels.workerRequests();
	}

	DirectChannel workerReplies() {
		return channels.workerReplies();
	}

	public interface PartitionWorker {

		String WORKER_REQUESTS = "workerRequests";

		String WORKER_REPLIES = "workerReplies";

		@Input(WORKER_REQUESTS)
		DirectChannel workerRequests();
		@Output(WORKER_REPLIES)
		DirectChannel workerReplies();

	}
}

package partition.workers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.MessageChannel;
import partition.Profiles;

@Configuration
@EnableBinding(PartitionWorkerChannels.PartitionWorker.class)
@Profile( Profiles.WORKER_PROFILE)
class PartitionWorkerChannels {

	@Autowired
	private PartitionWorker channels;

	MessageChannel workerRequests() {
		return channels.workerRequests();
	}

	MessageChannel workerReplies() {
		return channels.workerReplies();
	}

	public interface PartitionWorker {

		String WORKER_REQUESTS = "workerRequests";

		String WORKER_REPLIES = "workerReplies";

		@Input(WORKER_REQUESTS)
		MessageChannel workerRequests();

		@Output(WORKER_REPLIES)
		MessageChannel workerReplies();

	}
}

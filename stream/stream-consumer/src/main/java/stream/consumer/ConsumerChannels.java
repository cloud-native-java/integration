package stream.consumer;


import org.springframework.cloud.stream.annotation.Input;
import org.springframework.messaging.SubscribableChannel;

public interface ConsumerChannels {

	String DIRECTED = "directed";
	String BROADCASTS = "broadcasts";

	@Input(DIRECTED)
	SubscribableChannel directed();

	@Input(BROADCASTS)
	SubscribableChannel broadcasts();

}

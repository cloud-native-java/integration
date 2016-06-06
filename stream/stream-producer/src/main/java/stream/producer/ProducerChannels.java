package stream.producer;

import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;

// <1>
public interface ProducerChannels {

	String BROADCAST = "broadcastGreetings";

	String DIRECT = "directGreetings";

	@Output(DIRECT)
	MessageChannel directGreetings();

	@Output(BROADCAST)
	MessageChannel broadcastGreetings();
}


package processing;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.dsl.channel.MessageChannels;
import org.springframework.messaging.MessageChannel;

@Configuration
@IntegrationComponentScan
public class IntegrationConfiguration {

	@Bean
	MessageChannel invalidFiles() {
		return MessageChannels.direct().get();
	}


}

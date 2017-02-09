package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@SpringBootApplication
public class DemoApplication {

	@Configuration
	@Profile(Profiles.LEADER)
	@EnableBinding(LeaderChannels.class)
	public static class Leader {
	}

	@Configuration
	@Profile(Profiles.WORKER)
	@EnableBinding(WorkerChannels.class)
	public static class Worker {
	}

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}
}



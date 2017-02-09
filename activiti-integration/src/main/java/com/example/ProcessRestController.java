package com.example;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@Profile(Profiles.LEADER)
@RestController
class ProcessRestController {

	private final LeaderChannels leaderChannels;
	private final ProcessEngine processEngine;

	@Autowired
	ProcessRestController(
			LeaderChannels leaderChannels,
			ProcessEngine processEngine) {
		this.leaderChannels = leaderChannels;
		this.processEngine = processEngine;
	}

	@GetMapping("/start")
	Map<String, String> launch() {
		ProcessInstance asyncProcess = this.processEngine.getRuntimeService()
				.startProcessInstanceByKey("asyncProcess");
		return Collections.singletonMap("executionId", asyncProcess.getId());
	}

	@GetMapping("/resume/{executionId}")
	void resume(@PathVariable String executionId) {
		Message<String> build =
				MessageBuilder
						.withPayload(executionId)
						.setHeader("executionId", executionId).build();
		this.leaderChannels.leaderReplies().send(build);
	}
}

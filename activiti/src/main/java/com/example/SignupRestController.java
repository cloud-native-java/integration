package com.example;


import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.task.TaskInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/customers/{customerId}/signup")
class SignupRestController {

	public static final String CUSTOMER_ID_PV_KEY = "customerId";

	private final RuntimeService runtimeService;
	private final TaskService taskService;

	private Log log = LogFactory.getLog(getClass());

	@Autowired
	public SignupRestController(RuntimeService runtimeService, TaskService taskService) {
		this.runtimeService = runtimeService;
		this.taskService = taskService;
	}

	@RequestMapping(method = RequestMethod.POST)
	public ResponseEntity<?> startProcess(@PathVariable String customerId) {
		String processInstanceId = this.runtimeService.startProcessInstanceByKey("signup",
				Collections.singletonMap(CUSTOMER_ID_PV_KEY, customerId)).getId();
		this.log.info("starting processInstance ID " + processInstanceId);
		this.taskService.createTaskQuery()
				.active()
				.taskName("sign-up")
				.includeProcessVariables()
				.processVariableValueEquals(CUSTOMER_ID_PV_KEY, customerId)
				.list()
				.forEach(t -> {
					log.info(t.toString());
					taskService.complete(t.getId());
				});
		log.info("finished sign-up");
		return ResponseEntity.ok(processInstanceId);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/errors")
	public List<String> readErrors(@PathVariable String customerId) {
		return this.taskService.createTaskQuery()
				.active()
				.taskName("fix-errors")
				.includeProcessVariables()
				.processVariableValueEquals(CUSTOMER_ID_PV_KEY, customerId)
				.list()
				.stream()
				.map(TaskInfo::getId)
				.collect(Collectors.toList());
	}

	@RequestMapping(method = RequestMethod.POST, value = "/errors/{taskId}")
	public void fixErrors(@PathVariable String customerId,
	                      @PathVariable String taskId) {
		this.taskService.createTaskQuery()
				.active()
				.taskId(taskId)
				.includeProcessVariables()
				.processVariableValueEquals(CUSTOMER_ID_PV_KEY, customerId)
				.list()
				.forEach(t -> taskService.complete(t.getId(), Collections.singletonMap("formOK", true)));
	}

	@RequestMapping(method = RequestMethod.POST, value = "/confirmation")
	public void confirm(@PathVariable String customerId) {
		this.taskService.createTaskQuery()
				.active()
				.taskName("confirm-email")
				.includeProcessVariables()
				.processVariableValueEquals(CUSTOMER_ID_PV_KEY, customerId)
				.list()
				.forEach(t -> {
					log.info(t.toString());
					taskService.complete(t.getId());
				});
		this.log.info("confirmed email receipt for " + customerId);
	}
}
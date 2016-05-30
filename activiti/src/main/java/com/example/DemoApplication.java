package com.example;

import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.impl.pvm.delegate.ActivityExecution;
import org.activiti.engine.runtime.ProcessInstance;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}
}


@Component
class CheckForm {

	private Log log = LogFactory.getLog(getClass());

	@Autowired
	private RuntimeService runtimeService;

	public void execute(ActivityExecution activityExecution) throws Exception {
		this.log.info("in " + getClass().getName() +
				", customerId = " + activityExecution.getVariable("customerId"));

		String formOK = "formOK";
		Object variable = activityExecution.getVariable(formOK);

		Map<String, Object> vars = Collections.singletonMap(formOK,  null == variable ? false : true );

		this.runtimeService.setVariables(activityExecution.getId(), vars);

		this.runtimeService.getVariables(activityExecution.getId()).forEach((k, v) -> log.info('\t' + k + '=' + v));

	}

}

@Component
class SendConfirmationEmail {

	private Log log = LogFactory.getLog(getClass());

	public void execute(ActivityExecution execution) throws Exception {
		this.log.info("in " + getClass().getName() + ", customerId = "
				+ execution.getVariable("customerId"));
	}
}

@Component
class SignupProcessCommandLineRunner implements CommandLineRunner {

	public static String CUSTOMER_ID_PV_KEY = "customerId";

	private Log log = LogFactory.getLog(getClass());

	@Autowired
	private RuntimeService runtimeService;

	@Autowired
	private TaskService taskService;

	@Override
	public void run(String... args) throws Exception {

		String customerId = Long.toString(223232);

		Map<String, Object> params = Collections.singletonMap(CUSTOMER_ID_PV_KEY, customerId);

		// start a process
		ProcessInstance processInstance = this.runtimeService.startProcessInstanceByKey("signup", params);
		this.log.info("starting processInstance ID " + processInstance.getId());

		// find available work for a given user
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
		String processInstanceId = processInstance.getProcessInstanceId();
		this.runtimeService.getVariables(processInstanceId).forEach((k, v) -> log.info('\t' + k + '=' + v));
		this.runtimeService.getActiveActivityIds(processInstanceId)
				.forEach(activityId -> this.log.info("activityId=" + activityId));

		// do we need to fix it?
		this.taskService.createTaskQuery()
				.active()
				.taskName("fix-errors")
				.includeProcessVariables()
				.processVariableValueEquals(CUSTOMER_ID_PV_KEY, customerId)
				.list()
				.forEach(t -> {
					log.info(t.toString());
					taskService.complete(t.getId(), Collections.singletonMap("formOK", true));
				});

		// confirm user email
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

		this.log.info("end");

	}
}
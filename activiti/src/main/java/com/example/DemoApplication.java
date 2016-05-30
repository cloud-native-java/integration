package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}
}



/*

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
}*/

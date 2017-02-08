package com.example;

import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.task.TaskInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/customers")
class SignupRestController {

	public static final String CUSTOMER_ID_PV_KEY = "customerId";

	private final RuntimeService runtimeService;
	private final TaskService taskService;
	private final CustomerRepository customerRepository;
	private Log log = LogFactory.getLog(getClass());

	// <1>
	@Autowired
	public SignupRestController(RuntimeService runtimeService,
	                            TaskService taskService, CustomerRepository repository) {
		this.runtimeService = runtimeService;
		this.taskService = taskService;
		this.customerRepository = repository;
	}

	// <2>
	@PostMapping
	public ResponseEntity<?> startProcess(@RequestBody Customer customer) {
		Assert.notNull(customer);
		Customer save = this.customerRepository.save(new Customer(customer
				.getFirstName(), customer.getLastName(), customer.getEmail()));

		String processInstanceId = this.runtimeService
				.startProcessInstanceByKey(
						"signup",
						Collections.singletonMap(CUSTOMER_ID_PV_KEY,
								Long.toString(save.getId()))).getId();
		this.log.info("started sign-up. the processInstance ID is "
				+ processInstanceId);

		return ResponseEntity.ok(save.getId());
	}

	// <3>
	@GetMapping("/{customerId}/signup/errors")
	public List<String> readErrors(@PathVariable String customerId) {
		// @formatter:off
		return this.taskService
				.createTaskQuery()
					.active()
					.taskName("fix-errors")
					.includeProcessVariables()
					.processVariableValueEquals(CUSTOMER_ID_PV_KEY, customerId)
				.list()
					.stream()
					.map(TaskInfo::getId)
				.collect(Collectors.toList());
		// @formatter:on
	}

	// <4>
	@PostMapping("/{customerId}/signup/errors/{taskId}")
	public void fixErrors(@PathVariable String customerId,
	                      @PathVariable String taskId, @RequestBody Customer fixedCustomer) {

		Customer customer = this.customerRepository.findOne(Long
				.parseLong(customerId));
		customer.setEmail(fixedCustomer.getEmail());
		customer.setFirstName(fixedCustomer.getFirstName());
		customer.setLastName(fixedCustomer.getLastName());
		this.customerRepository.save(customer);

		this.taskService
				.createTaskQuery()
				.active()
				.taskId(taskId)
				.includeProcessVariables()
				.processVariableValueEquals(CUSTOMER_ID_PV_KEY, customerId)
				.list()
				.forEach(
						t -> {
							log.info("fixing customer# " + customerId
									+ " for taskId " + taskId);
							taskService.complete(t.getId(),
									Collections.singletonMap("formOK", true));
						});
	}

	// <5>
	@PostMapping("/{customerId}/signup/confirmation")
	public void confirm(@PathVariable String customerId) {
		this.taskService
				.createTaskQuery()
				.active()
				.taskName("confirm-email")
				.includeProcessVariables()
				.processVariableValueEquals(CUSTOMER_ID_PV_KEY, customerId)
				.list().forEach(t -> {
			log.info(t.toString());
			taskService.complete(t.getId());
		});
		this.log.info("confirmed email receipt for " + customerId);
	}
}
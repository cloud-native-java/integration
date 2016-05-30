package com.example;


import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.pvm.delegate.ActivityExecution;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Component
class CheckForm {

	private final RuntimeService runtimeService;
	private final CustomerRepository customerRepository;
	private Log log = LogFactory.getLog(getClass());

	@Autowired
	public CheckForm(RuntimeService runtimeService, CustomerRepository customerRepository) {
		this.runtimeService = runtimeService;
		this.customerRepository = customerRepository;
	}

	public void execute(ActivityExecution activityExecution) throws Exception {
		Long customerId = Long.parseLong(
				activityExecution.getVariable("customerId", String.class));

		this.log.info("in " + getClass().getName() + ", customerId = " + customerId);

		Customer customer = this.customerRepository.findOne(customerId);

		String formOK = "formOK";
		Map<String, Object> vars = Collections.singletonMap(formOK,
				this.validate(customer));
		this.runtimeService.setVariables(activityExecution.getId(), vars);
	}

	protected boolean validate(Customer customer) {
		return StringUtils.defaultString(customer.getEmail()).contains("@");
	}
}
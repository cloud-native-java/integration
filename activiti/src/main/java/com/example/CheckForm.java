package com.example;

import com.example.email.EmailValidationService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.pvm.delegate.ActivityExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isEmpty;

@Service
class CheckForm {

	private final RuntimeService runtimeService; // <1>
	private final CustomerRepository customerRepository;
	private final EmailValidationService emailValidationService;

	@Autowired
	public CheckForm(EmailValidationService emailValidationService,
			RuntimeService runtimeService, CustomerRepository customerRepository) {
		this.runtimeService = runtimeService;
		this.customerRepository = customerRepository;
		this.emailValidationService = emailValidationService;
	}

	// <2>
	public void execute(ActivityExecution e) throws Exception {
		Long customerId = Long.parseLong(e.getVariable("customerId",
				String.class));
		Map<String, Object> vars = Collections.singletonMap("formOK",
				validated(this.customerRepository.findOne(customerId)));
		this.runtimeService.setVariables(e.getId(), vars); // <3>
	}

	private boolean validated(Customer customer) {
		return !isEmpty(customer.getFirstName())
				&& !isEmpty(customer.getLastName())
				&& this.emailValidationService
						.isEmailValid(customer.getEmail());
	}

}
package com.example;

import org.activiti.engine.impl.pvm.delegate.ActivityExecution;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

@Service
class SendConfirmationEmail {

	private Log log = LogFactory.getLog(getClass());

	public void execute(ActivityExecution execution) throws Exception {
		this.log.info("in " + getClass().getName() + ", customerId = "
				+ execution.getVariable("customerId"));
		// exercise to reader: send an email, perhaps
		// using Sendgrid?
	}
}

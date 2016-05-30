package com.example;

import org.activiti.engine.impl.pvm.delegate.ActivityExecution;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
class SendConfirmationEmail {

	private Log log = LogFactory.getLog(getClass());

	public void execute(ActivityExecution execution) throws Exception {
		this.log.info("in " + getClass().getName() + ", customerId = "
				+ execution.getVariable("customerId"));

//		String uuid = UUID.randomUUID().toString();
//		execution.setVariable("confirmationID", uuid);
	}
}

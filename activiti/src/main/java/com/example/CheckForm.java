package com.example;


import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.pvm.delegate.ActivityExecution;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Component
class CheckForm {

	private final RuntimeService runtimeService;

	private Log log = LogFactory.getLog(getClass());

	@Autowired
	public CheckForm(RuntimeService runtimeService) {
		this.runtimeService = runtimeService;
	}

	public void execute(ActivityExecution activityExecution) throws Exception {
		this.log.info("in " + getClass().getName() + ", customerId = " + activityExecution.getVariable("customerId"));
		String formOK = "formOK";
		Object variable = activityExecution.getVariable(formOK);
		Map<String, Object> vars = Collections.singletonMap(formOK, null == variable ? false : true);
		this.runtimeService.setVariables(activityExecution.getId(), vars);
	}

}
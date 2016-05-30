package com.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.engine.RuntimeService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@SpringApplicationConfiguration(classes = DemoApplication.class)
public class DemoApplicationTests {

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext wac;

	@Before
	public void before() throws Exception {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
	}

	private Log log = LogFactory.getLog(getClass());

	@Test
	public void signupFlow() throws Exception {

		// customers/{customerId}/signup
		// first let's start the signup process

		String customerId = Long.toString(123);

		String rootUrl = "/customers/" + customerId + "/signup";

		// start signup
		this.mockMvc.perform(post(rootUrl))
				.andExpect(status().isOk())
				.andExpect(mvcResult -> {
					String contentAsString = mvcResult.getResponse().getContentAsString();
					Long processInstanceId = Long.parseLong(contentAsString);
					assertNotNull(processInstanceId);
					assertTrue(processInstanceId > 0);
					log.info("processInstanceId = " + processInstanceId);
				});

		// see if there are any errors to be corrected
		String contentAsString = this.mockMvc
				.perform(get(rootUrl + "/errors"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		ObjectMapper mapper = new ObjectMapper();
		TypeReference<List<Long>> typeReference = new TypeReference<List<Long>>() {
		};
		List<Long> errantSignupFixTaskIds = mapper.readerFor(typeReference).readValue(contentAsString);
		log.info("errant signups:  " + errantSignupFixTaskIds.toString());

		// if necessary, fix them
		errantSignupFixTaskIds.forEach(
				taskId -> {
					try {
						this.mockMvc.perform(post(rootUrl + "/errors/" + taskId))
								.andExpect(status().isOk());

					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
		);

		this.mockMvc.perform(post(rootUrl + "/confirmation"))
				.andExpect(status().isOk());


	}
}

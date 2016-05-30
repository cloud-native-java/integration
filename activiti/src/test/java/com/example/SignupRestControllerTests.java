package com.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aopalliance.intercept.MethodInterceptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@SpringApplicationConfiguration(classes = DemoApplication.class)
public class SignupRestControllerTests {

	private MockMvc mockMvc;

	private ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	private WebApplicationContext wac;

	@Before
	public void before() throws Exception {
		this.repository.deleteAll();
		counter.set(0);
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
	}

	@Autowired
	private CustomerRepository repository;
	private static Log log = LogFactory.getLog(SignupRestController.class);

	String jsonForCustomer(Customer customer) throws Exception {
		return this.objectMapper.writerFor(Customer.class)
				.writeValueAsString(customer);
	}

	protected void doTestSignup(Customer input) throws Exception {
		// customers/{customerId}/signup
		// first let's start the signup process

		String email = input.getEmail();

		String drJson = jsonForCustomer(input);

		String rootUrl = "/customers"; // + customerId + "/signup";

		// start signup
		this.mockMvc.perform(post(rootUrl).content(drJson).contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(mvcResult -> {
					String contentAsString = mvcResult.getResponse().getContentAsString();
					Long customerId = Long.parseLong(contentAsString);
					assertNotNull(customerId);
					assertTrue(customerId > 0);
				});

		Customer customer = this.repository.findByEmail(email).orElseThrow(
				() -> new AssertionError("no record stored in the database for email '" + email + "'"));

		String customerId = Long.toString(customer.getId());


		// see if there are any errors to be corrected
		String contentAsString = this.mockMvc
				.perform(get(rootUrl + "/" + customerId + "/signup/errors"))
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
						customer.setEmail("valid@email.com");
						this.mockMvc.perform(post(rootUrl + "/" + customerId + "/signup/errors/" + taskId)
								.contentType(MediaType.APPLICATION_JSON)
								.content(jsonForCustomer(customer)))
								.andExpect(status().isOk());

					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
		);

		// confirm receipt of email
		this.mockMvc.perform(post(rootUrl + "/" + customerId + "/signup/confirmation"))
				.andExpect(status().isOk());
	}

	@Test
	public void signupFlowHappyPath() throws Exception {
		this.doTestSignup(new Customer("Dave", "Syer", "dsyer@email.com"));
		int i = counter.get();
		assertEquals(i, 1);
		log.info("signupFlowHappyPath: " + i);
	}

	@Test
	public void signupFlowSadPath() throws Exception {
		this.doTestSignup(new Customer("Phil", "Webb", "pwebb"));
		int i = counter.get();
		assertEquals(i, 2);
		log.info("signupFlowSadPath: " + i);
	}

	@Component
	public static class CheckFormBPP implements BeanPostProcessor {

		@Override
		public Object postProcessBeforeInitialization(Object o, String s) throws BeansException {
			return o;
		}

		@Override
		public Object postProcessAfterInitialization(Object o, String s) throws BeansException {
			LogFactory.getLog(getClass()).info("post processing " + o.getClass());
			if (o.getClass().isAssignableFrom(CheckForm.class)) {
				return this.counting(CheckForm.class.cast(o));
			}
			return o;
		}

		private Object counting(CheckForm cf) {
			log.info("creating a counting proxy for " + cf.getClass());
			ProxyFactoryBean pfb = new ProxyFactoryBean();
			pfb.addAdvice((MethodInterceptor) invocation -> {
				if (invocation.getMethod().getName().equals("execute")) {
					counter.incrementAndGet();
				}
				return invocation.proceed();
			});
			pfb.setTarget(cf);
			return pfb.getObject();
		}
	}

	private static final AtomicInteger counter = new AtomicInteger();
}

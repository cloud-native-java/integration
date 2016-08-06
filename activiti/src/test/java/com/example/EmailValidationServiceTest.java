package com.example;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@SpringApplicationConfiguration(classes = DemoApplication.class)
public class EmailValidationServiceTest {

	@Test
	public void testFeignPoweredClient() throws Exception {

		Assert.assertTrue(this.emailValidationService
				.isEmailValid("george@email.com"));
		Assert.assertFalse(this.emailValidationService.isEmailValid("george"));
	}

	@Autowired
	private EmailValidationService emailValidationService;

}
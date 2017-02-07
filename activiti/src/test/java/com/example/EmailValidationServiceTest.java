package com.example;

import com.example.email.EmailValidationService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = DemoApplication.class)
public class EmailValidationServiceTest {

	@Autowired
	private EmailValidationService emailValidationService;

	@Test
	public void testEmailValidation() throws Exception {
		Assert.assertTrue(this.emailValidationService
				.isEmailValid("george@email.com"));
		Assert.assertFalse(this.emailValidationService.isEmailValid("george"));
	}
}
package demo;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.TimeoutRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ClientConfiguration.class)
public class ActivitiIntegrationTest {

	private final RestTemplate restTemplate = new RestTemplateBuilder()
			.basicAuthorization("operator", "operator")
			.build();

	private final RetryTemplate retryTemplate = new RetryTemplate();

	@Autowired
	private CloudFoundryHelper helper;

	@Before
	public void before() throws Throwable {
		this.retryTemplate.setBackOffPolicy(new ExponentialBackOffPolicy());
		this.retryTemplate.setRetryPolicy(new TimeoutRetryPolicy());
	}

	@Test
	public void testDistributedWorkflows() throws Throwable {

		boolean endTimeExists =
				this.helper.uriFor("activiti-leader").map(al -> {

					ResponseEntity<Map<String, String>> entity =
							this.restTemplate.exchange(al + "/start",
									HttpMethod.GET,
									null,
									new ParameterizedTypeReference<Map<String, String>>() {
									});

					Assert.assertEquals(entity.getStatusCode(), (HttpStatus.OK));

					String processInstanceId = entity.getBody().get("processInstanceId");

					try {
						RetryCallback<Boolean, RuntimeException> rt =
								new RetryCallback<Boolean, RuntimeException>() {
									@Override
									public Boolean doWithRetry(RetryContext retryContext) throws RuntimeException {
										String url = al + "/history/historic-process-instances/" + processInstanceId;
										Map<String, Object> instanceInformation =
												restTemplate.exchange(url, HttpMethod.GET, null,
														new ParameterizedTypeReference<Map<String, Object>>() {
														}).getBody();

										if (instanceInformation.get("endTime") != null) {
											return true;
										}
										throw new RuntimeException("the endTime attribute was null");
									}
								};
						return retryTemplate.execute(rt, retryContext -> false);

					} catch (Throwable throwable) {
						throw new RuntimeException(throwable);
					}
				})
						.orElse(false);
		Assert.assertTrue("the end time is nigh (or, at least, it should be)!", endTimeExists);
	}
}
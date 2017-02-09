package demo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
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

	private Log log = LogFactory.getLog(getClass());

	@Before
	public void before() throws Throwable {
		this.retryTemplate.setBackOffPolicy(new ExponentialBackOffPolicy());
		this.retryTemplate.setRetryPolicy(new SimpleRetryPolicy(20,
				Collections.singletonMap(RuntimeException.class, true)));
	}

	@Test
	public void testDistributedWorkflows() throws Throwable {

		boolean endTimeExists =
				this.helper.uriFor("activiti-leader").map(al -> {


					RetryCallback<String, RuntimeException> pidRetryCallback =
							new RetryCallback<String, RuntimeException>() {
								@Override
								public String doWithRetry(RetryContext retryContext) throws RuntimeException {

									String url = al + "/start";
									log.info("calling " + url + ".");

									ResponseEntity<Map<String, String>> entity =
											restTemplate.exchange(url,
													HttpMethod.GET,
													null,
													new ParameterizedTypeReference<Map<String, String>>() {
													});

									Assert.assertEquals(entity.getStatusCode(), HttpStatus.OK);
									return entity.getBody().get("processInstanceId");
								}
							};

					String processInstanceId = retryTemplate.execute(pidRetryCallback);

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
											log.info("endTime was not null..");
											return true;
										}
										log.info("endTime was null..");
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
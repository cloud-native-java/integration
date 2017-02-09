package demo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ClientConfiguration.class)
public class ActivitiIntegrationTest {

	@Autowired
	private CloudFoundryHelper helper;

	private final RestTemplate restTemplate = new RestTemplateBuilder()
			.basicAuthorization("operator", "operator")
			.build();

	private Log log = LogFactory.getLog(getClass());

	@Test
	public void testDistributedWorkflows() throws Throwable {

		boolean endTimeExists = this.helper.uriFor("activiti-leader")
				.map(al -> {

					ResponseEntity<Map<String, String>> entity =
							this.restTemplate.exchange(al + "/start", HttpMethod.GET, null,
									new ParameterizedTypeReference<Map<String, String>>() {
									});

					Assert.assertEquals(entity.getStatusCode(), (HttpStatus.OK));
					String processInstanceId = entity.getBody().get("processInstanceId");

					int attempts = 0;
					log.info("processInstanceId: " + processInstanceId);
					boolean valid = false;
					while (attempts++ < 30) {
						try {
							log.info("waiting 1 second.");
							Thread.sleep(1000);
							String url = al + "/history/historic-process-instances/" + processInstanceId;
							Map<String, Object> instanceInformation =
									this.restTemplate
											.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<Map<String, Object>>() {
											})
											.getBody();

							if (instanceInformation.get("endTime") != null) {
								valid = true;
								break;
							}

						} catch (InterruptedException e) {
							throw new RuntimeException(e);
						}
					}
					return valid;
				})
				.orElse(false);

		Assert.assertTrue("the end time is nigh (or, at least, it should be)!",
				endTimeExists);
	}
}

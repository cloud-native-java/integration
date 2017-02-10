package integration;

import cnj.CloudFoundryService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.springframework.http.HttpMethod.GET;

/**
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = RemotePartitioningIT.Config.class)
public class RemotePartitioningIT {

	@SpringBootApplication
	public static class Config {
	}

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private CloudFoundryService cloudFoundryService;

	@Before
	public void before () throws Throwable {

	}

	@Test
	public void partitionedJob() {

		String leaderUrl = cloudFoundryService.urlForApplication("partition-leader");

		String migrationJobUrl = leaderUrl + "/migrate";
		ResponseEntity<Map<String, String>> jsonResponse =
				this.restTemplate.exchange(
						migrationJobUrl, GET, null,
						new ParameterizedTypeReference<Map<String, String>>() {
						});
		assertTrue(jsonResponse.getBody().get("exitCode")
				.equalsIgnoreCase("completed"));
		assertTrue(jsonResponse.getBody().get("running")
				.equalsIgnoreCase("false"));
		assertTrue("the job should have completed successfully.",
				jsonResponse.getStatusCode().is2xxSuccessful());
		Map<String, Number> status =
				this.restTemplate
						.exchange(leaderUrl + "/status", GET, null,
								new ParameterizedTypeReference<Map<String, Number>>() {
								})
						.getBody();

		Assert.assertEquals("there should be an identical number" +
						" of records in the source and destination table",
				status.get("people.count"), status.get("new_people.count"));
	}
}

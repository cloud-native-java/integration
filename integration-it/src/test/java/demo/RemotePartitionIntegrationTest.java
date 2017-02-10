package demo;
/*
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertTrue;
import static org.springframework.http.HttpMethod.GET;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ClientConfiguration.class)
public class RemotePartitionIntegrationTest {

	@Autowired
	private CloudFoundryHelper helper;

	private final RestTemplate restTemplate = new RestTemplate();

	@Test
	public void remotePartitionBatchJob() {
		Optional<Boolean> optional = this.helper.uriFor("partition-leader")
				.map(pm -> {
					String migrationJobUrl = pm + "/migrate";
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
									.exchange(pm + "/status", GET, null,
											new ParameterizedTypeReference<Map<String, Number>>() {
											})
									.getBody();
					return status.get("people.count")
							.equals(status.get("new_people.count"));
				});
		assertTrue(optional.orElse(false));
	}
} */

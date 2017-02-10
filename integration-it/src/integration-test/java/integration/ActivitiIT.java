package integration;

import cnj.CloudFoundryService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.Arrays;

@SpringBootTest(classes = ActivitiIT.Config.class)
@RunWith(SpringRunner.class)
public class ActivitiIT {

	@SpringBootApplication
	public static class Config {
	}

	private final RestTemplate restTemplate = new RestTemplateBuilder()
			.basicAuthorization("operator", "operator")
			.build();

	@Autowired
	private CloudFoundryService cloudFoundryService;

	private Log log = LogFactory.getLog(getClass());

	@Before
	public void before() throws Throwable {
	}

	@Test
	public void testDistributedWorkflows() throws Throwable {


		// deploy the activiti application, twice, to CF as a leader and a worker node.

		File activitiIntegrationFolder = new File(new File("."), "../activiti-integration");
		log.info("activiti folder: " + activitiIntegrationFolder.getAbsolutePath());
		Arrays.asList(activitiIntegrationFolder.listFiles()).forEach(log::info);

		/*String mysql = "activiti-mysql", rmq = "activiti-rabbitmq",
				leader = "activiti-leader", worker = "activiti-worker";

		Stream.of(leader, worker).forEach(app -> this.cloudFoundryService.destroyApplicationIfExists(app));
		Stream.of(mysql, rmq).forEach(svc -> this.cloudFoundryService.destroyServiceIfExists(svc));

		this.cloudFoundryService.createService("p-mysql", "100mb", mysql);
		this.cloudFoundryService.createService("cloudamqp", "lemur", rmq);

*/
		// invoke an endpoint in the leader
		//	String urlForApplication = this.cloudFoundryService.urlForApplication("activiti-leader");

/*
		cd ${integration}/activiti-integration ;

		mysql=activiti-mysql
		rmq=activiti-rabbitmq

    # reset..
		cf d -f activiti-leader
		cf d -f activiti-worker
		cf ds -f $mysql
		cf ds -f $rmq

    # deploy..
		cf cs p-mysql 100mb $mysql
		cf cs cloudamqp lemur $rmq

		cf push -f manifest-leader.yml
		cf push -f manifest-worker.yml
*/

/*
		boolean endTimeExists =
				this.helper.urlForApplication("activiti-leader").map(al -> {

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
		Assert.assertTrue("the end time is nigh (or, at least, it should be)!", endTimeExists);*/
	}
}
package demo;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.Map;

@Component
public class CircuitBreakerGreetingClient implements GreetingClient {

	private Log log = LogFactory.getLog(getClass());
	private final RestTemplate restTemplate;
	private final String serviceUri;

	@Autowired
	public CircuitBreakerGreetingClient(RestTemplate restTemplate,
			@Value("${greeting-service.domain:127.0.0.1}") String domain,
			@Value("${greeting-service.port:8080}") int port) {
		this.restTemplate = restTemplate;
		this.serviceUri = "http://" + domain + ":" + port + "/hi/{name}";
	}

	@Override
	@HystrixCommand(fallbackMethod = "fallback")
	public String greet(String name) {
		long time = System.currentTimeMillis();
		Date now = new Date(time);
		this.log.info("attempting to call " + "the greeting-service " + time
				+ "/" + now.toString());

		ParameterizedTypeReference<Map<String, String>> ptr = new ParameterizedTypeReference<Map<String, String>>() {
		};

		return this.restTemplate
				.exchange(this.serviceUri, HttpMethod.GET, null, ptr, name)
				.getBody().get("greeting");
	}

	public String fallback(String name) {
		return "OHAI";
	}

}

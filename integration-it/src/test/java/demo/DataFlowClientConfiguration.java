package demo;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.dataflow.rest.client.DataFlowTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.net.MalformedURLException;
import java.net.URI;

@Configuration
class DataFlowClientConfiguration {

	@Bean
	CloudFoundryHelper helper(CloudFoundryClient cf) {
		return new CloudFoundryHelper(cf);
	}

	@Bean
	CloudCredentials cloudCredentials(
			@Value("${cf.user}") String email,
			@Value("${cf.password}") String pw) {
		return new CloudCredentials(email, pw);
	}

	@Bean
	CloudFoundryClient cloudFoundryClient(
			@Value("${cf.api}") String url,
			CloudCredentials cc) throws MalformedURLException {
		URI uri = URI.create(url);
		CloudFoundryClient cloudFoundryClient = new CloudFoundryClient(cc, uri.toURL());
		cloudFoundryClient.login();
		return cloudFoundryClient;
	}

	@Bean
	DataFlowTemplate dataFlowTemplate(
			@Value("${cloudfoundry-dataflow-server-name:cfdf}") String cfDfServerName,
			CloudFoundryHelper helper) throws Exception {
		return
				helper
						.uriFor(cfDfServerName)
						.map(u -> new DataFlowTemplate(URI.create(u), new RestTemplate()))
						.orElseThrow(() -> new RuntimeException(
								"can't find a URI for the Spring Cloud Data Flow server!"));
	}
}

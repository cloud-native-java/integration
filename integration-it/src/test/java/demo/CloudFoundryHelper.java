package demo;

import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;

import java.util.Optional;
import java.util.stream.Stream;

class CloudFoundryHelper {

	private final CloudFoundryClient cf;
	private final RetryTemplate rt;

	public CloudFoundryHelper(CloudFoundryClient cf,
	                          RetryTemplate rt) {
		this.cf = cf;
		this.rt = rt;
	}

	Optional<String> uriFor(String appName) {
		try {
			RetryCallback<String, Throwable> retryCallback = retryContext -> {
				Stream<CloudApplication> apps = cf
						.getApplications()
						.stream()
						.filter(ca -> ca.getName().equals(appName));
				Optional<String> uri = apps
						.findFirst()
						.map(app -> app.getUris().iterator().next());
				return uri.map(x -> "http://" + x)
						.orElseThrow(() -> new RuntimeException("can't resolve the application's name!"));
			};
			return Optional.ofNullable(rt.execute(retryCallback));
		}
		catch (Throwable throwable) {
			return Optional.empty();
		}

	}
}

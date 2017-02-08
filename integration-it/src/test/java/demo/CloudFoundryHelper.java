package demo;

import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;

import java.util.Optional;
import java.util.stream.Stream;

class CloudFoundryHelper {

	private final CloudFoundryClient cf;

	CloudFoundryHelper(CloudFoundryClient cf) {
		this.cf = cf;
	}

	Optional<String> uriFor(String appName) {
		Stream<CloudApplication> apps = cf
				.getApplications()
				.stream()
				.filter(ca -> ca.getName().equals(appName));
		Optional<String> uri = apps
				.findFirst()
				.map(app -> app.getUris().iterator().next());
		return uri.map(x -> "http://" + x) ;
	}
}

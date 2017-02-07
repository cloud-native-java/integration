package task;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "simple-batch-task")
public class BatchTaskProperties {
	private Resource input, output;

	public Resource getInput() {
		return input;
	}

	public void setInput(Resource input) {
		this.input = input;
	}
}

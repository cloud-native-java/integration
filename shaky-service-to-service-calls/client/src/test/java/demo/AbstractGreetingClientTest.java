package demo;


import org.jboss.logging.Logger;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

abstract public class AbstractGreetingClientTest<T extends GreetingClient> {


	private final Logger log = Logger.getLogger(getClass());

	protected ApplicationContext serviceContext;
	protected ApplicationContext clientContext;
	protected T client;

	@Test
	public void happy() throws Exception {
		this.init(true);
		this.doTest("Hello, world!!");
	}

	@Test
	public void sad() throws Exception {
		this.init(false);
		this.doTest("OHAI");
	}

	protected void doTest(String expected) {
		String greet = this.client.greet("world!");
		this.log.info("greeting = " + greet);
		assertEquals(greet, expected);
	}

	protected void init(boolean working) {
		int port = 8080;
		if (working) {
			this.serviceContext = SpringApplication.run(GreetingApplication.class, argsFor("server.port=" + port));
		}
		this.clientContext = SpringApplication.run(GreetingClientApplication.class,
				argsFor("greeting-service.port=" + port, "server.port=0"));
		this.client = this.obtainClient();
	}

	protected abstract T obtainClient();

	protected String[] argsFor(String... args) {
		List<String> argsList = Arrays.asList("spring.jmx.default-domain=" + Math.random(),
				"spring.jmx.enabled=false", "endpoints.jmx.enabled=false");
		List<String> result = new ArrayList<>();
		result.addAll(argsList);
		result.addAll(Arrays.asList(args));

		List<String> strings = result.stream().map(a -> a.startsWith("--") ? a : "--" + a)
				.collect(Collectors.toList());
		return strings.toArray(new String[strings.size()]);
	}
}

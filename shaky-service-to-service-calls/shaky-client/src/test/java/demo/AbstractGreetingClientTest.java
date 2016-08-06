package demo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.embedded.EmbeddedServletContainerInitializedEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public abstract class AbstractGreetingClientTest<T extends GreetingClient> {

	private static AtomicInteger PORT;

	private final Log log = LogFactory.getLog(getClass());

	protected ConfigurableApplicationContext serviceContext;
	protected ConfigurableApplicationContext clientContext;

	protected T client;

	@Test
	public void happy() throws Exception {
		this.run(true, "Hello, world!!");
	}

	protected void run(boolean setup, String expectedResult) {
		PORT = new AtomicInteger();
		this.init(setup);
		this.doTest(expectedResult);
		this.clientContext.stop();
		if (null != this.serviceContext) {
			this.serviceContext.stop();
		}
		this.serviceContext = null;
		this.clientContext = null;
	}

	@Test
	public void sad() throws Exception {
		this.run(false, "OHAI");
	}

	protected void doTest(String expected) {
		String greet = this.client.greet("world!");
		this.log.info("greeting = " + greet);
		assertEquals(greet, expected);
	}

	@Configuration
	@Import(service.GreetingApplication.class)
	public static class ApplicationListenerConfig {

		@EventListener(EmbeddedServletContainerInitializedEvent.class)
		public void ready(EmbeddedServletContainerInitializedEvent evt) {
			PORT.set(evt.getEmbeddedServletContainer().getPort());
		}
	}

	protected void init(boolean working) {
		if (working) {
			this.serviceContext = SpringApplication.run(
					ApplicationListenerConfig.class, argsFor("server.port=0"));
		}
		int port = PORT.get();
		log.info("application initialized on port " + port);
		this.clientContext = SpringApplication.run(
				GreetingClientApplication.class,
				argsFor("greeting-service.port=" + port, "server.port=0"));
		this.client = this.obtainClient();

	}

	protected abstract T obtainClient();

	protected String[] argsFor(String... args) {
		List<String> argsList = Arrays.asList("spring.jmx.default-domain="
						+ Math.random(), "spring.jmx.enabled=false",
				"endpoints.jmx.enabled=false");
		List<String> result = new ArrayList<>();
		result.addAll(argsList);
		result.addAll(Arrays.asList(args));

		List<String> strings = result.stream()
				.map(a -> a.startsWith("--") ? a : "--" + a)
				.collect(Collectors.toList());
		return strings.toArray(new String[strings.size()]);
	}
}

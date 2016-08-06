package demo;

public class CircuitBreakerGreetingClientTest
 extends AbstractGreetingClientTest<CircuitBreakerGreetingClient> {

	@Override
	protected CircuitBreakerGreetingClient obtainClient() {
		return this.clientContext.getBean(CircuitBreakerGreetingClient.class);
	}
}
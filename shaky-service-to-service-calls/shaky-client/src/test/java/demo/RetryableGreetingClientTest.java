package demo;

public class RetryableGreetingClientTest
	extends AbstractGreetingClientTest<RetryableGreetingClient> {

	@Override
	protected RetryableGreetingClient obtainClient() {
		return clientContext.getBean(RetryableGreetingClient.class);
	}
}
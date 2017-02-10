package integration;


import org.junit.Assert;
import org.junit.Test;

public class SimpleIT {

	@Test
	public void test() {
		System.out.println("test in IT!");
		Assert.assertTrue("this should be run!", true);
	}
}

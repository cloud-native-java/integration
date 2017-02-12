package integration;

import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ShakyServiceIT.Config.class)
public class ShakyServiceIT {

	@SpringBootApplication
	public static class Config {}



}

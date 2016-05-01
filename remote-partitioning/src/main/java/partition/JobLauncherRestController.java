package partition;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.RestController;

@Profile("!worker")
@RestController
public class JobLauncherRestController {

}

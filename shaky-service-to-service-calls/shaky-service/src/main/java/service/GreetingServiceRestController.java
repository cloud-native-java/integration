package service;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
public class GreetingServiceRestController {

	@RequestMapping(method = RequestMethod.GET, value = "/hi/{name}")
	Map<String, String> greetings(@PathVariable String name) {
		return Collections.singletonMap("greeting", "Hello, " + name + "!");
	}
}

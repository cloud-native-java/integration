package task;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.PageRequest;

import java.util.stream.Stream;

@EnableTask // <1>
@SpringBootApplication
public class HelloTask {

	private Log log = LogFactory.getLog(getClass());

	@Bean
	CommandLineRunner runAndExplore(TaskExplorer taskExplorer) {
		return args -> {
			Stream.of(args).forEach(log::info);

			// <2>
			taskExplorer.findAll(new PageRequest(0, 1))
					.forEach(taskExecution -> log.info(taskExecution.toString()));
		};
	}

	public static void main(String args[]) {
		SpringApplication.run(HelloTask.class, args);
	}
}

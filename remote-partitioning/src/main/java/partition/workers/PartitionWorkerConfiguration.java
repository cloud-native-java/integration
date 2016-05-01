package partition.workers;


import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.step.StepLocator;
import org.springframework.batch.integration.partition.BeanFactoryStepLocator;
import org.springframework.batch.integration.partition.StepExecutionRequest;
import org.springframework.batch.integration.partition.StepExecutionRequestHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;

import static partition.workers.PartitionWorkerChannels.PartitionWorker.WORKER_REPLIES;
import static partition.workers.PartitionWorkerChannels.PartitionWorker.WORKER_REQUESTS;

import static partition.workers.PartitionWorkerConfiguration.*;

@Configuration
@Profile(WORKER_PROFILE)
class PartitionWorkerConfiguration {

	public static final String WORKER_PROFILE = "worker";

	@Bean
	StepLocator stepLocator() {
		return new BeanFactoryStepLocator();
	}

	@Bean
	StepExecutionRequestHandler stepExecutionRequestHandler(JobExplorer explorer, StepLocator stepLocator) {
		StepExecutionRequestHandler handler = new StepExecutionRequestHandler();
		handler.setStepLocator(stepLocator);
		handler.setJobExplorer(explorer);
		return handler;
	}

	@MessageEndpoint
	@Profile(WORKER_PROFILE)
	public static class StepExecutionRequestHandlerDelegator {

		@Autowired
		private StepExecutionRequestHandler handler;

		@ServiceActivator(inputChannel = WORKER_REQUESTS, outputChannel = WORKER_REPLIES)
		public StepExecution handle(StepExecutionRequest request) {
			return this.handler.handle(request);
		}
	}

}
package demo;

/**
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ClientConfiguration.class)
public class DataFlowIntegrationTest {


	@Autowired
	private CloudFoundryHelper helper;

	@Autowired
	private DataFlowTemplate df;

	private Log log = LogFactory.getLog(getClass());

	List<String> appDefinitions() {
		List<String> apps = new ArrayList<>();
		apps.add("http://repo.spring.io/libs-release-local/org/springframework/cloud/task/app/spring-cloud-task-app-descriptor/Addison.RELEASE/spring-cloud-task-app-descriptor-Addison.RELEASE.task-apps-maven");
		apps.add("http://repo.spring.io/libs-release/org/springframework/cloud/stream/app/spring-cloud-stream-app-descriptor/Avogadro.SR1/spring-cloud-stream-app-descriptor-Avogadro.SR1.stream-apps-rabbit-maven");
		this.helper
				.uriFor("server-definitions")
				.map(x -> x + "/dataflow-example-apps.properties")
				.ifPresent(apps::add);

		apps.forEach(x -> log.info("registering: " + x));
		return apps;
	}

	private Runnable awaitingRunnable(Runnable runnable, CountDownLatch cdl) {
		return () -> {
			try {
				runnable.run();
				cdl.countDown();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		};
	}

	private Runnable deployStreams() {
		return () -> {
			Map<String, String> streams = new HashMap<>();
			streams.put("ttl", "time | brackets | log");

			log.info("going to deploy " + streams.size() + " new stream(s).");
			streams.entrySet()
					.parallelStream()
					.forEach(stream -> {
						String streamName = stream.getKey();

						StreamSupport.stream(Spliterators.spliteratorUnknownSize(df.streamOperations().list().iterator(),
								Spliterator.ORDERED), false)
								.filter(sdr -> sdr.getName().equals(streamName)).forEach(tdr -> {
							log.info("deploying stream " + streamName);
							df.streamOperations().destroy(streamName);
						});

						df.streamOperations()
								.createStream(streamName, stream.getValue(), true);
					});
		};
	}

	private Runnable deployTasks() {
		return () -> {
			Map<String, String> tasks = new HashMap<>();
			tasks.put("my-simple-task", "simple-task");

			log.info("going to deploy " + tasks.size() + " new task(s).");
			tasks.entrySet()
					.parallelStream()
					.forEach(task -> {

						String taskName = task.getKey();

						StreamSupport.stream(Spliterators.spliteratorUnknownSize(df.taskOperations().list().iterator(),
								Spliterator.ORDERED), false)
								.filter(tdr -> tdr.getName().equals(taskName)).forEach(tdr -> {
							log.info("destroying task " + taskName);
							df.taskOperations().destroy(taskName);
						});

						log.info("deploying task " + taskName);
						TaskOperations to = df.taskOperations();
						to.create(taskName, task.getValue());
						to.launch(taskName,
								Collections.emptyMap(),
								Collections.singletonList(System.currentTimeMillis() + ""));
					});
		};
	}

	@Test
	public void dataFlowIsFlowing() throws Exception {
		appDefinitions()
				.parallelStream()
				.forEach(u -> {
					log.info("importing " + u);
					df.appRegistryOperations().importFromResource(u, true);
				});

		List<Runnable> runnables = Arrays.asList(deployStreams(), deployTasks());
		int parties = runnables.size();
		CountDownLatch cdl = new CountDownLatch(parties);
		ExecutorService executorService = Executors.newFixedThreadPool(parties);
		runnables
				.stream()
				.map(r -> awaitingRunnable(r, cdl))
				.forEach(executorService::submit);
		cdl.await();
		log.info("deployed tasks and streams.");
	}
} */
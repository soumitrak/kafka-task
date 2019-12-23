package sk.task.exec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

@Singleton
public class TaskLocatorService implements TaskLocator {

    private static Logger logger = LoggerFactory.getLogger(TaskLocatorService.class);

    private final ServiceLoader<Task> loader = ServiceLoader.load(Task.class);

    private final Map<String, Task> tid2t = new HashMap<String, Task>();

    @Inject
    public TaskLocatorService() {
        StreamSupport.stream(loader.spliterator(), false)
                .forEach(c -> {
                    logger.info("Loaded task {}", c.getClass().getCanonicalName());
                    tid2t.put(c.tid(), c);
                });
    }

    @Override
    public Task task(String tid) {
        return tid2t.get(tid);
    }
}

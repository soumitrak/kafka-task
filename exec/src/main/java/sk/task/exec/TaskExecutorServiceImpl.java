package sk.task.exec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.task.msg.Input;
import sk.task.msg.Output;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


@Singleton
public class TaskExecutorServiceImpl implements TaskExecutorService {

    private static Logger logger = LoggerFactory.getLogger(TaskExecutorServiceImpl.class);

    private final TaskLocator taskLocator;

    private final BlockingQueue<Runnable> poolQueue;

    private final ExecutorService service;

    private final ExecutorCompletionService<Output> completionService;

    private final FutureTask<Void> monitorTask;

    private final CountDownLatch shutdownLatch = new CountDownLatch(1);

    @Inject
    public TaskExecutorServiceImpl(
            final TaskLocator taskLocator,
            final int nThreads) {
        this.taskLocator = taskLocator;

        poolQueue = new LinkedBlockingQueue<Runnable>();

        service = new ThreadPoolExecutor(nThreads, nThreads,
                0L, TimeUnit.MILLISECONDS,
                poolQueue,
                r -> {
                    Thread t = new Thread(r, "task-thread");
                    t.setDaemon(true);
                    return t;
                });

        completionService = new ExecutorCompletionService<Output>(this.service);

        monitorTask = new FutureTask<Void>(new TaskMonitor(1000), null);
        Thread t = new Thread(monitorTask);
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void submit(List<Input> inputs) {
        inputs.forEach(i -> {
                    final Task task = taskLocator.task(i.eid());
                    if (task != null) {
                        completionService.<Output>submit(i.rp() ?
                                new ProcessTaskExecutor(shutdownLatch, task, i)
                                : new ThreadTaskExecutor(task, i));
                    } else {
                        logger.error("Cannot find task for id {}", i.eid());
                    }
                }
        );
    }

    @Override
    public boolean ready() {
        return poolQueue.isEmpty();
    }

    @Override
    public void shutdown() {
        service.shutdown();
        shutdownLatch.countDown();
        try {
            monitorTask.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class TaskMonitor implements Runnable {
        private int milliSeconds;

        TaskMonitor(final int milliSeconds) {
            this.milliSeconds = milliSeconds;
        }

        @Override
        public void run() {
            boolean done = false;

            try {
                while (!done) {
                    done = service.awaitTermination(milliSeconds, TimeUnit.MILLISECONDS);

                    Future<Output> future;
                    // Drain the queue of finished tasks as much as possible.
                    while ((future = completionService.poll()) != null) {
                        try {
                            Output o = future.get();
                            if (o != null) {
                                logger.info("Task output {}", o.output());
                            }
                        } catch (Exception e) {
                            logger.error("Exception during execution", e);
                        }
                    }
                }
            } catch (InterruptedException ie) {
                logger.error("Error in task monitor", ie);
            }
        }
    }
}

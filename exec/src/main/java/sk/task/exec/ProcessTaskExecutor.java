package sk.task.exec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.task.msg.Input;
import sk.task.msg.Output;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ProcessTaskExecutor extends TaskExecutor {

    private static Logger logger = LoggerFactory.getLogger(ProcessTaskExecutor.class);

    private final CountDownLatch shutdownLatch;

    public ProcessTaskExecutor(final CountDownLatch shutdownLatch, final Task task, final Input input) {
        super(task, input);
        this.shutdownLatch = shutdownLatch;
    }

    @Override
    public Output call() throws Exception {

        final Exec exec = new Exec(task.tid(), input);
        FutureTask<Output> futureTask = new FutureTask<Output>(exec);
        Thread t = new Thread(futureTask, "process-exec");
        t.setDaemon(true);
        t.start();

        while (!shutdownLatch.await(200, TimeUnit.MILLISECONDS)) {
            if (futureTask.isDone()) {
                break;
            }
        }

        Output out = null;
        if (futureTask.isDone()) {
            out = futureTask.get();
        } else {
            exec.kill();
        }

        return out;
    }

    static class Exec implements Callable<Output> {

        private static final ObjectMapper mapper = new ObjectMapper();

        private static final String main = "sk.task.kafka.App";

        private static final String cp = ManagementFactory.getRuntimeMXBean().getClassPath();

        private final String tid;

        private final Input input;

        volatile Process process = null;

        Exec(final String tid, final Input input) {
            this.tid = tid;
            this.input = input;
        }

        void kill() {
            if (process != null) {
                process.destroyForcibly();
            }
        }

        @Override
        public Output call() throws Exception {
            final ProcessBuilder processBuilder = new ProcessBuilder(
                    "java",
                    "-cp", cp, main,
                    "--task", tid,
                    "--input", input.toJson(mapper));
            process = processBuilder.start();
            String out = null;

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                logger.debug("SUBPROCESS: {}", line);
                if (line.startsWith("Output:")) {
                    out = line.substring(7);
                }
            }

            final Output output = out == null ? null : Output.fromJson(mapper, out);
            process.waitFor();
            return output;
        }
    }
}

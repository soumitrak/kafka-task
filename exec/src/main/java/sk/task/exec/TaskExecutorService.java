package sk.task.exec;

import sk.task.msg.Input;

import java.util.List;

public interface TaskExecutorService {
    void submit(final List<Input> inputs);
    boolean ready();
    void shutdown();
}

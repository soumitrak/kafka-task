package sk.task.exec;

import sk.task.msg.Input;
import sk.task.msg.Output;

import java.util.concurrent.Callable;

public abstract class TaskExecutor implements Callable<Output> {
    protected final Task task;

    protected final Input input;

    public TaskExecutor(final Task task, final Input input) {
        this.task = task;
        this.input = input;
    }
}

package sk.task.exec;

import sk.task.msg.Input;
import sk.task.msg.Output;

public class ThreadTaskExecutor extends TaskExecutor {

    public ThreadTaskExecutor(Task task, Input input) {
        super(task, input);
    }

    @Override
    public Output call() throws Exception {
        return task.exec(input);
    }
}

package sk.task.exec;

import sk.task.msg.Input;
import sk.task.msg.Output;

public interface Task {
    String tid();
    Output exec(final Input input);
}

package sk.task.text.exec;

import com.google.auto.service.AutoService;
import sk.task.exec.Task;
import sk.task.msg.Input;
import sk.task.msg.Output;

@AutoService(Task.class)
public class Capitalize implements Task {

    @Override
    public String tid() {
        return "capitalize";
    }

    @Override
    public Output exec(final Input input) {
        final String args = input.args();
        return new Output(args.substring(0, 1).toUpperCase() + args.substring(1));
    }
}

package sk.task.text.exec;

import com.google.auto.service.AutoService;
import sk.task.exec.Task;
import sk.task.msg.Input;
import sk.task.msg.Output;

@AutoService(Task.class)
public class Reverse implements Task {

    @Override
    public String tid() {
        return "reverse";
    }

    @Override
    public Output exec(final Input input) {
        final String args = input.args();
        final StringBuilder sb = new StringBuilder();
        for (int i = args.length() - 1; i >= 0; i--) {
            sb.append(args.charAt(i));
        }
        return new Output(sb.toString());
    }
}

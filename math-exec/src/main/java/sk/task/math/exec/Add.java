package sk.task.math.exec;

import com.google.auto.service.AutoService;
import sk.task.exec.Task;
import sk.task.msg.Input;
import sk.task.msg.Output;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@AutoService(Task.class)
public class Add implements Task {
    private final Pattern pattern;

    public Add() {
        pattern = Pattern.compile("(\\d+)\\s*\\+\\s*(\\d+)");
    }

    @Override
    public String tid() {
        return "add";
    }

    @Override
    public Output exec(final Input input) {
        final Matcher matcher = pattern.matcher(input.args());

        Output out = null;
        if (matcher.matches()) {
            out = new Output(Integer.toString(Integer.parseInt(matcher.group(1)) + Integer.parseInt(matcher.group(2))));
        }

        return out;
    }
}

package sk.task.text.exec;

import org.junit.jupiter.api.Test;
import sk.task.msg.Input;

import static org.junit.jupiter.api.Assertions.*;

class ReverseTest {

    @Test
    void exec() {
        assertEquals("tset", new Reverse().exec(new Input("reverse", false, "test")).output());
    }
}
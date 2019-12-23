package sk.task.text.exec;

import org.junit.jupiter.api.Test;
import sk.task.msg.Input;

import static org.junit.jupiter.api.Assertions.*;

class CapitalizeTest {

    @Test
    void exec() {
        assertEquals("Test", new Reverse().exec(new Input("capitalize", false, "test")).output());
    }
}
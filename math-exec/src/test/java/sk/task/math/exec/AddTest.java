package sk.task.math.exec;

import org.junit.jupiter.api.Test;
import sk.task.msg.Input;

import static org.junit.jupiter.api.Assertions.*;

class AddTest {

    @Test
    void exec() {
        assertEquals(Integer.parseInt(new Add().exec(new Input("add", false, "1+2")).output()), 3);
    }
}
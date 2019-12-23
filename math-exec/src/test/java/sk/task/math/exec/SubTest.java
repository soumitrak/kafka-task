package sk.task.math.exec;

import org.junit.jupiter.api.Test;
import sk.task.msg.Input;

import static org.junit.jupiter.api.Assertions.*;

class SubTest {

    @Test
    void exec() {
        assertEquals(Integer.parseInt(new Sub().exec(new Input("sub", false, "1-2")).output()), -1);
    }
}
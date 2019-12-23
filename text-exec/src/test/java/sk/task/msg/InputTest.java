package sk.task.msg;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InputTest {

    @Test
    void toJson() throws JsonProcessingException {
        Input in = new Input("add", false, "1+2");
        ObjectMapper mapper = new ObjectMapper();
        // System.out.println(in.toJson(mapper));
        assertEquals(in.args(), Input.fromJson(mapper, in.toJson(mapper)).args());
    }
}
package sk.task.msg;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Output of task execution
 */
public class Output {
    // Response message after execution message
    private final String output;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Output(@JsonProperty("output") final String output) {
        this.output = output;
    }

    @JsonProperty("output")
    public String output() {
        return output;
    }

    public String toJson(final ObjectMapper mapper) throws JsonProcessingException {
        return mapper.writeValueAsString(this);
    }

    public static Output fromJson(final ObjectMapper mapper, final String json) throws Exception {
        return mapper.readValue(json, Output.class);
    }
}

package sk.task.msg;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Task execution request message
 */
public class Input {
    // Header fields, helps to pick the task executor and execution details

    // Executor id
    private final String eid;

    // Run task in a different process
    private final boolean rp;

    // Input String
    private final String args;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Input(@JsonProperty("eid") final String eid,
                 @JsonProperty("rp") final boolean rp,
                 @JsonProperty("args") final String args) {
        this.eid = eid;
        this.rp = rp;
        this.args = args;
    }

    @JsonProperty("eid")
    public String eid() {
        return eid;
    }

    @JsonProperty("rp")
    public boolean rp() {
        return rp;
    }

    @JsonProperty("args")
    public String args() {
        return args;
    }

    public String toJson(final ObjectMapper mapper) throws JsonProcessingException {
        return mapper.writeValueAsString(this);
    }

    public static Input fromJson(final ObjectMapper mapper, final String json) throws JsonProcessingException {
        return mapper.readValue(json, Input.class);
    }
}

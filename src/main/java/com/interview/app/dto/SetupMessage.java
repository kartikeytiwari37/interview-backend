package com.interview.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class SetupMessage {
    private Map<String, Object> setup;

    public SetupMessage() {}

    public SetupMessage(Map<String, Object> setup) {
        this.setup = setup;
    }

    public Map<String, Object> getSetup() {
        return setup;
    }

    public void setSetup(Map<String, Object> setup) {
        this.setup = setup;
    }
}

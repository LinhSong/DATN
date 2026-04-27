package model;

import java.util.Map;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TestCase {
    public String id;
    public String method;
    public String endpoint;
    public Map<String, Object> pathParams;
    public Map<String, String> headers;
    public Object body;

    @JsonProperty("expected_status")
    public int expectedStatus;

    public boolean is_valid_testcase;
    public String error_type;
    public String description;
}
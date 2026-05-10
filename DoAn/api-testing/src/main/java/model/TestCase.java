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

    @JsonProperty("is_valid_testcase")
    public boolean isValidTestcase;

    @JsonProperty("error_type")
    public String errorType;

    public String description;

    public Map<String, String> labels;
}
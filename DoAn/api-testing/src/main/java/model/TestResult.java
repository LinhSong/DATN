package model;

public class TestResult {

    public String id;

    public int expectedStatus;
    public int actualStatus;

    public boolean passed;

    public String requestBody;
    public String responseBody;

    public String errorType;
    public String method;
    public String endpoint;
    public String testCaseId;
}
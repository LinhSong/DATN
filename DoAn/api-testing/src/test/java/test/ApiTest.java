package test;

import executor.ApiExecutor;
import model.TestCase;
import model.TestResult;

import org.testng.annotations.AfterClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import utils.JsonReader;

import java.util.ArrayList;
import java.util.List;

import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;

@Listeners({io.qameta.allure.testng.AllureTestNg.class})

@Epic("API Testing Framework")
@Feature("Swagger Petstore API")

public class ApiTest {

    private static int total = 0;
    private static int passedCount = 0;

    // RQ2 COUNTERS
    private static int assertionErrors = 0;
    private static int invalidDataErrors = 0;
    private static int runtimeErrors = 0;
    private static int invalidEndpointErrors = 0;

    // COVERAGE
    private static int getCount = 0;
    private static int postCount = 0;
    private static int putCount = 0;
    private static int deleteCount = 0;

    // LISTS
    private static final List<String> assertionList = new ArrayList<>();
    private static final List<String> invalidDataList = new ArrayList<>();
    private static final List<String> runtimeList = new ArrayList<>();
    private static final List<String> endpointList = new ArrayList<>();

    private static final List<TestResult> results = new ArrayList<>();

    @DataProvider(name = "api-data")
    public Object[][] getData() throws Exception {

        String file = System.getProperty("file");

        if (file == null) {
            throw new RuntimeException("Missing -Dfile parameter");
        }

        List<TestCase> testCases = JsonReader.readTestCases(file);

        Object[][] data = new Object[testCases.size()][1];

        for (int i = 0; i < testCases.size(); i++) {
            data[i][0] = testCases.get(i);
        }

        return data;
    }

    @Test(dataProvider = "api-data")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Execute API test case from JSON")
    public void runTest(TestCase tc) {

        total++;

        // COVERAGE COUNT
        switch (tc.method.toUpperCase()) {

            case "GET":
                getCount++;
                break;

            case "POST":
                postCount++;
                break;

            case "PUT":
                putCount++;
                break;

            case "DELETE":
                deleteCount++;
                break;
        }

        Allure.step("Execute Test Case: " + tc.id);

        Allure.label("endpoint", tc.endpoint);
        Allure.label("method", tc.method);

        TestResult result = ApiExecutor.execute(tc);

        // ADD META INFO
        result.method = tc.method;
        result.endpoint = tc.endpoint;
        result.testCaseId = tc.id;

        results.add(result);

        int expected = result.expectedStatus;
        int actual = result.actualStatus;

        boolean passed = (expected == actual);

        if (passed) {
            passedCount++;
        }

        // CONSOLE LOG
        System.out.println(
                tc.id +
                " | Expected: " + expected +
                " | Actual: " + actual +
                " | Passed: " + passed
        );

        // ALLURE ATTACHMENTS
        Allure.addAttachment("Test Case ID", tc.id);
        Allure.addAttachment("Method", tc.method);
        Allure.addAttachment("Endpoint", tc.endpoint);
        if (tc.labels != null) {

        for (String key : tc.labels.keySet()) {

                Allure.label(
                        key,
                        tc.labels.get(key)
                );
        }
        }
        Allure.addAttachment("Expected Status",
                String.valueOf(expected));

        Allure.addAttachment("Actual Status",
                String.valueOf(actual));

        Allure.addAttachment("Result",
                passed ? "PASS" : "FAIL");

        Allure.addAttachment("Request",
                String.valueOf(result.requestBody));

        Allure.addAttachment("Response",
                String.valueOf(result.responseBody));

        // ERROR CLASSIFICATION
        if (!passed) {

            String errorType;

            // Runtime Error
            if (actual == 0) {

                errorType = "runtime_error";

                runtimeErrors++;

                runtimeList.add(
                        tc.id + " | No response from API"
                );
            }

            // Invalid Endpoint
            else if (tc.endpoint.contains("unknown")
                    || tc.endpoint.contains("invalid")) {

                errorType = "invalid_endpoint";

                invalidEndpointErrors++;

                endpointList.add(
                        tc.id +
                        " | Endpoint: " +
                        tc.endpoint
                );
            }

            // Invalid Data
                else if (
                        tc.labels != null
                        && "invalid".equalsIgnoreCase(tc.labels.get("validity"))
                        && actual >= 400
                        && actual < 500
                )
                    {

                errorType = "invalid_test_data";

                invalidDataErrors++;

                invalidDataList.add(
                        tc.id +
                        " | Expected: " + expected +
                        " | Actual: " + actual
                );
            }

            // Assertion Error
            else {

                errorType = "assertion_error";

                assertionErrors++;

                assertionList.add(
                        tc.id +
                        " | Expected: " + expected +
                        " | Actual: " + actual
                );
            }

            result.errorType = errorType;

            Allure.label("errorType", errorType);

            Allure.step("ERROR_TYPE: " + errorType);

            Allure.addAttachment(
                    "Error Type",
                    errorType
            );
        }

        Allure.step(
                tc.id +
                " | Expected: " + expected +
                " | Actual: " + actual +
                " | Passed: " + passed
        );

        // FORCE FAIL
        if (!passed) {

            throw new AssertionError(
                    tc.id +
                    " | Expected: " + expected +
                    " | Actual: " + actual +
                    " | ErrorType: " + result.errorType
            );
        }
    }

    @AfterClass
    public void summary() {

        int failed = total - passedCount;

        double passRate =
                total == 0
                        ? 0
                        : (passedCount * 100.0 / total);

        System.out.println("\n===== ERROR ANALYSIS SUMMARY =====");

        System.out.println("Total: " + total);
        System.out.println("Passed: " + passedCount);
        System.out.println("Failed: " + failed);

        System.out.printf(
                "Pass rate: %.2f%%\n",
                passRate
        );

        System.out.println("\n===== ERROR DISTRIBUTION =====");

        System.out.println(
                "Assertion Errors: " + assertionErrors
        );

        System.out.println(
                "Invalid Data Errors: " + invalidDataErrors
        );

        System.out.println(
                "Runtime Errors: " + runtimeErrors
        );

        System.out.println(
                "Invalid Endpoint Errors: " + invalidEndpointErrors
        );

        // COVERAGE
        System.out.println("\n===== API COVERAGE =====");

        System.out.println("GET: " + getCount);
        System.out.println("POST: " + postCount);
        System.out.println("PUT: " + putCount);
        System.out.println("DELETE: " + deleteCount);

        System.out.println("\n===== ASSERTION ERRORS =====");

        assertionList.forEach(System.out::println);

        System.out.println("\n===== INVALID TEST DATA =====");

        invalidDataList.forEach(System.out::println);

        System.out.println("\n===== RUNTIME ERRORS =====");

        runtimeList.forEach(System.out::println);

        // ENDPOINT
        System.out.println("\n===== INVALID ENDPOINT =====");

        endpointList.forEach(System.out::println);

        // ALLURE SUMMARY
        Allure.addAttachment(
                "Total Test Cases",
                String.valueOf(total)
        );

        Allure.addAttachment(
                "Passed",
                String.valueOf(passedCount)
        );

        Allure.addAttachment(
                "Failed",
                String.valueOf(failed)
        );

        Allure.addAttachment(
                "Pass Rate",
                String.format("%.2f%%", passRate)
        );

        // ERROR SUMMARY
        Allure.addAttachment(
                " Error Distribution",

                "Assertion Errors: " + assertionErrors + "\n" +
                "Invalid Data Errors: " + invalidDataErrors + "\n" +
                "Runtime Errors: " + runtimeErrors + "\n" +
                "Invalid Endpoint Errors: " + invalidEndpointErrors
        );

        // COVERAGE SUMMARY
        Allure.addAttachment(
                "Coverage Summary",

                "GET: " + getCount + "\n" +
                "POST: " + postCount + "\n" +
                "PUT: " + putCount + "\n" +
                "DELETE: " + deleteCount
        );

        // FULL ERROR REPORT
        Allure.addAttachment(
                "FULL REPORT",

                "===== ASSERTION ERRORS =====\n" +
                String.join("\n", assertionList) +

                "\n\n===== INVALID DATA =====\n" +
                String.join("\n", invalidDataList) +

                "\n\n===== RUNTIME ERRORS =====\n" +
                String.join("\n", runtimeList) +

                "\n\n===== INVALID ENDPOINT =====\n" +
                String.join("\n", endpointList)
        );

        // SAVE RESULT JSON
        try {

            utils.ResultWriter.write(
                    results,
                    "output/result.json"
            );

            System.out.println(
                    "Saved result to output/result.json"
            );

        } catch (Exception e) {

            System.out.println(
                    "Error writing result.json: "
                    + e.getMessage()
            );
        }
    }
}
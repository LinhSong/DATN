package test;

import executor.ApiExecutor;
import model.TestCase;
import model.TestResult;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.annotations.AfterClass;
import utils.JsonReader;

import java.util.List;
import java.util.ArrayList;

import io.qameta.allure.*;
import org.testng.annotations.Listeners;
//import org.testng.Assert;

@Listeners({io.qameta.allure.testng.AllureTestNg.class})
@Epic("API Testing Framework")
@Feature("Pet API")

public class ApiTest {

    private static int total = 0;
    private static int passedCount = 0;

    // COUNTERS 
    private static int assertionErrors = 0;
    private static int invalidDataErrors = 0;
    private static int runtimeErrors = 0;
    private static int invalidEndpointErrors = 0;

    // LISTS 
    private static List<String> assertionList = new ArrayList<>();
    private static List<String> invalidDataList = new ArrayList<>();
    private static List<String> runtimeList = new ArrayList<>();
    private static List<String> endpointList = new ArrayList<>();
    private static List<TestResult> results = new ArrayList<>();

    @DataProvider(name = "api-data")
    public Object[][] getData() throws Exception {

        String file = System.getProperty("file");

        if (file == null) {
            throw new RuntimeException("Missing -Dfile parameter!");
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
    @Description("Run API test from JSON")
    public void runTest(TestCase tc) {

        total++;

        Allure.step("Execute API: " + tc.id);

        // 🔥 LABEL THÊM (RẤT QUAN TRỌNG CHO REPORT)
        Allure.label("endpoint", tc.endpoint);
        Allure.label("method", tc.method);

        TestResult result = ApiExecutor.execute(tc);
        results.add(result);

        int expected = result.expectedStatus;
        int actual = result.actualStatus;
        boolean passed = (actual == expected);

        if (passed) {
            passedCount++;
        }

        System.out.println(
                tc.id +
                        " | Expected: " + expected +
                        " | Actual: " + actual +
                        " | Passed: " + passed
        );

        // ALLURE BASIC 
        Allure.addAttachment("Test Case ID", tc.id);
        Allure.addAttachment("Expected", String.valueOf(expected));
        Allure.addAttachment("Actual", String.valueOf(actual));
        Allure.addAttachment("Result", passed ? "PASS" : "FAIL");

        Allure.addAttachment("Request", String.valueOf(result.requestBody));
        Allure.addAttachment("Response", String.valueOf(result.responseBody));

        //  ERROR CLASSIFICATION 
        if (!passed) {

            String errorType;

            if (actual == 0) {
                errorType = "runtime_error";
                runtimeErrors++;
                runtimeList.add(tc.id + " | No response");
            }

            else if (actual == 404 && tc.endpoint.contains("unknown")) {
                errorType = "invalid_endpoint";
                invalidEndpointErrors++;
                endpointList.add(tc.id + " | " + tc.endpoint);
            }

            else if (actual >= 400 && actual < 500 && !tc.is_valid_testcase) {
                errorType = "invalid_test_data";
                invalidDataErrors++;
                invalidDataList.add(tc.id + " | Expected: " + expected + " | Actual: " + actual);
            }

            else {
                errorType = "assertion_error";
                assertionErrors++;
                assertionList.add(tc.id + " | Expected: " + expected + " | Actual: " + actual);
            }

            result.errorType = errorType;

            Allure.label("errorType", errorType);

            // 🔥 QUAN TRỌNG: để Allure match categories
            Allure.step("ERROR_TYPE: " + errorType);

            Allure.addAttachment("Error Type", errorType);
        }

        Allure.step(
                tc.id +
                        " | Expected: " + expected +
                        " | Actual: " + actual +
                        " | Passed: " + passed
        );

        // 🔥 FORCE FAIL (KHÔNG DÙNG Assert nữa)
        if (actual != expected) {
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
        double passRate = total == 0 ? 0 : (passedCount * 100.0) / total;

        System.out.println("\n===== RQ2 - ERROR ANALYSIS SUMMARY =====");
        System.out.println("Total: " + total);
        System.out.println("Passed: " + passedCount);
        System.out.println("Failed: " + failed);
        System.out.printf("Pass rate: %.2f%%\n", passRate);

        System.out.println("\n===== ERROR DISTRIBUTION =====");
        System.out.println("Assertion Errors: " + assertionErrors);
        System.out.println("Invalid Data Errors: " + invalidDataErrors);
        System.out.println("Runtime Errors: " + runtimeErrors);
        System.out.println("Invalid Endpoint Errors: " + invalidEndpointErrors);

        System.out.println("\n===== ASSERTION ERRORS =====");
        assertionList.forEach(System.out::println);

        System.out.println("\n===== INVALID TEST DATA =====");
        invalidDataList.forEach(System.out::println);

        System.out.println("\n===== RUNTIME ERRORS =====");
        runtimeList.forEach(System.out::println);

        System.out.println("\n===== INVALID ENDPOINT =====");
        endpointList.forEach(System.out::println);

        Allure.addAttachment("Total", String.valueOf(total));
        Allure.addAttachment("Passed", String.valueOf(passedCount));
        Allure.addAttachment("Failed", String.valueOf(failed));
        Allure.addAttachment("Pass rate", String.format("%.2f%%", passRate));

        Allure.addAttachment("Summary",
                "Assertion Errors: " + assertionErrors + "\n" +
                        "Invalid Data: " + invalidDataErrors + "\n" +
                        "Runtime Errors: " + runtimeErrors + "\n" +
                        "Invalid Endpoint: " + invalidEndpointErrors
        );

        Allure.addAttachment(
                "RQ2 FULL REPORT",
                "===== ASSERTION =====\n" + String.join("\n", assertionList) +
                        "\n\n===== INVALID DATA =====\n" + String.join("\n", invalidDataList) +
                        "\n\n===== RUNTIME =====\n" + String.join("\n", runtimeList) +
                        "\n\n===== ENDPOINT =====\n" + String.join("\n", endpointList)
        );
        try {
            utils.ResultWriter.write(results, "output/result.json");
            System.out.println("✅ Saved result to output/result.json");
        } catch (Exception e) {
            System.out.println("❌ Error writing result.json: " + e.getMessage());
        }
    }
}
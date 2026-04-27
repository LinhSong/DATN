package runner;

import executor.ApiExecutor;
import io.qameta.allure.Allure;
import io.qameta.allure.model.TestResult;
import model.TestCase;
import utils.JsonReader;

import java.util.List;
import java.util.UUID;

public class TestRunner {

    public static void main(String[] args) {

        String file = args[0];

        String testUUID = UUID.randomUUID().toString();

        Allure.getLifecycle().scheduleTestCase(
                new TestResult().setUuid(testUUID).setName("API Test Suite"));

        Allure.getLifecycle().startTestCase(testUUID);

        try {
            List<TestCase> testCases = JsonReader.readTestCases(file);

            int passed = 0;

            for (TestCase tc : testCases) {

                model.TestResult result = ApiExecutor.execute(tc);

                System.out.println(tc.id +
                        " | Expected: " + result.expectedStatus +
                        " | Actual: " + result.actualStatus +
                        " | Passed: " + result.passed);

                if (result.passed) passed++;
            }

            System.out.println("\n===== SUMMARY =====");
            System.out.println("Total: " + testCases.size());
            System.out.println("Passed: " + passed);
            System.out.println("Failed: " + (testCases.size() - passed));

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        Allure.getLifecycle().stopTestCase(testUUID);
        Allure.getLifecycle().writeTestCase(testUUID);
    }
}
package runner;

import executor.ApiExecutor;
import io.qameta.allure.Allure;
import io.qameta.allure.model.TestResult;
import model.TestCase;
import utils.JsonReader;
import utils.ResultWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TestRunner {

    public static void main(String[] args) {

        // ✅ CHECK INPUT
        if (args.length == 0) {
            System.out.println("❌ Missing testcase file!");
            System.out.println("👉 Example: mvn exec:java -Dexec.args=\"testcases/pet_testcases.json\"");
            return;
        }

        String file = args[0];

        String testUUID = UUID.randomUUID().toString();

        Allure.getLifecycle().scheduleTestCase(
                new TestResult().setUuid(testUUID).setName("API Test Suite")
        );

        Allure.getLifecycle().startTestCase(testUUID);

        try {
            List<TestCase> testCases = JsonReader.readTestCases(file);

            int passed = 0;

            // ✅ LIST KẾT QUẢ
            List<model.TestResult> results = new ArrayList<>();

            for (TestCase tc : testCases) {

                model.TestResult result = ApiExecutor.execute(tc);

                // ✅ ADD VÀO LIST
                results.add(result);

                System.out.println(tc.id +
                        " | Expected: " + result.expectedStatus +
                        " | Actual: " + result.actualStatus +
                        " | Passed: " + result.passed);

                if (result.passed) passed++;
            }

            // ✅ GHI FILE CHO STREAMLIT
            ResultWriter.write(results, "output/result.json");

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
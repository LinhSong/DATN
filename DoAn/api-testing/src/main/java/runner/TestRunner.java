package runner;

import executor.ApiExecutor;
import model.TestCase;
import model.TestResult;
import utils.JsonReader;
import utils.ResultWriter;

import java.util.ArrayList;
import java.util.List;

public class TestRunner {

    public static void main(String[] args) {

        // 1. validate input
        if (args.length == 0) {
            System.out.println("❌ Please provide test case file path");
            System.out.println("Example: testcases/pet.json");
            return;
        }

        String filePath = args[0];

        try {
            // 2. load testcases
            List<TestCase> testCases = JsonReader.readTestCases(filePath);

            List<TestResult> results = new ArrayList<>();

            int pass = 0;

            // 3. execute
            for (TestCase tc : testCases) {

                TestResult result = ApiExecutor.execute(tc);
                results.add(result);

                System.out.println(
                        tc.id +
                        " | Expected: " + result.expectedStatus +
                        " | Actual: " + result.actualStatus +
                        " | Passed: " + result.passed
                );

                if (result.passed) pass++;
            }

            // 4. summary
            System.out.println("\n===== SUMMARY =====");
            System.out.println("Total: " + testCases.size());
            System.out.println("Passed: " + pass);
            System.out.println("Failed: " + (testCases.size() - pass));
            System.out.println("Success Rate: " + (pass * 100.0 / testCases.size()) + "%");

            // 5. export result
            ResultWriter.write(results, "results/output.json");

        } catch (Exception e) {
            System.out.println("❌ Error running framework");
            e.printStackTrace();
        }
    }
}
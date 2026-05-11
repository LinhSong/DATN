package runner;

import executor.ApiExecutor;
import io.qameta.allure.Allure;
//import io.qameta.allure.model.TestResult;
import model.TestCase;
import utils.ExcelReader;
import utils.ResultWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TestRunner {

    public static void main(String[] args) {

        if (args.length == 0) {
            System.out.println(" Missing testcase file!");
            return;
        }

        String file = args[0];
        String testUUID = UUID.randomUUID().toString();

        Allure.getLifecycle().startTestCase(testUUID);

        try {
            List<TestCase> testCases = ExcelReader.readTestCases(file);

            List<model.TestResult> results = new ArrayList<>();

            int passed = 0;

            for (TestCase tc : testCases) {

                model.TestResult result = ApiExecutor.execute(tc);
                results.add(result);

                if (result.passed) passed++;

                System.out.println(tc.id +
                        " | Expected: " + result.expectedStatus +
                        " | Actual: " + result.actualStatus +
                        " | Passed: " + result.passed);
            }

            ResultWriter.write(results, "output/result.json");

            System.out.println("\nTotal: " + testCases.size());
            System.out.println("Passed: " + passed);

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        Allure.getLifecycle().stopTestCase(testUUID);
    }
}
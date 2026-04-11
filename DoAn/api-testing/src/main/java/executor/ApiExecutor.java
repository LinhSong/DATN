package executor;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import model.TestCase;
import model.TestResult;

public class ApiExecutor {

    public static TestResult execute(TestCase tc) {

        TestResult result = new TestResult();
        result.id = tc.id;
        result.expectedStatus = tc.expectedStatus;

        try {
            Response response = RestAssured
                    .given()
                    .baseUri("https://petstore.swagger.io/v2")
                    .headers(tc.headers)
                    .pathParams(tc.pathParams)
                    .body(tc.body)
                    .when()
                    .request(tc.method, tc.endpoint);

            result.actualStatus = response.getStatusCode();
            result.passed = (result.actualStatus == result.expectedStatus);

        } catch (Exception e) {
            result.passed = false;
            result.errorType = "runtime_error";
        }

        return result;
    }
}
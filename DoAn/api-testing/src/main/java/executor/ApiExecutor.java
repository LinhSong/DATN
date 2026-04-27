package executor;

import io.qameta.allure.Allure;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import model.TestCase;
import model.TestResult;

import java.util.Map;

public class ApiExecutor {

    private static final String BASE_URL = "https://petstore.swagger.io/v2";

    public static TestResult execute(TestCase tc) {

        TestResult result = new TestResult();
        result.id = tc.id;
        result.expectedStatus = tc.expectedStatus;

        try {

            // BUILD ENDPOINT + PATH PARAMS
            String endpoint = tc.endpoint;

            if (tc.pathParams != null && !tc.pathParams.isEmpty()) {
                for (Map.Entry<String, Object> entry : tc.pathParams.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();

                    // thay thế {orderId} nếu có
                    endpoint = endpoint.replace("{" + key + "}", String.valueOf(value));
                }
            }

            Allure.step("Request: " + tc.method + " " + endpoint);

            // REQUEST BODY HANDLE (FIX STRING CASE)
            Object requestBody = tc.body;
            String payload;

            if (tc.body instanceof String) {
                payload = (String) tc.body;
                requestBody = payload;
            } else if (tc.body != null) {
                payload = tc.body.toString();
            } else {
                payload = "";
            }

            // GỬI REQUEST
            Response response = RestAssured
                    .given()
                    .baseUri(BASE_URL)
                    .headers(tc.headers)
                    .body(requestBody)
                    .when()
                    .request(tc.method, endpoint);

            int status = response.getStatusCode();

            // SAVE RESULT
            result.requestBody = payload;
            result.responseBody = response.asString();

            //ALLURE ATTACH
            Allure.addAttachment("Request JSON", "application/json", payload);
            Allure.addAttachment("Response JSON", "application/json", response.asString());
            Allure.addAttachment("Response Pretty", response.asPrettyString());
            Allure.addAttachment("Status Code", String.valueOf(status));

            result.actualStatus = status;
            result.passed = (status == result.expectedStatus);

        } catch (Exception e) {

             result.passed = false;
            result.errorType = "runtime_error";
            result.actualStatus = 0;
            Allure.addAttachment("ERROR", e.toString());
             
        }

        return result;
    }
}
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

            // ===== BUILD ENDPOINT =====
            String endpoint = tc.endpoint;

            if (tc.pathParams != null && !tc.pathParams.isEmpty()) {
                for (Map.Entry<String, Object> entry : tc.pathParams.entrySet()) {
                    endpoint = endpoint.replace(
                            "{" + entry.getKey() + "}",
                            String.valueOf(entry.getValue())
                    );
                }
            }

            Allure.step("Request: " + tc.method + " " + endpoint);

            // ===== PREPARE PAYLOAD =====
            String payload = (tc.body != null) ? tc.body.toString() : "";

            Response response;

            // ===== HANDLE CONTENT-TYPE =====
            if (tc.headers != null &&
                "application/x-www-form-urlencoded".equalsIgnoreCase(
                        String.valueOf(tc.headers.get("Content-Type")))) {

                // ===== FORM DATA =====
                if (tc.body instanceof Map) {
                    response = RestAssured
                            .given()
                            .baseUri(BASE_URL)
                            .headers(tc.headers)
                            .body(tc.body)
                            .when()
                            .request(tc.method, endpoint);
                } else {
                    // ❗ body không phải map → gửi raw
                    response = RestAssured
                            .given()
                            .baseUri(BASE_URL)
                            .headers(tc.headers)
                            .body(payload)
                            .when()
                            .request(tc.method, endpoint);
                }

            } else {

                // ===== JSON =====
                if (tc.body instanceof Map) {
                    response = RestAssured
                            .given()
                            .baseUri(BASE_URL)
                            .headers(tc.headers)
                            .body(tc.body)
                            .when()
                            .request(tc.method, endpoint);
                } else {
                    // ❗ body là string (invalid test)
                    response = RestAssured
                            .given()
                            .baseUri(BASE_URL)
                            .headers(tc.headers)
                            .body(payload)
                            .when()
                            .request(tc.method, endpoint);
                }
            }

            int status = response.getStatusCode();

            // ===== SAVE RESULT =====
            result.requestBody = payload;
            result.responseBody = response.asString();
            result.actualStatus = status;
            result.passed = (status == result.expectedStatus);

            // ===== ALLURE ATTACH =====
            Allure.addAttachment("Request", payload);
            Allure.addAttachment("Response", response.asString());
            Allure.addAttachment("Response Pretty", response.asPrettyString());
            Allure.addAttachment("Status Code", String.valueOf(status));

        } catch (Exception e) {

            result.passed = false;
            result.errorType = "runtime_error";
            result.actualStatus = 0;

            Allure.addAttachment("ERROR", e.toString());
        }

        return result;
    }
}
package executor;

import io.qameta.allure.Allure;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import model.TestCase;
import model.TestResult;

import java.util.Map;

public class ApiExecutor {

    public static TestResult execute(TestCase tc) {

        String BASE_URL = System.getProperty("baseUrl");

        if (BASE_URL == null || BASE_URL.isEmpty()) {
            throw new RuntimeException("Missing -DbaseUrl parameter");
        }

        TestResult result = new TestResult();

        result.id = tc.id;
        result.expectedStatus = tc.expectedStatus;

        try {

            String endpoint = tc.endpoint;

            // ===== REPLACE PATH PARAMS =====
            if (tc.pathParams != null) {

                for (Map.Entry<String, Object> entry
                        : tc.pathParams.entrySet()) {

                    endpoint = endpoint.replace(
                            "{" + entry.getKey() + "}",
                            String.valueOf(entry.getValue())
                    );
                }
            }

            Allure.step(
                    "Request: " +
                            tc.method +
                            " " +
                            endpoint
            );

            String payload =
                    (tc.body != null)
                            ? tc.body.toString()
                            : "";

            Response response;

            // ===== HANDLE FORM DATA =====
            if (tc.headers != null
                    && tc.headers.containsKey("Content-Type")
                    && tc.headers.get("Content-Type")
                    .equalsIgnoreCase(
                    "application/x-www-form-urlencoded")) {

                RequestSpecification request =
                        RestAssured
                                .given()
                                .baseUri(BASE_URL);

                // HEADERS
                if (tc.headers != null) {
                    request.headers(tc.headers);
                }

                // FORM PARAMS
                if (tc.body instanceof Map) {

                    request.formParams(
                            (Map<String, ?>) tc.body
                    );

                } else if (tc.body != null) {

                    request.body(payload);
                }
                System.out.println("BASE URL: " + BASE_URL);
                System.out.println("ENDPOINT: " + endpoint);
                System.out.println("FULL URL: " + BASE_URL + endpoint);
                response = request
                        .when()
                        .request(tc.method, endpoint);

            }

            // ===== NORMAL JSON =====
            else {

                RequestSpecification request =
                        RestAssured
                                .given()
                                .baseUri(BASE_URL);

                // HEADERS
                if (tc.headers != null) {
                    request.headers(tc.headers);
                }

                // BODY
                boolean allowBody =
                        tc.method.equalsIgnoreCase("POST")
                                || tc.method.equalsIgnoreCase("PUT")
                                || tc.method.equalsIgnoreCase("PATCH");

                if (allowBody && tc.body != null) {

                String contentType = "";

                if (tc.headers != null
                        && tc.headers.containsKey("Content-Type")) {

                        contentType = tc.headers.get("Content-Type");
                }

                // text/plain
                if (contentType.equalsIgnoreCase("text/plain")) {

                        request.body(tc.body.toString());

                }

                // json
                else {

                        request.body(tc.body);
                }
                }

                response = request
                        .when()
                        .request(tc.method, endpoint);
            }

            int status = response.getStatusCode();

            // ===== SAVE RESULT =====
            result.requestBody = payload;
            result.responseBody = response.asString();
            result.actualStatus = status;

            result.passed =
                    (status == result.expectedStatus);

            // ===== ALLURE =====
            Allure.addAttachment(
                    "Request",
                    payload
            );

            Allure.addAttachment(
                    "Response",
                    response.asString()
            );

            Allure.addAttachment(
                    "Response Pretty",
                    response.asPrettyString()
            );

            Allure.addAttachment(
                    "Status Code",
                    String.valueOf(status)
            );

        } catch (Exception e) {

            e.printStackTrace();

            result.passed = false;

            // runtime error
            result.actualStatus = 0;

            result.errorType = "runtime_error";

            Allure.addAttachment(
                    "ERROR",
                    e.toString()
            );
        }

        return result;
    }
}
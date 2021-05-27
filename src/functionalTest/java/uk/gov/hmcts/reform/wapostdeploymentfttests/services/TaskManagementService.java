package uk.gov.hmcts.reform.wapostdeploymentfttests.services;

import io.restassured.http.Headers;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapSerializer;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapValueExpander;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapValueExtractor;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static net.serenitybdd.rest.SerenityRest.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.SpringBootFunctionalBaseTest.DEFAULT_POLL_INTERVAL_SECONDS;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.SpringBootFunctionalBaseTest.DEFAULT_TIMEOUT_SECONDS;

@Service
public class TaskManagementService {

    @Autowired
    protected AuthorizationHeadersProvider authorizationHeadersProvider;

    @Autowired
    private MapValueExpander mapValueExpander;

    @Value("${wa_task_management_api.url}")
    private String taskManagementUrl;

    public String searchByCaseId(String scenarioSource,
                                 String caseId,
                                 Headers authorizationHeaders) throws IOException {


        Map<String, String> additionalValues = Map.of("caseId", caseId);

        Map<String, Object> scenario = deserializeWithExpandedValues(scenarioSource, additionalValues);

        final int expectedStatus = MapValueExtractor.extractOrDefault(scenario, "expectation.status", 200);
        final int expectedTasks = MapValueExtractor.extractOrDefault(
            scenario,
            "expectation.numberOfTasksAvailable",
            1
        );

        Map<String, Object> searchParameter = Map.of(
            "key", "caseId",
            "operator", "IN",
            "values", singletonList(caseId)
        );

        Map<String, List<Object>> requestBody = Map.of("search_parameters", singletonList(searchParameter));

        AtomicReference<String> actualResponseBody = new AtomicReference<>();
        await().ignoreException(AssertionError.class)
            .pollInterval(DEFAULT_POLL_INTERVAL_SECONDS, SECONDS)
            .atMost(DEFAULT_TIMEOUT_SECONDS, SECONDS)
            .until(
                () -> {

                    Response result = given()
                        .headers(authorizationHeaders)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .body(requestBody)
                        .when()
                        .post(taskManagementUrl + "/task");

                    result.then().assertThat()
                        .statusCode(expectedStatus)
                        .contentType(APPLICATION_JSON_VALUE)
                        .body("tasks.size()", is(expectedTasks));

                    actualResponseBody.set(
                        result.then()
                            .extract()
                            .body().asString()
                    );

                    return true;
                });


        System.out.println("Response body: " + actualResponseBody);

        return actualResponseBody.get();
    }


    private Map<String, Object> deserializeWithExpandedValues(String source,
                                                              Map<String, String> additionalValues) throws IOException {
        Map<String, Object> data = MapSerializer.deserialize(source);
        mapValueExpander.expandValues(data, additionalValues);
        return data;
    }

}

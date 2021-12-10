package uk.gov.hmcts.reform.wapostdeploymentfttests.services;

import io.restassured.http.Headers;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.DeserializeValuesUtil;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapValueExpander;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapValueExtractor;

import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static net.serenitybdd.rest.SerenityRest.given;
import static org.hamcrest.CoreMatchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Service
@Slf4j
public class TaskManagementService {

    @Autowired
    protected AuthorizationHeadersProvider authorizationHeadersProvider;

    @Autowired
    private MapValueExpander mapValueExpander;
    @Autowired
    private DeserializeValuesUtil deserializeValuesUtil;
    @Autowired
    private TaskMonitorService taskMonitorService;

    @Value("${wa_task_management_api.url}")
    private String taskManagementUrl;

    public String searchByCaseId(Map<String, Object> clauseValues,
                                 String caseId,
                                 Headers authorizationHeaders) {

        int expectedStatus = MapValueExtractor.extractOrDefault(
            clauseValues, "expectation.status", 200);
        int expectedTasks = MapValueExtractor.extractOrDefault(
            clauseValues, "expectation.numberOfTasksAvailable", 1);

        Map<String, Object> searchParameter = Map.of(
            "key", "caseId",
            "operator", "IN",
            "values", singletonList(caseId)
        );

        Map<String, List<Object>> requestBody = Map.of("search_parameters", singletonList(searchParameter));


        //Also trigger (CRON) Jobs programmatically
        taskMonitorService.triggerInitiationJob(authorizationHeaders);
        taskMonitorService.triggerTerminationJob(authorizationHeaders);

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


        String actualResponseBody = result.then()
            .extract()
            .body().asString();

        log.info("Response body: " + actualResponseBody);

        return actualResponseBody;
    }

}

package uk.gov.hmcts.reform.wapostdeploymentfttests.domain.taskretriever;

import groovy.transform.ToString;
import io.restassured.http.Headers;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Map;

@ToString
@EqualsAndHashCode(callSuper = false)
@Getter
public class CamundaTaskRetrievableParameter extends TaskRetrievableParameter {
    private final Map<String, Object> scenario;
    private final Headers requestAuthorizationHeaders;
    private final String testCaseId;

    public CamundaTaskRetrievableParameter(Map<String, Object> scenario,
                                           Headers requestAuthorizationHeaders,
                                           String testCaseId) {
        this.scenario = scenario;
        this.requestAuthorizationHeaders = requestAuthorizationHeaders;
        this.testCaseId = testCaseId;
    }
}

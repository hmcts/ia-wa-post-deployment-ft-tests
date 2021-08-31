package uk.gov.hmcts.reform.wapostdeploymentfttests.domain.taskretriever;

import groovy.transform.ToString;
import io.restassured.http.Headers;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Map;

@ToString
@EqualsAndHashCode(callSuper = false)
@Getter
public class TaskMgmApiRetrievableParameter extends TaskRetrievableParameter {

    private final Map<String, Object> scenario;
    private final String scenarioSource;
    private final String testCaseId;
    private final Headers requestAuthorizationHeaders;

    public TaskMgmApiRetrievableParameter(Map<String, Object> scenario,
                                          String scenarioSource,
                                          String testCaseId,
                                          Headers requestAuthorizationHeaders) {
        this.scenario = scenario;
        this.scenarioSource = scenarioSource;
        this.testCaseId = testCaseId;
        this.requestAuthorizationHeaders = requestAuthorizationHeaders;
    }
}

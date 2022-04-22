package uk.gov.hmcts.reform.wapostdeploymentfttests.domain;

import io.restassured.http.Headers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class TestScenario {

    private final Map<String, Object> scenarioMapValues;
    private final String scenarioSource;
    private final Map<String, Object> beforeClauseValues;
    private final Map<String, Object> testClauseValues;
    private final String jurisdiction;
    private final String caseType;

    private final Map<String, String> caseIdMap;
    private Headers requestAuthorizationHeaders;
    private Headers expectationAuthorizationHeaders;

    public TestScenario(@NotNull Map<String, Object> scenarioMapValues,
                        @NotNull String scenarioSource,
                        @NotNull String jurisdiction,
                        @NotNull String caseType,
                        @Nullable Map<String, Object> beforeClauseValues,
                        @NotNull Map<String, Object> testClauseValues) {
        this.scenarioMapValues = scenarioMapValues;
        this.scenarioSource = scenarioSource;
        this.jurisdiction = jurisdiction;
        this.caseType = caseType;
        this.beforeClauseValues = beforeClauseValues;
        this.testClauseValues = testClauseValues;
        this.caseIdMap = new HashMap<>();
    }

    public Map<String, Object> getScenarioMapValues() {
        return scenarioMapValues;
    }

    @NotNull
    public String getScenarioSource() {
        return scenarioSource;
    }


    public void addAssignedCaseId(String key, String caseId) {
        caseIdMap.put(key, caseId);
    }

    public String getAssignedCaseId(String key) {
        return caseIdMap.get(key);
    }

    public String getCaseType() {
        return caseType;
    }

    public Headers getRequestAuthorizationHeaders() {
        return requestAuthorizationHeaders;
    }

    public void setRequestAuthorizationHeaders(Headers requestAuthorizationHeaders) {
        this.requestAuthorizationHeaders = requestAuthorizationHeaders;
    }

    public Headers getExpectationAuthorizationHeaders() {
        return expectationAuthorizationHeaders;
    }

    public void setExpectationAuthorizationHeaders(Headers expectationAuthorizationHeaders) {
        this.expectationAuthorizationHeaders = expectationAuthorizationHeaders;
    }

    public Map<String, Object> getBeforeClauseValues() {
        return beforeClauseValues;
    }

    public Map<String, Object> getTestClauseValues() {
        return testClauseValues;
    }

    public String getJurisdiction() {
        return jurisdiction;
    }
}

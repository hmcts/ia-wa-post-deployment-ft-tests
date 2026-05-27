package uk.gov.hmcts.reform.wapostdeploymentfttests.domain;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapValueExtractor.extractOrDefault;

@Slf4j
public class TestScenario {

    @Getter
    private final Map<String, Object> scenarioMapValues;
    private final String scenarioSource;
    @Getter
    private final Map<String, Object> beforeClauseValues;
    @Getter
    private final Map<String, Object> testClauseValues;
    @Getter
    private final Map<String, Object> postRoleAssignmentClauseValues;
    @Getter
    private final Map<String, Object> updateCaseClauseValues;
    @Getter
    private final String jurisdiction;
    @Getter
    private final String caseType;

    @Getter
    private final List<String> taskIds;
    @Setter
    @Getter
    private String assigneeId;
    private final Map<String, String> caseIdMap;
    @Getter
    private final Set<Map<String, Object>> searchMap;

    public TestScenario(@NonNull Map<String, Object> scenarioMapValues,
                        @NonNull String scenarioSource,
                        @NonNull String jurisdiction,
                        @NonNull String caseType,
                        @Nullable Map<String, Object> beforeClauseValues,
                        @NonNull Map<String, Object> testClauseValues,
                        @Nullable Map<String, Object> postRoleAssignmentClauseValues,
                        @Nullable Map<String, Object> updateCaseClauseValues) {
        this.scenarioMapValues = scenarioMapValues;
        this.scenarioSource = scenarioSource;
        this.jurisdiction = jurisdiction;
        this.caseType = caseType;
        this.beforeClauseValues = beforeClauseValues;
        this.testClauseValues = testClauseValues;
        this.postRoleAssignmentClauseValues = postRoleAssignmentClauseValues;
        this.updateCaseClauseValues = updateCaseClauseValues;
        this.caseIdMap = new HashMap<>();
        this.searchMap = new HashSet<>();
        this.taskIds = new ArrayList<>();
    }

    @NonNull
    public String getScenarioSource() {
        return scenarioSource;
    }


    public void addAssignedCaseId(String key, String caseId) {
        caseIdMap.putIfAbsent(key, caseId);
    }

    public String getAssignedCaseId(String key) {
        String description = extractOrDefault(scenarioMapValues, "description", "Unnamed scenario");
        log.info("Getting caseId for key {} from scenario {}", key, description);
        return caseIdMap.get(key);
    }

    public Map<String, String> getAssignedCaseIdMap() {
        return caseIdMap;
    }

    public void addSearchMap(Map<String, Object> map) {
        searchMap.add(map);
    }

    public void addTaskId(String taskId) {
        assertNotNull(taskId);
        taskIds.add(taskId);
    }

}

package uk.gov.hmcts.reform.wapostdeploymentfttests.domain;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;

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
    private final String fileName;

    @Getter
    private final List<String> taskIds;
    @Setter
    @Getter
    private String assigneeId;
    @Getter
    private final Set<Map<String, Object>> searchMap;
    @Getter
    @Setter
    private String assignedCaseId;

    public TestScenario(@NonNull Map<String, Object> scenarioMapValues,
                        @NonNull String scenarioSource,
                        @NonNull String jurisdiction,
                        @NonNull String caseType,
                        @Nullable Map<String, Object> beforeClauseValues,
                        @NonNull Map<String, Object> testClauseValues,
                        @Nullable Map<String, Object> postRoleAssignmentClauseValues,
                        @Nullable Map<String, Object> updateCaseClauseValues,
                        @NonNull String fileName) {
        this.scenarioMapValues = scenarioMapValues;
        this.scenarioSource = scenarioSource;
        this.jurisdiction = jurisdiction;
        this.caseType = caseType;
        this.beforeClauseValues = beforeClauseValues;
        this.testClauseValues = testClauseValues;
        this.postRoleAssignmentClauseValues = postRoleAssignmentClauseValues;
        this.updateCaseClauseValues = updateCaseClauseValues;
        this.searchMap = new HashSet<>();
        this.taskIds = new ArrayList<>();
        this.fileName = fileName;
        this.assignedCaseId = null;
    }

    public TestScenario(TestScenario other) {
        this.scenarioMapValues = deepCopy(other.scenarioMapValues);
        this.scenarioSource = other.scenarioSource;
        this.beforeClauseValues = deepCopy(other.beforeClauseValues);
        this.testClauseValues = deepCopy(other.testClauseValues);
        this.postRoleAssignmentClauseValues = deepCopy(other.postRoleAssignmentClauseValues);
        this.updateCaseClauseValues = deepCopy(other.updateCaseClauseValues);
        this.jurisdiction = other.jurisdiction;
        this.caseType = other.caseType;
        this.fileName = other.fileName;
        this.taskIds = new ArrayList<>();
        this.taskIds.addAll(other.taskIds);
        this.assigneeId = other.assigneeId;
        this.searchMap = new HashSet<>();
        this.searchMap.addAll(deepCopy(other.searchMap));
        this.assignedCaseId = other.assignedCaseId;
    }

    @NonNull
    public String getScenarioSource() {
        return scenarioSource;
    }

    public void addSearchMap(Map<String, Object> map) {
        searchMap.add(map);
    }

    public void addTaskId(String taskId) {
        assertNotNull(taskId);
        taskIds.add(taskId);
    }

    @SuppressWarnings("unchecked")
    public static <T> T deepCopy(T obj) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(obj);

            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            ObjectInputStream in = new ObjectInputStream(bis);

            return (T) in.readObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

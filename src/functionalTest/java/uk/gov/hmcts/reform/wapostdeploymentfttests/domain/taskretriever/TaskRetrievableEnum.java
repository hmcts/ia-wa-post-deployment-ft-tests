package uk.gov.hmcts.reform.wapostdeploymentfttests.domain.taskretriever;

import lombok.Getter;

@Getter
public enum TaskRetrievableEnum {
    CAMUNDA_API("camunda-api"), TASK_MGM_API("task-management-api");

    private final String id;

    TaskRetrievableEnum(String id) {
        this.id = id;
    }
}

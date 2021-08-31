package uk.gov.hmcts.reform.wapostdeploymentfttests.services.taskretriever;

import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.taskretriever.TaskRetrievableParameter;

public interface TaskRetrieverService<T extends TaskRetrievableParameter> {

    void retrieveTask(T taskRetrievableParameter);
}

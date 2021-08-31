package uk.gov.hmcts.reform.wapostdeploymentfttests.services.taskretriever;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wapostdeploymentfttests.clients.CamundaClient;
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.entities.CamundaTask;
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.taskretriever.CamundaTaskRetrievableParameter;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapValueExtractor;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.SpringBootFunctionalBaseTest.DEFAULT_POLL_INTERVAL_SECONDS;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.SpringBootFunctionalBaseTest.DEFAULT_TIMEOUT_SECONDS;

@Component
@Slf4j
public class CamundaTaskRetrieverService implements TaskRetrieverService<CamundaTaskRetrievableParameter> {

    private final CamundaClient camundaClient;

    public CamundaTaskRetrieverService(CamundaClient camundaClient) {
        this.camundaClient = camundaClient;
    }

    @Override
    public void retrieveTask(CamundaTaskRetrievableParameter camundaTaskRetrievableParameter) {
        String jurisdiction = MapValueExtractor.extract(
            camundaTaskRetrievableParameter.getScenario(),
            "jurisdiction"
        );
        String caseType = MapValueExtractor.extract(
            camundaTaskRetrievableParameter.getScenario(),
            "caseType"
        );
        String numberOfTasksAvailable = MapValueExtractor.extractOrDefault(
            camundaTaskRetrievableParameter.getScenario(),
            "expectations.numberOfTasksAvailable",
            "1"
        );
        AtomicReference<List<CamundaTask>> camundaTaskList = new AtomicReference<>();
        await().ignoreException(AssertionError.class)
            .pollInterval(DEFAULT_POLL_INTERVAL_SECONDS, SECONDS)
            .atMost(DEFAULT_TIMEOUT_SECONDS, SECONDS)
            .until(
                () -> {
                    camundaTaskList.set(camundaClient.getTasksByTaskVariables(
                        camundaTaskRetrievableParameter
                            .getRequestAuthorizationHeaders().getValue("ServiceAuthorization"),
                        "caseId_eq_" + camundaTaskRetrievableParameter.getTestCaseId()
                        + ",jurisdiction_eq_" + jurisdiction
                        + ",caseTypeId_eq_" + caseType,
                        "created",
                        "desc"
                    ));
                    return (camundaTaskList.get().size() == Integer.parseInt(numberOfTasksAvailable));
                });
        log.info("camunda task retrieved successfully: " + camundaTaskList);
    }
}

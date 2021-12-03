package uk.gov.hmcts.reform.wapostdeploymentfttests.services.taskretriever;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.core.ConditionEvaluationLogger;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.TestScenario;
import uk.gov.hmcts.reform.wapostdeploymentfttests.services.TaskManagementService;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.DeserializeValuesUtil;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapMerger;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapSerializer;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapValueExtractor;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.StringResourceLoader;
import uk.gov.hmcts.reform.wapostdeploymentfttests.verifiers.Verifier;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.SpringBootFunctionalBaseTest.DEFAULT_POLL_INTERVAL_SECONDS;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.SpringBootFunctionalBaseTest.DEFAULT_TIMEOUT_SECONDS;

@Component
@Slf4j
public class TaskMgmApiRetrieverService implements TaskRetrieverService {

    private final TaskManagementService taskManagementService;
    private final DeserializeValuesUtil deserializeValuesUtil;
    private final List<Verifier> verifiers;

    public TaskMgmApiRetrieverService(TaskManagementService taskManagementService,
                                      DeserializeValuesUtil deserializeValuesUtil,
                                      List<Verifier> verifiers) {
        this.taskManagementService = taskManagementService;
        this.deserializeValuesUtil = deserializeValuesUtil;
        this.verifiers = verifiers;
    }

    @SneakyThrows
    @Override
    public void retrieveTask(Map<String, Object> clauseValues, TestScenario scenario) {

        Map<String, String> taskTemplatesByFilename =
            StringResourceLoader.load(
                "/templates/" + scenario.getJurisdiction().toLowerCase(Locale.ENGLISH) + "/task/*.json"
            );
        Map<String, String> additionalValues = Map.of("caseId", scenario.getCaseId());

        Map<String, Object> deserializedClauseValues =
            deserializeValuesUtil.expandMapValues(clauseValues, additionalValues);

        await()
            .ignoreException(AssertionError.class)
            .conditionEvaluationListener(new ConditionEvaluationLogger(log::info))
            .pollInterval(DEFAULT_POLL_INTERVAL_SECONDS, SECONDS)
            .atMost(DEFAULT_TIMEOUT_SECONDS, SECONDS)
            .until(
                () -> {

                    String actualResponseBody = taskManagementService.searchByCaseId(
                        deserializedClauseValues,
                        scenario.getCaseId(),
                        scenario.getExpectationAuthorizationHeaders()
                    );

                    String expectedResponseBody = buildTaskExpectationResponseBody(
                        deserializedClauseValues,
                        taskTemplatesByFilename,
                        Map.of("caseId", scenario.getCaseId())
                    );

                    Map<String, Object> actualResponse = MapSerializer.deserialize(actualResponseBody);
                    Map<String, Object> expectedResponse = MapSerializer.deserialize(expectedResponseBody);

                    verifiers.forEach(verifier ->
                        verifier.verify(
                            clauseValues,
                            expectedResponse,
                            actualResponse
                        )
                    );
                    return true;
                });
    }

    private String buildTaskExpectationResponseBody(Map<String, Object> clauseValues,
                                                    Map<String, String> taskTemplatesByFilename,
                                                    Map<String, String> additionalValues) throws IOException {

        Map<String, Object> scenario = deserializeValuesUtil.expandMapValues(clauseValues, additionalValues);

        Map<String, Object> expectation = MapValueExtractor.extract(scenario, "expectation");
        Map<String, Object> taskData = MapValueExtractor.extract(expectation, "taskData");

        String templateFilename = MapValueExtractor.extract(taskData, "template");

        Map<String, Object> taskDataExpectation = deserializeValuesUtil.deserializeStringWithExpandedValues(
            taskTemplatesByFilename.get(templateFilename),
            additionalValues
        );

        Map<String, Object> taskDataDataReplacements = MapValueExtractor.extract(taskData, "replacements");
        if (taskDataDataReplacements != null) {
            MapMerger.merge(taskDataExpectation, taskDataDataReplacements);
        }

        return MapSerializer.serialize(taskDataExpectation);

    }
}

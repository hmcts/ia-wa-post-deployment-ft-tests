package uk.gov.hmcts.reform.wapostdeploymentfttests.services.taskretriever;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.taskretriever.TaskMgmApiRetrievableParameter;
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

@Component
@Slf4j
public class TaskMgmApiRetrieverService implements TaskRetrieverService<TaskMgmApiRetrievableParameter> {

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
    public void retrieveTask(TaskMgmApiRetrievableParameter taskMgmApiRetrievableParameter) {
        final String scenarioJurisdiction = MapValueExtractor.extractOrThrow(
            taskMgmApiRetrievableParameter.getScenario(),
            "jurisdiction"
        );

        Map<String, String> taskTemplatesByFilename =
            StringResourceLoader.load(
                "/templates/" + scenarioJurisdiction.toLowerCase(Locale.ENGLISH) + "/task/*.json"
            );
        String actualResponseBody;
        try {
            actualResponseBody = taskManagementService.searchByCaseId(
                taskMgmApiRetrievableParameter.getScenarioSource(),
                taskMgmApiRetrievableParameter.getTestCaseId(),
                taskMgmApiRetrievableParameter.getRequestAuthorizationHeaders()
            );
        } catch (Exception e) {
            throw new RuntimeException("Error using the taskManagementService service:", e);
        }

        String expectedResponseBody = buildTaskExpectationResponseBody(
            taskMgmApiRetrievableParameter.getScenarioSource(),
            taskTemplatesByFilename,
            Map.of("caseId", taskMgmApiRetrievableParameter.getTestCaseId())
        );

        Map<String, Object> actualResponse = MapSerializer.deserialize(actualResponseBody);
        Map<String, Object> expectedResponse = MapSerializer.deserialize(expectedResponseBody);

        verifiers.forEach(verifier ->
                              verifier.verify(
                                  taskMgmApiRetrievableParameter.getScenario(),
                                  expectedResponse,
                                  actualResponse
                              )
        );

    }

    private String buildTaskExpectationResponseBody(String scenarioSource,
                                                    Map<String, String> taskTemplatesByFilename,
                                                    Map<String, String> additionalValues) throws IOException {

        Map<String, Object> scenario = deserializeValuesUtil.deserializeWithExpandedValues(
            scenarioSource,
            additionalValues
        );

        Map<String, Object> expectation = MapValueExtractor.extract(scenario, "expectation");
        Map<String, Object> taskData = MapValueExtractor.extract(expectation, "taskData");

        String templateFilename = MapValueExtractor.extract(taskData, "template");

        Map<String, Object> taskDataExpectation = deserializeValuesUtil.deserializeWithExpandedValues(
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

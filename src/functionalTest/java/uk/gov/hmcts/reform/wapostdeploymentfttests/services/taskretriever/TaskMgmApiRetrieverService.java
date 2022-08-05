package uk.gov.hmcts.reform.wapostdeploymentfttests.services.taskretriever;

import com.fasterxml.jackson.databind.JsonNode;
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
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
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

    private static final DateTimeFormatter CREATE_DATE_TIME_PATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss+SSSS");

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
    public void retrieveTask(Map<String, Object> clauseValues, TestScenario scenario, String caseId) {

        Map<String, String> taskTemplatesByFilename =
            StringResourceLoader.load(
                "/templates/" + scenario.getJurisdiction().toLowerCase(Locale.ENGLISH) + "/task/*.json"
            );

        Map<String, String> additionalValues = Map.of("caseId", caseId);

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
                        caseId,
                        scenario.getExpectationAuthorizationHeaders()
                    );

                    String expectedResponseBody = buildTaskExpectationResponseBody(
                        deserializedClauseValues,
                        taskTemplatesByFilename,
                        Map.of("caseId", caseId)
                    );

                    Map<String, Object> actualResponse = MapSerializer.deserialize(
                        MapSerializer.sortCollectionElement(actualResponseBody,"tasks", taskTitleComparator()));
                    Map<String, Object> expectedResponse = MapSerializer.deserialize(expectedResponseBody);

                    verifiers.forEach(verifier ->
                        verifier.verify(
                            clauseValues,
                            expectedResponse,
                            actualResponse
                        )
                    );

                    List<Map<String, Object>> tasks = MapValueExtractor.extract(actualResponse, "tasks");
                    String taskId = MapValueExtractor.extract(tasks.get(0), "id");
                    log.info("task id is {}", taskId);

                    String actualRoleResponseBody = taskManagementService.retrieveTaskRolePermissions(
                        clauseValues,
                        taskId,
                        scenario.getExpectationAuthorizationHeaders()
                    );


                    String rolesExpectationResponseBody = buildRolesExpectationResponseBody(
                        deserializedClauseValues,
                        Map.of("caseId", caseId)
                    );

                    log.info("expected roles: {}", rolesExpectationResponseBody);
                    Map<String, Object> actualRoleResponse = MapSerializer.deserialize(actualRoleResponseBody);
                    Map<String, Object> expectedRoleResponse = MapSerializer.deserialize(rolesExpectationResponseBody);

                    verifiers.forEach(verifier ->
                                          verifier.verify(
                                              clauseValues,
                                              expectedRoleResponse,
                                              actualRoleResponse
                                          )
                    );

                    return true;
                });
    }

    private Comparator<JsonNode> taskTitleComparator() {
        return (j1, j2) -> {
            String title1 = j1.findValue("task_title").asText();
            String title2 = j2.findValue("task_title").asText();;
            return title1.compareTo(title2);
        };
    }

    private String buildTaskExpectationResponseBody(Map<String, Object> clauseValues,
                                                    Map<String, String> taskTemplatesByFilename,
                                                    Map<String, String> additionalValues) throws IOException {

        Map<String, Object> scenario = deserializeValuesUtil.expandMapValues(clauseValues, additionalValues);

        Map<String, Object> taskData = MapValueExtractor.extract(scenario, "taskData");

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

    private String buildRolesExpectationResponseBody(Map<String, Object> clauseValues,
                                                    Map<String, String> additionalValues) throws IOException {

        Map<String, Object> scenario = deserializeValuesUtil.expandMapValues(clauseValues, additionalValues);
        Map<String, Object> roleData = MapValueExtractor.extract(scenario, "roleData");
        return MapSerializer.serialize(roleData);
    }
}

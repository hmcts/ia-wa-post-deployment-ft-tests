package uk.gov.hmcts.reform.wapostdeploymentfttests;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.RetryableException;
import io.restassured.http.Headers;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.core.ConditionEvaluationLogger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.util.StopWatch;
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.TestRequestType;
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.TestScenario;
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.entities.idam.CredentialRequest;
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.entities.idam.UserInfo;
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.taskretriever.TaskRetrieverEnum;
import uk.gov.hmcts.reform.wapostdeploymentfttests.preparers.Preparer;
import uk.gov.hmcts.reform.wapostdeploymentfttests.services.AuthorizationHeadersProvider;
import uk.gov.hmcts.reform.wapostdeploymentfttests.services.CcdCaseCreator;
import uk.gov.hmcts.reform.wapostdeploymentfttests.services.MessageInjector;
import uk.gov.hmcts.reform.wapostdeploymentfttests.services.RestMessageService;
import uk.gov.hmcts.reform.wapostdeploymentfttests.services.RoleAssignmentService;
import uk.gov.hmcts.reform.wapostdeploymentfttests.services.TaskManagementService;
import uk.gov.hmcts.reform.wapostdeploymentfttests.services.taskretriever.CamundaTaskRetrieverService;
import uk.gov.hmcts.reform.wapostdeploymentfttests.services.taskretriever.TaskMgmApiRetrieverService;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.CaseIdUtil;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.DeserializeValuesUtil;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.JsonUtil;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapSerializer;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapValueExtractor;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.StringResourceLoader;
import uk.gov.hmcts.reform.wapostdeploymentfttests.verifiers.TaskDataVerifier;
import uk.gov.hmcts.reform.wapostdeploymentfttests.verifiers.Verifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Collections.emptyMap;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.services.AuthorizationHeadersProvider.AUTHORIZATION;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.services.AuthorizationHeadersProvider.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.CaseIdUtil.addAssignedCaseId;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapValueExpander.ENVIRONMENT_PROPERTIES;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapValueExtractor.extractOrDefault;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapValueExtractor.extractOrThrow;

@Slf4j
@SpringBootTest(classes = Application.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ScenarioRunnerTest extends SpringBootFunctionalBaseTest {

    protected final MessageInjector messageInjector;
    protected final TaskDataVerifier taskDataVerifier;
    protected final AuthorizationHeadersProvider authorizationHeadersProvider;
    private final CamundaTaskRetrieverService camundaTaskRetrievableService;
    private final TaskMgmApiRetrieverService taskMgmApiRetrievableService;
    private final RoleAssignmentService roleAssignmentService;
    private final TaskManagementService taskManagementService;
    private final Environment environment;
    private final DeserializeValuesUtil deserializeValuesUtil;
    private final ObjectMapper objectMapper;
    private final List<Verifier> verifiers;
    private final List<Preparer> preparers;
    private final CcdCaseCreator ccdCaseCreator;
    private final RestMessageService restMessageService;

    private final Map<String, String> scenarioSources = new HashMap<>();

    @Autowired
    public ScenarioRunnerTest(
        MessageInjector messageInjector,
        TaskDataVerifier taskDataVerifier,
        AuthorizationHeadersProvider authorizationHeadersProvider,
        CamundaTaskRetrieverService camundaTaskRetrievableService,
        TaskMgmApiRetrieverService taskMgmApiRetrievableService,
        RoleAssignmentService roleAssignmentService,
        TaskManagementService taskManagementService,
        Environment environment,
        DeserializeValuesUtil deserializeValuesUtil,
        ObjectMapper objectMapper,
        List<Verifier> verifiers,
        List<Preparer> preparers,
        CcdCaseCreator ccdCaseCreator,
        RestMessageService restMessageService
    ) {
        this.messageInjector = messageInjector;
        this.taskDataVerifier = taskDataVerifier;
        this.authorizationHeadersProvider = authorizationHeadersProvider;
        this.camundaTaskRetrievableService = camundaTaskRetrievableService;
        this.taskMgmApiRetrievableService = taskMgmApiRetrievableService;
        this.roleAssignmentService = roleAssignmentService;
        this.taskManagementService = taskManagementService;
        this.environment = environment;
        this.deserializeValuesUtil = deserializeValuesUtil;
        this.objectMapper = objectMapper;
        this.verifiers = verifiers;
        this.preparers = preparers;
        this.ccdCaseCreator = ccdCaseCreator;
        this.restMessageService = restMessageService;
    }

    @BeforeAll
    public void beforeAll() throws Exception {
        loadPropertiesIntoMapValueExpander();

        for (Preparer preparer : preparers) {
            preparer.prepare();
        }

        assertFalse(verifiers.isEmpty(), "Verifiers configured successfully");

        MapSerializer.setObjectMapper(objectMapper);
        JsonUtil.setObjectMapper(objectMapper);
        String scenarioPattern = System.getProperty("scenario");
        if (scenarioPattern == null) {
            scenarioPattern = "*.json";
        } else {
            scenarioPattern = "*" + scenarioPattern + "*.json";
        }
        scenarioSources.putAll(StringResourceLoader.load("/scenarios/ia/" + scenarioPattern));
        log.info("-------------------------------------------------------------------");
        log.info("⚙️️ FOUND {} SCENARIOS", scenarioSources.size());
        log.info("-------------------------------------------------------------------");
    }

    @Execution(ExecutionMode.CONCURRENT)
    @ParameterizedTest(name = "{0}:{1}")
    @MethodSource("caseTypeScenarios")
    public void scenarios_should_behave_as_specified(String fileName,
                                                     String description,
                                                     int counter,
                                                     TestScenario scenario,
                                                     Map<String, Object> scenarioValues) throws Exception {
        assumeFalse(fileName.startsWith("Disabled:"), "ℹ️ SCENARIO: " + description + " **disabled**");
        Thread.sleep(counter);
        int retryCount = 5;
        for (int i = 0; i < retryCount; i++) {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            try {
                createBaseCcdCase(scenario);

                addSearchParameters(scenario, scenarioValues);

                if (scenario.getBeforeClauseValues() != null) {
                    log.info("ℹ️ SCENARIO: Found BEFORE Clause processing setup scenario");
                    processBeforeClauseScenario(scenario);
                }

                if (scenario.getPostRoleAssignmentClauseValues() != null) {
                    log.info("ℹ️ SCENARIO: POST_ROLE_ASSIGNMENTS Clause found");
                    processRoleAssignment(scenario.getPostRoleAssignmentClauseValues(), scenario);
                }

                if (scenario.getUpdateCaseClauseValues() != null) {
                    log.info("ℹ️ SCENARIO: Update case Clause found");
                    updateBaseCcdCase(scenario);
                }

                processTestClauseScenario(scenario);
                stopWatch.stop();
                log.info("✅ SCENARIO {}: Total time taken to complete test {} seconds", description,
                         stopWatch.getTotalTimeSeconds());
                break;
            } catch (Error | RetryableException | NullPointerException e) {
                stopWatch.stop();
                log.error("Scenario failed after {} seconds with error {}",
                          stopWatch.getTotalTimeSeconds(), e.getMessage());
                log.info("Retrying scenario. Attempt {}/{}", i + 1, retryCount);
                if (i == retryCount - 1) {
                    throw e;
                }
            }
        }
    }

    private Stream<Arguments> caseTypeScenarios() {
        return scenarioSources.entrySet().stream().map(entry -> {
            String fileName = entry.getKey();
            String scenarioSource = entry.getValue();
            try {
                Map<String, Object> scenarioValues = deserializeValuesUtil
                    .deserializeStringWithExpandedValues(scenarioSource, emptyMap());

                String description = extractOrDefault(scenarioValues, "description", "Unnamed scenario");
                Object scenarioDisabled = MapValueExtractor.extractOrDefault(scenarioValues, "disabled", false);
                if (Boolean.parseBoolean(scenarioDisabled.toString())) {
                    return Arguments.of("Disabled: " + fileName, description, null, null);
                }

                log.info("ℹ️ SCENARIO {}", description);

                Map<String, Object> beforeClauseValues = extractOrDefault(scenarioValues, "before", null);
                Map<String, Object> testClauseValues = Objects.requireNonNull(
                    MapValueExtractor.extract(scenarioValues, "test"));
                Map<String, Object> postRoleAssignmentClauseValues = extractOrDefault(
                    scenarioValues,
                    "postRoleAssignments", null
                );
                Map<String, Object> updateCaseClauseValues = extractOrDefault(
                    scenarioValues,
                    "updateCase",
                    null
                );

                String scenarioJurisdiction = extractOrThrow(scenarioValues, "jurisdiction");
                String caseType = extractOrThrow(scenarioValues, "caseType");

                TestScenario scenario = new TestScenario(
                    scenarioValues,
                    scenarioSource,
                    scenarioJurisdiction,
                    caseType,
                    beforeClauseValues,
                    testClauseValues,
                    postRoleAssignmentClauseValues,
                    updateCaseClauseValues
                );
                return Arguments.of(
                    fileName,
                    description,
                    Math.floor(Math.random() * 6 * 500),
                    scenario,
                    scenarioValues
                );
            } catch (IOException e) {
                log.info("Failed to load scenario: {}", String.valueOf(e));
                return null;
            }
        });
    }

    private void processRoleAssignment(Map<String, Object> postRoleAssignmentClauseValues, TestScenario scenario)
        throws IOException {
        Map<String, Object> postRoleAssignmentValues = scenario.getPostRoleAssignmentClauseValues();

        CredentialRequest credentialRequest = extractCredentialRequest(postRoleAssignmentValues, "credentials");
        Headers requestAuthorizationHeaders = authorizationHeadersProvider.getIaUserAuthorization(credentialRequest);

        String userToken = requestAuthorizationHeaders.getValue(AUTHORIZATION);
        String serviceToken = requestAuthorizationHeaders.getValue(SERVICE_AUTHORIZATION);
        UserInfo userInfo = authorizationHeadersProvider.getUserInfo(userToken);

        roleAssignmentService.processRoleAssignments(
            scenario,
            postRoleAssignmentClauseValues, userToken, serviceToken, userInfo
        );
    }

    private void processBeforeClauseScenario(TestScenario scenario) throws Exception {
        processScenario(scenario.getBeforeClauseValues(), scenario);
    }

    private void processTestClauseScenario(TestScenario scenario) throws Exception {
        processScenario(scenario.getTestClauseValues(), scenario);
    }

    private void createBaseCcdCase(TestScenario scenario) throws IOException {
        Map<String, Object> scenarioValues = scenario.getScenarioMapValues();

        CredentialRequest credentialRequest = extractCredentialRequest(scenarioValues, "required.credentials");
        Headers requestAuthorizationHeaders = authorizationHeadersProvider.getIaUserAuthorization(credentialRequest);

        List<Map<String, Object>> ccdCaseToCreate = new ArrayList<>(Objects.requireNonNull(
            MapValueExtractor.extract(scenarioValues, "required.ccd")));

        ccdCaseToCreate.forEach(caseValues -> {
            try {
                String caseId = ccdCaseCreator.createCase(
                    caseValues,
                    scenario.getJurisdiction(),
                    scenario.getCaseType(),
                    requestAuthorizationHeaders
                );
                addAssignedCaseId(caseValues, caseId, scenario);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void updateBaseCcdCase(TestScenario scenario) throws IOException {
        Map<String, Object> scenarioValues = scenario.getScenarioMapValues();

        CredentialRequest credentialRequest = extractCredentialRequest(scenarioValues, "updateCase.credentials");
        Headers requestAuthorizationHeaders = authorizationHeadersProvider.getIaUserAuthorization(credentialRequest);

        List<Map<String, Object>> ccdCaseToUpdate = new ArrayList<>(Objects.requireNonNull(
            MapValueExtractor.extract(scenarioValues, "updateCase.ccd")));

        ccdCaseToUpdate.forEach(caseValues -> {
            try {
                String caseId = CaseIdUtil.extractAssignedCaseIdOrDefault(caseValues, scenario);
                ccdCaseCreator.updateCase(
                    caseId,
                    caseValues,
                    scenario.getJurisdiction(),
                    scenario.getCaseType(),
                    requestAuthorizationHeaders
                );
                addAssignedCaseId(caseValues, caseId, scenario);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void processScenario(Map<String, Object> values,
                                 TestScenario scenario) throws Exception {
        processTestRequest(values, scenario);

        String taskRetrieverOption = MapValueExtractor.extract(
            scenario.getScenarioMapValues(),
            "options.taskRetrievalApi"
        );

        List<Map<String, Object>> expectations = new ArrayList<>(Objects.requireNonNull(
            MapValueExtractor.extract(values, "expectations")));

        for (Map<String, Object> expectationValue : expectations) {
            int expectedTasks = extractOrDefault(
                expectationValue, "numberOfTasksAvailable", 0);
            int expectedMessages = extractOrDefault(
                expectationValue, "numberOfMessagesToCheck", 0);
            List<String> expectationCaseIds = CaseIdUtil.extractAllAssignedCaseIdOrDefault(expectationValue, scenario);

            verifyTasks(scenario, taskRetrieverOption, expectationValue, expectedTasks, expectationCaseIds);

            verifyMessages(expectationValue, expectedMessages, expectationCaseIds.getFirst());

            removeInvalidMessages(expectationCaseIds.getFirst());
        }
    }

    private void processTestRequest(Map<String, Object> values, TestScenario scenario) throws Exception {

        Map<String, Object> request = extractOrThrow(values, "request");

        TestRequestType requestType = TestRequestType.valueOf(MapValueExtractor.extractOrDefault(
            request, "type", "MESSAGE"));

        CredentialRequest credentialRequest = extractCredentialRequest(request, "credentials");
        Headers requestAuthorizationHeaders = authorizationHeadersProvider.getIaUserAuthorization(credentialRequest);

        String userToken = requestAuthorizationHeaders.getValue(AUTHORIZATION);
        UserInfo userInfo = authorizationHeadersProvider.getUserInfo(userToken);

        log.info("{} request", requestType);
        switch (requestType) {
            case MESSAGE:
                messageInjector.injectMessage(
                    request,
                    scenario,
                    userInfo
                );
                break;
            case CLAIM:
                taskManagementService.claimTask(scenario, requestAuthorizationHeaders, userInfo);
                break;
            case ASSIGN:
                UserInfo assignee = getAssigneeInfo(request);
                taskManagementService.assignTask(scenario, requestAuthorizationHeaders, assignee);
                break;
            case COMPLETE:
                taskManagementService.completeTask(scenario, requestAuthorizationHeaders, userInfo);
                break;
            default:
                throw new Exception("Invalid request type [" + requestType + "]");
        }
    }

    private void verifyMessages(Map<String, Object> expectationValue, int expectedMessages, String expectationCaseId) {
        if (expectedMessages > 0) {
            await()
                .conditionEvaluationListener(new ConditionEvaluationLogger(log::info))
                .pollInterval(DEFAULT_POLL_INTERVAL_SECONDS, SECONDS)
                .atMost(DEFAULT_TIMEOUT_SECONDS, SECONDS)
                .until(
                    () -> {
                        String actualMessageResponse = restMessageService.getCaseMessages(expectationCaseId);

                        String expectedMessageResponse = buildMessageExpectationResponseBody(
                            expectationValue,
                            Map.of("caseId", expectationCaseId)
                        );

                        Map<String, Object> actualResponse = MapSerializer.deserialize(actualMessageResponse);
                        Map<String, Object> expectedResponse = MapSerializer.deserialize(expectedMessageResponse);

                        verifiers.forEach(verifier ->
                                              verifier.verify(
                                                  expectationValue,
                                                  expectedResponse,
                                                  actualResponse
                                              )
                        );

                        return true;
                    });
        }
    }

    private void verifyTasks(TestScenario scenario, String taskRetrieverOption, Map<String, Object> expectationValue,
                             int expectedTasks, List<String> expectationCaseIds) throws IOException {
        if (expectedTasks > 0) {
            CredentialRequest credentialRequest = extractCredentialRequest(expectationValue, "credentials");
            Headers expectationAuthorizationHeaders =
                authorizationHeadersProvider.getIaUserAuthorization(credentialRequest);

            if (TaskRetrieverEnum.CAMUNDA_API.getId().equals(taskRetrieverOption)) {
                camundaTaskRetrievableService.retrieveTask(
                    expectationValue,
                    scenario,
                    expectationCaseIds.getFirst(),
                    expectationAuthorizationHeaders
                );
            } else {
                taskMgmApiRetrievableService.retrieveTask(
                    expectationValue,
                    scenario,
                    expectationCaseIds,
                    expectationAuthorizationHeaders
                );
            }
        }
    }

    private UserInfo getAssigneeInfo(Map<String, Object> request) throws IOException {
        CredentialRequest credentialRequest = extractCredentialRequest(request, "input.assignee.credentials");
        Headers requestAuthorizationHeaders = authorizationHeadersProvider.getIaUserAuthorization(credentialRequest);

        String userToken = requestAuthorizationHeaders.getValue(AUTHORIZATION);
        return authorizationHeadersProvider.getUserInfo(userToken);
    }

    private CredentialRequest extractCredentialRequest(Map<String, Object> map, String path) {
        String credentialsKey = extractOrThrow(map, path + ".key");
        boolean granularPermission = extractOrDefault(map, path + ".granularPermission", false);

        return new CredentialRequest(credentialsKey, granularPermission);
    }

    @SneakyThrows
    private void removeInvalidMessages(String expectationCaseId) {

        log.info("Checking Invalid Messages for caseId: {}", expectationCaseId);
        String actualMessageResponse = restMessageService.getCaseMessages(expectationCaseId);

        Map<String, Object> actualResponse = MapSerializer.deserialize(actualMessageResponse);

        List<Map<String, Object>> messagesSent = new ArrayList<>(Objects.requireNonNull(
            MapValueExtractor.extract(actualResponse, "caseEventMessages")));

        messagesSent.forEach(messageData -> {
            String state = MapValueExtractor.extract(messageData, "State");
            log.info("State: {}", state);
            if ("UNPROCESSABLE".equals(state)) {
                String messageId = MapValueExtractor.extract(messageData, "MessageId");
                String caseId = MapValueExtractor.extract(messageData, "CaseId");
                log.info("Found UNPROCESSABLE messageId: {} caseId:{}", messageId, caseId);
                restMessageService.deleteMessage(messageId, caseId);
            }
        });
    }

    private String buildMessageExpectationResponseBody(Map<String, Object> clauseValues,
                                                       Map<String, String> additionalValues)
        throws IOException {

        Map<String, Object> scenario = deserializeValuesUtil.expandMapValues(clauseValues, additionalValues);
        Map<String, Object> roleData = MapValueExtractor.extract(scenario, "messageData");
        return MapSerializer.serialize(roleData);
    }


    private void loadPropertiesIntoMapValueExpander() {

        MutablePropertySources propertySources = ((AbstractEnvironment) environment).getPropertySources();
        StreamSupport
            .stream(propertySources.spliterator(), false)
            .filter(EnumerablePropertySource.class::isInstance)
            .map(propertySource -> ((EnumerablePropertySource<?>) propertySource).getPropertyNames())
            .flatMap(Arrays::stream)
            .filter(name -> environment.getProperty(name) != null)
            .forEach(name -> ENVIRONMENT_PROPERTIES.setProperty(name, environment.getProperty(name)));
    }

    private void addSearchParameters(TestScenario scenario, Map<String, Object> scenarioValues) {

        List<Map<String, Object>> searchParameterObjects = new ArrayList<>();
        searchParameterObjects = extractOrDefault(scenarioValues, "searchParameters", searchParameterObjects);
        searchParameterObjects.forEach(scenario::addSearchMap);

    }
}

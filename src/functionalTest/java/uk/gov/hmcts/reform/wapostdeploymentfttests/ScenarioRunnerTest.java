package uk.gov.hmcts.reform.wapostdeploymentfttests;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.http.Headers;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.taskretriever.CamundaTaskRetrievableParameter;
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.taskretriever.TaskMgmApiRetrievableParameter;
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.taskretriever.TaskRetrievableEnum;
import uk.gov.hmcts.reform.wapostdeploymentfttests.preparers.Preparer;
import uk.gov.hmcts.reform.wapostdeploymentfttests.services.AuthorizationHeadersProvider;
import uk.gov.hmcts.reform.wapostdeploymentfttests.services.AzureMessageInjector;
import uk.gov.hmcts.reform.wapostdeploymentfttests.services.CcdCaseCreator;
import uk.gov.hmcts.reform.wapostdeploymentfttests.services.taskretriever.CamundaTaskRetrieverService;
import uk.gov.hmcts.reform.wapostdeploymentfttests.services.taskretriever.TaskMgmApiRetrieverService;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.DeserializeValuesUtil;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.Logger;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapSerializer;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapValueExtractor;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.StringResourceLoader;
import uk.gov.hmcts.reform.wapostdeploymentfttests.verifiers.TaskDataVerifier;
import uk.gov.hmcts.reform.wapostdeploymentfttests.verifiers.Verifier;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.StreamSupport;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertFalse;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.LoggerMessage.SCENARIO_DISABLED;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.LoggerMessage.SCENARIO_ENABLED;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.LoggerMessage.SCENARIO_FINISHED;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.LoggerMessage.SCENARIO_START;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.LoggerMessage.SCENARIO_SUCCESSFUL;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapValueExpander.ENVIRONMENT_PROPERTIES;

@Slf4j
public class ScenarioRunnerTest extends SpringBootFunctionalBaseTest {

    @Autowired
    protected AzureMessageInjector azureMessageInjector;
    @Autowired
    protected TaskDataVerifier taskDataVerifier;
    @Autowired
    private CamundaTaskRetrieverService camundaTaskRetrievableService;
    @Autowired
    private TaskMgmApiRetrieverService taskMgmApiRetrievableService;
    @Autowired
    protected AuthorizationHeadersProvider authorizationHeadersProvider;
    @Autowired
    private Environment environment;
    @Autowired
    private DeserializeValuesUtil deserializeValuesUtil;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private List<Verifier> verifiers;
    @Autowired
    private List<Preparer> preparers;
    @Autowired
    private CcdCaseCreator ccdCaseCreator;

    @Before
    public void setUp() {
        MapSerializer.setObjectMapper(objectMapper);
    }

    @Test
    public void scenarios_should_behave_as_specified() throws IOException {

        loadPropertiesIntoMapValueExpander();

        for (Preparer preparer : preparers) {
            preparer.prepare();
        }

        assertFalse("Verifiers configured successfully", verifiers.isEmpty());

        String scenarioPattern = System.getProperty("scenario");
        if (scenarioPattern == null) {
            scenarioPattern = "*.json";
        } else {
            scenarioPattern = "*" + scenarioPattern + "*.json";
        }

        Collection<String> scenarioSources =
            StringResourceLoader
                .load("/scenarios/" + scenarioPattern)
                .values();

        Logger.say(SCENARIO_START, scenarioSources.size());

        for (String scenarioSource : scenarioSources) {

            Map<String, Object> scenario = deserializeValuesUtil
                .deserializeWithExpandedValues(scenarioSource, emptyMap());

            String description = MapValueExtractor.extract(scenario, "description");

            Boolean scenarioEnabled = MapValueExtractor.extract(scenario, "enabled");

            if (scenarioEnabled == null) {
                scenarioEnabled = true;
            }

            if (!scenarioEnabled) {
                Logger.say(SCENARIO_DISABLED, description);
                continue;
            } else {
                Logger.say(SCENARIO_ENABLED, description);

                String requestCredentials = MapValueExtractor.extract(scenario, "request.credentials");

                final Headers requestAuthorizationHeaders = getAuthorizationHeaders(requestCredentials);

                final String scenarioJurisdiction = MapValueExtractor.extractOrThrow(scenario, "jurisdiction");

                final String testCaseId = ccdCaseCreator.createCase(
                    scenario,
                    scenarioJurisdiction,
                    requestAuthorizationHeaders
                );

                azureMessageInjector.injectMessage(
                    scenarioSource,
                    testCaseId,
                    scenarioJurisdiction,
                    requestAuthorizationHeaders
                );

                String taskRetrievableOption = MapValueExtractor.extract(
                    scenario,
                    "options.taskRetrievalApi"
                );

                if (TaskRetrievableEnum.CAMUNDA_API.getId().equals(taskRetrievableOption)) {
                    camundaTaskRetrievableService.retrieveTask(
                        new CamundaTaskRetrievableParameter(scenario, requestAuthorizationHeaders, testCaseId));
                } else {
                    taskMgmApiRetrievableService.retrieveTask(new TaskMgmApiRetrievableParameter(
                        scenario,
                        scenarioSource,
                        testCaseId,
                        getAuthorizationHeaders(Objects.requireNonNull(
                            MapValueExtractor.extract(scenario, "expectation.credentials")))
                    ));
                }
            }
            Logger.say(SCENARIO_SUCCESSFUL, description);
            Logger.say(SCENARIO_FINISHED, null);
        }
    }


    private void loadPropertiesIntoMapValueExpander() {

        MutablePropertySources propertySources = ((AbstractEnvironment) environment).getPropertySources();
        StreamSupport
            .stream(propertySources.spliterator(), false)
            .filter(EnumerablePropertySource.class::isInstance)
            .map(propertySource -> ((EnumerablePropertySource) propertySource).getPropertyNames())
            .flatMap(Arrays::stream)
            .forEach(name -> ENVIRONMENT_PROPERTIES.setProperty(name, environment.getProperty(name)));
    }


    private Headers getAuthorizationHeaders(String credentials) {
        switch (credentials) {
            case "IALegalRepresentative":
                return authorizationHeadersProvider.getLegalRepAuthorization();
            case "IACaseworker":
                return authorizationHeadersProvider.getTribunalCaseworkerAAuthorization();
            case "WaSystemUser":
                return authorizationHeadersProvider.getWaSystemUserAuthorization();
            default:
                return new Headers();
        }

    }
}

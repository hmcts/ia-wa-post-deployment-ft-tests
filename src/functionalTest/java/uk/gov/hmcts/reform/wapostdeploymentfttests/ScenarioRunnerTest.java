package uk.gov.hmcts.reform.wapostdeploymentfttests;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.http.Headers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import uk.gov.hmcts.reform.wapostdeploymentfttests.services.AuthorizationHeadersProvider;
import uk.gov.hmcts.reform.wapostdeploymentfttests.services.AzureMessageInjector;
import uk.gov.hmcts.reform.wapostdeploymentfttests.services.CcdCaseCreator;
import uk.gov.hmcts.reform.wapostdeploymentfttests.services.TaskManagementService;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.Logger;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapMerger;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapSerializer;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapValueExpander;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapValueExtractor;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.StringResourceLoader;
import uk.gov.hmcts.reform.wapostdeploymentfttests.verifiers.TaskDataVerifier;
import uk.gov.hmcts.reform.wapostdeploymentfttests.verifiers.Verifier;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.StreamSupport;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertFalse;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.LoggerMessage.SCENARIO_DISABLED;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.LoggerMessage.SCENARIO_ENABLED;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.LoggerMessage.SCENARIO_FINISHED;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.LoggerMessage.SCENARIO_START;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.LoggerMessage.SCENARIO_SUCCESSFUL;

public class ScenarioRunnerTest extends SpringBootFunctionalBaseTest {

    @Autowired
    protected AzureMessageInjector azureMessageInjector;
    @Autowired
    protected TaskDataVerifier taskDataVerifier;
    @Autowired
    protected TaskManagementService taskManagementService;
    @Autowired
    protected AuthorizationHeadersProvider authorizationHeadersProvider;
    @Autowired
    private Environment environment;
    @Autowired
    private MapValueExpander mapValueExpander;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private List<Verifier> verifiers;
    @Autowired
    private CcdCaseCreator ccdCaseCreator;

    @Before
    public void setUp() {
        MapSerializer.setObjectMapper(objectMapper);
    }

    @Test
    public void scenarios_should_behave_as_specified() throws IOException {

        loadPropertiesIntoMapValueExpander();

        assertFalse(
            "Verifiers configured successfully",
            verifiers.isEmpty()
        );

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

            Map<String, Object> scenario = deserializeWithExpandedValues(scenarioSource, emptyMap());

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

                final String testCaseId = ccdCaseCreator.createCase(scenario, scenarioJurisdiction, requestAuthorizationHeaders);

                azureMessageInjector.injectMessage(scenarioSource, testCaseId, scenarioJurisdiction, requestAuthorizationHeaders);

                String expectationCredentials = MapValueExtractor.extract(scenario, "expectation.credentials");
                final Headers expectationAuthorizationHeaders = getAuthorizationHeaders(expectationCredentials);

                String actualResponseBody = taskManagementService.searchByCaseId(
                    scenarioSource,
                    testCaseId,
                    expectationAuthorizationHeaders
                );

                Map<String, String> taskTemplatesByFilename =
                    StringResourceLoader.load(
                        "/templates/" + scenarioJurisdiction.toLowerCase(Locale.ENGLISH) + "/task/*.json"
                    );


                String expectedResponseBody = buildTaskExpectationResponseBody(
                    MapValueExtractor.extract(scenario, "expectation"),
                    taskTemplatesByFilename,
                    testCaseId
                );

                Map<String, Object> actualResponse = MapSerializer.deserialize(actualResponseBody);
                Map<String, Object> expectedResponse = MapSerializer.deserialize(expectedResponseBody);

                verifiers.forEach(verifier ->
                    verifier.verify(
                        scenario,
                        expectedResponse,
                        actualResponse
                    )
                );
            }
            Logger.say(SCENARIO_SUCCESSFUL, description);
            Logger.say(SCENARIO_FINISHED, null);
        }
    }

    private String buildTaskExpectationResponseBody(Map<String, Object> expectation,
                                                    Map<String, String> taskTemplatesByFilename,
                                                    String caseId) throws IOException {

        Map<String, Object> taskData = MapValueExtractor.extract(expectation, "taskData");

        String templateFilename = MapValueExtractor.extract(taskData, "template");

        Map<String, Object> taskDataExpectation = deserializeWithExpandedValues(
            taskTemplatesByFilename.get(templateFilename),
            Map.of("caseId", caseId)
        );

        Map<String, Object> taskDataDataReplacements = MapValueExtractor.extract(taskData, "replacements");
        if (taskDataDataReplacements != null) {
            MapMerger.merge(taskDataExpectation, taskDataDataReplacements);
        }

        return MapSerializer.serialize(taskDataExpectation);

    }

    private void loadPropertiesIntoMapValueExpander() {

        MutablePropertySources propertySources = ((AbstractEnvironment) environment).getPropertySources();
        StreamSupport
            .stream(propertySources.spliterator(), false)
            .filter(propertySource -> propertySource instanceof EnumerablePropertySource)
            .map(propertySource -> ((EnumerablePropertySource) propertySource).getPropertyNames())
            .flatMap(Arrays::stream)
            .forEach(name -> MapValueExpander.ENVIRONMENT_PROPERTIES.setProperty(name, environment.getProperty(name)));
    }


    private Map<String, Object> deserializeWithExpandedValues(String source,
                                                              Map<String, String> additionalValues) throws IOException {
        Map<String, Object> data = MapSerializer.deserialize(source);
        mapValueExpander.expandValues(data, additionalValues);
        return data;
    }

    private Headers getAuthorizationHeaders(String credentials) {

        if ("IALegalRepresentative".equalsIgnoreCase(credentials)) {

            return authorizationHeadersProvider.getLegalRepAuthorization();
        }

        if ("IACaseworker".equalsIgnoreCase(credentials)) {
            return authorizationHeadersProvider.getTribunalCaseworkerAAuthorization();
        }

        return new Headers();
    }
}

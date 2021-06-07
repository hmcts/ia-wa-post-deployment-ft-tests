package uk.gov.hmcts.reform.wapostdeploymentfttests.services;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import io.restassured.http.Headers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.entities.idam.UserInfo;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapMerger;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapSerializer;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapValueExpander;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapValueExtractor;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.StringResourceLoader;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import static java.lang.String.format;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.services.AuthorizationHeadersProvider.AUTHORIZATION;

@Service
public class AzureMessageInjector {

    @Autowired
    protected AuthorizationHeadersProvider authorizationHeadersProvider;

    @Autowired
    private MapValueExpander mapValueExpander;

    @Autowired
    private ServiceBusSenderClient senderClient;

    @Value("${azure.servicebus.message-author}")
    private String messageAuthor;

    public void injectMessage(String scenarioSource,
                              String testCaseId,
                              String jurisdiction,
                              Headers authorizationHeaders) throws IOException {

        Map<String, String> eventMessageTemplatesByFilename =
            StringResourceLoader.load(
                "/templates/" + jurisdiction.toLowerCase(Locale.ENGLISH) + "/message/*.json"
            );

        String userToken = authorizationHeaders.getValue(AUTHORIZATION);
        UserInfo userInfo = authorizationHeadersProvider.getUserInfo(userToken);

        Map<String, String> additionalValues = Map.of(
            "caseId", testCaseId,
            "userId", userInfo.getUid()
        );

        Map<String, Object> scenario = deserializeWithExpandedValues(scenarioSource, additionalValues);

        String eventMessage = getMessageData(
            MapValueExtractor.extract(scenario, "request.input.eventMessage"),
            eventMessageTemplatesByFilename,
            additionalValues
        );

        sendMessage(eventMessage, testCaseId);
    }

    private void sendMessage(String message, String caseId) {
        ServiceBusMessage serviceBusMessage = new ServiceBusMessage(message);

        serviceBusMessage.getApplicationProperties().put("message_context", "wa-ft-" + caseId);
        serviceBusMessage.getApplicationProperties().put("message_author", messageAuthor);
        serviceBusMessage.setSessionId(caseId);

        System.out.println(
            format("Attempting to inject a message into azure service bus to session with ID: %s", caseId)
        );
        senderClient.sendMessage(serviceBusMessage);
        System.out.println("Message injected successfully");
    }


    private String getMessageData(
        Map<String, Object> messageDataInput,
        Map<String, String> templatesByFilename,
        Map<String, String> additionalValues
    ) throws IOException {

        String templateFilename = MapValueExtractor.extract(messageDataInput, "template");

        Map<String, Object> eventMessageData = deserializeWithExpandedValues(
            templatesByFilename.get(templateFilename),
            additionalValues
        );

        Map<String, Object> eventMessageDataReplacements = MapValueExtractor.extract(messageDataInput, "replacements");

        if (eventMessageDataReplacements != null) {
            MapMerger.merge(eventMessageData, eventMessageDataReplacements);
        }

        return MapSerializer.serialize(eventMessageData);
    }


    private Map<String, Object> deserializeWithExpandedValues(String source,
                                                              Map<String, String> additionalValues) throws IOException {
        Map<String, Object> data = MapSerializer.deserialize(source);
        mapValueExpander.expandValues(data, additionalValues);
        return data;
    }
}

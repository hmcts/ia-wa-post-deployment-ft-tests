package uk.gov.hmcts.reform.wapostdeploymentfttests.services;

import io.restassured.http.Headers;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

import static net.serenitybdd.rest.SerenityRest.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@Service
public class RestMessageService {

    @Value("${wa_case_event_handler.url}")
    private String caseEventHandlerUrl;

    @Autowired
    protected AuthorizationHeadersProvider authorizationHeadersProvider;

    public void sendMessage(String message, String caseId, boolean fromDlq) {
        String messageId = randomMessageId();

        log.info(
            "Attempting to inject a message into Case Event Handler using REST endpoint with "
                + "caseId: {}, messageId: {}", caseId, messageId
        );

        Headers systemUserUserToken = authorizationHeadersProvider.getWaSystemUserAuthorization();

        Response result = given()
            .headers(systemUserUserToken)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(message)
            .when()
            .post(caseEventHandlerUrl + "/messages/" + messageId + (fromDlq ? "?from_dlq=true" : "?from_dlq=false"));

        JsonPath response = result.then().assertThat()
            .statusCode(HttpStatus.CREATED.value())
            .contentType(APPLICATION_JSON_VALUE)
            .extract()
            .body()
            .jsonPath();
        assertEquals(messageId, response.get("MessageId"));

        String actualResponseBody = result.then()
            .extract()
            .body().asString();

        log.info("Message injected successfully using Case Event Handler REST endpoint");
        log.info("REST response message body: {}", actualResponseBody);
    }

    public String getCaseMessages(String caseId) {
        log.info(
            "Attempting to retrieve messages from Case Event Handler using REST endpoint with "
                + "caseId: {}", caseId
        );

        Headers systemUserUserToken = authorizationHeadersProvider.getWaSystemUserAuthorization();

        Response result = given()
            .headers(systemUserUserToken)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .when()
            .get(caseEventHandlerUrl + "/messages/query?case_id=" + caseId);

        return result.then().extract().body().asString();
    }

    @NonNull
    private String randomMessageId() {
        return "messageId_" + ThreadLocalRandom.current().nextLong(1000000000);
    }

    public void deleteMessage(String messageId, String caseId) {

        log.info(
            "Attempting to delete a message from Case Event Handler using REST endpoint with "
                + "caseId: {}, messageId: {}", caseId, messageId
        );

        Headers systemUserUserToken = authorizationHeadersProvider.getWaSystemUserAuthorization();

        Response result = given()
            .headers(systemUserUserToken)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(messageId)
            .when()
            .delete(caseEventHandlerUrl + "/messages/" + messageId);

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value());

        log.info("Message deleted successfully using Case Event Handler REST endpoint");
    }

}

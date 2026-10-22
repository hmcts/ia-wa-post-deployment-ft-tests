package uk.gov.hmcts.reform.wapostdeploymentfttests.verifiers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapFieldAsserter;

import java.util.Map;

@Component
public class TaskDataVerifier implements Verifier {

    private final MapFieldAsserter mapFieldAsserter;

    public TaskDataVerifier(MapFieldAsserter mapFieldAsserter) {
        this.mapFieldAsserter = mapFieldAsserter;
    }

    public void verify(
        String fileName,
        Map<String, Object> expectedResponse,
        Map<String, Object> actualResponse
    ) {
        try {
            mapFieldAsserter.assertFields(expectedResponse, actualResponse, (fileName + ": "));
        } catch (AssertionError e) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                String expectedResponseJson = objectMapper.writeValueAsString(expectedResponse);
                String actualResponseJson = objectMapper.writeValueAsString(actualResponse);
                System.out.println("Expected Response JSON: " + expectedResponseJson);
                System.out.println("Actual Response JSON: " + actualResponseJson);
                throw e;
            } catch (JsonProcessingException jsonProcessingException) {
                throw new RuntimeException(jsonProcessingException);
            }
        }
    }
}

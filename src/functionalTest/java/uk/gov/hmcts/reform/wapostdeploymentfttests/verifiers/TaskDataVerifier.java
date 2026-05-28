package uk.gov.hmcts.reform.wapostdeploymentfttests.verifiers;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapFieldAsserter;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapValueExtractor;

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
        mapFieldAsserter.assertFields(expectedResponse, actualResponse, (fileName + ": "));
    }
}

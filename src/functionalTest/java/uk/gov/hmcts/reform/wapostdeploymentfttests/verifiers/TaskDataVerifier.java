package uk.gov.hmcts.reform.wapostdeploymentfttests.verifiers;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapFieldAsserter;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapValueExtractor;

import java.util.Map;

@Component
@SuppressWarnings("unchecked")
public class TaskDataVerifier implements Verifier {

    public void verify(
        Map<String, Object> scenario,
        Map<String, Object> expectedResponse,
        Map<String, Object> actualResponse
    ) {
        String description = MapValueExtractor.extract(scenario, "description");

        MapFieldAsserter.assertFields(expectedResponse, actualResponse, (description + ": "));
    }
}

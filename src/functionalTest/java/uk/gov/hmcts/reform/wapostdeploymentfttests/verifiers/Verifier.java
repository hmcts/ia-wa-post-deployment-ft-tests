package uk.gov.hmcts.reform.wapostdeploymentfttests.verifiers;

import java.util.Map;

public interface Verifier {

    void verify(
        String fileName,
        Map<String, Object> expectedResponse,
        Map<String, Object> actualResponse
    );
}

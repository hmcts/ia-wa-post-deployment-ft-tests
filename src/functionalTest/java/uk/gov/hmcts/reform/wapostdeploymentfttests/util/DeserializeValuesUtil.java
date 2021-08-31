package uk.gov.hmcts.reform.wapostdeploymentfttests.util;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class DeserializeValuesUtil {

    private final MapValueExpander mapValueExpander;

    public DeserializeValuesUtil(MapValueExpander mapValueExpander) {
        this.mapValueExpander = mapValueExpander;
    }

    public Map<String, Object> deserializeWithExpandedValues(String source,
                                                             Map<String, String> additionalValues)
        throws IOException {
        Map<String, Object> data = MapSerializer.deserialize(source);
        mapValueExpander.expandValues(data, additionalValues);
        return data;
    }
}

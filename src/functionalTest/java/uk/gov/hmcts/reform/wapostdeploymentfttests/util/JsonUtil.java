package uk.gov.hmcts.reform.wapostdeploymentfttests.util;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

public class JsonUtil {

    private static ObjectMapper MAPPER = new JsonMapper();

    private JsonUtil() {
        // noop
    }

    public static void setObjectMapper(
        ObjectMapper objectMapper
    ) {
        MAPPER = objectMapper;
    }


    public static String toJsonString(Map<String, String> attributes) {
        String json = null;

        try {
            json = MAPPER.writeValueAsString(attributes);
        } catch (JacksonException e) {
            e.printStackTrace();
        }

        return json;
    }

    public static String toJsonString(List<String> attributes) {
        String json = null;

        try {
            json = MAPPER.writeValueAsString(attributes);
        } catch (JacksonException e) {
            e.printStackTrace();
        }

        return json;
    }

}

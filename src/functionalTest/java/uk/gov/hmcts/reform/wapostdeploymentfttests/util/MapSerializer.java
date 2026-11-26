package uk.gov.hmcts.reform.wapostdeploymentfttests.util;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class MapSerializer {

    private static ObjectMapper MAPPER = new JsonMapper();

    private MapSerializer() {
        // noop
    }

    public static void setObjectMapper(
        ObjectMapper objectMapper
    ) {
        MAPPER = objectMapper;
    }

    public static Map<String, Object> deserialize(String source) throws IOException {

        return MAPPER.readValue(
            source,
            new TypeReference<>() {
            }
        );
    }

    public static String sortCollectionElement(String source, String elementNameToSort, Comparator<JsonNode> comparator)
        throws IOException {
        JsonNode node = MAPPER.readTree(source);

        if (node instanceof ObjectNode originalObject) {
            ObjectNode objectNode = originalObject.deepCopy();

            if (objectNode.get(elementNameToSort) instanceof ArrayNode list) {
                List<JsonNode> sortList = new ArrayList<>();
                for (JsonNode childNode : list) {
                    sortList.add(childNode);
                }
                sortList.sort(comparator);
                ArrayNode array = MAPPER.createArrayNode();
                sortList.forEach(array::add);
                objectNode.set(elementNameToSort, array);

                return MAPPER.writeValueAsString(objectNode);
            }
        }

        return source;
    }

    public static String serialize(Map<String, Object> map) throws IOException {

        return MAPPER.writeValueAsString(map);
    }
}

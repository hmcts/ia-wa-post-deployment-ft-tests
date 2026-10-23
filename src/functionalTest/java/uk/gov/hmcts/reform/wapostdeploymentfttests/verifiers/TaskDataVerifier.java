package uk.gov.hmcts.reform.wapostdeploymentfttests.verifiers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapFieldAsserter;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

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
            Map<String, List<Object>> differences = diff(expectedResponse, actualResponse);
            System.out.println("Expected value -> Actual value");
            differences.forEach((key, values) ->
                                    System.out.printf(
                                        "%s: %s -> %s%n",
                                        key,
                                        values.getFirst(),
                                        values.get(1)
                                    )
            );
        }
    }

    public static Map<String, Object> flatten(Map<String, Object> input) {
        Map<String, Object> result = new TreeMap<>();
        flatten("", input, result);
        return result;
    }

    private static void flatten(
        String prefix,
        Object value,
        Map<String, Object> result
    ) {
        if (value instanceof Map<?, ?> map) {
            map.forEach((key, val) -> {
                String path = prefix.isEmpty()
                    ? String.valueOf(key)
                    : prefix + "." + key;

                flatten(path, val, result);
            });
        } else if (value instanceof List<?> list) {
            List<?> sorted = list.stream()
                .sorted(Comparator.comparing(String::valueOf))
                .toList();

            for (int i = 0; i < sorted.size(); i++) {
                flatten(prefix + "[" + i + "]", sorted.get(i), result);
            }
        } else {
            result.put(prefix, value);
        }
    }

    public static Map<String, List<Object>> diff(
        Map<String, Object> expected,
        Map<String, Object> actual
    ) {
        Map<String, Object> expectedFlat = flatten(expected);
        Map<String, Object> actualFlat = flatten(actual);

        Set<String> keys = new TreeSet<>(expectedFlat.keySet());
        keys.addAll(actualFlat.keySet());

        Map<String, List<Object>> differences = new LinkedHashMap<>();

        for (String key : keys) {
            Object oldValue = expectedFlat.get(key);
            Object newValue = actualFlat.get(key);

            if (oldValue != null && !Objects.equals(oldValue, newValue)
                || expectedFlat.containsKey(key) != actualFlat.containsKey(key)) {
                differences.put(key, Arrays.asList(oldValue, newValue));
            }
        }

        return differences;
    }

}

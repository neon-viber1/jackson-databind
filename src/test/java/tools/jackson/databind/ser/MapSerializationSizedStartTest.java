package tools.jackson.databind.ser;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.util.JsonGeneratorDelegate;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Verifies that unfiltered Maps pass exact size to generator, filtered ones do not
public class MapSerializationSizedStartTest extends DatabindTestUtil
{
    static class Recorder extends JsonGeneratorDelegate {
        final List<Integer> sizes;
        Recorder(JsonGenerator g, List<Integer> sizes) { super(g); this.sizes = sizes; }
        @Override public JsonGenerator writeStartObject(Object fv) { sizes.add(-1); return super.writeStartObject(fv); }
        @Override public JsonGenerator writeStartObject(Object fv, int size) { sizes.add(size); return super.writeStartObject(fv, size); }
    }

    static class Holder {
        public Map<String,Integer> map = new LinkedHashMap<>();
        { map.put("a", 1); map.put("b", 2); }
    }

    static class NullsHolder {
        @JsonInclude(value = JsonInclude.Include.NON_NULL, content = JsonInclude.Include.NON_NULL)
        public Map<String,Integer> map = new LinkedHashMap<>();
        { map.put("a", null); map.put("b", 2); }
    }

    @JsonIgnoreProperties({ "a" })
    static class IgnoringHolder {
        @JsonIgnoreProperties({ "a" })
        public Map<String,Integer> map = new LinkedHashMap<>();
        { map.put("a", 1); map.put("b", 2); }
    }

    private final ObjectMapper MAPPER = JsonMapper.builder().build();

    private List<Integer> sizes(Object value, String expJson) {
        List<Integer> sizes = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        java.io.StringWriter w = new java.io.StringWriter();
        try (JsonGenerator g = new Recorder(MAPPER.createGenerator(w), sizes)) {
            MAPPER.writeValue(g, value);
        }
        assertEquals(expJson, w.toString());
        return sizes;
    }

    @Test
    void typedAndUntypedMap() {
        Map<String,Integer> m = new LinkedHashMap<>();
        m.put("a", 1);
        m.put("b", 2);
        assertEquals(List.of(2), sizes(m, "{\"a\":1,\"b\":2}"));
        assertEquals(List.of(2), sizes(new TreeMap<>(m), "{\"a\":1,\"b\":2}"));
        assertEquals(List.of(0), sizes(new HashMap<String,Integer>(), "{}"));
    }

    @Test
    void nestedInPojo() {
        assertEquals(List.of(-1, 2), sizes(new Holder(), "{\"map\":{\"a\":1,\"b\":2}}"));
    }

    @Test
    void objectNodeBaseline() {
        assertEquals(List.of(1), sizes(MAPPER.createObjectNode().put("x", 1), "{\"x\":1}"));
    }

    @Test
    void suppressNullsUnsized() {
        assertEquals(List.of(-1, -1), sizes(new NullsHolder(), "{\"map\":{\"b\":2}}"));
    }

    @Test
    void ignoredPropertiesUnsized() {
        assertEquals(List.of(-1, -1), sizes(new IgnoringHolder(), "{\"map\":{\"b\":2}}"));
    }
}

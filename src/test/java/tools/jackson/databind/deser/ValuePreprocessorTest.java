package tools.jackson.databind.deser;

import java.util.*;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class ValuePreprocessorTest extends DatabindTestUtil
{
    static class Bean {
        public String name;
        public List<String> tags;
        public int count;
    }

    static class IntBean {
        public int value;
    }

    // Replaces "${x}" with "X"; records (name, value) calls
    static class Recorder implements ValuePreprocessor {
        final List<String> calls = new ArrayList<>();

        @Override
        public String preprocess(String name, String value) {
            calls.add(name + "=" + value);
            return value.replace("${x}", "X");
        }
    }

    private static final ValuePreprocessor SUBST = (n, v) -> v.replace("${x}", "X");

    private final ObjectMapper PLAIN = newJsonMapper();

    private final ObjectMapper MAPPER = JsonMapper.builder()
            .valuePreprocessor(SUBST).build();

    @Test
    void pojoProperty() throws Exception {
        Bean b = MAPPER.readValue("{\"name\":\"a-${x}-b\",\"count\":3}", Bean.class);
        assertEquals("a-X-b", b.name);
        assertEquals(3, b.count);
    }

    @Test
    void noPreprocessorUnchanged() throws Exception {
        Bean b = PLAIN.readValue("{\"name\":\"a-${x}-b\"}", Bean.class);
        assertEquals("a-${x}-b", b.name);
    }

    @Test
    void namesPassedAndArrayElements() throws Exception {
        Recorder rec = new Recorder();
        ObjectMapper m = JsonMapper.builder().valuePreprocessor(rec).build();
        m.readValue("""
                {"name":"n1","tags":["t1",["t2"]],"count":5, "o":{"in":"v"}}
                """, JsonNode.class);
        assertEquals(List.of("name=n1", "tags=t1", "tags=t2", "in=v"), rec.calls);
    }

    @Test
    void rootString() throws Exception {
        Recorder rec = new Recorder();
        ObjectMapper m = JsonMapper.builder().valuePreprocessor(rec).build();
        assertEquals("X", m.readValue(q("${x}"), String.class));
        assertEquals(List.of("null=${x}"), rec.calls);
    }

    @Test
    void propertyNamesAndNonStringsUntouched() throws Exception {
        Recorder rec = new Recorder();
        ObjectMapper m = JsonMapper.builder().valuePreprocessor(rec).build();
        Map<String, Object> map = m.readValue("""
                {"${x}":1, "b":true, "c":null, "d":2.5}
                """, new TypeReference<Map<String, Object>>() { });
        assertEquals(4, map.size());
        assertTrue(map.containsKey("${x}"));
        assertEquals(List.of(), rec.calls);
    }

    @Test
    void mapListAndUntyped() throws Exception {
        Map<String, String> map = MAPPER.readValue("{\"a\":\"${x}\"}",
                new TypeReference<Map<String, String>>() { });
        assertEquals("X", map.get("a"));
        List<String> list = MAPPER.readValue("[\"${x}\",\"y\"]",
                new TypeReference<List<String>>() { });
        assertEquals(List.of("X", "y"), list);
        Object ob = MAPPER.readValue("{\"a\":[\"${x}\"]}", Object.class);
        assertEquals(Map.of("a", List.of("X")), ob);
    }

    @Test
    void treeReading() throws Exception {
        JsonNode n = MAPPER.readTree("{\"a\":\"${x}\",\"b\":[\"${x}\"]}");
        assertEquals("X", n.path("a").asString());
        assertEquals("X", n.path("b").path(0).asString());
    }

    @Test
    void readValuesSequence() throws Exception {
        try (MappingIterator<String> it = MAPPER.readerFor(String.class)
                .readValues("\"${x}\" \"y\" \"a${x}\"")) {
            assertEquals(List.of("X", "y", "aX"), it.readAll());
        }
    }

    @Test
    void callerSuppliedParser() throws Exception {
        try (JsonParser p = PLAIN.createParser("{\"name\":\"${x}\"}")) {
            Bean b = MAPPER.readValue(p, Bean.class);
            assertEquals("X", b.name);
        }
    }

    @Test
    void convertValue() throws Exception {
        Bean b = MAPPER.convertValue(Map.of("name", "${x}"), Bean.class);
        assertEquals("X", b.name);
    }

    @Test
    void readerWithOnPlainMapper() throws Exception {
        ObjectReader r = PLAIN.readerFor(Bean.class).with(SUBST);
        assertEquals("X", r.<Bean>readValue("{\"name\":\"${x}\"}").name);
        // original unchanged
        assertEquals("${x}", PLAIN.readValue("{\"name\":\"${x}\"}", Bean.class).name);
    }

    @Test
    void readerRemoveAndOverride() throws Exception {
        ObjectReader base = MAPPER.readerFor(Bean.class);
        ObjectReader none = base.withoutValuePreprocessor();
        ObjectReader other = base.with((n, v) -> "other");
        String json = "{\"name\":\"${x}\"}";
        assertEquals("${x}", none.<Bean>readValue(json).name);
        assertEquals("other", other.<Bean>readValue(json).name);
        // originals unchanged
        assertEquals("X", base.<Bean>readValue(json).name);
        assertEquals("X", MAPPER.readValue(json, Bean.class).name);
    }

    @Test
    void rebuildKeepsPreprocessor() throws Exception {
        ObjectMapper m2 = MAPPER.rebuild().build();
        assertEquals("X", m2.readValue("{\"name\":\"${x}\"}", Bean.class).name);
    }

    @Test
    void nullResultMeansUnchanged() throws Exception {
        ObjectMapper m = JsonMapper.builder().valuePreprocessor((n, v) -> null).build();
        assertEquals("abc", m.readValue(q("abc"), String.class));
    }

    @Test
    void unbindableReplacementFails() throws Exception {
        ObjectMapper m = JsonMapper.builder().valuePreprocessor((n, v) -> "oops").build();
        assertThrows(InvalidFormatException.class,
                () -> m.readValue("{\"value\":\"12\"}", IntBean.class));
        // and replacement can make a coercible value
        ObjectMapper m2 = JsonMapper.builder().valuePreprocessor((n, v) -> "7").build();
        assertEquals(7, m2.readValue("{\"value\":\"12\"}", IntBean.class).value);
    }
}

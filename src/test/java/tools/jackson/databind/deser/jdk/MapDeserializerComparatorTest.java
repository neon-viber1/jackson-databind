package tools.jackson.databind.deser.jdk;

import java.util.*;

import org.junit.jupiter.api.Test;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;
import tools.jackson.databind.cfg.HandlerInstantiator;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.Annotated;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.TypeIdResolver;
import tools.jackson.databind.jsontype.TypeResolverBuilder;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// Tests for HandlerInstantiator-supplied Comparator for TreeMap deserialization
public class MapDeserializerComparatorTest extends DatabindTestUtil
{
    static class ComparatorInstantiator extends HandlerInstantiator
    {
        private final Comparator<Object> _comparator;

        ComparatorInstantiator(Comparator<Object> c) { _comparator = c; }

        @Override
        public Comparator<Object> sortedMapComparator(DeserializationConfig config,
                JavaType mapType) {
            return _comparator;
        }

        @Override
        public ValueDeserializer<?> deserializerInstance(DeserializationConfig config,
                Annotated annotated, Class<?> deserClass) { return null; }
        @Override
        public KeyDeserializer keyDeserializerInstance(DeserializationConfig config,
                Annotated annotated, Class<?> keyDeserClass) { return null; }
        @Override
        public ValueSerializer<?> serializerInstance(SerializationConfig config,
                Annotated annotated, Class<?> serClass) { return null; }
        @Override
        public TypeResolverBuilder<?> typeResolverBuilderInstance(MapperConfig<?> config,
                Annotated annotated, Class<?> builderClass) { return null; }
        @Override
        public TypeIdResolver typeIdResolverInstance(MapperConfig<?> config,
                Annotated annotated, Class<?> resolverClass) { return null; }
    }

    static class MyTreeMap<K, V> extends TreeMap<K, V> { }

    static class Holder {
        public TreeMap<String, Integer> map;
        public SortedMap<String, Integer> sorted;
        public NavigableMap<String, Integer> nav;
        public MyTreeMap<String, Integer> custom;
    }

    private static final Comparator<Object> REVERSE = (a, b) -> ((String) b).compareTo((String) a);

    private static final String JSON = "{\"a\":1,\"c\":3,\"b\":2}";

    private JsonMapper mapperWith(Comparator<Object> c) {
        return JsonMapper.builder().handlerInstantiator(new ComparatorInstantiator(c)).build();
    }

    @Test
    void treeMapRoot() throws Exception {
        TreeMap<String, Integer> result = mapperWith(REVERSE).readValue(JSON,
                new TypeReference<TreeMap<String, Integer>>() { });
        assertEquals("[c, b, a]", result.keySet().toString());
        assertSame(REVERSE, result.comparator());
    }

    @Test
    void sortedAndNavigableRoot() throws Exception {
        JsonMapper m = mapperWith(REVERSE);
        SortedMap<String, Integer> s = m.readValue(JSON, new TypeReference<SortedMap<String, Integer>>() { });
        assertEquals("[c, b, a]", s.keySet().toString());
        NavigableMap<String, Integer> n = m.readValue(JSON, new TypeReference<NavigableMap<String, Integer>>() { });
        assertEquals("[c, b, a]", n.keySet().toString());
    }

    @Test
    void beanProperties() throws Exception {
        String json = "{\"map\":" + JSON + ",\"sorted\":" + JSON + ",\"nav\":" + JSON
                + ",\"custom\":" + JSON + "}";
        Holder h = mapperWith(REVERSE).readValue(json, Holder.class);
        assertEquals("[c, b, a]", h.map.keySet().toString());
        assertEquals("[c, b, a]", h.sorted.keySet().toString());
        assertEquals("[c, b, a]", h.nav.keySet().toString());
        // user-defined subclasses unaffected
        assertEquals("[a, b, c]", h.custom.keySet().toString());
        assertNull(h.custom.comparator());
    }

    @Test
    void nullComparatorGivesNaturalOrder() throws Exception {
        TreeMap<String, Integer> result = mapperWith(null).readValue(JSON,
                new TypeReference<TreeMap<String, Integer>>() { });
        assertEquals("[a, b, c]", result.keySet().toString());
        assertNull(result.comparator());
    }

    @Test
    void noHandlerInstantiator() throws Exception {
        TreeMap<String, Integer> result = newJsonMapper().readValue(JSON,
                new TypeReference<TreeMap<String, Integer>>() { });
        assertNull(result.comparator());
    }

    @Test
    void nonComparableKeys() throws Exception {
        Comparator<Object> byLen = Comparator.comparingInt(o -> o.toString().length());
        JsonMapper m = JsonMapper.builder()
                .handlerInstantiator(new ComparatorInstantiator(byLen))
                .build();
        // Object keys are not Comparable-safe in general; comparator is what orders them
        TreeMap<Object, Integer> result = m.readValue("{\"ccc\":1,\"a\":2,\"bb\":3}",
                new TypeReference<TreeMap<Object, Integer>>() { });
        assertEquals("[a, bb, ccc]", result.keySet().toString());
    }
}

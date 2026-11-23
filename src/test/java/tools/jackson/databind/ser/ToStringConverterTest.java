package tools.jackson.databind.ser;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class ToStringConverterTest extends DatabindTestUtil
{
    static class Point {
        public int x, y;
        public Point(int x, int y) { this.x = x; this.y = y; }
    }

    static class Other { }

    static class PointKeySerializer extends ValueSerializer<Point>
        implements ToStringConverter
    {
        @Override
        public void serialize(Point value, JsonGenerator g, SerializationContext ctxt) {
            g.writeName(convertToString(ctxt, value));
        }

        @Override
        public String convertToString(SerializationContext ctxt, Object value) {
            Point p = (Point) value;
            return p.x + "," + p.y;
        }
    }

    static class PlainKeySerializer extends ValueSerializer<Other> {
        @Override
        public void serialize(Other value, JsonGenerator g, SerializationContext ctxt) {
            g.writeName("other");
        }
    }

    // Contextual: converter is only reachable via the contextualized instance
    static class ContextualKeySerializer extends ValueSerializer<Point>
        implements ToStringConverter
    {
        final String _sep;

        ContextualKeySerializer(String sep) { _sep = sep; }

        @Override
        public ValueSerializer<?> createContextual(SerializationContext ctxt, BeanProperty prop) {
            return new ContextualKeySerializer(";");
        }

        @Override
        public void serialize(Point value, JsonGenerator g, SerializationContext ctxt) {
            g.writeName(convertToString(ctxt, value));
        }

        @Override
        public String convertToString(SerializationContext ctxt, Object value) {
            Point p = (Point) value;
            return p.x + _sep + p.y;
        }
    }

    // Value serializer that looks up converters during serialization
    static class LookupSerializer extends ValueSerializer<Object> {
        final Class<?> _type;
        String result;
        boolean found;

        LookupSerializer(Class<?> type) { _type = type; }

        @Override
        public void serialize(Object value, JsonGenerator g, SerializationContext ctxt) {
            ValueSerializer<Object> ks = ctxt.findKeySerializer(_type, null);
            if (ks instanceof ToStringConverter conv) {
                found = true;
                result = conv.convertToString(ctxt, value);
            }
            g.writeString("ok");
        }
    }

    static class ValueConverterSerializer extends ValueSerializer<Point>
        implements ToStringConverter
    {
        @Override
        public void serialize(Point value, JsonGenerator g, SerializationContext ctxt) {
            g.writeString(convertToString(ctxt, value));
        }

        @Override
        public String convertToString(SerializationContext ctxt, Object value) {
            return "P" + ((Point) value).x;
        }
    }

    private LookupSerializer lookup(Class<?> type, Object value, ValueSerializer<?> keySer,
            Class<?> keyType)
    {
        SimpleModule module = new SimpleModule();
        @SuppressWarnings("unchecked")
        ValueSerializer<Object> ks = (ValueSerializer<Object>) keySer;
        module.addKeySerializer((Class<Object>) keyType, ks);
        LookupSerializer ls = new LookupSerializer(type);
        module.addSerializer(Object.class, ls);
        ObjectMapper mapper = JsonMapper.builder().addModule(module).build();
        mapper.writeValueAsString(value);
        return ls;
    }

    @Test
    public void keySerializerImplementsConverter() throws Exception {
        LookupSerializer ls = lookup(Point.class, new Point(1, 2),
                new PointKeySerializer(), Point.class);
        assertTrue(ls.found);
        assertEquals("1,2", ls.result);
    }

    @Test
    public void keySerializerWithoutConverter() throws Exception {
        LookupSerializer ls = lookup(Other.class, new Other(),
                new PlainKeySerializer(), Other.class);
        assertFalse(ls.found);
        assertNull(ls.result);
    }

    @Test
    public void contextualizedConverter() throws Exception {
        LookupSerializer ls = lookup(Point.class, new Point(1, 2),
                new ContextualKeySerializer(","), Point.class);
        assertTrue(ls.found);
        assertEquals("1;2", ls.result);
    }

    @Test
    public void valueSerializerImplementsConverter() throws Exception {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Point.class, new ValueConverterSerializer());
        ObjectMapper mapper = JsonMapper.builder().addModule(module).build();
        assertEquals(q("P3"), mapper.writeValueAsString(new Point(3, 4)));
        assertEquals("P3", ((ToStringConverter) new ValueConverterSerializer())
                .convertToString(null, new Point(3, 4)));
    }

    @Test
    public void keyWriteViaMap() throws Exception {
        SimpleModule module = new SimpleModule();
        module.addKeySerializer(Point.class, new PointKeySerializer());
        ObjectMapper mapper = JsonMapper.builder().addModule(module).build();
        assertEquals("{\"1,2\":1}",
                mapper.writeValueAsString(java.util.Map.of(new Point(1, 2), 1)));
    }
}

package tools.jackson.databind.ser;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.databind.*;
import tools.jackson.databind.exc.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.DoubleNode;
import tools.jackson.databind.node.FloatNode;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// Tests for SerializationFeature.FAIL_ON_NON_FINITE_NUMBERS
public class StrictNonFiniteNumberSerTest extends DatabindTestUtil
{
    static class DoubleBean {
        public double value;
        public DoubleBean(double v) { value = v; }
    }

    static class StringShapeBean {
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        public double value = Double.NaN;
    }

    private final ObjectMapper DEFAULT = newJsonMapper();

    private final ObjectMapper STRICT = JsonMapper.builder()
            .enable(SerializationFeature.FAIL_ON_NON_FINITE_NUMBERS)
            .build();

    @Test
    public void defaultWritesStrings() throws Exception {
        assertEquals("\"NaN\"", DEFAULT.writeValueAsString(Double.NaN));
        assertEquals("\"Infinity\"", DEFAULT.writeValueAsString(Float.POSITIVE_INFINITY));
        assertEquals("[\"NaN\"]", DEFAULT.writeValueAsString(new double[] { Double.NaN }));
        assertEquals("{\"value\":\"-Infinity\"}",
                DEFAULT.writeValueAsString(new DoubleBean(Double.NEGATIVE_INFINITY)));
        assertEquals("\"NaN\"", DEFAULT.writeValueAsString(DoubleNode.valueOf(Double.NaN)));
    }

    @Test
    public void nonFiniteFails() throws Exception {
        for (double d : new double[] { Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY }) {
            _assertFails(Double.valueOf(d));
            _assertFails(Float.valueOf((float) d));
            _assertFails(new double[] { 1.0, d });
            _assertFails(new float[] { 1.0f, (float) d });
            _assertFails(List.of(Double.valueOf(d)));
            _assertFails(new DoubleBean(d));
            _assertFails(Map.of("a", Double.valueOf(d)));
            _assertFails(DoubleNode.valueOf(d));
            _assertFails(FloatNode.valueOf((float) d));
            _assertFails(Map.of("a", (Number) Double.valueOf(d)));
        }
    }

    private void _assertFails(Object value) throws Exception {
        DatabindException e = assertThrows(DatabindException.class,
                () -> STRICT.writeValueAsString(value));
        verifyException(e, "Non-finite number");
        verifyException(e, "FAIL_ON_NON_FINITE_NUMBERS");
    }

    @Test
    public void finiteValuesUnchanged() throws Exception {
        for (Object v : new Object[] { 0.0, -0.0, Double.MAX_VALUE, Float.MIN_VALUE,
                new double[] { 0.5, -1.5 }, new float[] { 0.5f }, DoubleNode.valueOf(2.5),
                new DoubleBean(3.25) }) {
            assertEquals(DEFAULT.writeValueAsString(v), STRICT.writeValueAsString(v));
        }
    }

    @Test
    public void longsUnaffected() throws Exception {
        assertEquals("9223372036854775807", STRICT.writeValueAsString(Long.MAX_VALUE));
        assertEquals("9007199254740993", STRICT.writeValueAsString((1L << 53) + 1));
    }

    @Test
    public void stringShapeUnaffected() throws Exception {
        assertEquals("{\"value\":\"NaN\"}", STRICT.writeValueAsString(new StringShapeBean()));
    }

    @Test
    public void perWriterToggle() throws Exception {
        assertThrows(DatabindException.class, () -> DEFAULT.writer()
                .with(SerializationFeature.FAIL_ON_NON_FINITE_NUMBERS)
                .writeValueAsString(Double.NaN));
        assertEquals("\"NaN\"", STRICT.writer()
                .without(SerializationFeature.FAIL_ON_NON_FINITE_NUMBERS)
                .writeValueAsString(Double.NaN));
    }
}

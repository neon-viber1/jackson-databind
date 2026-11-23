package tools.jackson.databind.deser.validate;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DeserializationFeature#FAIL_ON_MISSING_REQUIRED_PROPERTIES}.
 */
public class RequiredPropertiesDeserTest extends DatabindTestUtil
{
    static class FieldBean {
        @JsonProperty(required = true)
        public String name;
        public int other;
    }

    static class SetterBean {
        private String value;
        private String optional;

        @JsonProperty(required = true)
        public void setValue(String v) { value = v; }
        public void setOptional(String v) { optional = v; }
    }

    static class MultiBean {
        @JsonProperty(required = true)
        public String a;
        @JsonProperty(required = true)
        public String b;
    }

    static class CreatorBean {
        public final String id;
        @JsonProperty(required = true)
        public String extra;

        @JsonCreator
        public CreatorBean(@JsonProperty("id") String id) { this.id = id; }
    }

    static class PlainBean {
        public String x;
    }

    static class ViewA { }

    static class ViewBean {
        @JsonProperty(required = true)
        @com.fasterxml.jackson.annotation.JsonView(ViewA.class)
        public String inView;
        @JsonProperty(required = true)
        public String noView;
    }

    @JsonDeserialize(builder = ValueBuilder.class)
    static class Value {
        final String name;
        Value(String n) { name = n; }
    }

    @JsonPOJOBuilder(withPrefix = "")
    static class ValueBuilder {
        String name;

        @JsonProperty(required = true)
        public ValueBuilder name(String n) { name = n; return this; }

        public Value build() { return new Value(name); }
    }

    private final ObjectMapper STRICT = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_MISSING_REQUIRED_PROPERTIES)
            .build();

    private final ObjectMapper LENIENT = newJsonMapper();

    @Test
    public void missingFieldProperty() throws Exception
    {
        MismatchedInputException e = assertThrows(MismatchedInputException.class,
                () -> STRICT.readValue("{\"other\":3}", FieldBean.class));
        verifyException(e, "Missing required property");
        verifyException(e, "'name'");
    }

    @Test
    public void presentFieldProperty() throws Exception
    {
        FieldBean b = STRICT.readValue("{\"name\":\"x\",\"other\":3}", FieldBean.class);
        assertEquals("x", b.name);
    }

    @Test
    public void explicitNullCountsAsPresent() throws Exception
    {
        FieldBean b = STRICT.readValue("{\"name\":null}", FieldBean.class);
        assertNull(b.name);
    }

    @Test
    public void emptyObjectMissing() throws Exception
    {
        assertThrows(MismatchedInputException.class,
                () -> STRICT.readValue("{}", FieldBean.class));
    }

    @Test
    public void setterProperty() throws Exception
    {
        MismatchedInputException e = assertThrows(MismatchedInputException.class,
                () -> STRICT.readValue("{\"optional\":\"y\"}", SetterBean.class));
        verifyException(e, "'value'");
        assertEquals("v", STRICT.readValue("{\"value\":\"v\"}", SetterBean.class).value);
    }

    @Test
    public void allMissingNamed() throws Exception
    {
        MismatchedInputException e = assertThrows(MismatchedInputException.class,
                () -> STRICT.readValue("{\"a\":\"1\"}", MultiBean.class));
        verifyException(e, "'b'");
        e = assertThrows(MismatchedInputException.class,
                () -> STRICT.readValue("{}", MultiBean.class));
        verifyException(e, "'a'");
        verifyException(e, "'b'");
    }

    @Test
    public void featureDisabledNoChange() throws Exception
    {
        assertNull(LENIENT.readValue("{\"other\":3}", FieldBean.class).name);
        assertNull(LENIENT.readValue("{}", ValueBuilder.class).name);
        assertNull(LENIENT.readValue("{}", Value.class).name);
    }

    @Test
    public void noRequiredPropsUnchanged() throws Exception
    {
        assertNull(STRICT.readValue("{}", PlainBean.class).x);
        assertEquals("a", STRICT.readValue("{\"x\":\"a\"}", PlainBean.class).x);
    }

    @Test
    public void withCreator() throws Exception
    {
        MismatchedInputException e = assertThrows(MismatchedInputException.class,
                () -> STRICT.readValue("{\"id\":\"1\"}", CreatorBean.class));
        verifyException(e, "'extra'");
        CreatorBean b = STRICT.readValue("{\"extra\":\"e\",\"id\":\"1\"}", CreatorBean.class);
        assertEquals("e", b.extra);
        // creator property not reported by this feature
        assertEquals("e", STRICT.readValue("{\"extra\":\"e\"}", CreatorBean.class).extra);
    }

    @Test
    public void builderMissing() throws Exception
    {
        MismatchedInputException e = assertThrows(MismatchedInputException.class,
                () -> STRICT.readValue("{}", Value.class));
        verifyException(e, "'name'");
        assertEquals("n", STRICT.readValue("{\"name\":\"n\"}", Value.class).name);
        assertNull(STRICT.readValue("{\"name\":null}", Value.class).name);
    }

    @Test
    public void viewExcludedPropertyNotRequired() throws Exception
    {
        ObjectReader r = STRICT.readerFor(ViewBean.class).withView(ViewA.class);
        ViewBean b = r.readValue("{\"inView\":\"x\"}");
        assertEquals("x", b.inView);
        assertThrows(MismatchedInputException.class,
                () -> r.readValue("{}"));
    }

    @Test
    public void readerConfigPerCall() throws Exception
    {
        ObjectReader r = LENIENT.readerFor(FieldBean.class);
        assertNull(r.<FieldBean>readValue("{}").name);
        assertThrows(MismatchedInputException.class, () -> r
                .with(DeserializationFeature.FAIL_ON_MISSING_REQUIRED_PROPERTIES)
                .readValue("{}"));
    }
}

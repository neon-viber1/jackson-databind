package tools.jackson.databind.deser.builder;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Annotations of value class are not consulted when deserializing via Builder;
// verifies current behavior and the supported ways to share annotations.
public class BuilderWithValueClassAnnotations2391Test
    extends DatabindTestUtil
{
    static class DoublingDeserializer extends ValueDeserializer<Integer> {
        @Override
        public Integer deserialize(JsonParser p, DeserializationContext ctxt) {
            return p.getIntValue() * 2;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @JacksonAnnotationsInside
    @JsonDeserialize(using = DoublingDeserializer.class)
    @interface Doubling { }

    // Annotation only on value class
    @JsonDeserialize(builder = ValueOnly.Builder.class)
    static final class ValueOnly {
        @Doubling
        final int age;

        ValueOnly(int a) { age = a; }

        @JsonPOJOBuilder(withPrefix = "")
        static final class Builder {
            private int age;
            public Builder age(int a) { age = a; return this; }
            public ValueOnly build() { return new ValueOnly(age); }
        }
    }

    // Annotation on builder too
    @JsonDeserialize(builder = Both.Builder.class)
    static final class Both {
        final int age;

        Both(int a) { age = a; }

        @JsonPOJOBuilder(withPrefix = "")
        static final class Builder {
            @Doubling
            private int age;
            public Builder age(int a) { age = a; return this; }
            public Both build() { return new Both(age); }
        }
    }

    // Value class used as mix-in for the builder: field names must match
    static final class MixInSource {
        @Doubling
        int age;
    }

    private static final String JSON = "{\"age\":5}";

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    void valueClassAnnotationNotAppliedToBuilder() throws Exception {
        assertEquals(5, MAPPER.readValue(JSON, ValueOnly.class).age);
    }

    @Test
    void builderAnnotationApplied() throws Exception {
        assertEquals(10, MAPPER.readValue(JSON, Both.class).age);
    }

    @Test
    void valueClassAsMixInForBuilder() throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .addMixIn(ValueOnly.Builder.class, ValueOnly.class)
                .build();
        assertEquals(10, mapper.readValue(JSON, ValueOnly.class).age);
    }

    @Test
    void dedicatedMixInForBuilder() throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .addMixIn(ValueOnly.Builder.class, MixInSource.class)
                .build();
        assertEquals(10, mapper.readValue(JSON, ValueOnly.class).age);
    }
}

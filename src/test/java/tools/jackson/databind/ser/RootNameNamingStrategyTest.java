package tools.jackson.databind.ser;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonRootName;

import tools.jackson.databind.*;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.AnnotatedClass;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RootNameNamingStrategyTest extends DatabindTestUtil
{
    static class Bean {
        public int x = 1;
    }

    @JsonRootName("Explicit")
    static class Annotated {
        public int x = 2;
    }

    static class LowerRoot extends PropertyNamingStrategy {
        @Override
        public String nameForRootValue(MapperConfig<?> config, AnnotatedClass cls,
                String defaultName) {
            return defaultName.toLowerCase();
        }
    }

    private JsonMapper.Builder wrapping() {
        return JsonMapper.builder()
                .enable(SerializationFeature.WRAP_ROOT_VALUE)
                .enable(DeserializationFeature.UNWRAP_ROOT_VALUE);
    }

    @Test
    public void customStrategyRoundTrip() throws Exception {
        JsonMapper m = wrapping().propertyNamingStrategy(new LowerRoot()).build();
        assertEquals("{\"bean\":{\"x\":1}}", m.writeValueAsString(new Bean()));
        // repeated call, from cache
        assertEquals("{\"bean\":{\"x\":1}}", m.writeValueAsString(new Bean()));
        assertEquals(1, m.readValue("{\"bean\":{\"x\":1}}", Bean.class).x);
        assertEquals("{\"explicit\":{\"x\":2}}", m.writeValueAsString(new Annotated()));
        assertEquals(2, m.readValue("{\"explicit\":{\"x\":2}}", Annotated.class).x);
    }

    @Test
    public void readerWriterUseStrategy() throws Exception {
        JsonMapper m = wrapping().propertyNamingStrategy(new LowerRoot()).build();
        assertEquals("{\"bean\":{\"x\":1}}", m.writer().writeValueAsString(new Bean()));
        assertEquals(1, m.readerFor(Bean.class).readValue("{\"bean\":{\"x\":1}}") instanceof Bean b ? b.x : -1);
    }

    @Test
    public void noStrategyOrBuiltInUnchanged() throws Exception {
        assertEquals("{\"Bean\":{\"x\":1}}",
                wrapping().build().writeValueAsString(new Bean()));
        assertEquals("{\"Bean\":{\"x\":1}}",
                wrapping().propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).build()
                    .writeValueAsString(new Bean()));
        assertEquals("{\"Bean\":{\"x\":1}}",
                wrapping().propertyNamingStrategy(PropertyNamingStrategies.LOWER_CASE).build()
                    .writeValueAsString(new Bean()));
    }
}

package tools.jackson.databind.records;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class CreatorRequiredByDefaultTest extends DatabindTestUtil
{
    record Person(String firstName, String lastName) { }

    record WithOptional(String name, Optional<Integer> age) { }

    static class Pojo {
        final String a, b;
        @JsonCreator
        Pojo(@JsonProperty("a") String a, @JsonProperty("b") String b) { this.a = a; this.b = b; }
    }

    static class Factory {
        String a;
        @JsonCreator
        static Factory make(@JsonProperty("a") String a) { Factory f = new Factory(); f.a = a; return f; }
    }

    static class Delegating {
        String v;
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        Delegating(String v) { this.v = v; }
    }

    static class Injected {
        final String a, id;
        @JsonCreator
        Injected(@JsonProperty("a") String a, @JacksonInject("id") String id) { this.a = a; this.id = id; }
    }

    private final ObjectMapper ENABLED = JsonMapper.builder()
            .enable(MapperFeature.CREATOR_PROPERTIES_REQUIRED_BY_DEFAULT).build();
    private final ObjectMapper DEFAULT = newJsonMapper();

    @Test
    void recordMissingFails() throws Exception {
        MismatchedInputException e = assertThrows(MismatchedInputException.class,
                () -> ENABLED.readValue("{\"firstName\":\"a\"}", Person.class));
        verifyException(e, "lastName");
    }

    @Test
    void recordPresentAndNullPass() throws Exception {
        assertEquals("b", ENABLED.readValue("{\"firstName\":\"a\",\"lastName\":\"b\"}", Person.class).lastName());
        assertNull(ENABLED.readValue("{\"firstName\":\"a\",\"lastName\":null}", Person.class).lastName());
    }

    @Test
    void recordDisabledLenient() throws Exception {
        assertNull(DEFAULT.readValue("{\"firstName\":\"a\"}", Person.class).lastName());
    }

    @Test
    void optionalStaysOptional() throws Exception {
        assertEquals(Optional.empty(), ENABLED.readValue("{\"name\":\"a\"}", WithOptional.class).age());
    }

    @Test
    void pojoAndFactory() throws Exception {
        assertThrows(MismatchedInputException.class, () -> ENABLED.readValue("{\"a\":\"x\"}", Pojo.class));
        assertThrows(MismatchedInputException.class, () -> ENABLED.readValue("{}", Factory.class));
        assertEquals("x", ENABLED.readValue("{\"a\":\"x\"}", Factory.class).a);
    }

    @Test
    void delegatingUnaffected() throws Exception {
        assertEquals("x", ENABLED.readValue("\"x\"", Delegating.class).v);
    }

    @Test
    void injectedUnaffected() throws Exception {
        Injected i = ENABLED.readerFor(Injected.class)
                .with(new InjectableValues.Std().addValue("id", "I"))
                .readValue("{\"a\":\"x\"}");
        assertEquals("I", i.id);
    }
}

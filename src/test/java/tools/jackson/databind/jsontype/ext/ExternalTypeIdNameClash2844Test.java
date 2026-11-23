package tools.jackson.databind.jsontype.ext;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// Duplicate "type" property when external type id name matches a POJO property
public class ExternalTypeIdNameClash2844Test extends DatabindTestUtil
{
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type",
            include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
    @JsonSubTypes(@JsonSubTypes.Type(value = Dog.class, name = "dog"))
    static abstract class AnimalDetails { }

    @JsonTypeName("dog")
    static class Dog extends AnimalDetails {
        public String type;

        public Dog() { }
        public Dog(String t) { type = t; }
    }

    @JsonPropertyOrder({ "type", "details" })
    static class Animal {
        public String type;

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type",
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
        @JsonSubTypes(@JsonSubTypes.Type(value = Puppy.class, name = "puppy"))
        public Pet details;

        public Animal() { }
        public Animal(String t, Pet d) { type = t; details = d; }
    }

    static abstract class Pet { }

    @JsonTypeName("puppy")
    static class Puppy extends Pet {
        public int age;
    }

    @JsonPropertyOrder({ "kind", "details" })
    static class NoClash {
        public String kind;

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type",
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY)
        @JsonSubTypes(@JsonSubTypes.Type(value = Puppy.class, name = "puppy"))
        public Pet details;
    }

    @JsonPropertyOrder({ "kind", "details" })
    static class IgnoredClash {
        @JsonIgnore
        public String type;

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type",
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY)
        @JsonSubTypes(@JsonSubTypes.Type(value = Puppy.class, name = "puppy"))
        public Pet details;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void containerClash() throws Exception
    {
        Puppy p = new Puppy();
        p.age = 2;
        String json = MAPPER.writeValueAsString(new Animal("GermanShepherd", p));
        assertEquals(a2q("{'type':'GermanShepherd','details':{'age':2}}"), json);
        JsonNode tree = MAPPER.valueToTree(new Animal("GermanShepherd", p));
        assertEquals(2, tree.size());
        assertEquals("GermanShepherd", tree.path("type").asString());
    }

    @Test
    public void containerClashRoundTrip() throws Exception
    {
        Puppy p = new Puppy();
        p.age = 2;
        String json = MAPPER.writeValueAsString(new Animal("puppy", p));
        Animal result = MAPPER.readValue(json, Animal.class);
        assertEquals("puppy", result.type);
        assertEquals(Puppy.class, result.details.getClass());
        assertEquals(2, ((Puppy) result.details).age);
    }

    @Test
    public void rootClash() throws Exception
    {
        assertEquals(a2q("{'type':'GermanShepherd'}"),
                MAPPER.writeValueAsString(new Dog("GermanShepherd")));
        assertEquals(1, MAPPER.valueToTree(new Dog("GermanShepherd")).size());
    }

    @Test
    public void noClashStillWritesTypeId() throws Exception
    {
        NoClash v = new NoClash();
        v.kind = "x";
        v.details = new Puppy();
        assertEquals(a2q("{'kind':'x','details':{'age':0},'type':'puppy'}"),
                MAPPER.writeValueAsString(v));
    }

    @Test
    public void ignoredClashStillWritesTypeId() throws Exception
    {
        IgnoredClash v = new IgnoredClash();
        v.type = "ignored";
        v.details = new Puppy();
        String json = MAPPER.writeValueAsString(v);
        assertEquals(a2q("{'details':{'age':0},'type':'puppy'}"), json);
    }
}

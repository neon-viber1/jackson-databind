package tools.jackson.databind.deser.builder;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
import tools.jackson.databind.exc.InvalidDefinitionException;

import static org.junit.jupiter.api.Assertions.*;

import static tools.jackson.databind.testutil.DatabindTestUtil.*;

// Tests for `@JsonDeserialize.builderMethod`
public class BuilderViaFactoryMethodTest
{
    // SuperBuilder-style layout: abstract public builder, private concrete impl
    @JsonDeserialize(builder = SuperValue.AbstractBuilder.class, builderMethod = "builder")
    static final class SuperValue {
        final int x;
        final String name;

        SuperValue(AbstractBuilder<?, ?> b) {
            x = b.x;
            name = b.name;
        }

        public static AbstractBuilder<?, ?> builder() {
            return new BuilderImpl();
        }

        abstract static class AbstractBuilder<C extends SuperValue, B extends AbstractBuilder<C, B>> {
            int x;
            String name;

            protected abstract B self();
            public abstract C build();

            public B withX(int x) { this.x = x; return self(); }
            public B withName(String n) { this.name = n; return self(); }
        }

        private static final class BuilderImpl extends AbstractBuilder<SuperValue, BuilderImpl> {
            private BuilderImpl() { }
            @Override protected BuilderImpl self() { return this; }
            @Override public SuperValue build() { return new SuperValue(this); }
        }
    }

    // Builder with private constructor, custom prefix/build method
    @JsonDeserialize(builder = CustomValue.Builder.class, builderMethod = "newBuilder")
    static class CustomValue {
        final int a;
        CustomValue(int a) { this.a = a; }

        static Builder newBuilder() { return new Builder(); }

        @JsonPOJOBuilder(withPrefix = "set", buildMethodName = "create")
        static class Builder {
            int a;
            private Builder() { }
            public Builder setA(int a) { this.a = a; return this; }
            public CustomValue create() { return new CustomValue(a); }
        }
    }

    // Builder method supplied via mix-in
    static class MixedValue {
        final int a;
        MixedValue(int a) { this.a = a; }
        static Builder make() { return new Builder(); }

        static class Builder {
            int a;
            private Builder() { }
            public Builder withA(int a) { this.a = a; return this; }
            public MixedValue build() { return new MixedValue(a); }
        }
    }

    @JsonDeserialize(builder = MixedValue.Builder.class, builderMethod = "make")
    static class MixIn { }

    // Failure cases
    @JsonDeserialize(builder = Plain.Builder.class, builderMethod = "nope")
    static class MissingMethod {
        static class Builder { public MissingMethod build() { return null; } }
    }

    @JsonDeserialize(builder = Plain.Builder.class, builderMethod = "builder")
    static class NonStatic {
        public Plain.Builder builder() { return new Plain.Builder(); }
    }

    @JsonDeserialize(builder = Plain.Builder.class, builderMethod = "builder")
    static class WithArgs {
        public static Plain.Builder builder(int x) { return new Plain.Builder(); }
    }

    @JsonDeserialize(builder = Plain.Builder.class, builderMethod = "builder")
    static class WrongReturn {
        public static String builder() { return ""; }
    }

    @JsonDeserialize(builderMethod = "builder")
    static class NoBuilder {
        public int x;
        public static Plain.Builder builder() { return new Plain.Builder(); }
    }

    // Existing behavior: builder only
    @JsonDeserialize(builder = Plain.Builder.class)
    static class Plain {
        final int x;
        Plain(int x) { this.x = x; }

        static class Builder {
            int x;
            public Builder withX(int x) { this.x = x; return this; }
            public Plain build() { return new Plain(x); }
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void superBuilderLayout() throws Exception {
        SuperValue v = MAPPER.readValue("""
                {"x":3,"name":"foo"}
                """, SuperValue.class);
        assertEquals(3, v.x);
        assertEquals("foo", v.name);
    }

    @Test
    public void customPrefixAndBuildMethod() throws Exception {
        assertEquals(7, MAPPER.readValue("{\"a\":7}", CustomValue.class).a);
    }

    @Test
    public void mixInAnnotation() throws Exception {
        ObjectMapper m = jsonMapperBuilder().addMixIn(MixedValue.class, MixIn.class).build();
        assertEquals(5, m.readValue("{\"a\":5}", MixedValue.class).a);
    }

    @Test
    public void builderOnlyUnchanged() throws Exception {
        assertEquals(2, MAPPER.readValue("{\"x\":2}", Plain.class).x);
    }

    @Test
    public void failMissingMethod() throws Exception {
        _verifyFail(MissingMethod.class, "MissingMethod`.nope()", "no no-argument method");
    }

    @Test
    public void failNonStatic() throws Exception {
        _verifyFail(NonStatic.class, "NonStatic`.builder()", "not static");
    }

    @Test
    public void failWithArguments() throws Exception {
        _verifyFail(WithArgs.class, "WithArgs`.builder()", "no no-argument method");
    }

    @Test
    public void failIncompatibleReturnType() throws Exception {
        _verifyFail(WrongReturn.class, "WrongReturn`.builder()", "not assignable");
    }

    @Test
    public void failWithoutBuilder() throws Exception {
        _verifyFail(NoBuilder.class, "NoBuilder`.builder()", "requires `builder`");
    }

    private void _verifyFail(Class<?> cls, String... fragments) throws Exception {
        try {
            MAPPER.readValue("{}", cls);
            fail("Should not pass");
        } catch (InvalidDefinitionException e) {
            for (String f : fragments) {
                assertTrue(e.getMessage().contains(f), "Missing '" + f + "' in: " + e.getMessage());
            }
        }
    }
}

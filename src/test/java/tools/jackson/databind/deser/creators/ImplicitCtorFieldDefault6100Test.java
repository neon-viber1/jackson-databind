package tools.jackson.databind.deser.creators;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;

import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// Documents how a field initializer interacts with an implicitly detected
// all-args constructor when the property is absent from input.
// Relies on constructor parameter names being available (test build uses `-parameters`).
public class ImplicitCtorFieldDefault6100Test extends DatabindTestUtil
{
    static class PrimitiveBean {
        public boolean historical = true;
        public PrimitiveBean() { }
        public PrimitiveBean(boolean historical) { this.historical = historical; }
    }

    static class BoxedBean {
        public Boolean historical = true;
        public BoxedBean() { }
        public BoxedBean(Boolean historical) { this.historical = historical; }
    }

    static class AnnotatedDefaultCtorBean {
        public boolean historical = true;
        @JsonCreator
        public AnnotatedDefaultCtorBean() { }
        public AnnotatedDefaultCtorBean(boolean historical) { this.historical = historical; }
    }

    static class AllArgsOnlyBean {
        public boolean historical = true;
        public AllArgsOnlyBean(boolean historical) { this.historical = historical; }
    }

    private static final String EMPTY = "{}";

    private final ObjectMapper DEFAULT_MAPPER = newJsonMapper();

    @Test
    public void primitiveAllArgsCtorWinsByDefault() throws Exception {
        assertFalse(DEFAULT_MAPPER.readValue(EMPTY, PrimitiveBean.class).historical);
    }

    @Test
    public void boxedAllArgsCtorWinsByDefault() throws Exception {
        assertNull(DEFAULT_MAPPER.readValue(EMPTY, BoxedBean.class).historical);
    }

    @Test
    public void allArgsOnlyUsesTypeDefault() throws Exception {
        assertFalse(DEFAULT_MAPPER.readValue(EMPTY, AllArgsOnlyBean.class).historical);
    }

    @Test
    public void disableParameterNameDetectionKeepsFieldDefault() throws Exception {
        ObjectMapper m = JsonMapper.builder()
                .disable(MapperFeature.DETECT_PARAMETER_NAMES).build();
        assertTrue(m.readValue(EMPTY, PrimitiveBean.class).historical);
        assertTrue(m.readValue(EMPTY, BoxedBean.class).historical);
    }

    @Test
    public void explicitCreatorOnDefaultCtorKeepsFieldDefault() throws Exception {
        assertTrue(DEFAULT_MAPPER.readValue(EMPTY, AnnotatedDefaultCtorBean.class).historical);
    }
}

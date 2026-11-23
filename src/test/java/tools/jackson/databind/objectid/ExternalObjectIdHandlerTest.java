package tools.jackson.databind.objectid;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.util.ExternalObjectIdHandler;

import static org.junit.jupiter.api.Assertions.*;

public class ExternalObjectIdHandlerTest extends DatabindTestUtil
{
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "name")
    static class Lib {
        public String name;
        public int value;

        protected Lib() { }
        public Lib(String n, int v) { name = n; value = v; }
    }

    static class Holder {
        public Lib first;
        public Lib second;
        public List<Lib> list = new ArrayList<>();
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class)
    static class Seq {
        public int x = 1;
    }

    static class SeqHolder {
        public Seq a, b;
    }

    static class MapHandler implements ExternalObjectIdHandler {
        final Map<Object, Object> byObject = new HashMap<>();
        final Map<Object, Object> byId = new HashMap<>();

        MapHandler register(Object id, Object ob) {
            byObject.put(ob, id);
            byId.put(id, ob);
            return this;
        }

        @Override
        public Object findExternalId(Object pojo, com.fasterxml.jackson.annotation.ObjectIdGenerator<?> g) {
            return byObject.get(pojo);
        }

        @Override
        public Object findExternalObject(com.fasterxml.jackson.annotation.ObjectIdGenerator.IdKey key) {
            return byId.get(key.key);
        }
    }

    private final Lib lib = new Lib("libA", 42);

    @Test
    public void serializeExternalAsIdOnly() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .externalObjectIdHandler(new MapHandler().register("libA", lib)).build();
        Holder h = new Holder();
        Lib local = new Lib("local", 1);
        h.first = lib;
        h.second = local;
        h.list.add(lib);
        h.list.add(local);
        assertEquals(a2q("{'first':'libA','list':['libA',{'name':'local','value':1}],'second':'local'}"),
                mapper.writeValueAsString(h));
    }

    @Test
    public void deserializeExternalToLibraryObject() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .externalObjectIdHandler(new MapHandler().register("libA", lib)).build();
        Holder h = mapper.readValue(a2q(
                "{'first':'libA','second':{'name':'local','value':1},'list':['libA','local']}"), Holder.class);
        assertSame(lib, h.first);
        assertEquals("local", h.second.name);
        assertSame(lib, h.list.get(0));
        assertSame(h.second, h.list.get(1));
    }

    @Test
    public void nonIntegerIdViaGeneratorAndNumericId() throws Exception
    {
        Seq s = new Seq();
        ObjectMapper mapper = JsonMapper.builder()
                .externalObjectIdHandler(new MapHandler().register(77, s)).build();
        SeqHolder in = new SeqHolder();
        in.a = s;
        in.b = new Seq();
        assertEquals(a2q("{'a':77,'b':{'@id':1,'x':1}}"), mapper.writeValueAsString(in));
        SeqHolder out = mapper.readValue("{\"a\":77,\"b\":{\"x\":3}}", SeqHolder.class);
        assertSame(s, out.a);
    }

    @Test
    public void unknownIdsKeepNormalBehavior() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .externalObjectIdHandler(new MapHandler().register("libA", lib)).build();
        Holder h = new Holder();
        h.first = new Lib("x", 5);
        h.second = h.first;
        assertEquals(a2q("{'first':{'name':'x','value':5},'list':[],'second':'x'}"),
                mapper.writeValueAsString(h));
        assertThrows(Exception.class, () -> mapper.readValue(a2q("{'first':'nope'}"), Holder.class));
    }

    @Test
    public void noHandlerDefaultUnchanged() throws Exception
    {
        ObjectMapper mapper = newJsonMapper();
        Holder h = new Holder();
        h.first = lib;
        assertEquals(a2q("{'first':{'name':'libA','value':42},'list':[],'second':null}"),
                mapper.writeValueAsString(h));
    }

    static class LibModule extends SimpleModule {
        private static final long serialVersionUID = 1L;
        @Override
        public void setupModule(SetupContext context) {
            super.setupModule(context);
            context.setExternalObjectIdHandler(new MapHandler().register("libA", new Lib("libA", 42)));
        }
    }

    @Test
    public void handlerViaModule() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder().addModule(new LibModule()).build();
        Holder h = mapper.readValue(a2q("{'first':'libA'}"), Holder.class);
        assertEquals(42, h.first.value);
    }

    @Test
    public void handlerRetainedByReaderAndWriter() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .externalObjectIdHandler(new MapHandler().register("libA", lib)).build();
        Holder h = new Holder();
        h.first = lib;
        ObjectWriter w = mapper.writer().with(SerializationFeature.INDENT_OUTPUT);
        assertTrue(w.writeValueAsString(h).contains("\"first\" : \"libA\""));
        ObjectReader r = mapper.readerFor(Holder.class).with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        assertSame(lib, r.<Holder>readValue(a2q("{'first':'libA'}")).first);
    }

    @Test
    public void dynamicHandlerChangesBetweenCalls() throws Exception
    {
        MapHandler handler = new MapHandler();
        ObjectMapper mapper = JsonMapper.builder().externalObjectIdHandler(handler).build();
        Holder h = new Holder();
        h.first = lib;
        assertTrue(mapper.writeValueAsString(h).contains("\"value\":42"));
        handler.register("libA", lib);
        assertEquals(a2q("{'first':'libA','list':[],'second':null}"), mapper.writeValueAsString(h));
    }
}

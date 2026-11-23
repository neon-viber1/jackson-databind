package tools.jackson.databind.node;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.JsonNodeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for optional tracking of token locations ({@link JsonNodeFeature#TRACK_TOKEN_LOCATIONS})
 * and their exposure via {@link TreeTraversingParser}.
 */
public class TreeTraversingParserLocationTest extends DatabindTestUtil
{
    private final static String DOC = """
{"a": 1,
 "b": [true, null, "x"],
 "c": {"d": 2.5}}""";

    private final ObjectMapper TRACKING = JsonMapper.builder()
            .enable(JsonNodeFeature.TRACK_TOKEN_LOCATIONS).build();
    private final ObjectMapper PLAIN = newJsonMapper();

    @Test
    public void featureOffGivesNA() throws Exception {
        JsonNode root = PLAIN.readTree(DOC);
        assertEquals(TokenStreamLocation.NA, root.tokenLocation());
        assertEquals(TokenStreamLocation.NA, root.get("a").tokenLocation());
        try (JsonParser p = PLAIN.treeAsTokens(root)) {
            while (p.nextToken() != null) {
                assertEquals(TokenStreamLocation.NA, p.currentTokenLocation());
                assertEquals(TokenStreamLocation.NA, p.currentLocation());
            }
        }
    }

    @Test
    public void nodeLocations() throws Exception {
        JsonNode root = TRACKING.readTree(DOC);
        assertLoc(root, 1, 1, 0);
        assertLoc(root.get("a"), 1, 7, 6);
        JsonNode arr = root.get("b");
        assertLoc(arr, 2, 7, 15);
        assertLoc(arr.get(0), 2, 8, 16);
        assertLoc(arr.get(1), 2, 14, 22);
        assertLoc(arr.get(2), 2, 20, 28);
        assertLoc(root.get("c"), 3, 7, 40);
        assertLoc(root.get("c").get("d"), 3, 13, 46);
    }

    @Test
    public void objectReaderReadTree() throws Exception {
        JsonNode root = TRACKING.reader().readTree(DOC);
        assertLoc(root.get("a"), 1, 7, 6);
        JsonNode n = TRACKING.readerFor(JsonNode.class).readValue("\n  true");
        assertLoc(n, 2, 3, 3);
        JsonNode ob = TRACKING.readerFor(ObjectNode.class).readValue(DOC);
        assertLoc(ob.get("c"), 3, 7, 40);
    }

    @Test
    public void sharedSingletonsNotMutated() throws Exception {
        JsonNode root = TRACKING.readTree("[null, true, false]");
        assertNotSame(NullNode.instance, root.get(0));
        assertNotSame(BooleanNode.TRUE, root.get(1));
        assertNotSame(BooleanNode.FALSE, root.get(2));
        assertEquals(NullNode.instance, root.get(0));
        assertEquals(BooleanNode.TRUE, root.get(1));
        assertEquals(BooleanNode.FALSE, root.get(2));
        assertEquals(TokenStreamLocation.NA, NullNode.instance.tokenLocation());
        assertEquals(TokenStreamLocation.NA, BooleanNode.TRUE.tokenLocation());
        assertEquals(TokenStreamLocation.NA, BooleanNode.FALSE.tokenLocation());
        assertEquals(TokenStreamLocation.NA, MissingNode.getInstance().tokenLocation());
        // and cached small ints as well
        JsonNode i = TRACKING.readTree("  1");
        assertLoc(i, 1, 3, 2);
        assertEquals(TokenStreamLocation.NA, IntNode.valueOf(1).tokenLocation());
    }

    @Test
    public void equalityIgnoresLocation() throws Exception {
        JsonNode a = TRACKING.readTree(DOC);
        JsonNode b = PLAIN.readTree(DOC);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertEquals(PLAIN.writeValueAsString(b), TRACKING.writeValueAsString(a));
        assertEquals(b.toString(), a.toString());
    }

    @Test
    public void deepCopyOfContainers() throws Exception {
        JsonNode root = TRACKING.readTree(DOC);
        JsonNode copy = root.deepCopy();
        assertEquals(root, copy);
        assertEquals(TokenStreamLocation.NA, copy.tokenLocation());
        // programmatically built nodes have none
        assertEquals(TokenStreamLocation.NA, TRACKING.createObjectNode().put("a", 1)
                .tokenLocation());
    }

    @Test
    public void treeAsTokensLocations() throws Exception {
        JsonNode root = TRACKING.readTree(DOC);
        try (JsonParser p = TRACKING.treeAsTokens(root)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertLoc(p, 1, 1, 0);
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertLoc(p, 1, 7, 6); // value location
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertLoc(p, 1, 7, 6);
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            assertLoc(p, 2, 7, 15);
            assertToken(JsonToken.VALUE_TRUE, p.nextToken());
            assertLoc(p, 2, 8, 16);
            assertToken(JsonToken.VALUE_NULL, p.nextToken());
            assertLoc(p, 2, 14, 22);
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertLoc(p, 2, 20, 28);
            assertToken(JsonToken.END_ARRAY, p.nextToken());
            assertLoc(p, 2, 7, 15);
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertLoc(p, 3, 7, 40);
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertLoc(p, 3, 13, 46);
            assertToken(JsonToken.END_OBJECT, p.nextToken());
            assertLoc(p, 3, 7, 40);
            assertToken(JsonToken.END_OBJECT, p.nextToken());
            assertLoc(p, 1, 1, 0);
            assertNull(p.nextToken());
            assertEquals(TokenStreamLocation.NA, p.currentTokenLocation());
        }
    }

    private void assertLoc(JsonNode n, int line, int col, long offset) {
        assertLoc(n.tokenLocation(), line, col, offset);
    }

    private void assertLoc(JsonParser p, int line, int col, long offset) {
        assertLoc(p.currentTokenLocation(), line, col, offset);
        assertLoc(p.currentLocation(), line, col, offset);
    }

    private void assertLoc(TokenStreamLocation loc, int line, int col, long offset) {
        assertEquals(line, loc.getLineNr(), "line");
        assertEquals(col, loc.getColumnNr(), "column");
        assertEquals(offset, loc.getCharOffset(), "offset");
    }
}

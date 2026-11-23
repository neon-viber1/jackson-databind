package tools.jackson.databind.deser.impl;

import java.io.Writer;

import tools.jackson.core.*;
import tools.jackson.core.sym.PropertyNameMatcher;
import tools.jackson.core.util.JsonParserDelegate;
import tools.jackson.databind.deser.ValuePreprocessor;

/**
 * {@link JsonParser} wrapper that passes values of String tokens through
 * a {@link ValuePreprocessor}, so that all accessors expose the replaced value.
 *
 * @since 3.4
 */
public class PreprocessingParser extends JsonParserDelegate
{
    protected final ValuePreprocessor _preprocessor;

    /**
     * Preprocessed value of the current token, if already calculated.
     */
    protected String _processed;

    public PreprocessingParser(JsonParser p, ValuePreprocessor preprocessor) {
        super(p);
        _preprocessor = preprocessor;
    }

    /**
     * Helper method for wrapping given parser, unless there is nothing to do.
     */
    public static JsonParser wrapIfNeeded(JsonParser p, ValuePreprocessor preprocessor) {
        if ((preprocessor == null) || (p instanceof PreprocessingParser)) {
            return p;
        }
        return new PreprocessingParser(p, preprocessor);
    }

    /*
    /**********************************************************************
    /* Token advancement: need to reset cached value
    /**********************************************************************
     */

    @Override
    public JsonToken nextToken() throws JacksonException {
        _processed = null;
        return super.nextToken();
    }

    @Override
    public String nextName() throws JacksonException {
        _processed = null;
        return super.nextName();
    }

    @Override
    public boolean nextName(SerializableString str) throws JacksonException {
        _processed = null;
        return super.nextName(str);
    }

    @Override
    public int nextNameMatch(PropertyNameMatcher matcher) throws JacksonException {
        _processed = null;
        return super.nextNameMatch(matcher);
    }

    @Override
    public int nextNameMatchAndToken(PropertyNameMatcher matcher) throws JacksonException {
        _processed = null;
        return super.nextNameMatchAndToken(matcher);
    }

    @Override
    public void clearCurrentToken() {
        _processed = null;
        super.clearCurrentToken();
    }

    /*
    /**********************************************************************
    /* Text accessors
    /**********************************************************************
     */

    protected String _processed(String raw) {
        String result = _processed;
        if (result == null) {
            String name = _enclosingName();
            result = _preprocessor.preprocess(name, raw);
            if (result == null) {
                result = raw;
            }
            _processed = result;
        }
        return result;
    }

    protected String _enclosingName() {
        TokenStreamContext ctxt = streamReadContext();
        // Elements of arrays use name of the property that contains the array
        while ((ctxt != null) && ctxt.inArray()) {
            ctxt = ctxt.getParent();
        }
        return (ctxt == null) ? null : ctxt.currentName();
    }

    @Override
    public String getString() throws JacksonException {
        String raw = delegate.getString();
        return (delegate.currentToken() == JsonToken.VALUE_STRING) ? _processed(raw) : raw;
    }

    @Override
    public String getText() throws JacksonException {
        return getString();
    }

    @Override
    public String getValueAsString() {
        return (delegate.currentToken() == JsonToken.VALUE_STRING)
                ? getString() : delegate.getValueAsString();
    }

    @Override
    public String getValueAsString(String def) {
        return (delegate.currentToken() == JsonToken.VALUE_STRING)
                ? getString() : delegate.getValueAsString(def);
    }

    @Override
    public int getString(Writer writer) throws JacksonException {
        if (delegate.currentToken() == JsonToken.VALUE_STRING) {
            String str = getString();
            try {
                writer.write(str);
            } catch (java.io.IOException e) {
                throw _constructReadException("Failed to write String value: "+e.getMessage(), e);
            }
            return str.length();
        }
        return delegate.getString(writer);
    }

    @Override
    public long readString(Writer writer) throws JacksonException {
        if (delegate.currentToken() == JsonToken.VALUE_STRING) {
            return getString(writer);
        }
        return delegate.readString(writer);
    }

    @Override
    public boolean hasStringCharacters() {
        // Characters of processed String are not directly accessible
        return (delegate.currentToken() != JsonToken.VALUE_STRING)
                && delegate.hasStringCharacters();
    }

    @Override
    public char[] getStringCharacters() throws JacksonException {
        if (delegate.currentToken() == JsonToken.VALUE_STRING) {
            return getString().toCharArray();
        }
        return delegate.getStringCharacters();
    }

    @Override
    public int getStringLength() throws JacksonException {
        if (delegate.currentToken() == JsonToken.VALUE_STRING) {
            return getString().length();
        }
        return delegate.getStringLength();
    }

    @Override
    public int getStringOffset() throws JacksonException {
        if (delegate.currentToken() == JsonToken.VALUE_STRING) {
            return 0;
        }
        return delegate.getStringOffset();
    }

    @Override
    public char[] getTextCharacters() throws JacksonException {
        return getStringCharacters();
    }

    @Override
    public int getTextLength() throws JacksonException {
        return getStringLength();
    }

    @Override
    public int getTextOffset() throws JacksonException {
        return getStringOffset();
    }
}

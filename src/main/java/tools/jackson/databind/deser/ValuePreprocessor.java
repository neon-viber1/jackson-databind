package tools.jackson.databind.deser;

/**
 * Handler that can be registered to modify (pre-process) String values
 * read from input, before any other databinding functionality sees them;
 * for example to implement variable substitution.
 * Registered via {@code MapperBuilder.valuePreprocessor(ValuePreprocessor)}
 * or per-call with {@code ObjectReader.with(ValuePreprocessor)}
 * (and removed with {@code ObjectReader.withoutValuePreprocessor()}).
 *<p>
 * Preprocessing is applied only to values of String tokens: property names,
 * numbers, booleans and other scalar values are not passed to the handler, and neither
 * is the input document as a whole. Each String value is passed separately,
 * including each String element of an array. This applies to all
 * content bound by databind (POJOs, {@code Map}s, {@code List}s, untyped
 * {@code Object}s and {@code JsonNode}s), including when the caller
 * passes a {@code JsonParser} to read from.
 *<p>
 * Implementations should be thread-safe and free of side effects, since
 * the same instance may be used by many threads, and may be called more than once
 * for the same value.
 *
 * @since 3.4
 */
@FunctionalInterface
public interface ValuePreprocessor
{
    /**
     * Method called with a String value read, to allow replacing it.
     *
     * @param name Name of the property that contains the value: for array
     *    elements, the name of the property that contains the array (for nested
     *    arrays, the outermost enclosing one that has a name);
     *    {@code null} if there is no enclosing property (for example for
     *    a root-level value or a value in a root-level array).
     * @param value Value as read from input (never {@code null})
     *
     * @return Value to use instead; if {@code null}, value is left as is
     *    (it is not possible to replace a String with a {@code null} value
     *    using this handler)
     */
    String preprocess(String name, String value);
}

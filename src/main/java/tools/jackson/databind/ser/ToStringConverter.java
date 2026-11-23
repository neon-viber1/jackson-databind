package tools.jackson.databind.ser;

import tools.jackson.databind.SerializationContext;

/**
 * Optional "tag-on" interface that can be implemented by a
 * {@link tools.jackson.databind.ValueSerializer}, either a regular value
 * serializer or a key serializer, to indicate that values it handles
 * can be converted into a plain {@link String}; this is useful for
 * "stringified" serialization of structured types (for example in
 * formats like CSV or Properties).
 *<p>
 * Converters are accessed through key serializers: caller finds the key
 * serializer for a type with
 * {@link SerializationContext#findKeySerializer(tools.jackson.databind.JavaType, tools.jackson.databind.BeanProperty)}
 * (which also contextualizes it) and checks whether it implements this interface.
 * If it does not, no converter is available for the type.
 *<p>
 * Databind itself does not use this interface; it is meant for
 * modules and applications.
 */
public interface ToStringConverter
{
    /**
     * Method for converting given (non-null) value into a String.
     *
     * @param ctxt Serialization context
     * @param value Value to convert; not {@code null}
     *
     * @return String representation of the value
     */
    String convertToString(SerializationContext ctxt, Object value);
}

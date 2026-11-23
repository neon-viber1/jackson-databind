package tools.jackson.databind.util;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;

/**
 * Handler that can supply pre-defined ("external") Object Ids, for objects
 * whose identity is managed outside the document being written or read
 * (for example, objects from a shared library or registry).
 *<p>
 * Handler is registered on the mapper (see
 * {@link tools.jackson.databind.cfg.MapperBuilder#externalObjectIdHandler}) or by a
 * module, and is consulted <b>before</b> the per-call Object Id tables, so the set
 * of known ids may be open-ended or dynamic. Only a single handler can be registered;
 * callers needing several sources should write a delegating implementation.
 *<p>
 * Implementations must be thread-safe: Jackson keeps no per-handler state.
 *
 * @since 3.4
 */
public interface ExternalObjectIdHandler
{
    /**
     * Called when serializing an object that has Object Id handling enabled.
     * If a non-null id is returned, the object is written as that id only
     * (never as a full Object), wherever it occurs.
     *
     * @param pojo Value being serialized
     * @param generator Generator configured for the type being serialized
     *
     * @return External id for the value, or {@code null} if value is not external
     */
    default Object findExternalId(Object pojo, ObjectIdGenerator<?> generator) {
        return null;
    }

    /**
     * Called when deserializing a reference to an Object Id, before local ids
     * are considered.
     *
     * @param key Key (id value and scope) being referenced
     *
     * @return Object that the id refers to, or {@code null} if id is not external
     */
    default Object findExternalObject(ObjectIdGenerator.IdKey key) {
        return null;
    }
}

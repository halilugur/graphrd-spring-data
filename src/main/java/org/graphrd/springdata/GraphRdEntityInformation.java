package org.graphrd.springdata;

import java.lang.reflect.Field;

import org.springframework.data.repository.core.support.AbstractEntityInformation;

import org.graphrd.rdf.RdfEntityManager;
import org.graphrd.rdf.RdfId;

/** Entity metadata for Spring Data: the id of an entity is its IRI. */
class GraphRdEntityInformation<T> extends AbstractEntityInformation<T, String> {

    private final Field id;

    GraphRdEntityInformation(Class<T> domainType, RdfEntityManager em) {
        super(domainType);
        this.id = idField(domainType);
    }

    /**
     * The IRI as it stands — null for an unsaved entity, so {@code isNew()}
     * answers truthfully (asking the manager would mint a generated IRI as a
     * side effect, or throw for a non-generated one).
     */
    @Override
    public String getId(T entity) {
        try {
            Object v = id.get(entity);
            return v == null ? null : v.toString();
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Field idField(Class<?> type) {
        for (Class<?> k = type; k != null && k != Object.class; k = k.getSuperclass()) {
            for (Field f : k.getDeclaredFields()) {
                if (f.isAnnotationPresent(RdfId.class)) {
                    f.setAccessible(true);
                    return f;
                }
            }
        }
        throw new IllegalArgumentException(type + " has no @RdfId field");
    }

    @Override
    public Class<String> getIdType() {
        return String.class;
    }
}

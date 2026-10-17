package org.graphrd.springdata;

import org.springframework.data.repository.core.support.AbstractEntityInformation;

import org.graphrd.rdf.RdfEntityManager;

/** Entity metadata for Spring Data: the id of an entity is its IRI. */
class GraphRdEntityInformation<T> extends AbstractEntityInformation<T, String> {

    private final RdfEntityManager em;

    GraphRdEntityInformation(Class<T> domainType, RdfEntityManager em) {
        super(domainType);
        this.em = em;
    }

    @Override
    public String getId(T entity) {
        return em.idOfPublic(entity);
    }

    @Override
    public Class<String> getIdType() {
        return String.class;
    }
}

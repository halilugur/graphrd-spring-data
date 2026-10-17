package org.graphrd.springdata;

import org.graphrd.rdf.EntityQuery;

/**
 * A dynamic query, the way JPA's Specification is one — but over the
 * mapper's criteria. Compose them with {@link GraphRdSpecifications} and
 * hand them to the repository: {@code employees.findAll(salaryAbove(70000),
 * pageable)}. No custom repository implementation for a search, ever.
 */
@FunctionalInterface
public interface GraphRdSpecification<T> {

    /** Refine the query with this condition. */
    EntityQuery<T> apply(EntityQuery<T> query);

    /** Both conditions. */
    default GraphRdSpecification<T> and(GraphRdSpecification<T> other) {
        return q -> other.apply(this.apply(q));
    }
}

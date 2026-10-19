package org.graphrd.springdata;

import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.ValueExpressionDelegate;
import org.springframework.data.repository.query.RepositoryQuery;

import org.graphrd.rdf.RdfEntityManager;

/** Creates the backing repository and turns derived methods into GraphRD queries. */
class GraphRdRepositoryFactory extends RepositoryFactorySupport {

    private final RdfEntityManager em;

    GraphRdRepositoryFactory(RdfEntityManager em) {
        this.em = em;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T, ID> EntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {
        return (EntityInformation<T, ID>) new GraphRdEntityInformation<>(domainClass, em);
    }

    @Override
    protected Object getTargetRepository(RepositoryInformation metadata) {
        return new SimpleGraphRdRepository<>(metadata.getDomainType(), em);
    }

    @Override
    protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
        return SimpleGraphRdRepository.class;
    }

    @Override
    protected java.util.Optional<QueryLookupStrategy> getQueryLookupStrategy(
            QueryLookupStrategy.Key key, ValueExpressionDelegate valueExpressionDelegate) {
        return java.util.Optional.of((method, metadata, projections, namedQueries) -> {
            Query annotation = method.getAnnotation(Query.class);
            return annotation != null
                    ? new GraphRdStringQuery(method, metadata, projections, annotation, em)
                    : new GraphRdPartQuery(method, metadata, projections, em);
        });
    }
}

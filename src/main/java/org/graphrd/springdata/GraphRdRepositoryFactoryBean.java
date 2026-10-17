package org.graphrd.springdata;

import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

import org.graphrd.rdf.RdfEntityManager;

/**
 * The bridge Spring Data instantiates per repository interface; carries the
 * manager to the factory. Constructed with the repository interface, wired
 * with the {@code rdfEntityManager} bean by the configuration extension.
 */
public class GraphRdRepositoryFactoryBean<T extends GraphRdRepository<S, ID>, S, ID>
        extends RepositoryFactoryBeanSupport<T, S, ID> {

    private RdfEntityManager rdfEntityManager;

    @SuppressWarnings("unused") // Spring Data passes the repository interface here
    public GraphRdRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
        super(repositoryInterface);
    }

    @SuppressWarnings("unused") // set from the configuration extension
    public void setRdfEntityManager(RdfEntityManager em) {
        this.rdfEntityManager = em;
    }

    @Override
    protected RepositoryFactorySupport createRepositoryFactory() {
        return new GraphRdRepositoryFactory(rdfEntityManager);
    }
}

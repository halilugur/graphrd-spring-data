package org.graphrd.springdata;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

import org.graphrd.rdf.RdfEntityManager;

/**
 * The bridge Spring Data instantiates per repository interface; carries the
 * manager to the factory. Constructed with the repository interface; the
 * manager is the one set explicitly, else the bean named
 * {@code rdfEntityManager}, else the single {@link RdfEntityManager} bean.
 */
public class GraphRdRepositoryFactoryBean<T extends GraphRdRepository<S, ID>, S, ID>
        extends RepositoryFactoryBeanSupport<T, S, ID> {

    private RdfEntityManager rdfEntityManager;
    private BeanFactory beanFactory;

    @SuppressWarnings("unused") // Spring Data passes the repository interface here
    public GraphRdRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
        super(repositoryInterface);
    }

    /** An explicit manager; without one the bean factory is asked. */
    public void setRdfEntityManager(RdfEntityManager em) {
        this.rdfEntityManager = em;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        super.setBeanFactory(beanFactory);
        this.beanFactory = beanFactory;
    }

    @Override
    protected RepositoryFactorySupport createRepositoryFactory() {
        RdfEntityManager em = rdfEntityManager;
        if (em == null && beanFactory != null) {
            em = beanFactory.containsBean("rdfEntityManager")
                    ? beanFactory.getBean("rdfEntityManager", RdfEntityManager.class)
                    : beanFactory.getBean(RdfEntityManager.class);
        }
        if (em == null) {
            throw new IllegalStateException("no RdfEntityManager for " + getObjectType());
        }
        return new GraphRdRepositoryFactory(em);
    }
}

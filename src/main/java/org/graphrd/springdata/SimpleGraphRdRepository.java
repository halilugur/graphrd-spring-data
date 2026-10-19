package org.graphrd.springdata;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.graphrd.rdf.EntityQuery;
import org.graphrd.rdf.RdfEntityManager;

/**
 * The base repository every {@link GraphRdRepository} interface is backed
 * with: Spring's standard CRUD over the mapper — saves in one chained
 * request, lists with references resolved in bulk, pages with the total.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
class SimpleGraphRdRepository<T> implements GraphRdRepository<T, String> {

    private final Class<T> domainType;
    private final RdfEntityManager em;

    SimpleGraphRdRepository(Class<T> domainType, RdfEntityManager em) {
        this.domainType = domainType;
        this.em = em;
    }

    @Override
    public <S extends T> S save(S entity) {
        em.save(entity);
        return entity;
    }

    @Override
    public <S extends T> List<S> saveAll(Iterable<S> entities) {
        List<S> out = new ArrayList<>();
        entities.forEach(out::add);
        em.saveAll(out);
        return out;
    }

    @Override
    public Optional<T> findById(String id) {
        return Optional.ofNullable(em.find(domainType, id));
    }

    @Override
    public boolean existsById(String id) {
        return findById(id).isPresent();
    }

    @Override
    public List<T> findAll() {
        return em.findAll(domainType);
    }

    @Override
    public List<T> findAllById(Iterable<String> ids) {
        List<String> list = new ArrayList<>();
        ids.forEach(list::add);
        return em.findAllByIds(domainType, list);
    }

    @Override
    public long count() {
        return em.count(domainType);
    }

    @Override
    public void deleteById(String id) {
        em.delete(domainType, id);
    }

    @Override
    public void delete(T entity) {
        em.delete(domainType, em.idOfPublic(entity));
    }

    @Override
    public void deleteAllById(Iterable<? extends String> ids) {
        ids.forEach(id -> em.delete(domainType, id));
    }

    @Override
    public void deleteAll(Iterable<? extends T> entities) {
        entities.forEach(e -> delete(e));
    }

    @Override
    public void deleteAll() {
        em.deleteAll(domainType);
    }

    @Override
    public List<T> findAll(Sort sort) {
        EntityQuery<T> q = em.query(domainType);
        applySort(q, sort);
        return q.list();
    }

    @Override
    public Page<T> findAll(Pageable pageable) {
        return page(em.query(domainType), pageable);
    }

    @Override
    public List<T> findAll(GraphRdSpecification<T> specification) {
        return specification.apply(em.query(domainType)).list();
    }

    @Override
    public List<T> findAll(GraphRdSpecification<T> specification, Sort sort) {
        EntityQuery<T> q = specification.apply(em.query(domainType));
        applySort(q, sort);
        return q.list();
    }

    @Override
    public Page<T> findAll(GraphRdSpecification<T> specification, Pageable pageable) {
        return page(specification.apply(em.query(domainType)), pageable);
    }

    /** One page with the total — or everything when the request is unpaged. */
    private static <T> Page<T> page(EntityQuery<T> q, Pageable pageable) {
        applySort(q, pageable.getSort());
        if (pageable.isUnpaged()) {
            List<T> all = q.list();
            return new PageImpl<>(all, pageable, all.size());
        }
        EntityQuery.Paged<T> paged = q.page(pageable.getPageNumber(), pageable.getPageSize());
        return new PageImpl<>(paged.items(), pageable, paged.total());
    }

    @Override
    public long count(GraphRdSpecification<T> specification) {
        return specification.apply(em.query(domainType)).count();
    }

    static void applySort(EntityQuery<?> q, Sort sort) {
        boolean first = true;
        for (Sort.Order order : sort) {
            if (first) {
                q.orderBy(order.getProperty(), order.isDescending());
                first = false;
            } else {
                q.thenBy(order.getProperty(), order.isDescending());
            }
        }
    }
}

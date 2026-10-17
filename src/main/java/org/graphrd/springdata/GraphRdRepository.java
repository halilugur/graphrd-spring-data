package org.graphrd.springdata;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * The store interface, in Spring Data's own shape: everything
 * {@link CrudRepository} and {@link PagingAndSortingRepository} promise, with
 * Spring's {@code Pageable}/{@code Sort} honored. Extend it and add derived
 * queries — {@code findByEmail(String)}, {@code findByAgeGreaterThan(int, Pageable)} —
 * Spring Data parses the names, GraphRD answers them.
 */
@NoRepositoryBean
public interface GraphRdRepository<T, ID> extends ListCrudRepository<T, ID>, ListPagingAndSortingRepository<T, ID> {

    /** Everything matching the specification. */
    List<T> findAll(GraphRdSpecification<T> specification);

    /** Everything matching, sorted. */
    List<T> findAll(GraphRdSpecification<T> specification, Sort sort);

    /** A page of what matches — the total counted under the same conditions. */
    Page<T> findAll(GraphRdSpecification<T> specification, Pageable pageable);

    /** How many match. */
    long count(GraphRdSpecification<T> specification);
}

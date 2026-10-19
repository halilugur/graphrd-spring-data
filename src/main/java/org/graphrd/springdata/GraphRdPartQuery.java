package org.graphrd.springdata;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;

import org.graphrd.rdf.EntityQuery;
import org.graphrd.rdf.RdfEntityManager;

/**
 * One derived method — Spring Data parses the name into a {@link PartTree},
 * this class translates the parts into the mapper's criteria. Conditions
 * join with {@code And}; a trailing {@link Pageable} pages the result (with
 * the total), a {@link Sort} sorts it; {@code IgnoreCase} string parts become
 * case-blind searches; an equality on a reference field takes the IRI.
 */
class GraphRdPartQuery implements RepositoryQuery {

    private final Method method;
    private final QueryMethod queryMethod;
    private final Class<?> domainType;
    private final RdfEntityManager em;

    GraphRdPartQuery(Method method, RepositoryMetadata metadata,
                     ProjectionFactory projections, RdfEntityManager em) {
        this.method = method;
        this.queryMethod = new QueryMethod(method, metadata, projections);
        this.domainType = metadata.getDomainType();
        this.em = em;
    }

    @Override
    public Object execute(Object[] parameters) {
        PartTree tree = new PartTree(method.getName(), domainType);

        // peel off Pageable / Sort tail parameters
        Pageable pageable = null;
        Sort sort = null;
        Object[] conditions = new Object[parameters.length];
        int n = 0;
        for (Object p : parameters) {
            if (p instanceof Pageable pg) {
                pageable = pg;
            } else if (p instanceof Sort s) {
                sort = s;
            } else {
                conditions[n++] = p;
            }
        }

        EntityQuery<?> q = em.query(domainType);
        int arg = 0;
        for (Part part : tree.getParts()) {
            arg = apply(q, part, conditions, arg);
        }

        Class<?> returns = method.getReturnType();
        if (tree.isDelete()) {
            List<?> victims = q.list();
            victims.forEach(v -> em.delete(domainType, em.idOfPublic(v)));
            if (Collection.class.isAssignableFrom(returns) || returns == Iterable.class) {
                return victims; // the removed entities, as Spring Data's stores answer
            }
            return returns == void.class || returns == Void.class ? null : number(victims.size(), returns);
        }
        if (tree.isCountProjection()) {
            return number(q.count(), returns);
        }
        if (tree.isExistsProjection()) {
            return q.limit(1).count() > 0;
        }
        if (tree.getMaxResults() != null) {
            q = q.limit(tree.getMaxResults());
        }

        // OrderBy in the name first, then a Sort parameter, then the Pageable's sort
        Sort order = tree.getSort();
        if (sort != null) {
            order = order.and(sort);
        }
        if (pageable != null) {
            order = order.and(pageable.getSort());
        }
        SimpleGraphRdRepository.applySort(q, order);

        boolean paged = pageable != null && pageable.isPaged();
        if (Slice.class.isAssignableFrom(returns)) { // Page and Slice
            if (!paged) {
                List<?> all = q.list();
                return new PageImpl<>(all, pageable == null ? Pageable.unpaged() : pageable, all.size());
            }
            EntityQuery.Paged<?> page = q.page(pageable.getPageNumber(), pageable.getPageSize());
            return new PageImpl<>(page.items(), pageable, page.total());
        }
        if (Optional.class == returns) {
            return q.limit(1).list().stream().findFirst();
        }
        if (Iterable.class.isAssignableFrom(returns) || Stream.class.isAssignableFrom(returns)) {
            List<?> list = paged
                    ? q.limit(pageable.getPageSize()).offset(pageable.getOffset()).list()
                    : q.list();
            if (Set.class.isAssignableFrom(returns)) {
                return new LinkedHashSet<>(list);
            }
            return Stream.class.isAssignableFrom(returns) ? list.stream() : list;
        }
        if (long.class == returns || Long.class == returns || int.class == returns || Integer.class == returns) {
            return number(q.count(), returns);
        }
        if (boolean.class == returns || Boolean.class == returns) {
            return !q.limit(1).list().isEmpty();
        }
        // a single entity of the domain type
        List<?> one = q.limit(1).list();
        return one.isEmpty() ? null : one.get(0);
    }

    /** A count in the declared numeric return type (int or long). */
    private static Object number(long n, Class<?> returns) {
        return returns == int.class || returns == Integer.class ? (Object) (int) n : (Object) n;
    }

    /** An In argument: a collection, an array, or a single value. */
    private static List<?> values(Object v) {
        if (v instanceof Collection<?> c) {
            return new ArrayList<>(c);
        }
        if (v instanceof Object[] a) {
            return java.util.Arrays.asList(a);
        }
        if (v != null && v.getClass().isArray()) {
            List<Object> out = new ArrayList<>();
            for (int i = 0; i < java.lang.reflect.Array.getLength(v); i++) {
                out.add(java.lang.reflect.Array.get(v, i));
            }
            return out;
        }
        return v == null ? List.of() : List.of(v);
    }

    /** One part of the tree, consuming its arguments. */
    private int apply(EntityQuery<?> q, Part part, Object[] args, int arg) {
        String field = part.getProperty().getSegment();
        boolean ignoreCase = part.shouldIgnoreCase() != Part.IgnoreCaseType.NEVER;
        Object v = arg < args.length ? args[arg] : null;
        switch (part.getType()) {
            case SIMPLE_PROPERTY -> {
                arg++;
                if (v instanceof String iri && em.isReferenceField(domainType, field)) {
                    q.whereRef(field, iri);
                } else if (ignoreCase && v instanceof String text) {
                    q.whereIgnoreCase(field, text);
                } else {
                    q.where(field, EntityQuery.Op.EQ, v);
                }
            }
            case NEGATING_SIMPLE_PROPERTY -> {
                arg++;
                q.where(field, EntityQuery.Op.NE, v);
            }
            case GREATER_THAN -> { arg++; q.where(field, EntityQuery.Op.GT, v); }
            case GREATER_THAN_EQUAL -> { arg++; q.where(field, EntityQuery.Op.GE, v); }
            case LESS_THAN -> { arg++; q.where(field, EntityQuery.Op.LT, v); }
            case LESS_THAN_EQUAL -> { arg++; q.where(field, EntityQuery.Op.LE, v); }
            case BETWEEN -> {
                Object hi = args[arg + 1];
                arg += 2;
                q.where(field, EntityQuery.Op.GE, v).where(field, EntityQuery.Op.LE, hi);
            }
            case IS_NULL, IS_EMPTY -> q.whereNull(field);
            case IS_NOT_NULL, IS_NOT_EMPTY -> q.whereNotNull(field);
            case TRUE -> q.where(field, EntityQuery.Op.EQ, true);
            case FALSE -> q.where(field, EntityQuery.Op.EQ, false);
            case IN -> {
                arg++;
                q.whereIn(field, values(v));
            }
            case LIKE, STARTING_WITH, ENDING_WITH, CONTAINING -> {
                arg++;
                String pattern = switch (part.getType()) {
                    case STARTING_WITH -> v + "%";
                    case ENDING_WITH -> "%" + v;
                    case CONTAINING -> "%" + v + "%";
                    default -> v.toString(); // LIKE: the argument carries its own wildcards
                };
                if (ignoreCase) {
                    q.whereILike(field, pattern);
                } else {
                    q.whereLike(field, pattern);
                }
            }
            default -> throw new UnsupportedOperationException(
                    "part not mapped to GraphRD yet: " + part.getType() + " in " + method.getName());
        }
        return arg;
    }

    @Override
    public QueryMethod getQueryMethod() {
        return queryMethod;
    }
}

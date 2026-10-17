package org.graphrd.springdata;

import java.util.List;

import org.graphrd.rdf.EntityQuery.Op;
import org.graphrd.rdf.Getter;

/**
 * The factories of a dynamic search — the counterpart of JPA's
 * {@code Specification.where(…)}, typed where a getter is at hand:
 *
 * <pre>{@code
 * var over30AtAcme = GraphRdSpecifications
 *         .<Person>greaterThan(Person::getAge, 30)
 *         .and(GraphRdSpecifications.ref("worksFor", "http://ex.org/acme"));
 * Page<Person> page = people.findAll(over30AtAcme, pageable);
 * }</pre>
 */
public final class GraphRdSpecifications {

    private GraphRdSpecifications() {
    }

    /** Field equals value; a String value on a reference field compares by IRI. */
    public static <T> GraphRdSpecification<T> eq(String field, Object value) {
        return q -> q.where(field, Op.EQ, value);
    }

    /** Getter-typed equality. */
    public static <T, V> GraphRdSpecification<T> eq(Getter<T, V> field, V value) {
        return q -> q.where(field.fieldName(), Op.EQ, value);
    }

    public static <T> GraphRdSpecification<T> ne(String field, Object value) {
        return q -> q.where(field, Op.NE, value);
    }

    public static <T, V> GraphRdSpecification<T> greaterThan(Getter<T, V> field, V value) {
        return q -> q.where(field.fieldName(), Op.GT, value);
    }

    public static <T> GraphRdSpecification<T> greaterThan(String field, Object value) {
        return q -> q.where(field, Op.GT, value);
    }

    public static <T, V> GraphRdSpecification<T> greaterThanEqual(Getter<T, V> field, V value) {
        return q -> q.where(field.fieldName(), Op.GE, value);
    }

    public static <T, V> GraphRdSpecification<T> lessThan(Getter<T, V> field, V value) {
        return q -> q.where(field.fieldName(), Op.LT, value);
    }

    public static <T, V> GraphRdSpecification<T> lessThanEqual(Getter<T, V> field, V value) {
        return q -> q.where(field.fieldName(), Op.LE, value);
    }

    /** A closed range. */
    public static <T, V> GraphRdSpecification<T> between(Getter<T, V> field, V lo, V hi) {
        return q -> q.where(field.fieldName(), Op.GE, lo).where(field.fieldName(), Op.LE, hi);
    }

    /** SQL-LIKE with {@code %} and {@code _}, case-blind. */
    public static <T> GraphRdSpecification<T> ilike(String field, String pattern) {
        return q -> q.whereILike(field, pattern);
    }

    public static <T> GraphRdSpecification<T> ilike(Getter<T, String> field, String pattern) {
        return q -> q.whereILike(field.fieldName(), pattern);
    }

    /** Field is one of the values. */
    public static <T> GraphRdSpecification<T> in(String field, List<?> values) {
        return q -> q.whereIn(field, values);
    }

    /** An IRI-reference condition: the field points at this resource. */
    public static <T> GraphRdSpecification<T> ref(String field, String iri) {
        return q -> q.whereRef(field, iri);
    }

    public static <T> GraphRdSpecification<T> isNull(String field) {
        return q -> q.whereNull(field);
    }

    public static <T> GraphRdSpecification<T> notNull(String field) {
        return q -> q.whereNotNull(field);
    }

    /** Compose several with and. */
    @SafeVarargs
    public static <T> GraphRdSpecification<T> all(GraphRdSpecification<T>... specs) {
        return q -> {
            for (GraphRdSpecification<T> s : specs) {
                q = s.apply(q);
            }
            return q;
        };
    }
}

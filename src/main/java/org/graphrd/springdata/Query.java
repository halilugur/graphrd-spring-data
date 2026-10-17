package org.graphrd.springdata;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A hand-written SPARQL SELECT on a repository method — the custom-query
 * side of Spring Data, in SPARQL. Method parameters bind by name as
 * {@code :param}; a record return type maps SPARQL variable names to the
 * record's components, {@code long} takes the first value of the first row.
 *
 * <pre>{@code
 * @Query("PREFIX ex: <http://ex.org/> SELECT ?company (COUNT(?e) AS ?heads) "
 *      + "WHERE { ?e a ex:Person ; ex:worksFor [ ex:name ?company ] } GROUP BY ?company")
 * List<Headcount> headcount();
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Query {

    /** The SPARQL SELECT, with {@code :param} placeholders for method parameters. */
    String value();
}

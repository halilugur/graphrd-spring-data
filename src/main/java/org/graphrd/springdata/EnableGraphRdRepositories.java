package org.graphrd.springdata;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Import;

/**
 * Scans for {@link GraphRdRepository} interfaces and backs them with the
 * GraphRD client. Needs a {@link org.graphrd.rdf.RdfEntityManager} bean (the
 * library's auto-configuration provides one for Spring Boot applications).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(GraphRdRegistrar.class)
public @interface EnableGraphRdRepositories {

    String[] value() default {};

    String[] basePackages() default {};

    Class<?>[] basePackageClasses() default {};

    Filter[] includeFilters() default {};

    Filter[] excludeFilters() default {};

    boolean considerNestedRepositories() default false;

    Class<?> repositoryFactoryBeanClass() default GraphRdRepositoryFactoryBean.class;

    String repositoryImplementationPostfix() default "Impl";

    String queryLookupStrategy() default "";

    String namedQueriesLocation() default "";
}

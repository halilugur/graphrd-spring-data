package org.graphrd.springdata;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

/**
 * What the jar being on the classpath means: the repositories are found and
 * wired without a single annotation — the application's own packages are
 * scanned (like every Spring Boot store module does). Backs off entirely when
 * an explicit {@link EnableGraphRdRepositories @EnableGraphRdRepositories} is
 * present, and can be switched off with
 * {@code spring.data.graphrd.repositories.enabled=false}.
 */
@AutoConfiguration(after = org.graphrd.spring.GraphRdAutoConfiguration.class)
@ConditionalOnClass(name = "org.graphrd.rdf.RdfEntityManager")
@ConditionalOnMissingBean(GraphRdRepositoryFactoryBean.class)
@ConditionalOnProperty(prefix = "spring.data.graphrd.repositories", name = "enabled",
        havingValue = "true", matchIfMissing = true)
@Import(GraphRdAutoRegistrar.class)
public class GraphRdAutoConfiguration {
}

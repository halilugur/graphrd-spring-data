package org.graphrd.spring;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import org.graphrd.RdfClient;
import org.graphrd.rdf.RdfEntityManager;

/**
 * Springs the client up when it is on the classpath: add the jar, set
 * {@code graphrd.base-url} and {@code graphrd.repository} in application.yml,
 * and inject {@link RdfClient} or {@link RdfEntityManager} anywhere. The
 * beans are closed on shutdown; your own beans of the same types take
 * precedence.
 */
@AutoConfiguration
@ConditionalOnClass(RdfClient.class)
@EnableConfigurationProperties(GraphRdProperties.class)
public class GraphRdAutoConfiguration {

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    RdfClient rdfClient(GraphRdProperties settings) {
        if (settings.baseUrl() == null || settings.baseUrl().isBlank()) {
            throw new IllegalStateException("graphrd.base-url is not set (e.g. http://localhost:7200)");
        }
        return RdfClient.builder()
                .baseUrl(settings.baseUrl())
                .connectTimeout(settings.connectTimeout())
                .requestTimeout(settings.requestTimeout())
                .build();
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    RdfEntityManager rdfEntityManager(RdfClient client, GraphRdProperties settings) {
        if (settings.repository() == null || settings.repository().isBlank()) {
            throw new IllegalStateException("graphrd.repository is not set (the repository id to use)");
        }
        client.createRepository(settings.repository());
        return new RdfEntityManager(client, settings.repository(), settings.namespace());
    }
}

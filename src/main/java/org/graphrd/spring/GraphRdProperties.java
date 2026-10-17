package org.graphrd.spring;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Connection settings, from application.yml (prefix {@code graphrd}). */
@ConfigurationProperties(prefix = "graphrd")
public record GraphRdProperties(String baseUrl, String repository, String namespace,
                                Duration connectTimeout, Duration requestTimeout) {

    /** Sensible defaults when the application.yml names only the essentials. */
    public GraphRdProperties {
        if (connectTimeout == null) {
            connectTimeout = Duration.ofSeconds(5);
        }
        if (requestTimeout == null) {
            requestTimeout = Duration.ofSeconds(60);
        }
        if (namespace == null) {
            namespace = "";   // annotations carry full IRIs
        }
    }
}

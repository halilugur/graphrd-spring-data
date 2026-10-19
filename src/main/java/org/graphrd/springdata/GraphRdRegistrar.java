package org.graphrd.springdata;

import java.lang.annotation.Annotation;

import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.repository.config.RepositoryBeanDefinitionRegistrarSupport;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;

/** Wires @EnableGraphRdRepositories into Spring Data's scanning machinery. */
class GraphRdRegistrar extends RepositoryBeanDefinitionRegistrarSupport {

    @Override
    protected Class<? extends Annotation> getAnnotation() {
        return EnableGraphRdRepositories.class;
    }

    @Override
    protected RepositoryConfigurationExtension getExtension() {
        return extension();
    }

    static RepositoryConfigurationExtension extension() {
        return new GraphRdRepositoryConfigurationExtension();
    }

    /** The repository beans find the manager themselves (see the factory bean). */
    static class GraphRdRepositoryConfigurationExtension extends RepositoryConfigurationExtensionSupport {

        @Override
        public String getModulePrefix() {
            return "graphrd";
        }

        @Override
        public String getModuleIdentifier() {
            return getModulePrefix();
        }

        @Override
        public String getRepositoryFactoryBeanClassName() {
            return GraphRdRepositoryFactoryBean.class.getName();
        }
    }
}

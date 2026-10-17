package org.graphrd.springdata;

import java.lang.annotation.Annotation;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.repository.config.RepositoryBeanDefinitionRegistrarSupport;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;
import org.springframework.data.repository.config.RepositoryConfigurationSource;

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

    /** Every repository bean gets the manager bean named rdfEntityManager. */
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

        @Override
        public void postProcess(BeanDefinitionBuilder builder, RepositoryConfigurationSource config) {
            builder.addPropertyReference("rdfEntityManager", "rdfEntityManager");
        }
    }
}

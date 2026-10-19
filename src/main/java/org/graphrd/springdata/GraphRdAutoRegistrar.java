package org.graphrd.springdata;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.repository.config.RepositoryConfigurationDelegate;
import org.springframework.data.util.Streamable;

/**
 * The auto path, in Spring Boot's own shape (like
 * {@code JpaRepositoriesAutoConfiguration}'s registrar): the annotation
 * source says "scan the application's auto-configuration packages" instead
 * of this module's package.
 */
class GraphRdAutoRegistrar
        implements ImportBeanDefinitionRegistrar, BeanFactoryAware, ResourceLoaderAware, EnvironmentAware {

    private BeanFactory beanFactory;
    private ResourceLoader resourceLoader;
    private Environment environment;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry,
                                        BeanNameGenerator names) {
        // the annotation attributes come from a marked source class (Spring Boot's own trick);
        // the base packages are overridden to the application's own
        var source = new AnnotationRepositoryConfigurationSource(
                AnnotationMetadata.introspect(AutoSource.class),
                EnableGraphRdRepositories.class, resourceLoader, environment, registry, names) {
            @Override
            public Streamable<String> getBasePackages() {
                return Streamable.of(() -> AutoConfigurationPackages.get(beanFactory).iterator());
            }
        };
        var delegate = new RepositoryConfigurationDelegate(source, resourceLoader, environment);
        delegate.registerRepositoriesIn(registry, GraphRdRegistrar.extension());
    }

    /** Only a carrier for the annotation's default attributes. */
    @EnableGraphRdRepositories
    private static class AutoSource {
    }
}

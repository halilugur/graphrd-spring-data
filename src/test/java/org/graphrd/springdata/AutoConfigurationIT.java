package org.graphrd.springdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;

import org.graphrd.RdfClient;
import org.graphrd.rdf.RdfEntityManager;
import org.graphrd.springdata.autodemo.AutoPeople;
import org.graphrd.springdata.autodemo.AutoPerson;

/**
 * The promise of the module: the jar on the classpath is the whole story —
 * no @EnableGraphRdRepositories anywhere, the application's packages are
 * scanned, repositories and derived queries just work.
 */
class AutoConfigurationIT {

    static class DemoConfig {

        @Bean(destroyMethod = "close")
        RdfClient rdfClient() throws Exception {
            return new RdfClient(TestServer.start());
        }

        @Bean(destroyMethod = "close")
        RdfEntityManager rdfEntityManager(RdfClient client) {
            client.createRepository("auto");
            return new RdfEntityManager(client, "auto");
        }
    }

    @BeforeAll
    static void up() throws Exception {
        TestServer.start();
    }

    @AfterAll
    static void down() {
    }

    @Test
    void theStarterWiresClientAndManager() {
        new ApplicationContextRunner()
                .withPropertyValues("graphrd.base-url=" + TestServer.url(),
                        "graphrd.repository=starter")
                .withConfiguration(AutoConfigurations.of(org.graphrd.spring.GraphRdAutoConfiguration.class))
                .run(ctx -> {
                    assertTrue(ctx.containsBean("rdfClient"), "client bean");
                    assertTrue(ctx.containsBean("rdfEntityManager"), "manager bean");
                });
    }

    @Test
    void repositoriesAppearWithoutAnyAnnotation() {
        new ApplicationContextRunner()
                .withInitializer(ctx -> AutoConfigurationPackages.register(
                        (BeanDefinitionRegistry) ctx.getBeanFactory(), "org.graphrd.springdata.autodemo"))
                .withUserConfiguration(DemoConfig.class)
                .withConfiguration(AutoConfigurations.of(GraphRdAutoConfiguration.class))
                .run(ctx -> {
                    AutoPeople people = ctx.getBean(AutoPeople.class);
                    assertNotNull(people, "the repository was found and wired");

                    people.save(new AutoPerson("http://ex.org/ann", "Ann", 31));
                    people.save(new AutoPerson("http://ex.org/berk", "Berk", 27));
                    assertEquals(2, people.count());
                    assertEquals("Ann", people.findById("http://ex.org/ann").orElseThrow().name);
                    assertEquals(1, people.findByNameStartingWith("Ber").size());
                });
    }
}

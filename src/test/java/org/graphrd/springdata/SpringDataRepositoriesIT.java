package org.graphrd.springdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;

import org.graphrd.RdfClient;
import org.graphrd.rdf.EntityQuery.Op;
import org.graphrd.rdf.RdfEntityManager;
import org.graphrd.rdf.RdfId;
import org.graphrd.rdf.RdfProperty;
import org.graphrd.rdf.RdfType;

/**
 * Spring Data repositories over a live GraphRD — the documented composition,
 * one repository per aggregate: standard CRUD from the base, derived names
 * parsed by Spring Data, @Query SPARQL, and a custom fragment, all behind a
 * single interface to inject.
 */
class SpringDataRepositoriesIT {

    @RdfType("Person")   // short names resolve against the manager's namespace
    static class Person {

        @RdfId
        String id;

        @RdfProperty("name")
        String name;

        @RdfProperty("age")
        Integer age;

        @RdfProperty("salary")
        Integer salary;

        @RdfProperty("worksFor")
        Company worksFor;

        public Person() {
        }

        public Person(String id, String name, Integer age, Integer salary, Company worksFor) {
            this.id = id;
            this.name = name;
            this.age = age;
            this.salary = salary;
            this.worksFor = worksFor;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public Integer getAge() {
            return age;
        }

        public Integer getSalary() {
            return salary;
        }
    }

    @RdfType("Company")
    static class Company {

        @RdfId
        String id;

        @RdfProperty("name")
        String name;

        public Company() {
        }

        public Company(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    record Headcount(String company, long heads) {
    }

    record Stats(long employees, long payroll, double average) {
    }

    record Third(double value) {
    }

    /** The one repository for Person: CRUD + derived + @Query — nothing else
     *  to write: dynamic searches are specifications the repository already runs. */
    interface People extends GraphRdRepository<Person, String> {

        Optional<Person> findByName(String name);

        List<Person> findByAgeGreaterThanAndSalaryLessThan(int age, int salary);

        List<Person> findByNameStartingWithIgnoreCase(String prefix);

        List<Person> findByWorksFor(String companyIri);

        Page<Person> findByAgeGreaterThan(int age, Pageable pageable);

        List<Person> findBySalaryGreaterThanOrderBySalaryDesc(int salary);

        long countByWorksFor(String companyIri);

        boolean existsByName(String name);

        void deleteByName(String name);

        @Query("PREFIX ex: <{ns}> SELECT ?company (COUNT(?e) AS ?heads)"
                + " WHERE { ?e a ex:Person ; ex:worksFor [ ex:name ?company ] }"
                + " GROUP BY ?company ORDER BY DESC(?heads)")
        List<Headcount> headcount();

        @Query("PREFIX ex: <{ns}> SELECT (COUNT(?e) AS ?employees)"
                + " (SUM(?s) AS ?payroll) (AVG(?s) AS ?average)"
                + " WHERE { ?e a ex:Person ; ex:salary ?s }")
        Stats stats();

        @Query("PREFIX ex: <{ns}> SELECT (SUM(?s) AS ?total) WHERE { ?s ?p ?s FILTER(false) }")
        long zero();

        @Query("PREFIX ex: <{ns}> SELECT ?n WHERE { ?e a ex:Person ; ex:name ?n ; ex:age ?a"
                + " . FILTER(?a >= :lo && ?a <= :hi) } ORDER BY ?n")
        List<String> namesBetween(int lo, int hi);

        List<Person> findByNameIgnoreCase(String name);

        Set<Person> findByAgeLessThan(int age);

        List<Person> findByNameIn(Set<String> names);

        int countByAgeGreaterThan(int age);

        int deleteBySalary(int salary);

        Page<Person> findBySalaryGreaterThan(int salary, Pageable pageable);

        Slice<Person> findByAgeLessThanEqual(int age, Pageable pageable);

        @Query("PREFIX ex: <{ns}> SELECT ((SUM(?a)) / 3 AS ?value) WHERE { ?e a ex:Person ; ex:age ?a }")
        Third ageSumThird();

        @Query("PREFIX ex: <{ns}> SELECT ?b WHERE { BIND(true AS ?b) }")
        boolean alwaysTrue();
    }

    interface Companies extends GraphRdRepository<Company, String> {
    }

    @Test
    void derivedAndQueryEdgeCases() {
        assertEquals(List.of("Alice"), people.findByNameIgnoreCase("aLiCe").stream().map(Person::getName).toList());
        assertEquals(2, people.findByAgeLessThan(40).size());
        assertEquals(2, people.findByNameIn(Set.of("Alice", "Dave")).size());
        assertEquals(3, people.countByAgeGreaterThan(30));

        Page<Person> unpaged = people.findBySalaryGreaterThan(60000, Pageable.unpaged());
        assertEquals(3, unpaged.getTotalElements());
        assertEquals(3, unpaged.getContent().size());
        Slice<Person> slice = people.findByAgeLessThanEqual(41, PageRequest.of(0, 2, Sort.by("age")));
        assertEquals(List.of("Bob", "Alice"), slice.getContent().stream().map(Person::getName).toList());
        assertEquals(4, people.findAll(Pageable.unpaged()).getContent().size());

        assertEquals(155 / 3.0, people.ageSumThird().value(), 1e-6);
        assertTrue(people.alwaysTrue());

        assertEquals(1, people.deleteBySalary(58000));
        assertEquals(3, people.count());
    }

    @Configuration
    @EnableGraphRdRepositories(basePackageClasses = SpringDataRepositoriesIT.class,
            considerNestedRepositories = true)
    static class Wiring {

        @Bean(destroyMethod = "close")
        RdfClient rdfClient() throws Exception {
            return new RdfClient(TestServer.start());
        }

        @Bean(destroyMethod = "close")
        RdfEntityManager rdfEntityManager(RdfClient client) {
            client.createRepository("sd");
            return new RdfEntityManager(client, "sd", "http://ex.org/");
        }
    }

    private static AnnotationConfigApplicationContext context;
    private static People people;
    private static Companies companies;
    private final Company acme = new Company("http://ex.org/acme", "Acme");
    private final Company initech = new Company("http://ex.org/init", "Initech");

    @BeforeAll
    static void up() {
        context = new AnnotationConfigApplicationContext(Wiring.class);
        people = context.getBean(People.class);
        companies = context.getBean(Companies.class);
    }

    @AfterAll
    static void down() {
        if (context != null) {
            context.close();
        }
    }

    @BeforeEach
    void fresh() {
        people.deleteAll();
        companies.deleteAll();
        companies.save(acme);
        companies.save(initech);
        people.saveAll(List.of(
                new Person("http://ex.org/alice", "Alice", 34, 82000, acme),
                new Person("http://ex.org/bob", "Bob", 28, 61000, acme),
                new Person("http://ex.org/carol", "Carol", 41, 95000, initech),
                new Person("http://ex.org/dave", "Dave", 52, 58000, initech)));
    }

    @Test
    void standardCrudFromTheBaseRepository() {
        assertEquals(4, people.count());
        assertTrue(people.existsById("http://ex.org/alice"));
        assertEquals("Alice", people.findById("http://ex.org/alice").orElseThrow().getName());
        assertEquals(2, people.findAllById(List.of("http://ex.org/alice", "http://ex.org/bob", "http://ex.org/nobody")).size());
        assertEquals(4, people.findAll().size());

        Person loaded = people.findById("http://ex.org/carol").orElseThrow();
        assertEquals("Initech", loaded.worksFor.name); // reference resolved

        people.deleteById("http://ex.org/dave");
        assertEquals(3, people.count());
        assertFalse(people.existsById("http://ex.org/dave"));
    }

    @Test
    void derivedQueriesParsedBySpringData() {
        assertEquals("Carol", people.findByName("Carol").orElseThrow().getName());
        assertEquals(3, people.findByAgeGreaterThanAndSalaryLessThan(30, 100000).size());
        assertEquals(List.of("Alice"), people.findByNameStartingWithIgnoreCase("aLi").stream()
                .map(Person::getName).toList());
        assertEquals(2, people.findByWorksFor("http://ex.org/acme").size());
        assertEquals(2, people.countByWorksFor("http://ex.org/init"));
        assertTrue(people.existsByName("Bob"));
        List<Person> top = people.findBySalaryGreaterThanOrderBySalaryDesc(60000);
        assertEquals(List.of("Carol", "Alice", "Bob"), top.stream().map(Person::getName).toList());
    }

    @Test
    void customQueriesWithQuery() {
        List<Headcount> rows = people.headcount();
        assertEquals(2, rows.size());
        assertEquals(new Headcount("Acme", 2), rows.get(0));

        Stats stats = people.stats();
        assertEquals(4, stats.employees());
        assertEquals(296000, stats.payroll());
        assertEquals(74000.0, stats.average());
        assertEquals(0, people.zero());
        assertEquals(List.of("Alice", "Carol"), people.namesBetween(30, 45));
    }

    @Test
    void dynamicSearchAsSpecification() {
        // no custom repository written: the specification composes, the repository runs it
        var wellPaid = GraphRdSpecifications.<Person, Integer>greaterThan(Person::getSalary, 70000);
        Page<Person> page = people.findAll(wellPaid, PageRequest.of(0, 2, Sort.by("salary").descending()));
        assertEquals(2, page.getTotalElements());
        assertEquals("Carol", page.getContent().get(0).getName());

        var over30AtAcme = GraphRdSpecifications.<Person, Integer>greaterThan(Person::getAge, 30)
                .and(GraphRdSpecifications.ref("worksFor", "http://ex.org/acme"));
        assertEquals(1, people.count(over30AtAcme));
        assertEquals(List.of("Alice"), people.findAll(over30AtAcme).stream()
                .map(Person::getName).toList());

        var named = GraphRdSpecifications.<Person>ilike(Person::getName, "aLi%");
        assertEquals(List.of("Alice"), people.findAll(named).stream()
                .map(Person::getName).toList());
    }

    @Test
    void pagesSortsAndDeleteBy() {
        Page<Person> page = people.findByAgeGreaterThan(25, PageRequest.of(0, 2, Sort.by("salary").descending()));
        assertEquals(4, page.getTotalElements());
        assertEquals(2, page.getContent().size());
        assertEquals("Carol", page.getContent().get(0).getName());

        List<Person> sorted = people.findAll(Sort.by("age"));
        assertEquals("Bob", sorted.get(0).getName());
        Page<Person> all = people.findAll(PageRequest.of(1, 2));
        assertEquals(4, all.getTotalElements());
        assertEquals(2, all.getContent().size());

        people.deleteByName("Bob");
        assertEquals(3, people.count());
    }
}

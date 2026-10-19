<p align="center"><img src="logo-mark.svg" alt="GraphRD" width="72"></p>

# GraphRD Spring Data module

[![Maven Central](https://img.shields.io/maven-central/v/org.graphrd/graphrd-spring-data.svg)](https://central.sonatype.com/artifact/org.graphrd/graphrd-spring-data)

Spring Data repositories for [GraphRD](https://graphrd.org) — Spring's own
repository structure on top of the [pure-Java client](https://github.com/halilugur/graphrd-client).
All Spring integration lives in this module: the client jar carries no Spring
at all, and this module brings both the starter (auto-wiring
`RdfClient`/`RdfEntityManager` from `graphrd.*` settings) and the repository
structure described below. Java 17+.

## Install

One dependency — the client comes with it. The module's version is the
Spring Boot version it is built on: pick the one that matches your
application (4.1.1 for Spring Boot 4.1.x).

```xml
<dependency>
  <groupId>org.graphrd</groupId>
  <artifactId>graphrd-spring-data</artifactId>
  <version>4.1.1</version>
</dependency>
```

## Getting started

**Being on the classpath is the whole setup** — the application's own packages
are scanned, no annotation anywhere (exactly like JPA in a Spring Boot
application):

```java
interface People extends GraphRdRepository<Person, String> {
    Optional<Person> findByName(String name);
    List<Person>  findByAgeGreaterThanAndSalaryLessThan(int age, int salary);
    List<Person>  findByWorksFor(String companyIri);      // reference equality, by IRI
    List<Person>  findByNameStartingWithIgnoreCase(String prefix);
    Page<Person>  findByAgeGreaterThan(int age, Pageable pageable);   // items + total
    List<Person>  findBySalaryGreaterThanOrderBySalaryDesc(int salary);
    long          countByWorksFor(String companyIri);
    void          deleteByName(String name);
}
```

That one interface carries:

- the **standard CRUD** of `CrudRepository`/`PagingAndSortingRepository` —
  saves in one chained request, lists with references resolved in bulk,
  `findAll(Pageable)` pages with the total, `findAll(Sort)` sorts (multi-key);
- **derived queries parsed by Spring Data itself** (`PartTree`), translated to
  the mapper's criteria: comparisons, `Between`, `In`, the `Like` family,
  `IgnoreCase`, `IsNull`, `True/False`, `OrderBy` in the name, `First`/`Top`
  limits, `exists…By`, `count…By`, `delete…By`;
- an equality on a reference field compares **IRIs** automatically.

An explicit `@EnableGraphRdRepositories` (with `basePackages`,
`considerNestedRepositories` and friends) still wins over the automatic scan,
and `spring.data.graphrd.repositories.enabled=false` turns it off entirely.

## Dynamic search: specifications

Free-form filter mixes that do not fit one method name are the mapper's
specifications — compose and run, no custom repository, ever:

```java
GraphRdSpecification<Person> spec = GraphRdSpecifications.greaterThan(Person::getAge, 30)
        .and(GraphRdSpecifications.ref("worksFor", companyIri))
        .and(GraphRdSpecifications.ilike(Person::getName, "a%"));

Page<Person> page = people.findAll(spec, pageable);   // also findAll(spec), findAll(spec, Sort), count(spec)
```

## Custom queries: @Query

Hand-written SPARQL, `:param` binding, answers mapped to records:

```java
@Query("PREFIX ex: <http://ex.org/> SELECT ?company (COUNT(?e) AS ?heads)"
     + " WHERE { ?e a ex:Person ; ex:worksFor [ ex:name ?company ] } GROUP BY ?company")
List<Headcount> headcount();   // record Headcount(String company, long heads)

@Query("… FILTER(?a >= :lo && ?a <= :hi) …")
List<String> namesBetween(int lo, int hi);   // parameters bind as literals
```

## Configuration

The starter wires everything from `graphrd.*` in application.yml — a Spring
Boot application writes no configuration code:

| Property | Default | Meaning |
|---|---|---|
| `graphrd.base-url` | — | the server, e.g. `http://localhost:7200` |
| `graphrd.repository` | — | the repository id |
| `graphrd.namespace` | *(empty)* | vocabulary namespace; short annotation names resolve against it, `{ns}` in `@Query` stands for it |
| `graphrd.connect-timeout` | `5s` | connection setup |
| `graphrd.request-timeout` | `60s` | per request |
| `spring.data.graphrd.repositories.enabled` | `true` | repository scanning on/off |

The [showcase application](https://github.com/halilugur/graphrd/tree/main/examples/spring-boot-graphrd)
uses derived queries, specifications and `@Query` side by side, with zero
configuration annotations.

## Requirements and tests

Java 17+, Spring Boot 4.x (Spring Framework 7, Spring Data 2026.x) and a running GraphRD server. `mvn test` boots a
throwaway server and runs the whole tour through a real Spring context: it
expects the release binary at `../../target/release/graphrd` (clone
[GraphRD](https://github.com/halilugur/graphrd) next to this repository and run
`cargo build --release`; override with `-Dgraphrd.bin=`). Without the binary
the tests skip.

## License

[Apache-2.0](LICENSE)

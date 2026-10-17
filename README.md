<p align="center"><img src="logo-mark.svg" alt="GraphRD" width="72"></p>

# GraphRD Spring Data module

Spring Data repositories over GraphRD for [GraphRD](https://graphrd.org) —
the pure-Java client library underneath, Spring's own interface structure on top.

```xml
<dependency>
  <groupId>org.graphrd</groupId>
  <artifactId>graphrd-client</artifactId>
  <version>1.0.0</version>
</dependency>
<dependency>
  <groupId>org.graphrd</groupId>
  <artifactId>graphrd-spring-data</artifactId>
  <version>1.0.0</version>
</dependency>
```

Spring Data repositories over GraphRD — Spring's own interface structure, the
pure-Java client library underneath. **All Spring integration lives here**:
the graphrd-client jar carries no Spring at all, and this module brings both
the starter (auto-wiring `RdfClient`/`RdfEntityManager` from `graphrd.*`
settings) and the repository structure described below.

**Being on the classpath is the whole setup** — the application's own
packages are scanned, no annotation anywhere (exactly like JPA in a Spring
Boot application):

```java
interface People extends GraphRdRepository<Person, String> { … }   // just this
```

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

What you get:

- the **standard CRUD** of `CrudRepository`/`PagingAndSortingRepository` —
  saves in one chained request, lists with references resolved in bulk,
  `findAll(Pageable)` pages with the total, `findAll(Sort)` sorts (multi-key);
- **derived queries parsed by Spring Data itself** (`PartTree`), translated to
  the mapper's criteria: comparisons, `Between`, `In`, `Like`,
  `StartingWith`, `Containing`, `IgnoreCase`, `IsNull`, `True/False`,
  `OrderBy` in the name, `First`/`Top` limits, `exists…By`, `count…By`,
  `delete…By`;
- an equality on a reference field compares **IRIs** automatically;
- **dynamic searches as specifications**, JPA's answer to "no custom
  repository, ever": compose `GraphRdSpecifications.greaterThan(Person::getAge, 30)
  .and(ref("worksFor", iri)).and(ilike(Person::getName, "a%"))` and run it with
  the executor methods every repository already carries —
  `findAll(spec)`, `findAll(spec, Sort)`, `Page<T> findAll(spec, Pageable)`
  (total included), `count(spec)`;
- **custom queries** with `@Query` — hand-written SPARQL, `:param` binding,
  answers mapped to records:

```java
@Query("PREFIX ex: <http://ex.org/> SELECT ?company (COUNT(?e) AS ?heads)"
     + " WHERE { ?e a ex:Person ; ex:worksFor [ ex:name ?company ] } GROUP BY ?company")
List<Headcount> headcount();   // record Headcount(String company, long heads)

@Query("… FILTER(?a >= :lo && ?a <= :hi) …")
List<String> namesBetween(int lo, int hi);   // parameters bind as literals
```

Needs a `RdfEntityManager` bean named `rdfEntityManager` — the client
library's Spring Boot auto-configuration provides one, so a Spring Boot
application writes none of this by hand. An explicit
`@EnableGraphRdRepositories` (with `basePackages`, `considerNestedRepositories`
and friends) still wins over the automatic scan, and
`spring.data.graphrd.repositories.enabled=false` turns it off entirely.
Free-form filter mixes that do not fit one method name are the mapper's
criteria — see the the [showcase application](https://github.com/halilugur/graphrd/tree/main/examples/spring-boot-graphrd), which
uses both with zero configuration annotations.

`mvn test` boots a throwaway GraphRD server and runs the whole tour through a
real Spring context; it expects the release binary at `../../target/release/graphrd`
(clone [GraphRD](https://github.com/halilugur/graphrd) next to this repository
and run `cargo build --release`).

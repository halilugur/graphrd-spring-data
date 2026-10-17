package org.graphrd.springdata.autodemo;

import java.util.List;

import org.graphrd.springdata.GraphRdRepository;

/** A user's repository interface: no @Enable anywhere — the jar is on the classpath. */
public interface AutoPeople extends GraphRdRepository<AutoPerson, String> {

    List<AutoPerson> findByNameStartingWith(String prefix);
}

package org.graphrd.springdata.autodemo;

import java.util.List;

import org.graphrd.rdf.RdfId;
import org.graphrd.rdf.RdfProperty;
import org.graphrd.rdf.RdfType;

@RdfType("http://ex.org/Person")
public class AutoPerson {

    @RdfId
    public String id;

    @RdfProperty("http://ex.org/name")
    public String name;

    @RdfProperty("http://ex.org/age")
    public Integer age;

    public AutoPerson() {
    }

    public AutoPerson(String id, String name, Integer age) {
        this.id = id;
        this.name = name;
        this.age = age;
    }
}

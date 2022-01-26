package org.getalp.dbnary.hdt;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.NoSuchElementException;
import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.rdfhdt.hdt.triples.TripleString;

class JenaModelsIteratorTest {

  private static Model a;
  private static Model b;
  private static Model c;
  private static Model d;
  private static Model e;

  @BeforeAll
  static void beforeAll() {
    a = ModelFactory.createDefaultModel();
    b = ModelFactory.createDefaultModel();
    c = ModelFactory.createDefaultModel();
    d = ModelFactory.createDefaultModel();
    e = ModelFactory.createDefaultModel();

    // a contains 6 statements (one of which is an anon id)
    Resource a1 = a.createResource("http://test.example.org/a1", FOAF.Person);
    Resource a2 = a.createResource("http://test.example.org/a2", FOAF.Person);
    Resource a3 = a.createResource("http://test.example.org/a3", FOAF.Person);
    a.add(a.createStatement(a1, FOAF.knows, a2));
    a.add(a.createStatement(a2, FOAF.knows, a3));
    Resource aAnonAmbiguous = a.createResource(AnonId.create("1234"));
    aAnonAmbiguous.addProperty(RDF.type, FOAF.Person);

    // b contains 5 statements
    Resource b1 = b.createResource("http://test.example.org/b1", FOAF.Person);
    Resource b2 = b.createResource("http://test.example.org/b2", FOAF.Person);
    Resource b3 = b.createResource("http://test.example.org/b3", FOAF.Person);
    b.add(b.createStatement(b1, FOAF.knows, b2));
    b.add(b.createStatement(b2, FOAF.knows, b3));

    // c will remain empty

    // d contains the same statement as a AND b
    d.add(a);
    d.add(b);

    // e contains anon ids one of which is ambiguous with one in another model
    Resource e1 = e.createResource(AnonId.create());
    Resource eAnonAmbiguous = e.createResource(AnonId.create("1234"));
    e1.addProperty(RDF.type, FOAF.Person);
    eAnonAmbiguous.addProperty(RDF.type, FOAF.Person);
  }

  @org.junit.jupiter.api.Test
  void iteratorWithNoModel() {
    JenaModelsIterator empty = new JenaModelsIterator();
    assertFalse(empty.hasNext(), "An iterator on no models should be empty.");
    assertEquals(0L, empty.estimatedNumResults(),
        "An iterator on no model should estimate no result.");
  }

  void testIterator(JenaModelsIterator iterator, int numberOfTriples, int numberOfUniqueTriples) {
    assertTrue(iterator.hasNext(), "An iterator with non empty model should have next.");
    assertTrue(iterator.estimatedNumResults() <= numberOfTriples,
        () -> "Estimate of iterator on first model should be lower or equals to "
            + numberOfTriples);
    TripleString item;
    int nb = 0;
    while (iterator.hasNext()) {
      nb++;
      assertNotNull(item = iterator.next());
      assertThat(item.getPredicate(), anyOf(is(RDF.type.toString()), is(FOAF.knows.toString())));
      if (item.getPredicate().equals(RDF.type.toString())) {
        assertThat(item.getObject(), is(FOAF.Person.toString()));
      } else {
        assertThat(item.getObject().toString(), startsWith("http://test.example.org/"));
      }
    }
    assertEquals(numberOfTriples, nb, "incorrect number of statements returned.");
    assertThrows(NoSuchElementException.class, iterator::next,
        "Calling next() after iterator exhaustion should throw a NoSuchMethodException.");

    iterator.goToStart();
    assertDoesNotThrow(iterator::next, "Unexpected Exception after restarting iterator.");

    iterator.goToStart();
    nb = 0;
    while (iterator.hasNext()) {
      nb++;
      assertNotNull(item = iterator.next());
      assertThat(item.getPredicate(), anyOf(is(RDF.type.toString()), is(FOAF.knows.toString())));
      if (item.getPredicate().equals(RDF.type.toString())) {
        assertThat(item.getObject(), is(FOAF.Person.toString()));
      } else {
        assertThat(item.getObject().toString(), startsWith("http://test.example.org/"));
      }
    }
    assertEquals(numberOfTriples, nb,
        "incorrect number of statements returned after iterator reinitialisation.");

    HashSet<String> items = new HashSet<>();
    iterator.goToStart();
    while (iterator.hasNext()) {
      items.add(iterator.next().toString());
    }
    assertEquals(numberOfUniqueTriples, items.size(), "Unexpected unique triples number");
  }

  @org.junit.jupiter.api.Test
  void iteratorWithOneModel() {
    JenaModelsIterator iterator = new JenaModelsIterator(a);
    testIterator(iterator, 6, 6);
  }

  @org.junit.jupiter.api.Test
  void iteratorWithTwoDistinctModels() {
    JenaModelsIterator iterator = new JenaModelsIterator(a, b);
    testIterator(iterator, 11, 11);
  }

  @org.junit.jupiter.api.Test
  void iteratorsWithANullModels() {
    JenaModelsIterator iterator = new JenaModelsIterator(a, null);
    testIterator(iterator, 6, 6);

    iterator = new JenaModelsIterator(null, a);
    testIterator(iterator, 6, 6);

    iterator = new JenaModelsIterator(null, a, b);
    testIterator(iterator, 11, 11);

    iterator = new JenaModelsIterator(a, null, b);
    testIterator(iterator, 11, 11);

    iterator = new JenaModelsIterator(a, b, null);
    testIterator(iterator, 11, 11);
  }

  @org.junit.jupiter.api.Test
  void iteratorsWithEmptyModels() {
    JenaModelsIterator iterator = new JenaModelsIterator(a, b, c);
    testIterator(iterator, 11, 11);

    iterator = new JenaModelsIterator(c, a, b);
    testIterator(iterator, 11, 11);

    iterator = new JenaModelsIterator(a, c, b);
    testIterator(iterator, 11, 11);
  }

  @org.junit.jupiter.api.Test
  void iteratorsWithOverLappingModels() {
    JenaModelsIterator iterator = new JenaModelsIterator(a, b, d);
    testIterator(iterator, 22, 11);

    iterator = new JenaModelsIterator(a, d, b);
    testIterator(iterator, 22, 11);

    iterator = new JenaModelsIterator(d, a, b);
    testIterator(iterator, 22, 11);
  }
}

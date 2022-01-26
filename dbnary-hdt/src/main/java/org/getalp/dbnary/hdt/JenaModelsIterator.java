package org.getalp.dbnary.hdt;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Stream;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.iterator.ClosableIterator;
import org.rdfhdt.hdt.enums.ResultEstimationType;
import org.rdfhdt.hdt.rdf.parsers.JenaNodeFormatter;
import org.rdfhdt.hdt.triples.IteratorTripleString;
import org.rdfhdt.hdt.triples.TripleString;

public class JenaModelsIterator implements IteratorTripleString {
  private final Model[] models;
  private StmtIterator[] iterators;
  private int currentModel = 0;

  public JenaModelsIterator(Model... models) {
    this.models = models;
    this.iterators = new StmtIterator[models.length];
  }

  private StmtIterator nextNonEmptyIterator() {
    StmtIterator ci = iterator();
    while (ci != null && !ci.hasNext()) {
      currentModel++;
      ci = iterator();
    }
    return ci;
  }

  private StmtIterator iterator() {
    while (currentModel < models.length && null == models[currentModel])
      currentModel++;
    if (currentModel >= models.length)
      return null;
    if (iterators[currentModel] == null)
      iterators[currentModel] = models[currentModel].listStatements();
    return iterators[currentModel];
  }

  @Override
  public boolean hasNext() {
    return null != nextNonEmptyIterator();
  }

  @Override
  public TripleString next() {
    StmtIterator it = nextNonEmptyIterator();
    if (null == it)
      throw new NoSuchElementException();
    Statement stm = it.nextStatement();

    return new TripleString(JenaNodeFormatter.format(stm.getSubject()),
        JenaNodeFormatter.format(stm.getPredicate()), JenaNodeFormatter.format(stm.getObject()));
  }

  @Override
  public void goToStart() {
    Arrays.stream(iterators).filter(Objects::nonNull).forEach(ClosableIterator::close);
    this.iterators = new StmtIterator[models.length];
    currentModel = 0;
  }

  @Override
  public long estimatedNumResults() {
    return Stream.of(models).filter(Objects::nonNull).map(Model::size).reduce(Long::sum).orElse(0L);
  }

  @Override
  public ResultEstimationType numResultEstimation() {
    return ResultEstimationType.MORE_THAN;
  }


  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

}

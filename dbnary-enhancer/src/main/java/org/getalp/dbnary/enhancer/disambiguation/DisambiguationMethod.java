package org.getalp.dbnary.enhancer.disambiguation;

import java.util.Set;
import org.apache.jena.rdf.model.Resource;

public interface DisambiguationMethod {

  /**
   * returns a set of LexicalSense that are considered relevant by the method as a disambiguation
   * for a LexicalEntry in a specific context.
   *
   * @param lexicalEntry the resource of the LEMON lexicalEntry to be disambiguated
   * @param context : the context used by the specific method as a disambiguation criterion
   * @return a set of resources corresponding to selected word senses.
   * @throws InvalidContextException if the given context object is not a context
   * @throws InvalidEntryException if the given resource does not represent an ontolex entry
   */
  public Set<? extends Resource> selectWordSenses(Resource lexicalEntry, Object context)
      throws InvalidContextException, InvalidEntryException;

}

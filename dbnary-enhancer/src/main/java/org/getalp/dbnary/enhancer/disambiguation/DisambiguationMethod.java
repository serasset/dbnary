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
   */
  public Set<? extends Resource> selectWordSenses(Resource lexicalEntry, Object context)
      throws InvalidContextException, InvalidEntryException;

}

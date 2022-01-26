package org.getalp.dbnary.languages;

import org.getalp.dbnary.StructuredGloss;

/**
 * Created by tchechem on 04/03/14.
 */
public class DefaultGlossFilter extends AbstractGlossFilter {

  @Override
  public StructuredGloss extractGlossStructure(String rawGloss) {
    if (null == rawGloss) {
      return null;
    }

    return new StructuredGloss(null, rawGloss);
  }
}

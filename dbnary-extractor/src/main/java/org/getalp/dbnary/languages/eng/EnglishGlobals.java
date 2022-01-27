package org.getalp.dbnary.languages.eng;

import java.util.HashMap;

public class EnglishGlobals {

  public final static HashMap<String, String> nymMarkerToNymName;

  static {
    nymMarkerToNymName = new HashMap<>(20);
    nymMarkerToNymName.put("Synonyms", "syn");
    nymMarkerToNymName.put("Antonyms", "ant");
    nymMarkerToNymName.put("Hyponyms", "hypo");
    nymMarkerToNymName.put("Hypernyms", "hyper");
    nymMarkerToNymName.put("Meronyms", "mero");
    nymMarkerToNymName.put("Holonyms", "holo");
    nymMarkerToNymName.put("Troponyms", "tropo");
  }
}

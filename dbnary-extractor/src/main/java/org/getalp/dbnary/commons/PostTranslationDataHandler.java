package org.getalp.dbnary.commons;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.DatatypeConverter;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.getalp.dbnary.languages.OntolexBasedRDFDataHandler;
import org.getalp.dbnary.StructuredGloss;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A data handler tailored to handle language editions where translation are given after all the
 * different entries and should be attached to correct entries somehow.
 */
public abstract class PostTranslationDataHandler extends OntolexBasedRDFDataHandler {
  private static final Logger log = LoggerFactory.getLogger(PostTranslationDataHandler.class);

  private String encodedWiktionaryPageName;
  protected Map<Resource, Set<Resource>> lexEntries = new HashMap<>();

  protected PostTranslationDataHandler(String lang, String tdbDir) {
    super(lang, tdbDir);
  }

  @Override
  public void initializeLanguageSection(String lang) {
    super.initializeLanguageSection(lang);
    lexEntries.clear();
    encodedWiktionaryPageName = uriEncode(currentPage.getName());
  }

  @Override
  public void finalizeLanguageSection() {
    super.finalizeLanguageSection();
    encodedWiktionaryPageName = null;
  }

  @Override
  public Resource initializeLexicalEntry(String pos, Resource lexinfoPOS, Resource type) {
    Resource entry = super.initializeLexicalEntry(pos, lexinfoPOS, type);
    addLexEntry(lexinfoPOS, entry);
    return entry;
  }

  @Override
  protected String getGlossResourceName(StructuredGloss gloss) {
    String key = gloss.getGloss() + gloss.getSenseNumber();
    key = DatatypeConverter.printBase64Binary(BigInteger.valueOf(key.hashCode()).toByteArray())
        .replaceAll("[/=+]", "-");
    return getPrefix() + "__" + shortEditionLanguageCode + "_gloss_" + key + "_"
        + encodedWiktionaryPageName;
  }

  private int countEntries() {
    return lexEntries.values().stream().mapToInt(set -> set.size()).sum();
  }

  @Override
  public void registerTranslation(String lang, Resource currentGloss, String usage, String word) {
    int nbEntries = countEntries();
    if (nbEntries == 0) {
      log.debug("Registering Translation when no lexical entry is defined in {}",
          currentPage.getName());
    } else if (nbEntries == 1) {
      super.registerTranslation(lang, currentGloss, usage, word);
    } else if (null == currentGloss) {
      log.debug("Attaching translations to Page (Null gloss and several lexical entries) in {}",
          currentPage.getName());
      super.registerTranslationToEntity(currentMainLexEntry, lang, currentGloss, usage, word);
    } else {
      // DONE: guess which translation is to be attached to which entry/sense
      List<Resource> entries = getLexicalEntryUsingGloss(currentGloss);
      if (entries.size() != 0) {
        log.trace("Attaching translations using part of speech in gloss : {}",
            currentPage.getName());
        if (entries.size() > 1) {
          log.trace("Attaching translations to several entries in {}", currentPage.getName());
        }
        for (Resource entry : entries) {
          super.registerTranslationToEntity(entry, lang, currentGloss, usage, word);
        }
      } else {
        Statement s = currentGloss.getProperty(RDF.value);
        String g = (null == s) ? "" : s.getString();
        log.debug("Several entries are defined in {} // {}", currentPage.getName(), g);
        // TODO: disambiguate and attach to the correct entry.
        super.registerTranslationToEntity(currentMainLexEntry, lang, currentGloss, usage, word);
      }
    }
  }

  protected abstract List<Resource> getLexicalEntryUsingGloss(Resource structuredGloss);

  protected void addAllResourceOfPoS(ArrayList<Resource> res, Resource pos) {
    Set<Resource> ares = lexEntries.get(pos);
    if (ares != null) {
      res.addAll(ares);
    }
  }

  private void addLexEntry(Resource pos, Resource entry) {
    Set<Resource> entrySet;
    if (null != (entrySet = lexEntries.get(pos))) {
      entrySet.add(entry);
    } else {
      entrySet = new HashSet<>();
      entrySet.add(entry);
      lexEntries.put(pos, entrySet);
    }
  }

  public void extractEtymmology() {
    // In language edition which use a post translation layout, the etymology sections usually
    // contain the translation. Hence, we should only attach translation belonging the the correct
    // lexical entries
  }
}

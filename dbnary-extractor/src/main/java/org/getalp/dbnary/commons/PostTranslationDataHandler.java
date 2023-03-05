package org.getalp.dbnary.commons;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.xml.bind.DatatypeConverter;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.getalp.dbnary.DBnaryOnt;
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
  protected Map<String, Set<Resource>> lexEntries = new HashMap<>();
  protected Map<String, Pair<Resource, Resource>> senses = new HashMap<>();

  protected PostTranslationDataHandler(String lang, String tdbDir) {
    super(lang, tdbDir);
  }

  @Override
  public void initializeLanguageSection(String lang) {
    super.initializeLanguageSection(lang);
    lexEntries.clear();
    senses.clear();
    encodedWiktionaryPageName = uriEncode(currentPage.getName());
  }

  public void startNewEtymologySection() {
    // Translations, entries and senses are local to an Etymology block in Spanish
    lexEntries.clear();
    senses.clear();
  }

  @Override
  public void finalizeLanguageSection() {
    super.finalizeLanguageSection();
    encodedWiktionaryPageName = null;
  }

  @Override
  public Resource initializeLexicalEntry(String pos, Resource lexinfoPOS, Resource type) {
    Resource entry = super.initializeLexicalEntry(pos, lexinfoPOS, type);
    addLexEntry(pos, lexinfoPOS, entry);
    return entry;
  }

  @Override
  public Resource registerNewDefinition(String def, String senseNumber) {
    Resource sense = super.registerNewDefinition(def, senseNumber);
    if (null != senseNumber && null != sense) {
      Pair<Resource, Resource> pair = senses.get(senseNumber);
      if (null != pair) {
        log.warn("Registering a new sense with an already existing sense number {}: {} ||| {} ",
            senseNumber, def, currentPagename());
        // In this case, should I remove all senses to avoid attaching translations to the
        // incorrect entry ?
      } else {
        pair = new ImmutablePair<>(sense, currentLexEntry);
        senses.put(senseNumber, pair);
      }
    }
    return sense;
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
    // WARN: the entries may be duplicate (as they are available under several keys,
    // we should count unique entries
    Set<Resource> uniqueEntries =
        lexEntries.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
    return uniqueEntries.size();
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
      List<List<Resource>> entries = getLexicalEntriesUsingGloss(currentGloss);
      if (entries.size() != 0) {
        log.trace("Attaching translations using part of speech/sense number in gloss : {}",
            currentPage.getName());
        if (entries.size() > 1) {
          log.trace("Attaching translations to several entries in {}", currentPage.getName());
        }
        for (List<Resource> entry : entries) {
          if (!entry.isEmpty()) {
            Resource first = entry.get(0);
            Resource trans =
                super.registerTranslationToEntity(first, lang, currentGloss, usage, word);
            for (int i = 1; i < entry.size(); i++) {
              trans.addProperty(DBnaryOnt.isTranslationOf, entry.get(i));
            }
          }
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

  protected abstract List<List<Resource>> getLexicalEntriesUsingGloss(Resource structuredGloss);

  protected void addAllResourceOfPoS(ArrayList<List<Resource>> res, Resource pos) {
    if (null != pos) {
      addAllResourceOfPoS(res, pos.toString());
    }
  }

  protected void addAllResourceOfPoS(ArrayList<List<Resource>> res, String pos) {
    Set<Resource> ares = lexEntries.get(pos);
    if (ares != null) {
      ares.forEach(r -> res.add(List.of(r)));
    }
  }

  private void addLexEntry(String wikiPos, Resource lexinfoPos, Resource entry) {
    addLexEntry(wikiPos, entry);
    if (null != lexinfoPos) {
      addLexEntry(lexinfoPos.toString(), entry);
    }
  }

  private void addLexEntry(String key, Resource entry) {
    Set<Resource> entrySet;
    if (null != (entrySet = lexEntries.get(key))) {
      entrySet.add(entry);
    } else {
      entrySet = new HashSet<>();
      entrySet.add(entry);
      lexEntries.put(key, entrySet);
    }
  }

  public void extractEtymology() {
    // In language edition which use a post translation layout, the etymology sections usually
    // contain the translation. Hence, we should only attach translation belonging the correct
    // lexical entries
    lexEntries.clear();
  }
}

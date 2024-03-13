package org.getalp.dbnary.api;

import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.getalp.dbnary.ExtractionFeature;
import org.getalp.dbnary.PronunciationPair;
import org.getalp.dbnary.PropertyObjectPair;
import org.getalp.dbnary.StructuredGloss;
import org.getalp.dbnary.enhancer.evaluation.EvaluationStats.Stat;
import org.getalp.dbnary.enhancer.evaluation.TranslationGlossesStat;
import org.getalp.dbnary.languages.AbstractGlossFilter;
import org.getalp.dbnary.morphology.InflectionData;

public interface IWiktionaryDataHandler {

  /**
   * close the dataset that eventually backs up the different feature boxes.
   * <p>
   * Does nothing when there is no dataset backing up the boxes.
   */
  void closeDataset();

  /**
   * Enable the extraction of morphological data in a second Model if available.
   *
   * @param f Feature
   */
  void enableEndolexFeatures(ExtractionFeature f);

  void enableExolexFeatures(ExtractionFeature f);

  Model getFeatureBox(ExtractionFeature f);

  Model getEndolexFeatureBox(ExtractionFeature f);

  Model getExolexFeatureBox(ExtractionFeature f);

  boolean isDisabled(ExtractionFeature f);

  void initializePageExtraction(String wiktionaryPageName);

  void finalizePageExtraction();

  void initializeLanguageSection(String language);

  void finalizeLanguageSection();

  /**
   * returns the short (2 letter code) id of the language of the current LexicalEntry
   * 
   * @return current entry short language code
   */
  String getCurrentEntryLanguage();

  /**
   * returns the short (2 letter code) id of the language of the language edition
   * 
   * @return wiktionary edition short language code
   */
  String getExtractedLanguage();

  void initializeLexicalEntry(String pos);

  /**
   * Register definition def for the current lexical entry.
   * <p>
   * This method will compute a sense number based on the rank of the definition in the entry.
   * <p>
   * It is equivalent to registerNewDefinition(def, 1);
   *
   * @param def a string
   * @return
   */
  Resource registerNewDefinition(String def);

  /**
   * Register definition def for the current lexical entry.
   * <p>
   * This method will compute a sense number based on the rank of the definition in the entry,
   * taking into account the level of the definition. 1, 1a, 1b, 1c, 2, etc.
   *
   * @param def the definition string
   * @param lvl an integer giving the level of the definition (1 or 2).
   * @return
   */
  Resource registerNewDefinition(String def, int lvl);

  /**
   * Register example ex for the current lexical sense.
   *
   * @param ex the example string
   * @param context map of property + RDFNode that are to be attached to the example object.
   * @return a Resource
   */
  Resource registerExample(String ex, Set<Pair<Property, RDFNode>> context);


  /**
   * Register definition def for the current lexical entry.
   * <p>
   * This method will use senseNumber as a sense number for this definition.
   *
   * @param def the definition string
   * @param senseNumber a string giving the sense number of the definition.
   * @return
   */
  Resource registerNewDefinition(String def, String senseNumber);


  void registerAlternateSpelling(String alt);

  void registerNymRelation(String target, String synRelation);

  AbstractGlossFilter getGlossFilter();

  Resource createGlossResource(String gloss, int rank);

  Resource createGlossResource(StructuredGloss gloss, int rank);

  Resource createGlossResource(String gloss);

  Resource createGlossResource(StructuredGloss gloss);

  void registerNymRelation(String target, String synRelation, Resource gloss, String usage);

  void registerTranslation(String lang, Resource currentGlose, String usage, String word);

  void registerPronunciation(String pron, String lang);

  int nbEntries();

  String currentPagename();

  /**
   * Write a serialized represention of this model in a specified language. The language in which to
   * write the model is specified by the lang argument. Predefined values are "RDF/XML",
   * "RDF/XML-ABBREV", "N-TRIPLE", "TURTLE", (and "TTL") and "N3". The default value, represented by
   * null, is "RDF/XML".
   *
   * @param model the Model to be dumped
   * @param out an OutputStream
   * @param format a String
   */
  void dump(Model model, OutputStream out, String format);

  default void registerNymRelationOnCurrentSense(String target, String synRelation) {
    registerNymRelationOnCurrentSense(target, synRelation, null, null);
  }

  void registerNymRelationOnCurrentSense(String target, String synRelation, Resource gloss,
      String usage);


  void registerPropertyOnLexicalEntry(Property p, RDFNode r);

  void registerPropertyOnCanonicalForm(Property p, RDFNode r);

  void registerInflection(String languageCode, String pos, String inflection, String canonicalForm,
      int defNumber, HashSet<PropertyObjectPair> properties,
      HashSet<PronunciationPair> pronunciations);

  void registerInflection(String languageCode, String pos, String inflection, String canonicalForm,
      int defNumber, HashSet<PropertyObjectPair> properties);

  void registerInflection(InflectionData key, Set<String> value);

  String currentWiktionaryPos();

  Resource currentLexinfoPos();

  void populateMetadata(Model metadataModel, Model sourceModel, String dumpFilename,
      String extractorVersion, boolean isExolex);

  void buildDatacubeObservations(String l, TranslationGlossesStat translationGlossesStat, Stat stat,
      String dumpFileVersion);

  void computeStatistics(Model statsModel, Model sourceModel, String dumpVersion);

  void dumpAllFeaturesAsHDT(OutputStream ostream, boolean isExolex);

  Resource addToCurrentWordSense(Set<Pair<Property, RDFNode>> context);
}

package org.getalp.dbnary;

import java.io.OutputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.getalp.dbnary.enhancer.evaluation.EvaluationStats.Stat;
import org.getalp.dbnary.enhancer.evaluation.TranslationGlossesStat;

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
  void enableFeature(ExtractionFeature f);

  Model getFeatureBox(ExtractionFeature f);

  boolean isDisabled(ExtractionFeature f);

  void initializePageExtraction(String wiktionaryPageName);

  void finalizePageExtraction();

  void initializeEntryExtraction(String wiktionaryPageName);

  void initializeEntryExtraction(String wiktionaryPageName, String lang);

  void finalizeEntryExtraction();

  String getCurrentEntryLanguage();

  void addPartOfSpeech(String pos);

  /**
   *
   * @param def the not cleaned up version of the definition. This version contains macros (that may
   *        represent subject fields) and links.
   */
  // TODO: maybe pass the cleaned up and the original def, so that the extractor takes what fits its
  // requirements.

  /**
   * Register definition def for the current lexical entry.
   * <p>
   * This method will compute a sense number based on the rank of the definition in the entry.
   * <p>
   * It is equivalent to registerNewDefinition(def, 1);
   *
   * @param def a string
   */
  void registerNewDefinition(String def);

  /**
   * Register definition def for the current lexical entry.
   * <p>
   * This method will compute a sense number based on the rank of the definition in the entry,
   * taking into account the level of the definition. 1, 1a, 1b, 1c, 2, etc.
   *
   * @param def the definition string
   * @param lvl an integer giving the level of the definition (1 or 2).
   */
  void registerNewDefinition(String def, int lvl);

  /**
   * Register example ex for the current lexical sense.
   *
   * @param ex the example string
   * @param context map of property + object that are to be attached to the example object.
   * @return a Resource
   */
  Resource registerExample(String ex, Map<Property, String> context);


  /**
   * Register definition def for the current lexical entry.
   * <p>
   * This method will use senseNumber as a sense number for this definition.
   *
   * @param def the definition string
   * @param senseNumber a string giving the sense number of the definition.
   */
  void registerNewDefinition(String def, String senseNumber);


  void registerAlternateSpelling(String alt);

  void registerNymRelation(String target, String synRelation);

  Resource createGlossResource(StructuredGloss gloss, int rank);

  Resource createGlossResource(StructuredGloss gloss);

  void registerNymRelation(String target, String synRelation, Resource gloss);

  void registerNymRelation(String target, String synRelation, Resource gloss, String usage);

  void registerTranslation(String lang, Resource currentGlose, String usage, String word);

  void registerPronunciation(String pron, String lang);

  int nbEntries();

  String currentLexEntry();

  /**
   * Write a serialized represention of this model in a specified language. The language in which to
   * write the model is specified by the lang argument. Predefined values are "RDF/XML",
   * "RDF/XML-ABBREV", "N-TRIPLE", "TURTLE", (and "TTL") and "N3". The default value, represented by
   * null, is "RDF/XML".
   *
   * @param f a Feature
   * @param out an OutputStream
   * @param format a String
   */
  void dump(ExtractionFeature f, OutputStream out, String format);

  void registerNymRelationOnCurrentSense(String target, String synRelation);

  void registerPropertyOnLexicalEntry(Property p, RDFNode r);

  void registerPropertyOnCanonicalForm(Property p, RDFNode r);

  void registerInflection(String languageCode, String pos, String inflection, String canonicalForm,
      int defNumber, HashSet<PropertyObjectPair> properties,
      HashSet<PronunciationPair> pronunciations);

  void registerInflection(String languageCode, String pos, String inflection, String canonicalForm,
      int defNumber, HashSet<PropertyObjectPair> properties);

  void registerInflection(InflectionData key, Set<String> value);

  int currentDefinitionNumber();

  String currentWiktionaryPos();

  Resource currentLexinfoPos();

  void populateMetadata(String dumpFilename, String extractorVersion);

  void buildDatacubeObservations(String l, TranslationGlossesStat translationGlossesStat, Stat stat,
      String dumpFileVersion);
}

package org.getalp.dbnary.fra;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.getalp.LangTools;
import org.getalp.dbnary.DBnaryOnt;
import org.getalp.dbnary.ExtractionFeature;
import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.OntolexBasedRDFDataHandler;
import org.getalp.dbnary.OntolexOnt;
import org.getalp.dbnary.PronunciationPair;
import org.getalp.dbnary.PropertyObjectPair;
import org.getalp.dbnary.StructuredGloss;
import org.getalp.dbnary.enhancer.evaluation.EvaluationStats.Stat;
import org.getalp.dbnary.enhancer.evaluation.TranslationGlossesStat;
import org.getalp.dbnary.morphology.InflectionData;
import org.getalp.model.dbnary.Page;
import org.getalp.model.ontolex.LexicalForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by serasset on 17/09/14.
 */
public class WiktionaryDataHandler extends OntolexBasedRDFDataHandler {

  private Logger log = LoggerFactory.getLogger(WiktionaryDataHandler.class);

  // A entry -> pos -> set of lexical forms hashmap used to store the inflected form which have
  // to be registered chen the main lexical entry is processed.
  private HashMap<String, HashMap<String, Set<LexicalForm>>> heldBackOtherForms = new HashMap<>();

  static {

    // French
    posAndTypeValueMap.put("-nom-", new PosAndType(LexinfoOnt.noun, OntolexOnt.Word));
    posAndTypeValueMap.put("-nom-pr-", new PosAndType(LexinfoOnt.properNoun, OntolexOnt.Word));
    posAndTypeValueMap.put("-prénom-", new PosAndType(LexinfoOnt.properNoun, OntolexOnt.Word));
    posAndTypeValueMap.put("-adj-", new PosAndType(LexinfoOnt.adjective, OntolexOnt.Word));
    posAndTypeValueMap.put("-verb-", new PosAndType(LexinfoOnt.verb, OntolexOnt.Word));
    posAndTypeValueMap.put("-adv-", new PosAndType(LexinfoOnt.adverb, OntolexOnt.Word));
    posAndTypeValueMap.put("-loc-adv-",
        new PosAndType(LexinfoOnt.adverb, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("-loc-adj-",
        new PosAndType(LexinfoOnt.adjective, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("-loc-nom-",
        new PosAndType(LexinfoOnt.noun, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("-loc-verb-",
        new PosAndType(LexinfoOnt.verb, OntolexOnt.MultiWordExpression));
  }

  public WiktionaryDataHandler(String lang, String tdbDir) {
    super(lang, tdbDir);
  }

  @Override
  public void initializeLanguageSection(String wiktionaryPageName, String language) {
    language = LangTools.getPart1OrId(language);
    if (null != language && language.equals(wktLanguageEdition)) {
      super.initializeLanguageSection(wiktionaryPageName);
    } else {
      super.initializeLanguageSection(wiktionaryPageName, language);
    }
  }



  @Override
  public void initializeLexicalEntry(String pos) {
    // DONE: compute if the entry is a phrase or a word.
    PosAndType pat = posAndTypeValueMap.get(pos);
    Resource typeR = typeResource(pat);
    if (currentWiktionaryPageName.startsWith("se ")) {
      if (currentWiktionaryPageName.substring(2).trim().contains(" ")) {
        typeR = OntolexOnt.MultiWordExpression;
      }
    } else if (currentWiktionaryPageName.contains(" ")) {
      typeR = OntolexOnt.MultiWordExpression;
    }
    // reset the sense number.
    currentSenseNumber = 0;
    currentSubSenseNumber = 0;
    initializeLexicalEntry(pos, posResource(pat), typeR);
    Model morphoBox = getFeatureBox(ExtractionFeature.MORPHOLOGY);
    if (null != morphoBox) {
      HashMap<String, Set<LexicalForm>> pos2forms =
          heldBackOtherForms.getOrDefault(currentWiktionaryPageName, new HashMap<>());
      Set<LexicalForm> forms = pos2forms.getOrDefault(pos, new HashSet<>());
      forms.forEach(f -> f.attachTo(currentLexEntry.inModel(morphoBox)));
    }
  }

  protected String computeSenseNum() {
    char s;
    if (currentSubSenseNumber > 26) {
      log.error("Subsense (alphabetical) number above z in {}", currentEncodedLexicalEntryName);
      s = (char) ('A' + currentSubSenseNumber - 1);
    } else {
      s = (char) ('a' + currentSubSenseNumber - 1);
    }
    return "" + currentSenseNumber + ((currentSubSenseNumber == 0) ? "" : s);
  }

  public void addLexicalForm(LexicalForm form) {
    // TODO: should we check if the lexical form exists ?
    // TODO: should we check if a compatible lexical form exists ?
    Model morphoBox = this.getFeatureBox(ExtractionFeature.MORPHOLOGY);

    if (null == morphoBox) {
      return;
    }

    form.attachTo(currentLexEntry.inModel(morphoBox));
  }

  public void registerInflection(LexicalForm form, String onLexicalEntry, String languageCode,
      String pos) {

    Resource posResource = posResource(pos);

    // First, we store the other form for all the existing entries
    Resource page = getPageResource(onLexicalEntry, true);

    Model morphoBox = this.getFeatureBox(ExtractionFeature.MORPHOLOGY);
    if (null != morphoBox) {
      page.listProperties(DBnaryOnt.describes).toList().stream().map(Statement::getResource)
          .filter(r -> aBox.contains(r, LexinfoOnt.partOfSpeech, posResource))
          .map(r -> r.inModel(morphoBox)).forEach(form::attachTo);
    }

    // Second, we store the other form for future possible matching entries
    Pair<String, String> key = new ImmutablePair<>(onLexicalEntry, pos);


    HashMap<String, Set<LexicalForm>> pos2forms =
        heldBackOtherForms.computeIfAbsent(onLexicalEntry, k -> new HashMap<>());
    Set<LexicalForm> otherForms = pos2forms.computeIfAbsent(pos, k -> new HashSet<>());

    otherForms.add(form);
  }

  @Override
  public void initializePageExtraction(String wiktionaryPageName) {
    Page currentPage = new Page("fra", wiktionaryPageName); // <-- HERE
    super.initializePageExtraction(wiktionaryPageName);
  }


  @Override
  public void finalizePageExtraction() {
    super.finalizePageExtraction();
    // Remove all inflections related to the current page as they cannot be attach to
    // another page anymore.
    heldBackOtherForms.remove(currentWiktionaryPageName);
  }


  /*---------- OTHER METHODS --------------*/

  @Override
  public String getCurrentEntryLanguage() {
    return super.getCurrentEntryLanguage();
  }

  @Override
  public void closeDataset() {
    super.closeDataset();
  }

  @Override
  public void initializeLanguageSection(String wiktionaryPageName) {
    super.initializeLanguageSection(wiktionaryPageName);
  }

  @Override
  public void finalizeLanguageSection() {
    super.finalizeLanguageSection();
  }

  @Override
  public Resource getLexEntry(String languageCode, String pageName, String pos, int defNumber) {
    return super.getLexEntry(languageCode, pageName, pos, defNumber);
  }

  @Override
  public Resource getLexEntry(String encodedPageName, Resource typeResource) {
    return super.getLexEntry(encodedPageName, typeResource);
  }

  @Override
  public int currentDefinitionNumber() {
    return super.currentDefinitionNumber();
  }

  @Override
  public String currentWiktionaryPos() {
    return super.currentWiktionaryPos();
  }

  @Override
  public Resource currentLexinfoPos() {
    return super.currentLexinfoPos();
  }

  @Override
  public void populateMetadata(String dumpFilename, String extractorVersion) {
    super.populateMetadata(dumpFilename, extractorVersion);
  }

  @Override
  public void buildDatacubeObservations(String l, TranslationGlossesStat tgs, Stat es,
      String dumpFileVersion) {
    super.buildDatacubeObservations(l, tgs, es, dumpFileVersion);
  }

  @Override
  public Resource initializeLexicalEntry(String originalPOS, Resource normalizedPOS,
      Resource normalizedType) {
    return super.initializeLexicalEntry(originalPOS, normalizedPOS, normalizedType);
  }

  @Override
  public Resource posResource(PosAndType pat) {
    return super.posResource(pat);
  }

  @Override
  public Resource typeResource(PosAndType pat) {
    return super.typeResource(pat);
  }

  @Override
  public Resource posResource(String pos) {
    return super.posResource(pos);
  }

  @Override
  public Resource typeResource(String pos) {
    return super.typeResource(pos);
  }

  @Override
  public void registerPropertyOnCanonicalForm(Property p, RDFNode r) {
    super.registerPropertyOnCanonicalForm(p, r);
  }

  @Override
  public void registerPropertyOnLexicalEntry(Property p, RDFNode r) {
    super.registerPropertyOnLexicalEntry(p, r);
  }

  @Override
  public void registerAlternateSpelling(String alt) {
    super.registerAlternateSpelling(alt);
  }

  @Override
  public void registerNewDefinition(String def) {
    super.registerNewDefinition(def);
  }

  @Override
  public void registerNewDefinition(String def, int lvl) {
    super.registerNewDefinition(def, lvl);
  }

  @Override
  public void registerNewDefinition(String def, String senseNumber) {
    super.registerNewDefinition(def, senseNumber);
  }

  @Override
  protected Resource registerTranslationToEntity(Resource entity, String lang,
      Resource currentGlose, String usage, String word) {
    return super.registerTranslationToEntity(entity, lang, currentGlose, usage, word);
  }

  @Override
  public void registerTranslation(String lang, Resource currentGlose, String usage, String word) {
    super.registerTranslation(lang, currentGlose, usage, word);
  }

  @Override
  public String getVocableResourceName(String vocable) {
    return super.getVocableResourceName(vocable);
  }

  @Override
  public Resource getPageResource(String vocable, boolean dontLinkWithType) {
    return super.getPageResource(vocable, dontLinkWithType);
  }

  @Override
  public Resource getPageResource(String vocable) {
    return super.getPageResource(vocable);
  }

  @Override
  protected void mergePropertiesIntoResource(HashSet<PropertyObjectPair> properties, Resource res) {
    super.mergePropertiesIntoResource(properties, res);
  }

  @Override
  protected boolean isResourceCompatible(Resource r, HashSet<PropertyObjectPair> properties) {
    return super.isResourceCompatible(r, properties);
  }

  @Override
  protected void addOtherFormPropertiesToLexicalEntry(Resource lexEntry,
      HashSet<PropertyObjectPair> properties) {
    super.addOtherFormPropertiesToLexicalEntry(lexEntry, properties);
  }

  @Override
  protected String computeOtherFormResourceName(Resource lexEntry,
      HashSet<PropertyObjectPair> properties) {
    return super.computeOtherFormResourceName(lexEntry, properties);
  }

  @Override
  public void registerInflection(String languageCode, String pos, String inflection,
      String canonicalForm, int defNumber, HashSet<PropertyObjectPair> props,
      HashSet<PronunciationPair> pronunciations) {
    super.registerInflection(languageCode, pos, inflection, canonicalForm, defNumber, props,
        pronunciations);
  }

  @Override
  public void registerInflection(String languageCode, String pos, String inflection,
      String canonicalForm, int defNumber, HashSet<PropertyObjectPair> props) {
    super.registerInflection(languageCode, pos, inflection, canonicalForm, defNumber, props);
  }

  @Override
  public void registerInflection(InflectionData key, Set<String> value) {
    super.registerInflection(key, value);
  }

  @Override
  public void registerNymRelationToEntity(String target, String synRelation, Resource entity) {
    super.registerNymRelationToEntity(target, synRelation, entity);
  }

  @Override
  public void registerNymRelationToEntity(String target, String synRelation, Resource entity,
      Resource gloss, String usage) {
    super.registerNymRelationToEntity(target, synRelation, entity, gloss, usage);
  }

  @Override
  public void registerNymRelation(String target, String synRelation) {
    super.registerNymRelation(target, synRelation);
  }

  @Override
  public Resource createGlossResource(StructuredGloss gloss) {
    return super.createGlossResource(gloss);
  }

  @Override
  public Resource createGlossResource(StructuredGloss gloss, int rank) {
    return super.createGlossResource(gloss, rank);
  }

  @Override
  protected String getGlossResourceName(StructuredGloss gloss) {
    return super.getGlossResourceName(gloss);
  }

  @Override
  public void registerNymRelation(String target, String synRelation, Resource gloss) {
    super.registerNymRelation(target, synRelation, gloss);
  }

  @Override
  public void registerNymRelation(String target, String synRelation, Resource gloss, String usage) {
    super.registerNymRelation(target, synRelation, gloss, usage);
  }

  @Override
  protected String computeNymId(String nym) {
    return super.computeNymId(nym);
  }

  @Override
  protected String computeNymId(String nym, String pagename) {
    return super.computeNymId(nym, pagename);
  }

  @Override
  public void registerNymRelationOnCurrentSense(String target, String synRelation) {
    super.registerNymRelationOnCurrentSense(target, synRelation);
  }

  @Override
  public void registerPronunciation(String pron, String lang) {
    super.registerPronunciation(pron, lang);
  }

  @Override
  protected void registerPronunciation(Resource writtenRepresentation, String pron, String lang) {
    super.registerPronunciation(writtenRepresentation, pron, lang);
  }

  @Override
  public void dump(ExtractionFeature f, OutputStream out, String format) {
    super.dump(f, out, format);
  }

  @Override
  public int nbEntries() {
    return super.nbEntries();
  }

  @Override
  public String currentLexEntry() {
    return super.currentLexEntry();
  }

  @Override
  public String getPrefix() {
    return super.getPrefix();
  }

  @Override
  public Resource registerExample(String ex, Map<Property, String> context) {
    return super.registerExample(ex, context);
  }

  @Override
  public void computeStatistics(String dumpVersion) {
    super.computeStatistics(dumpVersion);
  }
}

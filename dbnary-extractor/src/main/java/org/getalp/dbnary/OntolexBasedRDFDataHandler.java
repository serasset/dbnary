package org.getalp.dbnary;

import static org.getalp.dbnary.stats.Statistics.countRelations;
import static org.getalp.dbnary.stats.Statistics.countResourcesOfType;

import java.io.OutputStream;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.xml.bind.DatatypeConverter;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ReifiedStatement;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.tdb2.TDB2Factory;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.getalp.LangTools;
import org.getalp.dbnary.enhancer.evaluation.EvaluationStats;
import org.getalp.dbnary.enhancer.evaluation.TranslationGlossesStat;
import org.getalp.dbnary.model.DbnaryModel;
import org.getalp.dbnary.model.NymRelation;
import org.getalp.dbnary.morphology.InflectionData;
import org.getalp.dbnary.stats.Statistics;
import org.getalp.dbnary.tools.CounterSet;
import org.getalp.iso639.ISO639_3;
import org.getalp.model.dbnary.Page;
import org.getalp.model.ontolex.LexicalEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO : MAYBE separate Etymology/Translation/Definition/etc. sections extraction in separate
// classes ?
public class OntolexBasedRDFDataHandler extends DbnaryModel implements IWiktionaryDataHandler {

  private final Dataset dataset;

  protected static class PosAndType {

    public Resource pos;
    public Resource type;

    public PosAndType(Resource p, Resource t) {
      this.pos = p;
      this.type = t;
    }
  }

  private final Logger log = LoggerFactory.getLogger(OntolexBasedRDFDataHandler.class);

  protected Model aBox;

  private final Map<ExtractionFeature, Model> featureBoxes;

  // States used for processing
  @Deprecated
  protected Resource currentLexEntry;
  // @Deprecated protected Resource currentLexinfoPos;
  // @Deprecated protected String currentWiktionaryPos;

  protected LexicalEntry currentLexicalEntry;


  protected Resource currentSense;
  protected int currentSenseNumber;
  protected int currentSubSenseNumber;
  protected CounterSet translationCount = new CounterSet();
  private CounterSet reifiedNymCount = new CounterSet();

  // Language identification (edition and current section)
  protected final String shortEditionLanguageCode;
  protected final String longEditionLanguageCode;
  protected String shortSectionLanguageCode;
  protected String longSectionLanguageCode;


  protected Resource lexvoExtractedLanguage;
  protected Resource lexvoSectionLanguage;

  private Set<Statement> heldBackStatements = new HashSet<Statement>();

  protected int nbEntries = 0;
  private String NS;
  protected String WIKT;
  protected String currentEncodedLexicalEntryName;
  /**
   * @deprecated use currentPage.getName()
   */
  @Deprecated
  protected String currentWiktionaryPageName;
  @Deprecated
  protected CounterSet currentLexieCount = new CounterSet();
  /**
   * @deprecated should rely on currentPage (not a Resource anymore
   */
  @Deprecated
  protected Resource currentMainLexEntry;
  protected Page currentPage;
  protected Resource currentCanonicalForm;

  protected Set<PronunciationPair> currentSharedPronunciations;
  // private String currentSharedPronunciation;
  // private String currentSharedPronunciationLang;

  private HashMap<SimpleImmutableEntry<String, String>, HashSet<HashSet<PropertyObjectPair>>> heldBackOtherForms =
      new HashMap<SimpleImmutableEntry<String, String>, HashSet<HashSet<PropertyObjectPair>>>();

  // protected static HashMap<String, Property> nymPropertyMap = new HashMap<String, Property>();
  protected static HashMap<String, PosAndType> posAndTypeValueMap =
      new HashMap<String, PosAndType>();

  // Map of the String to lexvo language entity
  private HashMap<String, Resource> languages = new HashMap<String, Resource>();

  public OntolexBasedRDFDataHandler(String longEditionLanguageCode, String tdbDir) {
    super();

    if (null != tdbDir) {
      dataset = TDB2Factory.connectDataset(tdbDir);
      dataset.begin(ReadWrite.WRITE); // UGLY: using one LONG write transaction
    } else {
      dataset = null;
    }

    NS = DBNARY_NS_PREFIX + "/" + longEditionLanguageCode + "/";

    this.longEditionLanguageCode = longEditionLanguageCode;
    this.shortEditionLanguageCode = LangTools.getPart1OrId(longEditionLanguageCode);
    WIKT = "https://" + shortEditionLanguageCode + ".wiktionary.org/wiki/";
    lexvoExtractedLanguage = tBox.createResource(LEXVO + longEditionLanguageCode);

    // Create aBox
    aBox = createAndInitializeABox(longEditionLanguageCode);

    featureBoxes = new HashMap<>();
    featureBoxes.put(ExtractionFeature.MAIN, aBox);
  }

  private Model createAndInitializeABox(String lang) {
    return this.createAndInitializeABox(lang, ExtractionFeature.MAIN);
  }

  private Model createAndInitializeABox(String lang, ExtractionFeature f) {
    // Create aBox
    Model aBox;

    if (null != dataset) {
      aBox = dataset.getNamedModel("NS" + f.name().toLowerCase() + "/");
    } else {
      aBox = ModelFactory.createDefaultModel();
    }
    aBox.setNsPrefix(lang, NS);
    aBox.setNsPrefix("dbnary", DBnaryOnt.getURI());
    aBox.setNsPrefix("dbstats", DBNARY_NS_PREFIX + "/statistics/");
    aBox.setNsPrefix("dbetym", DBnaryEtymologyOnt.getURI());
    aBox.setNsPrefix("lexinfo", LexinfoOnt.getURI());
    aBox.setNsPrefix("rdfs", RDFS.getURI());
    aBox.setNsPrefix("dcterms", DCTerms.getURI());
    aBox.setNsPrefix("lexvo", LEXVO);
    aBox.setNsPrefix("rdf", RDF.getURI());
    aBox.setNsPrefix("olia", OliaOnt.getURI());
    aBox.setNsPrefix("ontolex", OntolexOnt.getURI());
    aBox.setNsPrefix("vartrans", VarTransOnt.getURI());
    aBox.setNsPrefix("synsem", SynSemOnt.getURI());
    aBox.setNsPrefix("lime", LimeOnt.getURI());
    aBox.setNsPrefix("decomp", DecompOnt.getURI());
    aBox.setNsPrefix("skos", SkosOnt.getURI());
    aBox.setNsPrefix("xs", XSD.getURI());
    aBox.setNsPrefix("wikt", WIKT);

    if (f == ExtractionFeature.STATISTICS) {
      aBox.setNsPrefix("qb", DataCubeOnt.getURI());
    }

    return aBox;
  }

  /**
   * returns the language of the current Entry
   *
   * @return a language code
   */
  @Override
  public String getCurrentEntryLanguage() {
    return shortSectionLanguageCode;
  }

  @Override
  public String getExtractedLanguage() {
    return shortEditionLanguageCode;
  }

  @Override

  public void closeDataset() {
    if (null != dataset) {
      try {
        dataset.commit();
      } finally {
        dataset.end();
      }
      dataset.close();
    }
  }

  @Override
  public void enableFeature(ExtractionFeature f) {
    Model box = createAndInitializeABox(longEditionLanguageCode, f);
    // fillInPrefixes(aBox, box);
    featureBoxes.put(f, box);
  }

  @Override
  public Model getFeatureBox(ExtractionFeature f) {
    return featureBoxes.get(f);
  }

  @Override
  public boolean isDisabled(ExtractionFeature f) {
    return !featureBoxes.containsKey(f);
  }

  @Override
  public void initializePageExtraction(String wiktionaryPageName) {
    currentPage = new Page(longEditionLanguageCode, wiktionaryPageName);
    currentWiktionaryPageName = wiktionaryPageName;
    currentLexieCount.resetAll();
  }

  @Override
  public void finalizePageExtraction() {
    currentWiktionaryPageName = null;
    currentPage = null;
  }

  @Override
  public void initializeLanguageSection(String language) {
    assert currentPage != null;
    String longLang = LangTools.getCode(language);
    String shortLang = LangTools.getPart1OrId(language);

    longSectionLanguageCode = longLang == null ? language : longLang;
    shortSectionLanguageCode = shortLang == null ? language : shortLang;
    lexvoSectionLanguage = tBox.createResource(LEXVO + longSectionLanguageCode);

    // assert currentLexicalEntry == null; // this is only possible when everybody finalizes lex
    // entries correctly...
    currentLexicalEntry = null;
    if (longEditionLanguageCode.equals(longSectionLanguageCode)) {
      aBox = getFeatureBox(ExtractionFeature.MAIN);
    } else {
      aBox = getFeatureBox(ExtractionFeature.FOREIGN_LANGUAGES);
    }
    initializeLanguageSection__noModel(currentPage.getName());
  }

  @Override
  public void finalizeLanguageSection() {
    finalizeLanguageSection__noModel();
    longSectionLanguageCode = null;
    shortSectionLanguageCode = null;
    lexvoSectionLanguage = null;
    aBox = getFeatureBox(ExtractionFeature.MAIN);
  }

  private void initializeLanguageSection__noModel(String wiktionaryPageName) {
    currentSense = null;
    currentSenseNumber = 0;
    currentSubSenseNumber = 0;
    translationCount.resetAll();
    reifiedNymCount.resetAll();

    // currentLexinfoPos = null;
    // currentWiktionaryPos = null;
    currentCanonicalForm = null;
    currentSharedPronunciations = new HashSet<PronunciationPair>();

    // Create a dummy lexical entry that points to the one that corresponds to a part of speech
    currentMainLexEntry = getPageResource(wiktionaryPageName, true);

    // Retain these statements to be inserted in the model when we know that the entry corresponds
    // to a proper part of speech
    heldBackStatements.add(aBox.createStatement(currentMainLexEntry, RDF.type, DBnaryOnt.Page));

    currentEncodedLexicalEntryName = null;
    currentLexEntry = null;
  }

  private void finalizeLanguageSection__noModel() {
    // Clear currentStatements. If statemenents do exist-s in it, it is because, there is no
    // extractable part of speech in the entry.
    heldBackStatements.clear();
    promoteNymProperties();
  }

  public String getEncodedPageName(String pageName, String pos, int defNumber) {
    StringBuilder nameBuilder = new StringBuilder();
    if (!this.longEditionLanguageCode.equals(this.longSectionLanguageCode)) {
      nameBuilder.append("_").append(longSectionLanguageCode).append("__");
    }
    nameBuilder.append(uriEncode(pageName, pos) + "__" + defNumber);
    return nameBuilder.toString();
  }

  public Resource getLexEntry(String languageCode, String pageName, String pos, int defNumber) {
    // FIXME this doesn't use its languageCode parameter
    return getLexEntry(getEncodedPageName(pageName, pos, defNumber), OntolexOnt.LexicalEntry
    // typeResource(pos)
    );
  }

  public Resource getLexEntry(String encodedPageName, Resource typeResource) {
    return aBox.createResource(getPrefix() + encodedPageName, typeResource);
  }

  @Override
  public String currentWiktionaryPos() {
    return currentLexicalEntry == null ? null : currentLexicalEntry.getWiktionaryPartOfSpeech();
  }

  @Override
  public Resource currentLexinfoPos() {
    return currentLexicalEntry == null ? null : currentLexicalEntry.getLexinfoPartOfSpeech();
  }


  public Resource posResource(PosAndType pat) {
    return (null == pat) ? null : pat.pos;
  }

  public Resource typeResource(PosAndType pat) {
    return (pat == null) ? OntolexOnt.LexicalEntry : pat.type;
  }

  public Resource posResource(String pos) {
    return posResource(decodePartOfSpeech(pos));
  }

  public Resource typeResource(String pos) {
    return typeResource(decodePartOfSpeech(pos));
  }

  protected PosAndType decodePartOfSpeech(String pos) {
    return posAndTypeValueMap.get(pos);
  }

  @Override
  public void initializeLexicalEntry(String pos) {
    assert longSectionLanguageCode != null && shortSectionLanguageCode != null;

    PosAndType pat = decodePartOfSpeech(pos);
    initializeLexicalEntry(pos, posResource(pat), typeResource(pat));
  }

  protected Resource initializeLexicalEntry(String pos, Resource lexinfoPOS, Resource type) {
    assert longSectionLanguageCode != null && shortSectionLanguageCode != null;
    currentLexicalEntry = currentPage.newEntry(currentPage.getName(), pos);

    currentLexicalEntry.setPartOfSpeech(lexinfoPOS);
    currentLexicalEntry.addResourceType(type);
    currentLexicalEntry.setLanguage(shortSectionLanguageCode);

    return initializeLexicalEntry__noModel(pos, lexinfoPOS, type);
  }


  public Resource initializeLexicalEntry__noModel(String originalPOS, Resource normalizedPOS,
      Resource normalizedType) {
    // DONE: create a LexicalEntry for this part of speech only and attach info to it.
    currentSense = null;
    currentSenseNumber = 0;
    currentSubSenseNumber = 0;
    // currentWiktionaryPos = originalPOS;
    // currentLexinfoPos = normalizedPOS;

    nbEntries++;

    currentEncodedLexicalEntryName = getEncodedPageName(currentPage.getName(), originalPOS,
        currentLexieCount.incr(shortSectionLanguageCode + "-" + currentWiktionaryPos()));
    currentLexEntry = getLexEntry(currentEncodedLexicalEntryName, normalizedType);

    if (!normalizedType.equals(OntolexOnt.LexicalEntry)) {
      // Add the Lexical Entry type so that users may refer to all entries using the top hierarchy
      // without any reasoner.
      aBox.add(aBox.createStatement(currentLexEntry, RDF.type, OntolexOnt.LexicalEntry));
    }

    // import other forms
    SimpleImmutableEntry<String, String> keyOtherForms =
        new SimpleImmutableEntry<String, String>(currentPage.getName(), originalPOS);
    HashSet<HashSet<PropertyObjectPair>> otherForms = heldBackOtherForms.get(keyOtherForms);

    // TODO: check that other forms point to valid entries and log faulty entries for wiktionary
    // correction.
    if (otherForms != null) {
      for (HashSet<PropertyObjectPair> otherForm : otherForms) {
        addOtherFormPropertiesToLexicalEntry(currentLexEntry, otherForm);
      }
    }

    // All translation numbers are local to a lexEntry
    translationCount.resetAll();
    reifiedNymCount.resetAll();

    currentCanonicalForm = aBox
        .createResource(getPrefix() + "__cf_" + currentEncodedLexicalEntryName, OntolexOnt.Form);

    // If a pronunciation was given before the first part of speech, it means that it is shared
    // amoung pos/etymologies
    for (PronunciationPair p : currentSharedPronunciations) {
      if (null != p.lang && p.lang.length() > 0) {
        aBox.add(currentCanonicalForm, OntolexOnt.phoneticRep, p.pron, p.lang);
      } else {
        aBox.add(currentCanonicalForm, OntolexOnt.phoneticRep, p.pron);
      }
    }

    aBox.add(currentLexEntry, OntolexOnt.canonicalForm, currentCanonicalForm);
    aBox.add(currentCanonicalForm, OntolexOnt.writtenRep, currentPage.getName(),
        getCurrentEntryLanguage());
    aBox.add(currentLexEntry, RDFS.label, currentPage.getName(), getCurrentEntryLanguage());
    aBox.add(currentLexEntry, DBnaryOnt.partOfSpeech, currentWiktionaryPos());
    if (null != currentLexinfoPos()) {
      aBox.add(currentLexEntry, LexinfoOnt.partOfSpeech, currentLexinfoPos());
    }

    aBox.add(currentLexEntry, LimeOnt.language, getCurrentEntryLanguage());
    aBox.add(currentLexEntry, DCTerms.language, lexvoSectionLanguage);

    // Register the pending statements.
    for (Statement s : heldBackStatements) {
      aBox.add(s);
    }
    heldBackStatements.clear();
    aBox.add(currentMainLexEntry, DBnaryOnt.describes, currentLexEntry);
    return currentLexEntry;
  }

  @Override
  public void registerPropertyOnCanonicalForm(Property p, RDFNode r) {
    if (null == currentLexEntry) {
      log.debug("Registering property when lex entry is null in \"{}\".", this.currentMainLexEntry);
      return; // Don't register anything if current lex entry is not known.
    }

    Resource canonicalForm = currentLexEntry.getPropertyResourceValue(OntolexOnt.canonicalForm);

    if (canonicalForm == null) {
      log.debug("Registering property when lex entry's canonicalForm is null in \"{}\".",
          this.currentMainLexEntry);
      return;
    }

    aBox.add(canonicalForm, p, r);
  }


  @Override
  public void registerPropertyOnLexicalEntry(Property p, RDFNode r) {
    if (null == currentLexEntry) {
      log.debug("Registering property on null lex entry in \"{}\".", this.currentMainLexEntry);
      return; // Don't register anything if current lex entry is not known.
    }

    aBox.add(currentLexEntry, p, r);
  }


  // TODO : Alternate spelling or lexical Variant ?
  // In Ontolex, orthographic variants are supposed to be given as a second writtenRep in the same
  // Form lexicalVariant should link 2 Lexical entries, same with varTrans lexicalRel
  @Override
  public void registerAlternateSpelling(String alt) {
    if (null == currentLexEntry) {
      log.debug("Registering Alternate Spelling when lex entry is null in \"{}\".",
          this.currentMainLexEntry);
      return; // Don't register anything if current lex entry is not known.
    }

    log.debug("Registering lexical Variant: {} for entry: {}", alt, currentEncodedLexicalEntryName);
    Resource altlemma = aBox.createResource();
    aBox.add(currentLexEntry, VarTransOnt.lexicalRel, altlemma);
    aBox.add(altlemma, OntolexOnt.writtenRep, alt, shortEditionLanguageCode);
  }

  @Override
  public void registerNewDefinition(String def) {
    this.registerNewDefinition(def, 1);
  }

  @Override
  public void registerNewDefinition(String def, int lvl) {
    if (null == currentLexEntry) {
      log.debug("Registering Word Sense when lex entry is null in \"{}\".",
          this.currentMainLexEntry);
      return; // Don't register anything if current lex entry is not known.
    }
    if (lvl > 1) {
      log.trace("registering sub sense for {}", currentEncodedLexicalEntryName);
      currentSubSenseNumber++;
    } else {
      currentSenseNumber++;
      currentSubSenseNumber = 0;
    }
    registerNewDefinition(def, computeSenseNum());
  }

  /**
   * Register a definition with the given sense number.
   *
   * @param def the definition string
   * @param senseNumber a string giving the sense number of the definition.
   */
  public void registerNewDefinition(String def, String senseNumber) {
    if (def == null || def.length() == 0) {
      return;
    }
    if (null == currentLexEntry) {
      log.debug("Registering Word Sense when lex entry is null in \"{}\".",
          this.currentMainLexEntry);
      return; // Don't register anything if current lex entry is not known.
    }

    // Create new word sense + a definition element
    currentSense = aBox.createResource(computeSenseId(senseNumber), OntolexOnt.LexicalSense);
    aBox.add(currentLexEntry, OntolexOnt.sense, currentSense);
    aBox.add(aBox.createLiteralStatement(currentSense, DBnaryOnt.senseNumber,
        aBox.createTypedLiteral(senseNumber)));
    // pos is not usefull anymore for word sense as they should be correctly linked to an entry with
    // only one pos.
    // if (currentPos != null && ! currentPos.equals("")) {
    // aBox.add(currentSense, LexinfoOnt.partOfSpeech, currentPos);
    // }

    Resource defNode = aBox.createResource();
    aBox.add(currentSense, SkosOnt.definition, defNode);
    // Keep a human readable version of the definition, removing all links annotations.
    aBox.add(defNode, RDF.value, AbstractWiktionaryExtractor.cleanUpMarkup(def, true),
        shortEditionLanguageCode);

    // TODO: Extract domain/usage field from the original definition.

  }

  private String computeSenseId(String senseNumber) {
    return getPrefix() + "__ws_" + senseNumber + "_" + currentEncodedLexicalEntryName;
  }

  protected String computeSenseNum() {
    return "" + currentSenseNumber
        + ((currentSubSenseNumber == 0) ? "" : ("." + currentSubSenseNumber));
  }

  protected Resource registerTranslationToEntity(Resource entity, String lang,
      Resource currentGlose, String usage, String word) {
    if (null == entity) {
      log.debug("Registering Translation when lex entry is null in \"{}\".",
          this.currentMainLexEntry);
      return null; // Don't register anything if current lex entry is not known.
    }
    word = word.trim();
    if (null != usage) {
      usage = usage.trim();
    }
    // Do not register empty translations
    if (word.length() == 0 && (usage == null || usage.length() == 0)) {
      return null;
    }
    // Ensure language is in its standard form.
    String tl = LangTools.getPart1OrId(lang);
    lang = LangTools.normalize(lang);
    Resource trans = aBox.createResource(computeTransId(lang, entity), DBnaryOnt.Translation);
    aBox.add(trans, DBnaryOnt.isTranslationOf, entity);
    aBox.add(createTargetLanguageProperty(trans, lang));

    if (null == tl) {
      aBox.add(trans, DBnaryOnt.writtenForm, word);
    } else {
      aBox.add(trans, DBnaryOnt.writtenForm, word, tl);
    }

    if (currentGlose != null && !currentGlose.equals("")) {
      aBox.add(trans, DBnaryOnt.gloss, currentGlose);
    }

    if (usage != null && usage.length() > 0) {
      aBox.add(trans, DBnaryOnt.usage, usage);
    }
    return trans;
  }

  @Override
  public void registerTranslation(String lang, Resource currentGlose, String usage, String word) {
    registerTranslationToEntity(currentLexEntry, lang, currentGlose, usage, word);
  }

  public String getVocableResourceName(String vocable) {
    return getPrefix() + uriEncode(vocable);
  }

  public Resource getPageResource(String vocable, boolean dontLinkWithType) {
    if (dontLinkWithType) {
      return aBox.createResource(getVocableResourceName(vocable));
    }
    return aBox.createResource(getVocableResourceName(vocable), DBnaryOnt.Page);
  }

  public Resource getPageResource(String page) {
    return getPageResource(page, false);
  }

  protected void mergePropertiesIntoResource(HashSet<PropertyObjectPair> properties, Resource res) {
    for (PropertyObjectPair p : properties) {
      if (!res.getModel().contains(res, p.getKey(), p.getValue())) {
        res.getModel().add(res, p.getKey(), p.getValue());
      }
    }
  }

  private boolean incompatibleProperties(Property p1, Property p2, boolean applyCommutativity) {
    return (p1 == LexinfoOnt.mood && p2 == LexinfoOnt.gender)
        || (applyCommutativity && incompatibleProperties(p2, p1, false));
  }

  private boolean incompatibleProperties(Property p1, Property p2) {
    return incompatibleProperties(p1, p2, true);
  }

  protected boolean isResourceCompatible(Resource r, HashSet<PropertyObjectPair> properties) {
    for (PropertyObjectPair pr : properties) {
      Property p = pr.getKey();

      Statement roStat = r.getProperty(p);

      if (roStat != null) {
        RDFNode ro = roStat.getObject();

        if (ro != null && !ro.equals(pr.getValue())) {
          return false;
        }

        StmtIterator i = r.listProperties();
        while (i.hasNext()) {
          if (incompatibleProperties(p, i.nextStatement().getPredicate())) {
            return false;
          }
        }
      }
    }
    return true;
  }

  protected void addOtherFormPropertiesToLexicalEntry(Resource lexEntry,
      HashSet<PropertyObjectPair> properties) {
    boolean foundCompatible = false;
    Model morphoBox = this.getFeatureBox(ExtractionFeature.MORPHOLOGY);

    if (null == morphoBox) {
      return;
    }

    lexEntry = lexEntry.inModel(morphoBox);

    StmtIterator otherForms = lexEntry.listProperties(OntolexOnt.otherForm);

    while (otherForms.hasNext()) {
      Resource otherForm = otherForms.next().getResource();
      if (isResourceCompatible(otherForm, properties)) {
        mergePropertiesIntoResource(properties, otherForm);
        foundCompatible = true;
        log.debug("Found a compatible property {} for {} in {}", otherForm, properties,
            currentLexEntry);
        break;
      }
    }

    if (!foundCompatible) {
      String otherFormNodeName = computeOtherFormResourceName(lexEntry, properties);
      Resource otherForm =
          morphoBox.createResource(getPrefix() + otherFormNodeName, OntolexOnt.Form);
      morphoBox.add(lexEntry, OntolexOnt.otherForm, otherForm);
      mergePropertiesIntoResource(properties, otherForm);
    }
  }

  protected String computeOtherFormResourceName(Resource lexEntry,
      HashSet<PropertyObjectPair> properties) {
    String lexEntryLocalName = lexEntry.getLocalName();
    String compactProperties =
        DatatypeConverter.printBase64Binary(BigInteger.valueOf(properties.hashCode()).toByteArray())
            .replaceAll("[/=\\+]", "-");

    return "__wf_" + compactProperties + "_" + lexEntryLocalName;
  }

  public void registerInflection(String languageCode, String pos, String inflection,
      String canonicalForm, int defNumber, HashSet<PropertyObjectPair> props,
      HashSet<PronunciationPair> pronunciations) {

    if (pronunciations != null) {
      for (PronunciationPair pronunciation : pronunciations) {
        // DONE: deprecating lexinfo:pronunciation in favour of ontolex:phoneticRep, remove
        // the former after a certain period.
        // props.add(PropertyObjectPair.get(LexinfoOnt.pronunciation,
        // aBox.createLiteral(pronunciation.pron, pronunciation.lang)));
        props.add(PropertyObjectPair.get(OntolexOnt.phoneticRep,
            aBox.createLiteral(pronunciation.pron, pronunciation.lang)));
      }
    }

    registerInflection(languageCode, pos, inflection, canonicalForm, defNumber, props);
  }

  /**
   * @param languageCode the language code of the inflection
   * @param pos the part of speech of the inflected form
   * @param inflection inflected form
   * @param canonicalForm canonical form
   * @param defNumber definition number of the word sense associated to the form
   * @param props morpho syntactic properties to be registered with the inflected form
   */
  public void registerInflection(String languageCode, String pos, String inflection,
      String canonicalForm, int defNumber, HashSet<PropertyObjectPair> props) {

    Resource posResource = posResource(pos);

    PropertyObjectPair p = PropertyObjectPair.get(OntolexOnt.writtenRep,
        aBox.createLiteral(inflection, getCurrentEntryLanguage()));

    props.add(p);

    if (defNumber == 0) {
      // the definition number was not specified, we have to register this
      // inflection for each entry.

      // First, we store the other form for all the existing entries
      Resource vocable = getPageResource(canonicalForm, true);

      StmtIterator entries = vocable.listProperties(DBnaryOnt.describes);

      ArrayList<Resource> addTo = new ArrayList<>();
      while (entries.hasNext()) {
        Resource lexEntry = entries.next().getResource();
        if (aBox.contains(lexEntry, LexinfoOnt.partOfSpeech, posResource)) {
          addTo.add(lexEntry);
        }
      }
      addTo.forEach(entry -> addOtherFormPropertiesToLexicalEntry(entry, props));

      // Second, we store the other form for future possible matching entries
      SimpleImmutableEntry<String, String> key =
          new SimpleImmutableEntry<String, String>(canonicalForm, pos);

      HashSet<HashSet<PropertyObjectPair>> otherForms =
          heldBackOtherForms.computeIfAbsent(key, k -> new HashSet<HashSet<PropertyObjectPair>>());

      otherForms.add(props);
    } else {
      // the definition number was specified, this makes registration easy.
      addOtherFormPropertiesToLexicalEntry(getLexEntry(languageCode, canonicalForm, pos, defNumber),
          props);
    }
  }

  @Override
  public void registerInflection(InflectionData key, Set<String> value) {
    HashSet<PropertyObjectPair> props = key.toPropertyObjectMap();
    for (String form : value) {
      PropertyObjectPair p = PropertyObjectPair.get(OntolexOnt.writtenRep,
          aBox.createLiteral(form, getCurrentEntryLanguage()));
      props.add(p);
    }
    addOtherFormPropertiesToLexicalEntry(currentLexEntry, props);
  }

  private Statement createTargetLanguageProperty(Resource trans, String lang) {
    lang = lang.trim();
    if (isAnISO639_3Code(lang)) {
      return aBox.createStatement(trans, DBnaryOnt.targetLanguage, getLexvoLanguageResource(lang));
    } else {
      return aBox.createStatement(trans, DBnaryOnt.targetLanguageCode, lang);
    }
  }

  private final static Pattern iso3letters = Pattern.compile("\\w{3}");

  private boolean isAnISO639_3Code(String lang) {
    // TODO For the moment, only check if the code is a 3 letter code...
    return iso3letters.matcher(lang).matches();
  }

  private String computeTransId(String lang, Resource entity) {
    lang = uriEncode(lang);
    return getPrefix() + "__tr_" + lang + "_" + translationCount.incr(lang) + "_"
        + entity.getURI().substring(getPrefix().length());
  }

  private Resource getLexvoLanguageResource(String lang) {
    Resource res = languages.computeIfAbsent(lang, l -> tBox.createResource(LEXVO + l));
    return res;
  }

  public void registerNymRelationToEntity(String target, String synRelation, Resource entity) {
    if (null == entity) {
      log.debug("Registering Lexical Relation when lex entry is null in \"{}\".",
          this.currentMainLexEntry);
      return; // Don't register anything if current lex entry is not known.
    }
    // Some links point to Annex pages or Images, just ignore these.
    int colon = target.indexOf(':');
    if (colon != -1) {
      return;
    }
    int hash = target.indexOf('#');
    if (hash != -1) {
      // The target contains an intra page href. Remove it from the target uri and keep it in the
      // relation.
      target = target.substring(0, hash);
      // TODO: keep additional intra-page href
      // aBox.add(nym, isAnnotatedBy, target.substring(hash));
    }

    Property nymProperty = NymRelation.of(synRelation).getProperty();
    // Property nymProperty = nymPropertyMap.get(synRelation);

    Resource targetResource = getPageResource(target);

    aBox.add(entity, nymProperty, targetResource);
  }

  public void registerNymRelationToEntity(String target, String synRelation, Resource entity,
      Resource gloss, String usage) {
    if (null == entity) {
      log.debug("Registering Lexical Relation when lex entry is null in \"{}\".",
          this.currentMainLexEntry);
      return; // Don't register anything if current lex entry is not known.
    }
    // Some links point to Annex pages or Images, just ignore these.
    int colon = target.indexOf(':');
    if (colon != -1) {
      return;
    }
    int hash = target.indexOf('#');
    if (hash != -1) {
      // The target contains an intra page href. Remove it from the target uri and keep it in the
      // relation.
      target = target.substring(0, hash);
      // TODO: keep additional intra-page href
      // aBox.add(nym, isAnnotatedBy, target.substring(hash));
    }
    Property nymProperty = NymRelation.of(synRelation).getProperty();
    // Property nymProperty = nymPropertyMap.get(synRelation);

    Resource targetResource = getPageResource(target);

    Statement nymR = aBox.createStatement(entity, nymProperty, targetResource);
    aBox.add(nymR);

    if (gloss == null && usage == null) {
      return;
    }

    ReifiedStatement rnymR = nymR.createReifiedStatement(computeNymId(synRelation));
    if (gloss != null) {
      rnymR.addProperty(DBnaryOnt.gloss, gloss);
    }
    if (usage != null) {
      rnymR.addProperty(DBnaryOnt.usage, usage);
    }


  }


  @Override
  public void registerNymRelation(String target, String synRelation) {
    registerNymRelationToEntity(target, synRelation, currentLexEntry);
  }

  @Override
  public Resource createGlossResource(StructuredGloss gloss) {
    return createGlossResource(gloss, -1);
  }

  @Override
  public Resource createGlossResource(StructuredGloss gloss, int rank) {
    if (gloss == null || ((gloss.getGloss() == null || gloss.getGloss().length() == 0)
        && (gloss.getSenseNumber() == null || gloss.getSenseNumber().length() == 0))) {
      return null;
    }

    Resource glossResource = aBox.createResource(getGlossResourceName(gloss), DBnaryOnt.Gloss);
    if (null != gloss.getGloss() && gloss.getGloss().trim().length() > 0) {
      aBox.add(aBox.createStatement(glossResource, RDF.value, gloss.getGloss(),
          shortEditionLanguageCode));
    }
    if (gloss.getSenseNumber() != null) {
      aBox.add(aBox.createStatement(glossResource, DBnaryOnt.senseNumber, gloss.getSenseNumber()));
    }
    if (rank > 0) {
      aBox.add(aBox.createLiteralStatement(glossResource, DBnaryOnt.rank, rank));
    }
    return glossResource;
  }

  protected String getGlossResourceName(StructuredGloss gloss) {
    String key = gloss.getGloss() + gloss.getSenseNumber();
    key = DatatypeConverter.printBase64Binary(BigInteger.valueOf(key.hashCode()).toByteArray())
        .replaceAll("[/=\\+]", "-");
    return getPrefix() + "__" + shortEditionLanguageCode + "_gloss_" + key + "_"
        + currentEncodedLexicalEntryName;
  }

  @Override
  public void registerNymRelation(String target, String synRelation, Resource gloss) {
    registerNymRelation(target, synRelation, gloss, null);
  }

  @Override
  public void registerNymRelation(String target, String synRelation, Resource gloss, String usage) {
    registerNymRelationToEntity(target, synRelation, currentLexEntry, gloss, usage);
  }

  protected String computeNymId(String nym) {
    return computeNymId(nym, currentEncodedLexicalEntryName);
  }

  protected String computeNymId(String nym, String pagename) {
    return getPrefix() + "__" + nym + "_" + reifiedNymCount.incr(nym) + "_" + pagename;
  }

  @Override
  public void registerNymRelationOnCurrentSense(String target, String synRelation) {
    if (null == currentSense) {
      log.debug("Registering Lexical Relation when current sense is null in \"{}\".",
          this.currentMainLexEntry);
      registerNymRelation(target, synRelation);
      return; // Don't register anything if current lex entry is not known.
    }
    // Some links point to Annex pages or Images, just ignore these.
    int colon = target.indexOf(':');
    if (colon != -1) {
      return;
    }
    int hash = target.indexOf('#');
    if (hash != -1) {
      // The target contains an intra page href. Remove it from the target uri and keep it in the
      // relation.
      target = target.substring(0, hash);
      // TODO: keep additional intra-page href
      // aBox.add(nym, isAnnotatedBy, target.substring(hash));
    }

    Property nymProperty = NymRelation.of(synRelation).getProperty();
    // Property nymProperty = nymPropertyMap.get(synRelation);

    Resource targetResource = getPageResource(target);

    aBox.add(currentSense, nymProperty, targetResource);
  }

  @Override
  public void registerPronunciation(String pron, String lang) {
    if (null == currentCanonicalForm) {
      // if pronunciation is provided before the first canonical form
      // assume that this pronunciation is shared by all and put it
      // into SharedPronunciations
      currentSharedPronunciations.add(new PronunciationPair(pron, lang));
    } else {
      registerPronunciation(currentCanonicalForm, pron, lang);
    }
  }

  // TODO: decide whether to use lexinfo:pronunciation or ontolex:phoneticRep
  protected void registerPronunciation(Resource writtenRepresentation, String pron, String lang) {
    if (null != lang && lang.length() > 0) {
      aBox.add(writtenRepresentation, OntolexOnt.phoneticRep, pron, lang);
    } else {
      aBox.add(writtenRepresentation, OntolexOnt.phoneticRep, pron);
    }
  }

  private void promoteNymProperties() {
    StmtIterator entries = currentMainLexEntry.listProperties(DBnaryOnt.describes);
    HashSet<Statement> toBeRemoved = new HashSet<Statement>();
    ArrayList<Statement> toBeAdded = new ArrayList<Statement>();
    while (entries.hasNext()) {
      Resource lu = entries.next().getResource();
      List<Statement> senses = lu.listProperties(OntolexOnt.sense).toList();
      if (senses.size() == 1) {
        Resource s = senses.get(0).getResource();
        HashSet<Property> alreadyProcessedNyms = new HashSet<Property>();
        for (NymRelation nymR : NymRelation.values()) {
          Property nymProp = nymR.getProperty();
          if (alreadyProcessedNyms.contains(nymProp)) {
            continue;
          }
          alreadyProcessedNyms.add(nymProp);
          StmtIterator nyms = lu.listProperties(nymProp);
          while (nyms.hasNext()) {
            Statement nymRel = nyms.next();
            toBeAdded.add(aBox.createStatement(s, nymProp, nymRel.getObject()));
            toBeRemoved.add(nymRel);
          }
        }
      }
    }
    aBox.add(toBeAdded);
    for (Statement s : toBeRemoved) {
      s.remove();
    }
  }

  @Override
  public void dump(ExtractionFeature f, OutputStream out, String format) {
    Model box = this.getFeatureBox(f);
    if (null != box) {
      box.write(out, format);
    }
  }

  @Override
  public void dumpAllAsHDT(OutputStream ostream) {

  }

  @Override
  public int nbEntries() {
    return nbEntries;
  }

  @Override
  public String currentLexEntry() {
    // TODO Auto-generated method stub
    return currentWiktionaryPageName;
  }

  public String getPrefix() {
    return NS;
  }

  @Override
  public Resource registerExample(String ex, Map<Property, String> context) {
    if (null == currentSense) {
      log.debug("Registering example when lex sense is null in \"{}\".", this.currentMainLexEntry);
      return null; // Don't register anything if current lex entry is not known.
    }

    // Create new example element
    Resource example = aBox.createResource();
    aBox.add(aBox.createStatement(example, RDF.value, ex, getCurrentEntryLanguage()));
    if (null != context) {
      for (Map.Entry<Property, String> c : context.entrySet()) {
        aBox.add(aBox.createStatement(example, c.getKey(), c.getValue(), shortEditionLanguageCode));
      }
    }
    aBox.add(aBox.createStatement(currentSense, SkosOnt.example, example));
    return example;
  }

  @Override
  public void computeStatistics(String dumpVersion) {
    if (isDisabled(ExtractionFeature.STATISTICS)) {
      return;
    }

    Model statsBox = getFeatureBox(ExtractionFeature.STATISTICS);

    {
      long ct = countResourcesOfType(DBnaryOnt.Translation, aBox);
      long ce = countResourcesOfType(OntolexOnt.LexicalEntry, aBox);
      long cp = countResourcesOfType(DBnaryOnt.Page, aBox);
      long cs = countResourcesOfType(OntolexOnt.LexicalSense, aBox);

      createGeneralStatisticsObservation(statsBox, dumpVersion, getPrefix(),
          longEditionLanguageCode, ct, cp, ce, cs);
    }

    for (NymRelation nym : NymRelation.values()) {
      long cr = countRelations(nym.getProperty(), aBox);
      createNymRelationObservation(statsBox, dumpVersion, getPrefix(), longEditionLanguageCode, nym,
          cr);
    }

    Map<String, Long> counts = Statistics.translationCounts(aBox);
    counts.forEach((l, c) -> OntolexBasedRDFDataHandler.createTranslationObservation(statsBox,
        dumpVersion, getPrefix(), longEditionLanguageCode, l, c));
  }

  public static void createGeneralStatisticsObservation(Model statsBox, String dumpVersion,
      String prefix, String lang, long translationCount, long pageCount, long entryCount,
      long senseCount) {
    String lg2 = LangTools.getPart1OrId(lang);

    Resource classesObs =
        statsBox.createResource(prefix + "___mainClassesObs__" + lang + "__" + dumpVersion);
    statsBox.add(statsBox.createStatement(classesObs, RDF.type, DataCubeOnt.Observation));
    statsBox.add(
        statsBox.createStatement(classesObs, DataCubeOnt.dataSet, DBnaryOnt.dbnaryStatisticsCube));
    statsBox.add(statsBox.createStatement(classesObs, DBnaryOnt.wiktionaryDumpVersion,
        statsBox.createTypedLiteral(dumpVersion)));
    statsBox.add(statsBox.createStatement(classesObs, DBnaryOnt.observationLanguage, lg2));

    statsBox.add(statsBox.createStatement(classesObs, DBnaryOnt.translationsCount,
        statsBox.createTypedLiteral(translationCount)));
    statsBox.add(statsBox.createStatement(classesObs, DBnaryOnt.pageCount,
        statsBox.createTypedLiteral(pageCount)));
    statsBox.add(statsBox.createStatement(classesObs, DBnaryOnt.lexicalEntryCount,
        statsBox.createTypedLiteral(entryCount)));
    statsBox.add(statsBox.createStatement(classesObs, DBnaryOnt.lexicalSenseCount,
        statsBox.createTypedLiteral(senseCount)));
  }

  public static void createNymRelationObservation(Model box, String dumpVersion, String prefix,
      String lang, NymRelation nym, long cr) {
    String lg2 = LangTools.getPart1OrId(lang);

    Resource nymObs = box.createResource(
        prefix + "___nymObs__" + lang + "__" + nym.name().toLowerCase() + "__" + dumpVersion);
    box.add(box.createStatement(nymObs, RDF.type, DataCubeOnt.Observation));
    box.add(box.createStatement(nymObs, DataCubeOnt.dataSet, DBnaryOnt.dbnaryNymRelationsCube));
    box.add(box.createStatement(nymObs, DBnaryOnt.wiktionaryDumpVersion,
        box.createTypedLiteral(dumpVersion)));
    box.add(box.createStatement(nymObs, DBnaryOnt.observationLanguage, lg2));
    box.add(box.createStatement(nymObs, DBnaryOnt.nymRelation, nym.getProperty()));

    box.add(box.createStatement(nymObs, DBnaryOnt.count, box.createTypedLiteral(cr)));
  }

  public static void createTranslationObservation(Model statsBox, String dumpVersion, String prefix,
      String sourceLanguage, String targetLanguage, long c) {
    // languages should be kept in 2 letter code if available.
    String slg2 = LangTools.getPart1OrId(sourceLanguage);
    String tlg2 = LangTools.getPart1OrId(targetLanguage);
    tlg2 = (null != tlg2) ? tlg2 : targetLanguage;

    Resource transObs = statsBox.createResource(
        prefix + "___transObs__" + sourceLanguage + "__" + targetLanguage + "__" + dumpVersion);
    statsBox.add(statsBox.createStatement(transObs, RDF.type, DataCubeOnt.Observation));
    statsBox.add(
        statsBox.createStatement(transObs, DataCubeOnt.dataSet, DBnaryOnt.dbnaryTranslationsCube));
    statsBox.add(statsBox.createStatement(transObs, DBnaryOnt.wiktionaryDumpVersion,
        statsBox.createTypedLiteral(dumpVersion)));
    statsBox.add(statsBox.createStatement(transObs, DBnaryOnt.observationLanguage, slg2));
    statsBox.add(statsBox.createStatement(transObs, LimeOnt.language, tlg2));

    statsBox
        .add(statsBox.createStatement(transObs, DBnaryOnt.count, statsBox.createTypedLiteral(c)));
  }

  /////// METADATA /////////
  @Override
  public void populateMetadata(String dumpFilename, String extractorVersion) {
    if (isDisabled(ExtractionFeature.LIME)) {
      return;
    }
    Model limeBox = this.getFeatureBox(ExtractionFeature.LIME);
    Resource creator = limeBox.createResource("http://serasset.bitbucket.io/");
    Resource lexicon = limeBox.createResource(
        getPrefix() + "___" + shortEditionLanguageCode + "_dbnary_dataset", LimeOnt.Lexicon);
    limeBox.add(limeBox.createStatement(lexicon, DCTerms.title,
        ISO639_3.sharedInstance.getLanguageNameInEnglish(shortEditionLanguageCode)
            + " DBnary Dataset",
        "en"));
    limeBox.add(limeBox.createStatement(lexicon, DCTerms.title, "Dataset DBnary "
        + ISO639_3.sharedInstance.getLanguageNameInFrench(shortEditionLanguageCode), "fr"));
    limeBox.add(limeBox.createStatement(lexicon, DCTerms.description,
        "This lexicon is extracted from the original wiktionary data that can be found"
            + " in http://" + shortEditionLanguageCode
            + ".wiktionary.org/ by the DBnary Extractor.",
        "en"));
    limeBox.add(limeBox.createStatement(lexicon, DCTerms.description,
        "Cet ensemble de données est extrait du wiktionnaire original disponible" + " à http://"
            + shortEditionLanguageCode
            + ".wiktionary.org/ par le programme d'extraction de DBnary.",
        "fr"));
    limeBox.add(limeBox.createStatement(lexicon, DCTerms.creator, creator));
    limeBox.add(limeBox.createLiteralStatement(lexicon, DCTerms.created,
        limeBox.createTypedLiteral(GregorianCalendar.getInstance())));
    limeBox.add(limeBox.createStatement(lexicon, DCTerms.source,
        "http://" + shortEditionLanguageCode + ".wiktionary.org/"));

    limeBox.add(
        limeBox.createStatement(lexicon, FOAF.homepage, "http://kaiko.getalp.org/about-dbnary"));
    limeBox.add(limeBox.createStatement(lexicon, FOAF.page,
        "http://kaiko.getalp.org/static/ontolex/" + shortEditionLanguageCode));

    limeBox.add(limeBox.createStatement(lexicon, LimeOnt.language, shortEditionLanguageCode));
    limeBox.add(limeBox.createStatement(lexicon, DCTerms.language, lexvoExtractedLanguage));
    limeBox.add(limeBox.createLiteralStatement(lexicon, LimeOnt.lexicalEntries, nbEntries()));
    limeBox.add(limeBox.createStatement(lexicon, LimeOnt.linguisticCatalog, LexinfoOnt.getURI()));
    limeBox.add(limeBox.createStatement(lexicon, LimeOnt.linguisticCatalog, OliaOnt.getURI()));
    limeBox.add(limeBox.createStatement(lexicon, LimeOnt.linguisticCatalog, LEXVO));

    // TODO: Add extractor version for the current dump
    limeBox.add(limeBox.createStatement(lexicon, DBnaryOnt.wiktionaryDumpVersion, dumpFilename));

    // TODO: Add VOID description : see https://www.w3.org/TR/void/#access
    // :DBpedia a void:Dataset;
    // void:sparqlEndpoint <http://dbpedia.org/sparql>;
    // :NYTimes a void:Dataset;
    // void:dataDump <http://data.nytimes.com/people.rdf>;
    // void:dataDump <http://data.nytimes.com/organizations.rdf>;
    // void:dataDump <http://data.nytimes.com/locations.rdf>;
    // void:dataDump <http://data.nytimes.com/descriptors.rdf>;
  }

  @Override
  public void buildDatacubeObservations(String l, TranslationGlossesStat tgs,
      EvaluationStats.Stat es, String dumpFileVersion) {
    if (isDisabled(ExtractionFeature.ENHANCEMENT) || isDisabled(ExtractionFeature.STATISTICS)) {
      return;
    }
    Model statsBox = this.getFeatureBox(ExtractionFeature.STATISTICS);

    {
      Resource glossObs = statsBox.createResource(getPrefix() + "___glossObs__"
          + shortEditionLanguageCode + "__" + date() + "_" + dumpFileVersion);
      statsBox.add(statsBox.createStatement(glossObs, RDF.type, DataCubeOnt.Observation));
      statsBox.add(statsBox.createStatement(glossObs, DataCubeOnt.dataSet,
          DBnaryOnt.translationGlossesCube));
      statsBox.add(statsBox.createStatement(glossObs, DBnaryOnt.wiktionaryDumpVersion,
          statsBox.createTypedLiteral(dumpFileVersion)));
      statsBox.add(statsBox.createStatement(glossObs, DBnaryOnt.observationLanguage, l));

      statsBox.add(statsBox.createStatement(glossObs, DBnaryOnt.translationsWithNoGloss,
          statsBox.createTypedLiteral(tgs.getTranslationsWithoutGlosses())));
      statsBox.add(statsBox.createStatement(glossObs, DBnaryOnt.translationsWithSenseNumber,
          statsBox.createTypedLiteral(tgs.getNbGlossesWithSenseNumberOnly())));
      statsBox.add(statsBox.createStatement(glossObs, DBnaryOnt.translationsWithTextualGloss,
          statsBox.createTypedLiteral(tgs.getNbGlossesWithTextOnly())));
      statsBox.add(
          statsBox.createStatement(glossObs, DBnaryOnt.translationsWithSenseNumberAndTextualGloss,
              statsBox.createTypedLiteral(tgs.getNbGlossesWithSensNumberAndText())));
    }

    {
      Resource enhObsRandom = statsBox.createResource(getPrefix() + "___enhObsRandom__"
          + shortEditionLanguageCode + "__" + date() + "_" + dumpFileVersion);
      statsBox.add(statsBox.createStatement(enhObsRandom, RDF.type, DataCubeOnt.Observation));
      statsBox.add(statsBox.createStatement(enhObsRandom, DataCubeOnt.dataSet,
          DBnaryOnt.enhancementConfidenceDataCube));
      statsBox.add(statsBox.createStatement(enhObsRandom, DBnaryOnt.wiktionaryDumpVersion,
          statsBox.createTypedLiteral(dumpFileVersion)));
      statsBox.add(statsBox.createStatement(enhObsRandom, DBnaryOnt.observationLanguage, l));
      statsBox.add(statsBox.createStatement(enhObsRandom, DBnaryOnt.enhancementMethod, "random"));
      statsBox.add(statsBox.createStatement(enhObsRandom, DBnaryOnt.f1Measure,
          statsBox.createTypedLiteral(es.getRandomF1Score())));
      statsBox.add(statsBox.createStatement(enhObsRandom, DBnaryOnt.precisionMeasure,
          statsBox.createTypedLiteral(es.getRandomPrecision())));
      statsBox.add(statsBox.createStatement(enhObsRandom, DBnaryOnt.recallMeasure,
          statsBox.createTypedLiteral(es.getRandomRecall())));
    }

    {
      Resource enhObs = statsBox.createResource(getPrefix() + "___enhObs__"
          + shortEditionLanguageCode + "__" + date() + "_" + dumpFileVersion);
      statsBox.add(statsBox.createStatement(enhObs, RDF.type, DataCubeOnt.Observation));
      statsBox.add(statsBox.createStatement(enhObs, DataCubeOnt.dataSet,
          DBnaryOnt.enhancementConfidenceDataCube));
      statsBox.add(statsBox.createStatement(enhObs, DBnaryOnt.wiktionaryDumpVersion,
          statsBox.createTypedLiteral(dumpFileVersion)));
      statsBox.add(statsBox.createStatement(enhObs, DBnaryOnt.observationLanguage, l));
      statsBox.add(statsBox.createStatement(enhObs, DBnaryOnt.enhancementMethod, "dbnary_tversky"));
      statsBox.add(statsBox.createStatement(enhObs, DBnaryOnt.f1Measure,
          statsBox.createTypedLiteral(es.getF1Score())));
      statsBox.add(statsBox.createStatement(enhObs, DBnaryOnt.precisionMeasure,
          statsBox.createTypedLiteral(es.getPrecision())));
      statsBox.add(statsBox.createStatement(enhObs, DBnaryOnt.recallMeasure,
          statsBox.createTypedLiteral(es.getRecall())));
    }

  }

  private static String date() {
    LocalDateTime d = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd'T'HH_mm_ss_SSS");
    return formatter.format(d);
  }
}

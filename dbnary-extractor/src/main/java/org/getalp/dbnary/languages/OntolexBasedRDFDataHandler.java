package org.getalp.dbnary.languages;

import static org.getalp.dbnary.stats.Statistics.countRelations;
import static org.getalp.dbnary.stats.Statistics.countResourcesOfType;

import java.io.OutputStream;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import jakarta.xml.bind.DatatypeConverter;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ReifiedStatement;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.tdb2.TDB2Factory;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.getalp.LangTools;
import org.getalp.dbnary.DBnaryEtymologyOnt;
import org.getalp.dbnary.DBnaryOnt;
import org.getalp.dbnary.DataCubeOnt;
import org.getalp.dbnary.DecompOnt;
import org.getalp.dbnary.ExtractionFeature;
import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.LimeOnt;
import org.getalp.dbnary.OliaOnt;
import org.getalp.dbnary.OntolexOnt;
import org.getalp.dbnary.PronunciationPair;
import org.getalp.dbnary.PropertyObjectPair;
import org.getalp.dbnary.SkosOnt;
import org.getalp.dbnary.StructuredGloss;
import org.getalp.dbnary.SynSemOnt;
import org.getalp.dbnary.VarTransOnt;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.commons.HierarchicalSenseNumber;
import org.getalp.dbnary.enhancer.evaluation.EvaluationStats;
import org.getalp.dbnary.enhancer.evaluation.TranslationGlossesStat;
import org.getalp.dbnary.hdt.Models2HDT;
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

  /**
   * @deprecated use getFeatureBox()
   */
  @Deprecated
  protected Model aBox;

  private final Map<ExtractionFeature, Model> endolexFeatureBoxes;
  private final Map<ExtractionFeature, Model> exolexFeatureBoxes;

  // States used for processing
  @Deprecated
  protected Resource currentLexEntry;
  // @Deprecated protected Resource currentLexinfoPos;
  // @Deprecated protected String currentWiktionaryPos;

  protected LexicalEntry currentLexicalEntry;


  protected Resource currentSense;
  protected HierarchicalSenseNumber currentSenseNumber;
  protected CounterSet translationCount = new CounterSet();
  protected CounterSet reifiedNymCount = new CounterSet();

  // Language identification (edition and current section)
  protected final String shortEditionLanguageCode;
  protected final String longEditionLanguageCode;
  protected String shortSectionLanguageCode;
  protected String longSectionLanguageCode;


  protected Resource lexvoExtractedLanguage;
  protected Resource lexvoSectionLanguage;

  private final Set<Statement> heldBackStatements = new HashSet<>();

  protected int nbEntries = 0;
  private final String NS;
  protected String WIKT;
  protected String currentEncodedLexicalEntryName;

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
      new HashMap<>();

  // protected static HashMap<String, Property> nymPropertyMap = new HashMap<String, Property>();
  protected static HashMap<String, PosAndType> posAndTypeValueMap = new HashMap<>();

  // Map of the String to lexvo language entity
  private HashMap<String, Resource> languages = new HashMap<>();

  protected final AbstractGlossFilter glossFilter;

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

    endolexFeatureBoxes = new HashMap<>();
    endolexFeatureBoxes.put(ExtractionFeature.MAIN, aBox);
    exolexFeatureBoxes = new HashMap<>();

    glossFilter = WiktionaryGlossFilterFactory.getGlossFilter(longEditionLanguageCode);
  }

  private Model createAndInitializeABox(String lang) {
    return this.createAndInitializeABox(lang, ExtractionFeature.MAIN, "");
  }

  private Model createAndInitializeABox(String lang, ExtractionFeature f, String graphSuffix) {
    // Create box
    Model box;

    if (null != dataset) {
      box = dataset.getNamedModel("NS" + f.name().toLowerCase() + "/" + graphSuffix);
    } else {
      box = ModelFactory.createDefaultModel();
    }
    box.setNsPrefix(lang, NS);
    box.setNsPrefix("dbnary", DBnaryOnt.getURI());
    box.setNsPrefix("dbstats", DBNARY_NS_PREFIX + "/statistics/");
    box.setNsPrefix("dbetym", DBnaryEtymologyOnt.getURI());
    box.setNsPrefix("lexinfo", LexinfoOnt.getURI());
    box.setNsPrefix("rdfs", RDFS.getURI());
    box.setNsPrefix("dcterms", DCTerms.getURI());
    box.setNsPrefix("lexvo", LEXVO);
    box.setNsPrefix("rdf", RDF.getURI());
    box.setNsPrefix("olia", OliaOnt.getURI());
    box.setNsPrefix("ontolex", OntolexOnt.getURI());
    box.setNsPrefix("vartrans", VarTransOnt.getURI());
    box.setNsPrefix("synsem", SynSemOnt.getURI());
    box.setNsPrefix("lime", LimeOnt.getURI());
    box.setNsPrefix("decomp", DecompOnt.getURI());
    box.setNsPrefix("skos", SkosOnt.getURI());
    box.setNsPrefix("xs", XSD.getURI());
    box.setNsPrefix("wikt", WIKT);

    if (f == ExtractionFeature.STATISTICS) {
      box.setNsPrefix("qb", DataCubeOnt.getURI());
    }

    return box;
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
  public void enableEndolexFeatures(ExtractionFeature f) {
    Model box = createAndInitializeABox(longEditionLanguageCode, f, "");
    // fillInPrefixes(aBox, box);
    endolexFeatureBoxes.put(f, box);
  }

  @Override
  public Model getFeatureBox(ExtractionFeature f) {
    Map<ExtractionFeature, Model> features;
    if (null == getCurrentEntryLanguage()) {
      features = endolexFeatureBoxes;
    } else if (getExtractedLanguage().equals(getCurrentEntryLanguage())) {
      features = endolexFeatureBoxes;
    } else {
      features = exolexFeatureBoxes;
    }
    return features.get(f);
  }

  @Override
  public Model getEndolexFeatureBox(ExtractionFeature f) {
    return endolexFeatureBoxes.get(f);
  }

  @Override
  public Model getExolexFeatureBox(ExtractionFeature f) {
    return exolexFeatureBoxes.get(f);
  }

  @Override
  public void enableExolexFeatures(ExtractionFeature f) {
    Model box = createAndInitializeABox(longEditionLanguageCode, f, "exolex");
    exolexFeatureBoxes.put(f, box);
  }


  @Override
  public boolean isDisabled(ExtractionFeature f) {
    Model features = getFeatureBox(f);
    return null == features;
  }

  @Override
  public void initializePageExtraction(String wiktionaryPageName) {
    currentPage = new Page(longEditionLanguageCode, wiktionaryPageName);
    currentLexieCount.resetAll();
  }

  @Override
  public void finalizePageExtraction() {
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

    assert currentLexicalEntry == null;

    if (longEditionLanguageCode.equals(longSectionLanguageCode)) {
      aBox = getEndolexFeatureBox(ExtractionFeature.MAIN);
    } else {
      aBox = getExolexFeatureBox(ExtractionFeature.MAIN);
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
    currentSenseNumber = new HierarchicalSenseNumber();
    translationCount.resetAll();
    reifiedNymCount.resetAll();

    // currentLexinfoPos = null;
    // currentWiktionaryPos = null;
    currentCanonicalForm = null;
    currentSharedPronunciations = new HashSet<>();

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
    nameBuilder.append(uriEncode(pageName, pos)).append("__").append(defNumber);
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
    currentLexicalEntry = currentPage.newEntry(pos);

    currentLexicalEntry.setPartOfSpeech(lexinfoPOS);
    currentLexicalEntry.addResourceType(type);
    currentLexicalEntry.setLanguage(shortSectionLanguageCode);

    return initializeLexicalEntry__noModel(pos, lexinfoPOS, type);
  }


  public Resource initializeLexicalEntry__noModel(String originalPOS, Resource normalizedPOS,
      Resource normalizedType) {
    // DONE: create a LexicalEntry for this part of speech only and attach info to it.
    currentSense = null;
    currentSenseNumber = new HierarchicalSenseNumber();
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
        new SimpleImmutableEntry<>(currentPage.getName(), originalPOS);
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
    String pos = currentWiktionaryPos();
    if (null != pos && pos.length() != 0)
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
    // TODO: keep alternate spelling for remaining entries (in the same etymology or for the page)
    if (null == currentLexEntry) {
      log.debug("Registering Alternate Spelling when lex entry is null in \"{}\".",
          this.currentMainLexEntry);
      return; // Don't register anything if current lex entry is not known.
    }

    log.debug("Registering lexical Variant: {} for entry: {}", alt, currentEncodedLexicalEntryName);
    Resource altLexEntry = aBox.createResource(OntolexOnt.LexicalEntry);
    aBox.add(currentLexEntry, VarTransOnt.lexicalRel, altLexEntry);
    Resource altForm = aBox.createResource();
    aBox.add(altLexEntry, OntolexOnt.canonicalForm, altForm);
    aBox.add(altForm, OntolexOnt.writtenRep, alt, shortSectionLanguageCode);
  }

  @Override
  public Resource registerNewDefinition(String def) {
    return this.registerNewDefinition(def, 1);
  }

  @Override
  public Resource registerNewDefinition(String def, int lvl) {
    if (null == currentLexEntry) {
      log.debug("Registering Word Sense when lex entry is null in \"{}\".",
          this.currentMainLexEntry);
      return null; // Don't register anything if current lex entry is not known.
    }
    currentSenseNumber.increment(lvl);
    return registerNewDefinition(def, computeSenseNum());
  }

  /**
   * Register a definition with the given sense number.
   *
   * @param def the definition string
   * @param senseNumber a string giving the sense number of the definition.
   * @return
   */
  public Resource registerNewDefinition(String def, String senseNumber) {
    if (def == null || def.length() == 0) {
      return null;
    }
    if (null == currentLexEntry) {
      log.debug("Registering Word Sense when lex entry is null in \"{}\".",
          this.currentMainLexEntry);
      return null;
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
    return currentSense;
  }

  private String computeSenseId(String senseNumber) {
    return getPrefix() + "__ws_" + senseNumber + "_" + currentEncodedLexicalEntryName;
  }

  protected String computeSenseNum() {
    return currentSenseNumber.toString();
  }

  final static Pattern CONTROL_CHAR = Pattern.compile("\\p{Cntrl}");

  protected Resource registerTranslationToEntity(Resource entity, String lang,
      Resource currentGloss, String usage, String word) {
    if (null == entity) {
      log.debug("Registering Translation when lex entry is null in \"{}\".",
          this.currentMainLexEntry);
      return null; // Don't register anything if current lex entry is not known.
    }
    word = word.trim();
    word = CONTROL_CHAR.matcher(word).replaceAll("");

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

    if (currentGloss != null) {
      aBox.add(trans, DBnaryOnt.gloss, currentGloss);
    }

    if (usage != null && usage.length() > 0) {
      aBox.add(trans, DBnaryOnt.usage, usage);
    }
    return trans;
  }

  @Override
  public void registerTranslation(String lang, Resource currentGloss, String usage, String word) {
    registerTranslationToEntity(currentLexEntry, lang, currentGloss, usage, word);
  }

  public String getPageResourceIRI(String vocable) {
    return getPrefix() + uriEncode(vocable);
  }

  public Resource getPageResource(String vocable, boolean dontLinkWithType) {
    if (dontLinkWithType) {
      return aBox.createResource(getPageResourceIRI(vocable));
    }
    return aBox.createResource(getPageResourceIRI(vocable), DBnaryOnt.Page);
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
    Model morphoBox = this.getFeatureBox(ExtractionFeature.MORPHOLOGY);

    if (null == morphoBox) {
      return;
    }

    lexEntry = lexEntry.inModel(morphoBox);

    StmtIterator otherForms = lexEntry.listProperties(OntolexOnt.otherForm);

    boolean foundCompatible = false;
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
            .replaceAll("[/=+]", "-");

    return "__wf_" + compactProperties + "_" + lexEntryLocalName;
  }

  @Override
  public void registerDerivation(String derived) {
    registerDerivation(derived, null);
  }

  @Override
  public void registerDerivation(String derived, String note) {
    if (null != derived && (derived = derived.trim()).isEmpty()) {
      return;
    }
    if (null == currentLexEntry) {
      log.debug("Registering derivation when no lex entry is defined");
      return;
    }
    Resource target = getPageResource(derived);
    aBox.add(target, DBnaryOnt.derivedFrom, currentLexEntry);
    Statement derivStmt = aBox.createStatement(target, DBnaryOnt.derivedFrom, currentLexEntry);
    if (null != note && !note.trim().isEmpty()) {
      ReifiedStatement derivReifiedStmt =
          derivStmt.createReifiedStatement(getDerivationStatementId(derived));
      derivReifiedStmt.addLiteral(SkosOnt.note, note);
      derivStmt = derivReifiedStmt.getStatement();
    }
    aBox.add(derivStmt);
  }

  public String getDerivationStatementId(String derived) {
    return getPrefix() + "__der_" + currentEncodedLexicalEntryName + "_" + uriEncode(derived);
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
      SimpleImmutableEntry<String, String> key = new SimpleImmutableEntry<>(canonicalForm, pos);

      HashSet<HashSet<PropertyObjectPair>> otherForms =
          heldBackOtherForms.computeIfAbsent(key, k -> new HashSet<>());

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
    return languages.computeIfAbsent(lang, l -> tBox.createResource(LEXVO + l));
  }

  public void registerNymRelationToEntity(String target, String synRelation, Resource entity) {
    registerNymRelationToEntity(target, synRelation, entity, null, null);
  }

  public void registerNymRelationToEntity(String target, String nymRelation, Resource entity,
      Resource gloss, String usage) {
    if (NymRelation.of(nymRelation) == null)
      return;

    if (null == entity) {
      log.debug("Registering Lexical Relation when lex entry is null in \"{}\".",
          this.currentMainLexEntry);
      return; // Don't register anything if current lex entry is not known.
    }
    // Some links point to Annex pages or Images, just ignore these.
    int colon = target.indexOf(':');
    if (colon != -1) {
      log.trace("IGNORING NYM VALUE: {} -- {} --> {}", entity, nymRelation, target);
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
    Property nymProperty = NymRelation.of(nymRelation).getProperty();
    // Property nymProperty = nymPropertyMap.get(synRelation);

    Resource targetResource = getPageResource(target);

    Statement nymR = aBox.createStatement(entity, nymProperty, targetResource);
    aBox.add(nymR);

    if (gloss == null && usage == null) {
      return;
    }

    // TODO: for Jena 5.x these class will disappear, Check ReifierStd class in jena
    ReifiedStatement rnymR = nymR.createReifiedStatement(computeNymId(nymRelation));
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
  public AbstractGlossFilter getGlossFilter() {
    return glossFilter;
  }

  @Override
  public Resource createGlossResource(String gloss) {
    return createGlossResource(gloss, -1);
  }

  @Override
  public Resource createGlossResource(StructuredGloss gloss) {
    return createGlossResource(gloss, -1);
  }

  @Override
  public Resource createGlossResource(String gloss, int rank) {
    return createGlossResource(glossFilter.extractGlossStructure(gloss), rank);
  }

  @Override
  public Resource createGlossResource(StructuredGloss gloss, int rank) {
    if (gloss == null || ((gloss.getGloss() == null || gloss.getGloss().isEmpty())
        && (gloss.getSenseNumber() == null || gloss.getSenseNumber().isEmpty()))) {
      return null;
    }

    Resource glossResource = aBox.createResource(getGlossResourceName(gloss), DBnaryOnt.Gloss);
    if (null != gloss.getGloss() && !gloss.getGloss().trim().isEmpty()) {
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
        .replaceAll("[/=+]", "-");
    return getPrefix() + "__" + shortEditionLanguageCode + "_gloss_" + key + "_"
        + currentEncodedLexicalEntryName;
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
  public void registerNymRelationOnCurrentSense(String target, String synRelation, Resource gloss,
      String usage) {
    if (null == currentSense) {
      log.debug("Registering Lexical Relation when current sense is null in \"{}\".",
          this.currentMainLexEntry);
      registerNymRelation(target, synRelation, gloss, usage);
    } else {
      registerNymRelationToEntity(target, synRelation, currentSense, gloss, usage);
    }
  }

  @Override
  public void registerPronunciation(String pron, String lang) {
    if (null == pron)
      return;
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
    HashSet<Statement> toBeRemoved = new HashSet<>();
    ArrayList<Statement> toBeAdded = new ArrayList<>();
    while (entries.hasNext()) {
      Resource lu = entries.next().getResource();
      List<Statement> senses = lu.listProperties(OntolexOnt.sense).toList();
      if (senses.size() == 1) {
        Resource s = senses.get(0).getResource();
        HashSet<Property> alreadyProcessedNyms = new HashSet<>();
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
  public void dump(Model box, OutputStream out, String format) {
    if (null != box) {
      box.write(out, format);
    }
  }

  @Override
  public void dumpAllFeaturesAsHDT(OutputStream ostream, boolean isExolex) {
    Map<ExtractionFeature, Model> features = isExolex ? exolexFeatureBoxes : endolexFeatureBoxes;
    Model[] boxes = features.values().toArray(new Model[0]);
    new Models2HDT().models2hdt(ostream, getPrefix(), boxes);
  }

  @Override
  public int nbEntries() {
    return nbEntries;
  }

  @Override
  public String currentPagename() {
    return currentPage.getName();
  }

  public String getPrefix() {
    return NS;
  }

  public Resource addTo(Resource target, Set<Pair<Property, RDFNode>> pv) {
    if (null == pv)
      return target;
    for (Pair<Property, RDFNode> p : pv) {
      target.addProperty(p.getLeft(), p.getRight());
    }
    return target;
  }

  public Resource addToCurrentWordSense(Set<Pair<Property, RDFNode>> pv) {
    if (currentSense == null) {
      log.debug("Addition to non-existing word sense in {}", currentPagename());
    }
    return addTo(currentSense, pv);
  }

  @Override
  public Resource registerExample(String ex, Set<Pair<Property, RDFNode>> context) {
    return registerExampleOnResource(ex, context, currentSense);
  }

  @Override
  public Resource registerExampleOnResource(String ex, Set<Pair<Property, RDFNode>> context,
      Resource sense) {
    if (null == context)
      context = new HashSet<>();
    context
        .add(Pair.of(RDF.value, ResourceFactory.createLangLiteral(ex, getCurrentEntryLanguage())));
    return registerExampleOnResource(context, sense);
  }

  public Resource registerExample(Set<Pair<Property, RDFNode>> context) {
    return registerExampleOnResource(context, currentSense);
  }

  public Resource registerExampleOnResource(Set<Pair<Property, RDFNode>> context, Resource sense) {
    if (null == sense) {
      log.debug("Registering example when lex sense is null in \"{}\".", this.currentMainLexEntry);
      return null; // Don't register anything if current lex entry is not known.
    }

    // Create new example element
    Resource example = aBox.createResource();
    addTo(example, context);

    aBox.add(aBox.createStatement(sense, SkosOnt.example, example));
    return example;
  }

  @Override
  public void computeStatistics(Model statsModel, Model sourceModel, String dumpVersion) {
    if (null == statsModel) {
      return;
    }

    {
      long ct = countResourcesOfType(DBnaryOnt.Translation, sourceModel);
      long ce = countResourcesOfType(OntolexOnt.LexicalEntry, sourceModel);
      long cp = countResourcesOfType(DBnaryOnt.Page, sourceModel);
      long cs = countResourcesOfType(OntolexOnt.LexicalSense, sourceModel);

      createGeneralStatisticsObservation(statsModel, dumpVersion, getPrefix(),
          longEditionLanguageCode, ct, cp, ce, cs);
    }

    for (NymRelation nym : NymRelation.values()) {
      long cr = countRelations(nym.getProperty(), sourceModel);
      createNymRelationObservation(statsModel, dumpVersion, getPrefix(), longEditionLanguageCode,
          nym, cr);
    }

    Map<String, Long> counts = Statistics.translationCounts(sourceModel);
    counts.forEach((l, c) -> OntolexBasedRDFDataHandler.createTranslationObservation(statsModel,
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
  public void populateMetadata(Model metadataModel, Model sourceModel, String dumpFilename,
      String extractorVersion, boolean isExolex) {
    if (null == metadataModel) {
      return;
    }
    String uriSuffix = isExolex ? "_dbnary_exolex_dataset" : "_dbnary_dataset";
    Resource creator = metadataModel.createResource("http://serasset.bitbucket.io/");
    Resource lexicon = metadataModel.createResource(
        getPrefix() + "___" + shortEditionLanguageCode + uriSuffix, LimeOnt.Lexicon);
    metadataModel.add(metadataModel.createStatement(lexicon, DCTerms.title,
        ISO639_3.sharedInstance.getLanguageNameInEnglish(shortEditionLanguageCode)
            + (isExolex ? " Exolex" : "") + " DBnary Dataset",
        "en"));
    metadataModel
        .add(
            metadataModel.createStatement(lexicon, DCTerms.title,
                "DBnary " + (isExolex ? "Exolex " : "")
                    + ISO639_3.sharedInstance.getLanguageNameInFrench(shortEditionLanguageCode),
                "fr"));
    metadataModel.add(metadataModel.createStatement(lexicon, DCTerms.description,
        "This lexicon is extracted from the original wiktionary data that can be found"
            + " in http://" + shortEditionLanguageCode
            + ".wiktionary.org/ by the DBnary Extractor.",
        "en"));
    metadataModel.add(metadataModel.createStatement(lexicon, DCTerms.description,
        "Cet ensemble de données est extrait du wiktionnaire original disponible" + " à http://"
            + shortEditionLanguageCode
            + ".wiktionary.org/ par le programme d'extraction de DBnary.",
        "fr"));
    metadataModel.add(metadataModel.createStatement(lexicon, DCTerms.creator, creator));
    metadataModel.add(metadataModel.createLiteralStatement(lexicon, DCTerms.created,
        metadataModel.createTypedLiteral(GregorianCalendar.getInstance())));
    metadataModel.add(metadataModel.createStatement(lexicon, DCTerms.source,
        "http://" + shortEditionLanguageCode + ".wiktionary.org/"));

    metadataModel.add(metadataModel.createStatement(lexicon, FOAF.homepage,
        "http://kaiko.getalp.org/about-dbnary"));
    metadataModel.add(metadataModel.createStatement(lexicon, FOAF.page,
        "http://kaiko.getalp.org/static/ontolex/" + shortEditionLanguageCode));

    if (isExolex) {
      metadataModel.add(metadataModel.createStatement(lexicon, LimeOnt.language, "mul"));
      metadataModel.add(metadataModel.createStatement(lexicon, DCTerms.language,
          metadataModel.createResource(LEXVO + "mul")));
    } else {
      metadataModel
          .add(metadataModel.createStatement(lexicon, LimeOnt.language, shortEditionLanguageCode));
      metadataModel
          .add(metadataModel.createStatement(lexicon, DCTerms.language, lexvoExtractedLanguage));
    }

    metadataModel.add(
        metadataModel.createStatement(lexicon, LimeOnt.linguisticCatalog, LexinfoOnt.getURI()));
    metadataModel
        .add(metadataModel.createStatement(lexicon, LimeOnt.linguisticCatalog, OliaOnt.getURI()));
    metadataModel.add(metadataModel.createStatement(lexicon, LimeOnt.linguisticCatalog, LEXVO));

    // TODO: Add extractor version for the current dump
    metadataModel
        .add(metadataModel.createStatement(lexicon, DBnaryOnt.wiktionaryDumpVersion, dumpFilename));
    try {
      LocalDate date = LocalDate.parse(dumpFilename, DateTimeFormatter.BASIC_ISO_DATE);
      Calendar dateCal =
          new GregorianCalendar(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
      XSDDateTime jenaDate = new XSDDateTime(dateCal);
      jenaDate.narrowType(XSDDatatype.XSDdate);
      lexicon.addLiteral(DCTerms.modified, jenaDate);
    } catch (DateTimeParseException e) {
      log.trace("Dump String {} is not a date.", dumpFilename);
    }

    // Connect all lexical entries to the dataset
    int entryCount = 0;
    for (final ResIterator entries =
        sourceModel.listSubjectsWithProperty(RDF.type, OntolexOnt.LexicalEntry); entries
            .hasNext();) {
      final Resource entry = entries.next();
      entryCount++;
      lexicon.addProperty(LimeOnt.entry, entry);
    }

    metadataModel
        .add(metadataModel.createLiteralStatement(lexicon, LimeOnt.lexicalEntries, entryCount));

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

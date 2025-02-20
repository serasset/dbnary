package org.getalp.dbnary.languages.eng;

import jakarta.xml.bind.DatatypeConverter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ReifiedStatement;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.shared.PrefixMapping.IllegalPrefixException;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.getalp.LangTools;
import org.getalp.dbnary.DBnaryEtymologyOnt;
import org.getalp.dbnary.DBnaryOnt;
import org.getalp.dbnary.ExtractionFeature;
import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.OliaOnt;
import org.getalp.dbnary.OntolexOnt;
import org.getalp.dbnary.PronunciationPair;
import org.getalp.dbnary.PropertyObjectPair;
import org.getalp.dbnary.SkosOnt;
import org.getalp.dbnary.languages.OntolexBasedRDFDataHandler;
import org.getalp.dbnary.model.NymRelation;
import org.getalp.dbnary.wiki.WikiText;
import org.getalp.dbnary.wiki.WikiText.Link;
import org.getalp.dbnary.wiki.WikiText.Text;
import org.getalp.dbnary.wiki.WikiText.Token;
import org.getalp.iso639.ISO639_3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by serasset on 17/09/14, pantaleo
 */
public class WiktionaryDataHandler extends OntolexBasedRDFDataHandler {

  private final Logger log = LoggerFactory.getLogger(WiktionaryDataHandler.class);

  /**
   * a HashSet to store the name of etymtree pages that have already been extracted
   */
  static HashSet<String> etymtreeHashSet = new HashSet<>();

  /**
   * Resources containing Etymology Entries.
   */
  public ArrayList<Resource> ancestors;
  public Resource currentEtymologyEntry;
  public Resource currentGlobalEtymologyEntry;
  /**
   * An integer counting the number of alternative etymologies for the same entry.
   */
  protected int currentEtymologyNumber;

  // protected String currentEntryLanguage = null;
  // protected String currentEntryLanguageName = null;

  static {
    // English
    posAndTypeValueMap.put("Noun", new PosAndType(LexinfoOnt.noun, LexinfoOnt.Noun));
    posAndTypeValueMap.put("Proper noun",
        new PosAndType(LexinfoOnt.properNoun, LexinfoOnt.ProperNoun));
    posAndTypeValueMap.put("Proper Noun",
        new PosAndType(LexinfoOnt.properNoun, LexinfoOnt.ProperNoun));

    posAndTypeValueMap.put("Adjective", new PosAndType(LexinfoOnt.adjective, LexinfoOnt.Adjective));
    posAndTypeValueMap.put("Verb", new PosAndType(LexinfoOnt.verb, LexinfoOnt.Verb));
    posAndTypeValueMap.put("Adverb", new PosAndType(LexinfoOnt.adverb, LexinfoOnt.Adverb));
    posAndTypeValueMap.put("Article", new PosAndType(LexinfoOnt.article, LexinfoOnt.Article));
    posAndTypeValueMap.put("Conjunction",
        new PosAndType(LexinfoOnt.conjunction, LexinfoOnt.Conjunction));
    posAndTypeValueMap.put("Determiner",
        new PosAndType(LexinfoOnt.determiner, LexinfoOnt.Determiner));

    posAndTypeValueMap.put("Numeral", new PosAndType(LexinfoOnt.numeral, LexinfoOnt.Numeral));
    posAndTypeValueMap.put("Cardinal numeral",
        new PosAndType(LexinfoOnt.cardinalNumeral, LexinfoOnt.Numeral));
    posAndTypeValueMap.put("Cardinal number",
        new PosAndType(LexinfoOnt.cardinalNumeral, LexinfoOnt.Numeral));

    posAndTypeValueMap.put("Number", new PosAndType(LexinfoOnt.numeral, LexinfoOnt.Number));
    posAndTypeValueMap.put("Particle", new PosAndType(LexinfoOnt.particle, LexinfoOnt.Particle));
    posAndTypeValueMap.put("Preposition",
        new PosAndType(LexinfoOnt.preposition, LexinfoOnt.Preposition));
    posAndTypeValueMap.put("Postposition",
        new PosAndType(LexinfoOnt.postposition, LexinfoOnt.Postposition));

    posAndTypeValueMap.put("Prepositional phrase",
        new PosAndType(null, LexinfoOnt.PrepositionPhrase));

    posAndTypeValueMap.put("Pronoun", new PosAndType(LexinfoOnt.pronoun, LexinfoOnt.Pronoun));
    posAndTypeValueMap.put("Symbol", new PosAndType(LexinfoOnt.symbol, LexinfoOnt.Symbol));

    posAndTypeValueMap.put("Prefix", new PosAndType(LexinfoOnt.prefix, LexinfoOnt.Prefix));
    posAndTypeValueMap.put("Suffix", new PosAndType(LexinfoOnt.suffix, LexinfoOnt.Suffix));
    posAndTypeValueMap.put("Affix", new PosAndType(LexinfoOnt.affix, LexinfoOnt.Affix));
    posAndTypeValueMap.put("Infix", new PosAndType(LexinfoOnt.infix, LexinfoOnt.Infix));
    posAndTypeValueMap.put("Interfix", new PosAndType(LexinfoOnt.affix, LexinfoOnt.Affix));
    posAndTypeValueMap.put("Circumfix", new PosAndType(LexinfoOnt.affix, LexinfoOnt.Affix));

    posAndTypeValueMap.put("Proverb",
        new PosAndType(LexinfoOnt.proverb, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("Interjection",
        new PosAndType(LexinfoOnt.interjection, LexinfoOnt.Interjection));
    posAndTypeValueMap.put("Phrase",
        new PosAndType(LexinfoOnt.phraseologicalUnit, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("Idiom",
        new PosAndType(LexinfoOnt.idiom, OntolexOnt.MultiWordExpression));

    // Initialism ?
    // Chinese chars uses Definitions to aggregate several PoS, How can we cretae an underspecified
    // entry from this ?
    posAndTypeValueMap.put("Definitions", null);
  }

  protected static final HashMap<Resource, Resource> wordToMutiWordPOSTypes = new HashMap<>();

  static {
    wordToMutiWordPOSTypes.put(LexinfoOnt.Noun, LexinfoOnt.NounPhrase);
    wordToMutiWordPOSTypes.put(LexinfoOnt.Adjective, LexinfoOnt.AdjectivePhrase);
    wordToMutiWordPOSTypes.put(LexinfoOnt.Verb, LexinfoOnt.VerbPhrase);
    wordToMutiWordPOSTypes.put(LexinfoOnt.Adverb, LexinfoOnt.NounPhrase);
  }

  protected static final HashSet<Resource> multiWordTypes = new HashSet<>();

  static {
    multiWordTypes.add(OntolexOnt.MultiWordExpression);
    multiWordTypes.add(LexinfoOnt.NounPhrase);
    multiWordTypes.add(LexinfoOnt.AdjectiveFrame);
    multiWordTypes.add(LexinfoOnt.VerbPhrase);
    multiWordTypes.add(LexinfoOnt.PrepositionPhrase);
  }

  protected static final HashSet<Resource> affixTypes = new HashSet<>();

  static {
    affixTypes.add(LexinfoOnt.Affix);
    affixTypes.add(LexinfoOnt.Prefix);
    affixTypes.add(LexinfoOnt.Infix);
    affixTypes.add(LexinfoOnt.Suffix);
  }

  protected static final HashSet<Resource> wordTypes = new HashSet<>();

  static {
    wordTypes.add(OntolexOnt.Word);
    wordTypes.add(LexinfoOnt.Noun);
    wordTypes.add(LexinfoOnt.ProperNoun);
    wordTypes.add(LexinfoOnt.CommonNoun);
    wordTypes.add(LexinfoOnt.Adjective);
    wordTypes.add(LexinfoOnt.Verb);
    wordTypes.add(LexinfoOnt.Adverb);
    wordTypes.add(LexinfoOnt.Preposition);
    wordTypes.add(LexinfoOnt.Interjection);
    wordTypes.add(LexinfoOnt.Conjunction);
    wordTypes.add(LexinfoOnt.Pronoun);
    wordTypes.add(LexinfoOnt.Numeral);
    wordTypes.add(LexinfoOnt.Adposition);
    wordTypes.add(LexinfoOnt.Particle);
    wordTypes.add(LexinfoOnt.FusedPreposition);
    wordTypes.add(LexinfoOnt.Determiner);
    wordTypes.add(LexinfoOnt.Symbol);
  }


  public WiktionaryDataHandler(String lang, String tdbDir) {
    super(lang, tdbDir);
  }

  public void initializeLanguageSection(String lang) {
    currentEtymologyNumber = 0;
    currentEtymologyEntry = null;
    currentGlobalEtymologyEntry = createGlobalEtymologyResource(currentPage.getName(), lang);
    super.initializeLanguageSection(lang);
  }

  private static final String spacePunctsRegex = "[\\s\\p{Punct}]+";
  private static final String wordPunct = "-־׳״'.·*’་•:";
  private static final String NonWordPunctRegex = "[^" + wordPunct + "]";
  private static final Pattern spacePuncts = Pattern.compile(spacePunctsRegex);
  private static final Pattern nonWordPunct = Pattern.compile(NonWordPunctRegex);

  @Override
  protected Resource initializeLexicalEntry(String pos, Resource lexinfoPOS, Resource type) {

    if (affixTypes.contains(type)) {
      return super.initializeLexicalEntry(pos, lexinfoPOS, type);
    } else if (multiWordTypes.contains(type)) {
      Resource entry = super.initializeLexicalEntry(pos, lexinfoPOS, type);
      if (!OntolexOnt.MultiWordExpression.equals(type)) {
        currentLexicalEntry.addResourceType(OntolexOnt.MultiWordExpression);
        currentLexEntry.addProperty(RDF.type, OntolexOnt.MultiWordExpression);
      }
      return entry;
    } else {
      // Compute if the entry is a phrase or a word.
      boolean isAPhrase = false;
      Matcher sp = spacePuncts.matcher(currentPage.getName().trim());
      while (sp.find()) {
        Matcher nwp = nonWordPunct.matcher(sp.group());
        if (nwp.find()) {
          isAPhrase = true;
          break;
        }
      }
      if (isAPhrase) {
        Resource multiWordType =
            wordToMutiWordPOSTypes.getOrDefault(type, OntolexOnt.MultiWordExpression);
        Resource entry = super.initializeLexicalEntry(pos, lexinfoPOS, multiWordType);
        if (!OntolexOnt.MultiWordExpression.equals(multiWordType)) {
          currentLexicalEntry.addResourceType(OntolexOnt.MultiWordExpression);
          currentLexEntry.addProperty(RDF.type, OntolexOnt.MultiWordExpression);
        }
        return entry;
      } else {
        Resource entry = super.initializeLexicalEntry(pos, lexinfoPOS, type);
        if (!OntolexOnt.Word.equals(type)) {
          currentLexicalEntry.addResourceType(OntolexOnt.Word);
          currentLexEntry.addProperty(RDF.type, OntolexOnt.Word);
        }
        return entry;
      }
    }
  }

  private Resource createGlobalEtymologyResource(String wiktionaryPageName, String lang) {
    if (wiktionaryPageName.trim().split("\\s+").length >= 3) {
      return null;
    }
    Model eBox = null;
    if ((eBox = this.getFeatureBox(ExtractionFeature.ETYMOLOGY)) != null) {
      // TODO : should I check that getPrefix returns null ?
      lang = EnglishLangToCode.threeLettersCode(lang);
      Resource r = eBox.createResource(getPrefix(lang) + "__ee_" + uriEncode(wiktionaryPageName),
          DBnaryEtymologyOnt.EtymologyEntry);
      Resource w = ResourceFactory.createResource(WIKT + uriEncode(wiktionaryPageName) + "#"
          + uriEncode(ISO639_3.sharedInstance.getLanguageNameInEnglish(lang)));
      eBox.add(r, RDFS.seeAlso, w);
      eBox.add(r, RDFS.label, wiktionaryPageName, lang);

      return r;
    }
    return null;
  }

  public static boolean isValidPOS(String pos) {
    return posAndTypeValueMap.containsKey(pos);
  }

  public void registerEtymologyPos(String wiktionaryPageName) {
    registerEtymologyPos("eng", "English", wiktionaryPageName);
  }

  public void registerEtymologyPos(String lang, String languageName, String wiktionaryPageName) {
    if (wiktionaryPageName.trim().split("\\s+").length >= 3) {
      return;
    }
    Model eBox = null;
    if ((eBox = this.getFeatureBox(ExtractionFeature.ETYMOLOGY)) == null) {
      return;
    }
    if (currentEtymologyEntry == null) { // there is no etymology section
      currentEtymologyEntry =
          eBox.createResource(getPrefix(lang) + "__ee_" + uriEncode(currentPagename()),
              DBnaryEtymologyOnt.EtymologyEntry);
      Resource w = ResourceFactory.createResource(WIKT + uriEncode(currentPagename()) + "#"
          + uriEncode(ISO639_3.sharedInstance.getLanguageNameInEnglish(shortSectionLanguageCode)));
      eBox.add(currentEtymologyEntry, RDFS.seeAlso, w);
      eBox.add(currentEtymologyEntry, RDFS.label, currentPagename(), lang);
    }
    eBox.add(currentEtymologyEntry, DBnaryOnt.describes, currentLexEntry);
  }

  private String computeEtymologyId(int etymologyNumber, String lang) {
    return getPrefix(lang) + "__ee_" + etymologyNumber + "_" + uriEncode(currentPagename());
  }

  Map<String, String> etymologyLanguages = new HashMap<>(4000);

  // TODO : check if we should create the prefixes in the aBox or in the eBox
  // TODO: getPrefix should never return null. It should return "unknown" if there is no language
  public String getPrefix(String lang) {
    // TODO : lang may be null ?
    // TODO : what if the
    if (lang == null) {
      log.debug("Null input language to function getPrefix.");
      lang = "unknown";
    }
    String code = EnglishLangToCode.threeLettersCode(lang);
    if (code == null) {
      code = "unknown";
    }
    lang = LangTools.normalize(code);
    lang = lang.trim();
    if (lang.equals("eng")) {
      return super.getPrefix();
    }
    // Map<String, String> pMap = box.getNsPrefixMap();
    String pName = lang + "-eng";
    String prefix = etymologyLanguages.get(pName);
    if (null == prefix) {
      prefix = DBNARY_NS_PREFIX + "/eng/" + lang + "/";
      etymologyLanguages.put(pName, prefix);
    }
    return prefix;
  }

  public void postProcessEtymology() {
    Model eBox = this.getEndolexFeatureBox(ExtractionFeature.ETYMOLOGY);
    protectedSetPrefixes(eBox);
    eBox = this.getExolexFeatureBox(ExtractionFeature.ETYMOLOGY);
    protectedSetPrefixes(eBox);
  }

  private void protectedSetPrefixes(Model eBox) {
    if (eBox != null) {
      for (Entry<String, String> p : etymologyLanguages.entrySet()) {
        try {
          eBox.setNsPrefix(p.getKey(), p.getValue());
        } catch (IllegalPrefixException e) {
          log.error("Etymology: Illegal prefix {} generated by etymology extractor", p.getKey());
        }
      }
    }
  }

  public void registerDerived(Etymology etymology) {
    Model eBox = null;
    if ((eBox = this.getFeatureBox(ExtractionFeature.ETYMOLOGY)) == null) {
      return;
    }

    if (etymology.symbols == null || etymology.symbols.size() == 0) {
      return;
    }

    String lang = null;
    Resource vocable0 = null;
    int counter = 0;
    for (Symbols b : etymology.symbols) {
      String word = b.args.get("word1").split(",")[0].trim();
      if (word.equals("")) {
        log.debug("Error: empty lemma found while processing derived words of {} in string {}",
            currentPagename(), etymology.string);
      } else {
        if (counter == 0) {
          lang = b.args.get("lang");
          // register derives_from
          vocable0 = eBox.createResource(getPrefix(lang) + "__ee_" + uriEncode(word),
              DBnaryEtymologyOnt.EtymologyEntry);
          eBox.add(vocable0, DBnaryEtymologyOnt.etymologicallyRelatedTo, currentEtymologyEntry);
          eBox.add(vocable0, DBnaryEtymologyOnt.etymologicallyDerivesFrom, currentEtymologyEntry);
          // TODO: when extracting a reconstructed word the URL of wiktionary page is not correctly
          // computed
          Resource w =
              ResourceFactory.createResource(WIKT + uriEncode(currentPagename()) + "#" + uriEncode(
                  ISO639_3.sharedInstance.getLanguageNameInEnglish(shortSectionLanguageCode)));
          eBox.add(vocable0, RDFS.seeAlso, w);
          eBox.add(vocable0, RDFS.label, word, lang);
        } else {
          // register etymologically_equivalent_to
          Resource vocable2 = eBox.createResource(getPrefix(lang) + "__ee_" + uriEncode(word),
              DBnaryEtymologyOnt.EtymologyEntry);
          eBox.add(vocable2, DBnaryEtymologyOnt.etymologicallyRelatedTo, vocable0);
          eBox.add(vocable2, DBnaryEtymologyOnt.etymologicallyEquivalentTo, vocable0);
          Resource w =
              ResourceFactory.createResource(WIKT + uriEncode(currentPagename()) + "#" + uriEncode(
                  ISO639_3.sharedInstance.getLanguageNameInEnglish(shortSectionLanguageCode)));
          eBox.add(vocable2, RDFS.seeAlso, w);
          eBox.add(vocable2, RDFS.label, word, lang);
        }
        counter++;
      }
    }
  }

  public void registerCurrentEtymologyEntry(String lang) {
    Model eBox = null;
    if ((eBox = this.getFeatureBox(ExtractionFeature.ETYMOLOGY)) == null) {
      return;
    }

    if (currentEtymologyEntry != null) {
      return;
    }
    currentEtymologyNumber++;
    currentEtymologyEntry = eBox.createResource(computeEtymologyId(currentEtymologyNumber, lang),
        DBnaryEtymologyOnt.EtymologyEntry);
    eBox.add(currentGlobalEtymologyEntry, DBnaryOnt.describes, currentEtymologyEntry);
    Resource w = ResourceFactory.createResource(WIKT + uriEncode(currentPagename()) + "#"
        + uriEncode(ISO639_3.sharedInstance.getLanguageNameInEnglish(shortSectionLanguageCode)));
    eBox.add(currentEtymologyEntry, RDFS.seeAlso, w);
  }

  public Resource createEtymologyEntryResource(Model eBox, String e, String lang) {
    String word = e.split(",")[0].trim();
    return eBox.createResource(getPrefix(lang) + "__ee_" + uriEncode(word),
        DBnaryEtymologyOnt.EtymologyEntry);
  }

  public void registerEtymology(Etymology etymology) {
    Model eBox = null;
    if ((eBox = this.getFeatureBox(ExtractionFeature.ETYMOLOGY)) == null) {
      return;
    }

    currentEtymologyEntry = null;

    if (etymology.symbols == null || etymology.symbols.size() == 0) {
      return;
    }

    registerCurrentEtymologyEntry(etymology.lang);
    Resource vocable0 = currentEtymologyEntry, vocable = null;
    String lang0 = etymology.lang, lang = null;
    for (int j = 0; j < etymology.symbols.size(); j++) {
      Symbols b = etymology.symbols.get(j);
      for (String values : b.values) {
        if (values.equals("LEMMA")) {
          boolean isEquivalent = false;
          lang = b.args.get("lang");
          // handle etymologically equivalent words (i.e., isEquivalent = true)
          if (lang != null && lang0 != null) {
            if (lang0.equals(lang)) {
              if (j > 1) {
                String tmp = etymology.symbols.get(j - 1).values.get(0);
                if (tmp.equals("COMMA") || tmp.equals("SLASH")) {
                  isEquivalent = true;
                }
              }
            }
          }
          if (isEquivalent) {// etymologically equivalent words
            String word1 = b.args.get("word1");
            if (word1 != null && !word1.equals("")) {
              vocable = createEtymologyEntryResource(eBox, word1, lang0);
              eBox.add(vocable0, DBnaryEtymologyOnt.etymologicallyEquivalentTo, vocable);
              eBox.add(vocable0, DBnaryEtymologyOnt.etymologicallyRelatedTo, vocable);
              Resource w = ResourceFactory
                  .createResource(WIKT + uriEncode(currentPagename()) + "#" + uriEncode(
                      ISO639_3.sharedInstance.getLanguageNameInEnglish(shortSectionLanguageCode)));
              eBox.add(vocable, RDFS.seeAlso, w);
              eBox.add(vocable, RDFS.label, word1, lang0);
            } else {
              log.debug("empty word in symbol %s\n", b.string);
            }
          } else {
            // parse template with multiple words (word1 word2 etc., and possibly lang1, lang2 etc.)
            boolean compound = false;
            Resource w = ResourceFactory
                .createResource(WIKT + uriEncode(currentPagename()) + "#" + uriEncode(
                    ISO639_3.sharedInstance.getLanguageNameInEnglish(shortSectionLanguageCode)));
            for (int kk = 1; kk < 12; kk++) {
              String word = b.args.get("word" + Integer.toString(kk));
              lang = b.args.get("lang" + Integer.toString(kk));
              if (lang == null) {
                lang = b.args.get("lang");
              }
              // TODO: When word is empty (but not null), it means same string as current entry
              if ("".equals(word)) {
                word = currentPagename();
              }
              if (word != null && !word.equals("") && lang != null) {
                if (kk > 1) {// it's some kind of compound
                  compound = true;
                }
                vocable = createEtymologyEntryResource(eBox, word, lang);
                eBox.add(vocable0, DBnaryEtymologyOnt.etymologicallyRelatedTo, vocable);
                eBox.add(vocable, RDFS.seeAlso, w);
                eBox.add(vocable, RDFS.label, word, lang);
              }
            }
            if (compound) {
              return;
            }
          }
          vocable0 = vocable;
          lang0 = lang;
        }
      }
    }
  }

  public void initializeAncestors() {
    ancestors = new ArrayList<Resource>();
  }

  public void finalizeAncestors() {
    ancestors.clear();
  }

  public void addAncestorsAndRegisterDescendants(Etymology etymology) {
    Model eBox = null;
    if ((eBox = this.getFeatureBox(ExtractionFeature.ETYMOLOGY)) == null) {
      return;
    }

    if (etymology.symbols == null || etymology.symbols.size() == 0) {
      Resource vocable = eBox.createResource("");
      ancestors.add(vocable);// add an empty vocable and don't register it
      return;
    }
    Resource ancestor = null;
    for (int i = 0; i < ancestors.size(); i++) {
      String a = ancestors.get(ancestors.size() - 1 - i).toString();
      if (!a.equals("")) {
        ancestor = ancestors.get(ancestors.size() - 1 - i);
        break;
      }
    }

    int counter = 0; // number of etymology.symbols
    for (Symbols b : etymology.symbols) {
      if (b.values != null) {
        if (b.values.get(0).equals("LEMMA")) {
          String word = b.args.get("word1").split(",")[0].trim();
          String lang = b.args.get("lang");
          Resource vocable = eBox.createResource(getPrefix(lang) + "__ee_" + uriEncode(word),
              DBnaryEtymologyOnt.EtymologyEntry);
          if (counter == 0) {
            if (ancestor != null) {
              eBox.add(vocable, DBnaryEtymologyOnt.etymologicallyRelatedTo, ancestor);
              Resource w = ResourceFactory
                  .createResource(WIKT + uriEncode(currentPagename()) + "#" + uriEncode(
                      ISO639_3.sharedInstance.getLanguageNameInEnglish(shortSectionLanguageCode)));
              eBox.add(vocable, RDFS.seeAlso, w);
              eBox.add(vocable, RDFS.label, word, lang);
            }
            ancestors.add(vocable);
          } else {
            eBox.add(vocable, DBnaryEtymologyOnt.etymologicallyEquivalentTo,
                ancestors.get(ancestors.size() - 1));
            eBox.add(vocable, DBnaryEtymologyOnt.etymologicallyRelatedTo,
                ancestors.get(ancestors.size() - 1));
            Resource w = ResourceFactory
                .createResource(WIKT + uriEncode(currentPagename()) + "#" + uriEncode(
                    ISO639_3.sharedInstance.getLanguageNameInEnglish(shortSectionLanguageCode)));
            eBox.add(vocable, RDFS.seeAlso, w);
            eBox.add(vocable, RDFS.label, word, lang);
          }
          counter++;
        }
      }
    }
  }

  @Override
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

  @Override
  protected void addOtherFormPropertiesToLexicalEntry(Resource lexEntry,
      HashSet<PropertyObjectPair> properties) {
    // Do not try to merge new form with an existing compatible one in English.
    // This would lead to a Past becoming a PastParticiple when registering the past participle
    // form.
    Model morphoBox = this.getFeatureBox(ExtractionFeature.MORPHOLOGY);

    if (null == morphoBox) {
      return;
    }

    lexEntry = lexEntry.inModel(morphoBox);

    String otherFormNodeName = computeOtherFormResourceName(lexEntry, properties);
    Resource otherForm = morphoBox.createResource(getPrefix() + otherFormNodeName, OntolexOnt.Form);
    morphoBox.add(lexEntry, OntolexOnt.otherForm, otherForm);
    mergePropertiesIntoResource(properties, otherForm);

  }

  public void registerInflection(String inflection, String note,
      HashSet<PropertyObjectPair> props) {

    // Keep it simple for english: register forms on the current lexical entry
    if (null != note) {
      PropertyObjectPair p =
          PropertyObjectPair.get(SkosOnt.note, aBox.createLiteral(note, shortEditionLanguageCode));
      props.add(p);
    }
    PropertyObjectPair p = PropertyObjectPair.get(OntolexOnt.writtenRep,
        aBox.createLiteral(inflection, getCurrentEntryLanguage()));
    props.add(p);

    addOtherFormPropertiesToLexicalEntry(currentLexEntry, props);

  }

  @Override
  public void registerInflection(String languageCode, String pos, String inflection,
      String canonicalForm, int defNumber, HashSet<PropertyObjectPair> props) {

    // Keep it simple for english: register forms on the current lexical entry
    // FIXME: check what is provided when we have different lex entries with the same pos and morph.

    PropertyObjectPair p = PropertyObjectPair.get(OntolexOnt.writtenRep,
        aBox.createLiteral(inflection, getCurrentEntryLanguage()));

    props.add(p);

    addOtherFormPropertiesToLexicalEntry(currentLexEntry, props);

  }

  public void addWrittenRep(String word) {
    if (currentLexEntry != null) {
      aBox.add(currentLexEntry, OntolexOnt.writtenRep, word, getCurrentEntryLanguage());
    }
  }

  public void uncountable() {
    if (currentLexEntry == null) {
      log.debug("Registering countability on non existant lex entry in  {}", currentPagename());
      return;
    }
    aBox.add(aBox.createStatement(currentLexEntry, OliaOnt.hasCountability, OliaOnt.Uncountable));
  }

  public void countable() {
    if (currentLexEntry == null) {
      log.debug("Registering countability on non existant lex entry in  {}", currentPagename());
      return;
    }
    aBox.add(aBox.createStatement(currentLexEntry, OliaOnt.hasCountability, OliaOnt.Countable));
  }

  public void comparable() {
    if (currentLexEntry == null) {
      log.debug("Registering comparativity on non existant lex entry in  {}", currentPagename());
      return;
    }
    // TODO: do we have a mean to say that an adjective is comparable ?
    // aBox.add(aBox.createStatement(currentLexEntry, OliaOnt., OliaOnt.Uncountable));
  }

  public void notComparable() {
    if (currentLexEntry == null) {
      log.debug("Registering comparativity on non existant lex entry in  {}", currentPagename());
      return;
    }
    // TODO: do we have a mean to say that an adjective is not comparable ?
    // aBox.add(aBox.createStatement(currentLexEntry, OliaOnt., OliaOnt.Uncountable));
  }

  public void addInflectionOnCanonicalForm(EnglishInflectionData infl) {
    this.mergePropertiesIntoResource(infl.toPropertyObjectMap(), currentCanonicalForm);
  }

  public Resource getGlossForWikisaurus(String id) {
    return aBox.createResource(getGlossURI(id), DBnaryOnt.Gloss);
  }

  public String getGlossURI(String id) {
    return getPrefix() + "__" + shortEditionLanguageCode + "_gloss_" + id + "_"
        + uriEncode(currentPagename());
  }

  public void registerWikisaurusNymFromTo(String currentPOS, String nym, String gloss,
      String targetGloss, String s, String t) {
    if (t.equals(s)) {
      return;
    }
    if (null == nym || NymRelation.of(nym) == null) {
      log.debug("null nym or unknown nym {} in Wikisaurus:{}", nym, currentPagename());
      return;
    }

    Property nymProperty = NymRelation.of(nym).getProperty();

    Statement nymR = aBox.createStatement(getPageResource(s), nymProperty, getPageResource(t));
    aBox.add(nymR);


    Resource glossResource = getGlossResource(gloss);
    Resource targetGlossResource = getGlossResource(targetGloss);
    Resource pos = posResource(currentPOS);

    if (null == pos && null == glossResource && null == targetGlossResource)
      return;

    ReifiedStatement rnymR =
        nymR.createReifiedStatement(computeNymId(nym, uriEncode(currentPagename())));

    if (null != pos) {
      rnymR.addProperty(DBnaryOnt.partOfSpeech, pos);
    }
    if (null != glossResource) {
      aBox.add(glossResource, RDF.value, gloss, getCurrentEntryLanguage());
      rnymR.addProperty(DBnaryOnt.gloss, glossResource);
    }
    if (null != targetGlossResource) {
      aBox.add(targetGlossResource, RDF.value, targetGloss, getCurrentEntryLanguage());
      rnymR.addProperty(DBnaryOnt.gloss4Target, targetGlossResource);
    }
  }

  private Resource getGlossResource(String gloss) {
    if (gloss == null)
      return null;
    String glossKey = gloss;
    glossKey =
        DatatypeConverter.printBase64Binary(BigInteger.valueOf(glossKey.hashCode()).toByteArray())
            .replaceAll("[/=\\+]", "-");
    Resource glossResource = getGlossForWikisaurus(glossKey);
    return glossResource;
  }

  public void registerWikisaurusNym(String currentPOS, String currentWS, String currentNym,
      String s) {
    if (s.equals(currentPagename())) {
      return;
    }
    if (null == currentNym || "".equals(currentNym.trim())) {
      log.debug("null nym in Wikisaurus:{}", currentPagename());
    }

    try {
      Property nymProperty = NymRelation.of(currentNym).getProperty();
      // Property nymProperty = nymPropertyMap.get(currentNym);

      Statement nymR = aBox.createStatement(currentMainLexEntry, nymProperty, getPageResource(s));
      aBox.add(nymR);

      if (currentWS == null && currentPOS == null) {
        return;
      }
      String gloss = currentNym + currentPOS + currentWS;
      gloss =
          DatatypeConverter.printBase64Binary(BigInteger.valueOf(gloss.hashCode()).toByteArray())
              .replaceAll("[/=\\+]", "-");
      Resource glossResource = getGlossForWikisaurus(gloss);
      Resource pos = posResource(currentPOS);
      if (null != pos) {
        aBox.add(aBox.createStatement(glossResource, DBnaryOnt.partOfSpeech, pos));
      }
      if (null != currentWS) {
        aBox.add(glossResource, RDF.value, currentWS, getCurrentEntryLanguage());
      }

      if (glossResource != null) {
        ReifiedStatement rnymR =
            nymR.createReifiedStatement(computeNymId(currentNym, uriEncode(currentPagename())));
        rnymR.addProperty(DBnaryOnt.gloss, glossResource);
      }
    } catch (NullPointerException npe) {
      log.debug("Unknown Nym Property in Wikisaurus:{}", currentNym);
    }
  }

  // INLINE codes in syn template:
  // t: gloss
  // alt: alternative display text
  // tr: transliteration
  // ts: transcription, for languages where the transliteration and pronunciation are markedly
  // different
  // q: qualifier, e.g. rare; this appears before the term, parenthesized and italicized
  // qq: qualifier, e.g. rare; this appears after the term, parenthesized and italicized
  // lit: literal meaning
  // pos: part of speech
  // g: comma-separated list of gender/number specifications
  // id: sense ID; see {{senseid}}
  // sc: script code
  // tag: dialect tag; see below
  private final static Pattern NYM_VALUE_INLINE_MODIFIER_PATTERN =
      Pattern.compile("<qq?:(?<qual>[^>]+)>|<(?:t|lit):(?<gloss>[^>]+)>|<alt:(?<alt>[^>]+)>|"
          + "<(?:tr|ts):(?<tr>[^>]+)>|<pos:(?<pos>[^>]+)>|<g:(?<g>[^>]+)>|<id:(?<sid>[^>]+)>|"
          + "<sc:([^>]+)>|<tag:([^>]+)>");

  @Override
  public void registerNymRelationToEntity(String target, String nymRelation, Resource entity,
      Resource gloss, String usage) {
    Matcher m = NYM_VALUE_INLINE_MODIFIER_PATTERN.matcher(target);
    while (m.find()) {
      String qualValue, glossValue, altValue, trValue, posValue, gValue, sidValue;
      if ((glossValue = m.group("gloss")) != null) {
        if (gloss == null) {
          gloss = createGlossResource(glossValue);
        } else {
          log.debug("Gloss already defined for {} ({})", target, currentPagename());
        }
      }
      if ((qualValue = m.group("qual")) != null) {
        usage = usage == null ? "|qual=" + qualValue : usage + "|qual=" + qualValue;
      }
      if ((altValue = m.group("alt")) != null) {
        usage = usage == null ? "|alt=" + altValue : usage + "|alt=" + altValue;
      }
      if ((trValue = m.group("tr")) != null) {
        usage = usage == null ? "|tr=" + trValue : usage + "|tr=" + trValue;
      }
      if ((posValue = m.group("pos")) != null) {
        usage = usage == null ? "|pos=" + posValue : usage + "|pos=" + posValue;
      }
      if ((gValue = m.group("g")) != null) {
        usage = usage == null ? "|g=" + gValue : usage + "|g=" + gValue;
      }
      if ((sidValue = m.group("sid")) != null) {
        // TODO: handle sense id
        log.debug("Sense id not handled for {} ({})", target, currentPagename());
      }
    }
    target = m.replaceAll("");
    if (target.contains("[[")) {
      // nym value is one or more links, resolve the links
      WikiText targetWT = new WikiText(target);
      StringBuilder out = new StringBuilder();
      for (Token t : targetWT.tokens()) {
        if (t instanceof Text) {
          out.append(t);
        } else if (t instanceof Link) {
          out.append(t.asLink().getTargetText());
        } else {
          log.debug("NYM: Unexpected token {} in nym value {} [{}]", t, target, currentPagename());
        }
      }
      target = out.toString();
    }
    super.registerNymRelationToEntity(target, nymRelation, entity, gloss, usage);
  }

  public void initializeNewEtymology() {
    // Etymology is a top level section that finalizes previous entry, canonical forms and
    // pronunciations
    currentCanonicalForm = null;
    currentLexicalEntry = null;
    currentLexEntry = null;
    currentSharedPronunciations.clear();
  }

}

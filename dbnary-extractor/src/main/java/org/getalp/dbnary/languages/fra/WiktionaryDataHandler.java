package org.getalp.dbnary.languages.fra;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDFS;
import org.getalp.LangTools;
import org.getalp.dbnary.*;
import org.getalp.dbnary.commons.HierarchicalSenseNumber;
import org.getalp.dbnary.languages.OntolexBasedRDFDataHandler;
import org.getalp.dbnary.wiki.WikiText;
import org.getalp.model.ontolex.LexicalForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by serasset on 17/09/14.
 */
public class WiktionaryDataHandler extends OntolexBasedRDFDataHandler {

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

  private final Logger log = LoggerFactory.getLogger(WiktionaryDataHandler.class);
  // A entry -> pos -> set of lexical forms hashmap used to store the inflected form which have
  // to be registered chen the main lexical entry is processed.
  private final HashMap<String, HashMap<String, Set<LexicalForm>>> heldBackOtherForms = new HashMap<>();
  Map<String, String> etymologyLanguages = new HashMap<>(4000);
  Resource Etymon = ResourceFactory.createResource(LemonEtyOnt.NS + "Etymon");
  Resource EtyLink = ResourceFactory.createResource(LemonEtyOnt.NS + "EtyLink");
  Resource Etymology = ResourceFactory.createResource(LemonEtyOnt.NS + "Etymology");
  Resource Cognate = ResourceFactory.createResource(LemonEtyOnt.NS + "Cognate");
  private Set<String> languagesOfCurrentPage = new HashSet<>();

  public WiktionaryDataHandler(String lang, String tdbDir) {
    super(lang, tdbDir);
  }

  @Override
  public void initializeLexicalEntry(String pos) {
    // DONE: compute if the entry is a phrase or a word.
    PosAndType pat = posAndTypeValueMap.get(pos);
    Resource typeR = typeResource(pat);
    if (currentPage.getName().startsWith("se ")) {
      if (currentPage.getName().substring(2).trim().contains(" ")) {
        typeR = OntolexOnt.MultiWordExpression;
      }
    } else if (currentPage.getName().contains(" ")) {
      typeR = OntolexOnt.MultiWordExpression;
    }
    // reset the sense number.
    currentSenseNumber = new HierarchicalSenseNumber();
    initializeLexicalEntry(pos, posResource(pat), typeR);
    Model morphoBox = getFeatureBox(ExtractionFeature.MORPHOLOGY);
    if (null != morphoBox) {
      String heldBackKey = computeLanguageSectionKey();
      HashMap<String, Set<LexicalForm>> pos2forms = heldBackOtherForms.getOrDefault(heldBackKey,
          new HashMap<>());
      Set<LexicalForm> forms = pos2forms.getOrDefault(pos, new HashSet<>());
      forms.forEach(f -> f.attachTo(currentLexEntry.inModel(morphoBox)));
    }
  }

  protected String computeSenseNum() {
    return currentSenseNumber.formatWithModel("naiiiiiiii");
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

  private String computeLanguageSectionKey() {
    return computeLanguageSectionKey(longSectionLanguageCode);
  }

  private String computeLanguageSectionKey(String language) {
    return computeLanguageSectionKey(currentPage.getName(), language);
  }

  private String computeLanguageSectionKey(String pagename, String language) {
    return pagename + "___/___" + language;
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

    String heldBackKey = computeLanguageSectionKey(onLexicalEntry, languageCode);
    HashMap<String, Set<LexicalForm>> pos2forms = heldBackOtherForms.computeIfAbsent(heldBackKey,
        k -> new HashMap<>());
    Set<LexicalForm> otherForms = pos2forms.computeIfAbsent(pos, k -> new HashSet<>());

    otherForms.add(form);
  }

  @Override
  public void initializePageExtraction(String wiktionaryPageName) {
    languagesOfCurrentPage.clear();
    super.initializePageExtraction(wiktionaryPageName);
  }


  @Override
  public void finalizePageExtraction() {
    // Remove all inflections related to the current page as they cannot be attach to
    // another page anymore.
    languagesOfCurrentPage.forEach(l -> heldBackOtherForms.remove(computeLanguageSectionKey(l)));
    super.finalizePageExtraction();
  }

  @Override
  public void initializeLanguageSection(String language) {
    /*
     * currentEtymologyNumber = 0; currentEtymologyEntry = null; currentGlobalEtymologyEntry =
     * createGlobalEtymologyResource(currentPage.getName(), lang);
     */
    super.initializeLanguageSection(language);
    languagesOfCurrentPage.add(longSectionLanguageCode);
  }


  /**
   * rewriting of getPrefix in org.getalp.dbnary.languages.eng.WiktionaryDataHandler , maybe rewrite
   * into a general function that will return prefix for any given language
   */
  public String getPrefix(String lang) {

    if (lang == null) {
      log.debug("Null input language to function getPrefix.");
      lang = "unknown";
    }
    String code = FrenchLangtoCode.threeLettersCode(lang);
    if (code == null) {
      code = "unknown";
    }
    lang = LangTools.normalize(code);
    lang = lang.trim();
    if (lang.equals("fra")) {
      return super.getPrefix();
    }

    String pName = lang + "-fr";
    String prefix = etymologyLanguages.get(pName);
    if (null == prefix) {
      prefix = DBNARY_NS_PREFIX + "/fr/" + lang + "/";
      etymologyLanguages.put(pName, prefix);
    }
    return prefix;

  }


  public void createEtymologyGraph(String wiktionaryPageName, String lang,
      FrenchEtymology frenchEtymology) {
    String etymonFiller = "__etymon__";
    String etyLinkFiller = "__etyLink__" + uriEncode(wiktionaryPageName);
    String etymologyFiller = "__ety__fr";
    if (wiktionaryPageName.trim().split("\\s+").length >= 3) {
      return;
    }
    Model etymologyBox = this.getFeatureBox(ExtractionFeature.ETYMOLOGY);
      if (etymologyBox == null) {
          return;
      }
    //initialisation du graph
    Resource etymology = etymologyBox.createResource(
        getPrefix(lang) + uriEncode(wiktionaryPageName) + etymologyFiller, Etymology);
    etymologyBox.add(getPageResource(currentPage.getName()), LemonEtyOnt.etymology, etymology);
    etymologyBox.add(getPageResource(currentPage.getName()), RDFS.seeAlso,
        ResourceFactory.createResource(WIKT + uriEncode(wiktionaryPageName)));
    etymologyBox.add(getPageResource(currentPage.getName()), RDFS.label, wiktionaryPageName, lang);

    // je commence avec la création des étymons car tous les mots liés sont des étymons, et ça va aider
    // avec les LinkedList
    for (FSymbols fSymbols : frenchEtymology.linkedTemplates) {
      Optional<WikiText.WikiContent> word = getWordFromFSymbolsTemplates(fSymbols, frenchEtymology);
      if (word.isPresent()) {
        String langCode = getLanguageFromSymbolsTemplats(fSymbols, frenchEtymology);
        etymologyBox.setNsPrefix(lang + "-" + langCode,
            getPrefix(getLanguageFromSymbolsTemplats(fSymbols, frenchEtymology)));
        Resource etymon =
            etymologyBox.createResource(
                getPrefix(getLanguageFromSymbolsTemplats(fSymbols, frenchEtymology)) + etymonFiller
                    + uriEncode(word.get().toString()), Etymon);
        etymologyBox.add(etymology, LemonEtyOnt.etymon, etymon);

        etymologyBox.add(etymon, RDFS.label, word.get().toString(), lang);
        etymologyBox.add(etymon, RDFS.seeAlso, WIKT + uriEncode(wiktionaryPageName));
        etymologyBox.add(etymon, LemonEtyOnt.isEtymonOf, etymology);
        log.trace("language{} {} {}", getLanguageFromSymbolsTemplats(fSymbols, frenchEtymology),
            word.get(), etymon.getURI());
      }
    }

    // construction des etyLinks
    int etyLinkCounter = 1;
    Resource firstLink = etymologyBox.getResource(
        getPrefix(lang) + uriEncode(wiktionaryPageName) + etymologyFiller);

    FSymbols firstSym = frenchEtymology.linkedTemplates.getFirst();
    Optional<WikiText.WikiContent> firstWord = getWordFromFSymbolsTemplates(firstSym,
        frenchEtymology);

    Resource prev;
    if (firstWord.isPresent()) {
      prev = etymologyBox.getResource(
          getPrefix(getLanguageFromSymbolsTemplats(firstSym, frenchEtymology)) +
              etymonFiller + uriEncode(firstWord.get().toString()));

      Resource etyLink1 = etymologyBox.createResource(
          getPrefix(lang) + etyLinkFiller + etyLinkCounter, EtyLink);

      etymologyBox.add(firstLink, LemonEtyOnt.startingLink, etyLink1);
      etymologyBox.add(firstLink, LemonEtyOnt.hasEtyLink, etyLink1);
      etymologyBox.add(etyLink1, LemonEtyOnt.etyLinkType, firstSym.relationShip.toLowerCase());
      etymologyBox.add(etyLink1, LemonEtyOnt.etySource, etymologyBox.getResource(
          getPrefix(lang) + uriEncode(wiktionaryPageName) + etymologyFiller));
      etymologyBox.add(etyLink1, LemonEtyOnt.etyTarget, prev);

      for (int i = 1; i < frenchEtymology.linkedTemplates.size(); i++) {

        FSymbols currentSym = frenchEtymology.linkedTemplates.get(i);
        Optional<WikiText.WikiContent> currentWord = getWordFromFSymbolsTemplates(currentSym,
            frenchEtymology);
        Resource current = null;
        if (currentWord.isPresent()) {
          current = etymologyBox.getResource(
              getPrefix(getLanguageFromSymbolsTemplats(currentSym, frenchEtymology)) +
                  etymonFiller + uriEncode(currentWord.get().toString()));

          // safety: prevent self-loop
          assert prev != null;
          if (prev.equals(current)) {
            continue;
          }

          etyLinkCounter++;

          Resource etyLink = etymologyBox.createResource(
              getPrefix(lang) + etyLinkFiller + etyLinkCounter, EtyLink);

          etymologyBox.add(firstLink, LemonEtyOnt.hasEtyLink, etyLink);
          etymologyBox.add(etyLink, LemonEtyOnt.etyLinkType, currentSym.relationShip);
          etymologyBox.add(etyLink, LemonEtyOnt.etySource, prev);
          etymologyBox.add(etyLink, LemonEtyOnt.etyTarget, current);

          prev = current;
        }
      }
    }
  }


  /**
   * @param fSymbols
   * @return the word that is described in the template
   */
  private Optional<WikiText.WikiContent> getWordFromFSymbolsTemplates(FSymbols fSymbols,
      FrenchEtymology frenchEtymology) {
    return frenchEtymology.getWordFromTemplateList(fSymbols.templates);
  }

  /**
   * @param fSymbols
   * @return the language that is described in the template
   */
  private String getLanguageFromSymbolsTemplats(FSymbols fSymbols,
      FrenchEtymology frenchEtymology) {
    String lang = null;
      if ((lang = frenchEtymology.getLanguageFromTemplateList(fSymbols.templates)) != null) {
          return lang;
      }
    return "fra";
  }


}

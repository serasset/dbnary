package org.getalp.dbnary.fra;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.getalp.LangTools;
import org.getalp.dbnary.DBnaryOnt;
import org.getalp.dbnary.ExtractionFeature;
import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.OntolexBasedRDFDataHandler;
import org.getalp.dbnary.OntolexOnt;
import org.getalp.model.ontolex.LexicalForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by serasset on 17/09/14.
 */
public class WiktionaryDataHandler extends OntolexBasedRDFDataHandler {

  private Logger log = LoggerFactory.getLogger(WiktionaryDataHandler.class);

  private HashMap<Pair<String, String>, Set<LexicalForm>> heldBackOtherForms =
      new HashMap<>();

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

  public void registerInflection(LexicalForm form,
      String onLexicalEntry, String languageCode, String pos) {

    Resource posResource = posResource(pos);

    // First, we store the other form for all the existing entries
    Resource page = getPageResource(onLexicalEntry, true);

    page.listProperties(DBnaryOnt.describes).toList().stream()
        .map(Statement::getResource)
        .filter(r -> aBox.contains(r, LexinfoOnt.partOfSpeech, posResource))
        .forEach(form::attachTo);

    // Second, we store the other form for future possible matching entries
    Pair<String, String> key =
        new ImmutablePair<>(onLexicalEntry, pos);


    Set<LexicalForm> otherForms =
        heldBackOtherForms.computeIfAbsent(key, k -> new HashSet<>());

    otherForms.add(form);
  }

}

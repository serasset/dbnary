package org.getalp.dbnary.fra;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
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

  private final Logger log = LoggerFactory.getLogger(WiktionaryDataHandler.class);

  // A entry -> pos -> set of lexical forms hashmap used to store the inflected form which have
  // to be registered chen the main lexical entry is processed.
  private final HashMap<String, HashMap<String, Set<LexicalForm>>> heldBackOtherForms =
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
    currentSenseNumber = 0;
    currentSubSenseNumber = 0;
    initializeLexicalEntry(pos, posResource(pat), typeR);
    Model morphoBox = getFeatureBox(ExtractionFeature.MORPHOLOGY);
    if (null != morphoBox) {
      String heldBackKey = currentPage.getName() + "___/___" + shortSectionLanguageCode;
      HashMap<String, Set<LexicalForm>> pos2forms =
          heldBackOtherForms.getOrDefault(heldBackKey, new HashMap<>());
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

    String heldBackKey = onLexicalEntry + "___/___"+ languageCode;
    HashMap<String, Set<LexicalForm>> pos2forms =
        heldBackOtherForms.computeIfAbsent(heldBackKey, k -> new HashMap<>());
    Set<LexicalForm> otherForms = pos2forms.computeIfAbsent(pos, k -> new HashSet<>());

    otherForms.add(form);
  }

  @Override
  public void initializePageExtraction(String wiktionaryPageName) {
    super.initializePageExtraction(wiktionaryPageName);
  }


  @Override
  public void finalizePageExtraction() {
    // Remove all inflections related to the current page as they cannot be attach to
    // another page anymore.
    heldBackOtherForms.remove(currentPagename());
    super.finalizePageExtraction();
  }

}

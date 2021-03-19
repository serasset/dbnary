package org.getalp.dbnary.fra;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.HashSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.getalp.dbnary.DBnaryOnt;
import org.getalp.dbnary.ExtractionFeature;
import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.OntolexBasedRDFDataHandler;
import org.getalp.dbnary.OntolexOnt;
import org.getalp.dbnary.PronunciationPair;
import org.getalp.dbnary.PropertyObjectPair;
import org.getalp.dbnary.morphology.InflectionScheme;
import org.getalp.ontolex.model.LexicalForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by serasset on 17/09/14.
 */
public class WiktionaryDataHandler extends OntolexBasedRDFDataHandler {

  private Logger log = LoggerFactory.getLogger(WiktionaryDataHandler.class);

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
  public void addPartOfSpeech(String pos) {
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
    addPartOfSpeech(pos, posResource(pat), typeR);
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

}

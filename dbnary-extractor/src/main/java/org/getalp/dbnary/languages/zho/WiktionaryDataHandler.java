package org.getalp.dbnary.languages.zho;

import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.OntolexOnt;
import org.getalp.dbnary.languages.OntolexBasedRDFDataHandler;
import org.getalp.dbnary.wiki.WikiPatterns;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;

public class WiktionaryDataHandler extends OntolexBasedRDFDataHandler {
  private Logger log =
      LoggerFactory.getLogger(org.getalp.dbnary.languages.eng.WiktionaryDataHandler.class);
  static {
    // Chinese
    posAndTypeValueMap.put("动詞", new PosAndType(LexinfoOnt.verb, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("名詞", new PosAndType(LexinfoOnt.noun, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("名词", new PosAndType(LexinfoOnt.noun, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("釋義", new PosAndType(LexinfoOnt.noun, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("副詞", new PosAndType(LexinfoOnt.adverb, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("专有名词", new PosAndType(LexinfoOnt.properNoun, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("專有名詞", new PosAndType(LexinfoOnt.properNoun, OntolexOnt.LexicalEntry));

    posAndTypeValueMap.put("代词", new PosAndType(LexinfoOnt.pronoun, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("形容词", new PosAndType(LexinfoOnt.adjective, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("谚语",
        new PosAndType(LexinfoOnt.proverb, OntolexOnt.MultiWordExpression));

    posAndTypeValueMap.put("成语", new PosAndType(LexinfoOnt.idiom, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("成語", new PosAndType(LexinfoOnt.idiom, OntolexOnt.LexicalEntry));

    posAndTypeValueMap.put("多义词",
        new PosAndType(LexinfoOnt.expression, OntolexOnt.MultiWordExpression));
    posAndTypeValueMap.put("感嘆詞", new PosAndType(LexinfoOnt.interjection, OntolexOnt.LexicalEntry));
  }

  public WiktionaryDataHandler(String longEditionLanguageCode, String tdbDir) {
    super(longEditionLanguageCode, tdbDir);
  }

  public static String getValidPOS(String head, String pageName) {
    // TODO Check what is meant by the words that are given after the POS.
    // TODO: check if the POS macros are given some args.
    // TODO: treat: ==={{noun}}?{{adverb}}===
    // DONE: some pos (like idiom) may be used as a POS or as a sub section in the entry. => Check
    // the header level.
    // Only keep level 3 headers ? --> No.
    // Heuristic is used: if entry length <= 2 then idiom is not a POS.
    PosAndType pos = null;
    String posKey = null;
    Matcher macro = WikiPatterns.macroPattern.matcher(head);
    if (macro.lookingAt()) { // the section starts by a wiki macro
      posKey = macro.group(1);
    } else {
      String[] h = head.split(":");
      posKey = h[0];
    }
    pos = posAndTypeValueMap.get(posKey);
    if (null != posKey && (posKey.equals("idiom") || posKey.equals("成句"))) {
      // When idiom is found on a 1 or 2 char entry, it is assumed to be a section giving the idioms
      // build from the entry.
      // Otherwise it is believed to be a Part Of Speech.
      if (pageName.length() <= 2) {
        pos = null;
      }
    }

    return (null == pos) ? null : posKey;
  }
}

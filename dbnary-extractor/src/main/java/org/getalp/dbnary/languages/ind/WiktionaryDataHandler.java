package org.getalp.dbnary.languages.ind;


import java.util.regex.Pattern;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.languages.OntolexBasedRDFDataHandler;
import org.getalp.dbnary.OntolexOnt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WiktionaryDataHandler extends OntolexBasedRDFDataHandler {

  private Logger log = LoggerFactory.getLogger(WiktionaryDataHandler.class);

  static {
    posAndTypeValueMap.put("adj", new PosAndType(LexinfoOnt.adjective, LexinfoOnt.Adjective));
    posAndTypeValueMap.put("noun", new PosAndType(LexinfoOnt.noun, LexinfoOnt.Noun));
    posAndTypeValueMap.put("properNoun",
        new PosAndType(LexinfoOnt.properNoun, LexinfoOnt.ProperNoun));
    posAndTypeValueMap.put("verb", new PosAndType(LexinfoOnt.verb, LexinfoOnt.Verb));
    posAndTypeValueMap.put("letter", new PosAndType(LexinfoOnt.letter, LexinfoOnt.Symbol));
    posAndTypeValueMap.put("prep", new PosAndType(LexinfoOnt.preposition, LexinfoOnt.Preposition));
    posAndTypeValueMap.put("pron", new PosAndType(LexinfoOnt.pronoun, LexinfoOnt.Pronoun));
    posAndTypeValueMap.put("inter",
        new PosAndType(LexinfoOnt.interjection, LexinfoOnt.Interjection));
    posAndTypeValueMap.put("abbrev",
        new PosAndType(LexinfoOnt.abbreviation, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("adv", new PosAndType(LexinfoOnt.adverb, LexinfoOnt.Adverb));
    posAndTypeValueMap.put("contraction",
        new PosAndType(LexinfoOnt.contraction, OntolexOnt.LexicalEntry));
    posAndTypeValueMap.put("conj", new PosAndType(LexinfoOnt.conjunction, LexinfoOnt.Conjunction));
    posAndTypeValueMap.put("num", new PosAndType(LexinfoOnt.number, LexinfoOnt.Number));
    posAndTypeValueMap.put("particle", new PosAndType(LexinfoOnt.particle, LexinfoOnt.Particle));
    posAndTypeValueMap.put("art", new PosAndType(LexinfoOnt.article, LexinfoOnt.Article));
    posAndTypeValueMap.put("akronim", new PosAndType(LexinfoOnt.acronym, OntolexOnt.LexicalEntry));
  }

  public WiktionaryDataHandler(String lang, String tdbDir) {
    super(lang, tdbDir);
  }

  public void addExtraPartOfSpeech(String pos) {
    PosAndType pat = posAndTypeValueMap.get(pos);
    if (null == pat) {
      log.debug("Unknown Part Of Speech value {} --in-- {}", pos, this.currentPagename());
    }
    if (null != typeResource(pat)) {
      aBox.add(aBox.createStatement(currentLexEntry, RDF.type, typeResource(pat)));
    }
    if (null != posResource(pat)) {
      aBox.add(currentLexEntry, LexinfoOnt.partOfSpeech, posResource(pat));
    }
  }

  public void addExtraPOSInfo(String blockS) {
    blockS = getBlockS(blockS);
    addExtraPartOfSpeech(blockS);
  }

  protected String getBlockS(String s) {
    String res = s;
    switch (s) {
      case "a":
      case "k_s":
      case "k s":
        res = "adj";
        break;
      case "nom":
      case "Nomina":
      case "n":
      case "k_b":
      case "k b":
        res = "noun";
        break;
      case "idproper noun":
        res = "properNoun";
        break;
      case "kan":
      case "k_k":
      case "k k":
      case "v":
      case "lihat v":
        res = "verb";
        break;
      case "Singkatan":
      case "singkatan":
        res = "abbrev";
        break;
      case "huruf":
        res = "letter";
        break;
      case "k_d":
      case "k d":
        res = "prep";
        break;
      case "k_kt":
      case "k kt":
        res = "adv";
        break;
      case "kontraksi":
        res = "contraction";
        break;
      case "kon":
      case "k_sb":
      case "k sb":
        res = "conj";
        break;
      case "p":
      case "part":
        res = "particle";
        break;
      case "k_bl":
      case "k bl":
        res = "num";
        break;
      case "k_g":
      case "k g":
        res = "pron";
        break;
      case "adj":
      case "prep":
      case "verb":
      case "art":
      case "num":
      case "akronim":
      case "adv":
      case "inter":
      case "pron": // pronoun
        res = s;
        break;
      default:
        log.debug("Unknown blocName value {} --in-- {}", s, this.currentPagename());
    }
    return res;
  }

  public void addTranslation(String lang, Resource gloss, String usage, String word) {
    if (currentLexEntry == null) {
      initializeLexicalEntry("none");
    }
    registerTranslation(lang, gloss, usage, word);
  }

  private final static Pattern CONTROL_CHAR = Pattern.compile("\\p{Cntrl}");

  public void addNewDefinition(String def, String sense) {
    if (currentLexEntry == null) {
      initializeLexicalEntry("none");
    }
    // Many control chars a present in definitions. Just get rid of them
    def = CONTROL_CHAR.matcher(def).replaceAll("");
    registerNewDefinition(def, sense);
  }

}

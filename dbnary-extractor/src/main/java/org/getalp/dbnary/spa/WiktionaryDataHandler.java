package org.getalp.dbnary.spa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.getalp.dbnary.DBnaryOnt;
import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.OntolexBasedRDFDataHandler;
import org.getalp.dbnary.OntolexOnt;
import org.getalp.dbnary.commons.PostTranslationDataHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by serasset on 17/09/14.
 */
public class WiktionaryDataHandler extends PostTranslationDataHandler {

  private Logger log = LoggerFactory.getLogger(WiktionaryDataHandler.class);

  public WiktionaryDataHandler(String lang, String tdbDir) {
    super(lang, tdbDir);
  }

  @Override
  public void initializeLexicalEntry(String pos) {
    if (pos.startsWith("{{")) {
      pos = pos.substring(2).trim();
    }
    pos = pos.split("\\|")[0];

    PosAndType pat = null;
    String spos = pos;
    // TODO : handle locucions

    if (pos.startsWith("adjetivo cardinal")) {
      pat = new PosAndType(LexinfoOnt.adjective, OntolexOnt.Word);
    } else if (pos.startsWith("adjetivo ordinal")) {
      pat = new PosAndType(LexinfoOnt.ordinalAdjective, OntolexOnt.Word);
    } else if (pos.startsWith("adjetivo posesivo")) {
      pat = new PosAndType(LexinfoOnt.possessiveAdjective, OntolexOnt.Word);
    } else if (pos.startsWith("adjetivo")) {
      pat = new PosAndType(LexinfoOnt.adjective, OntolexOnt.Word);
    } else if (pos.startsWith("sustantivo propio")) {
      pat = new PosAndType(LexinfoOnt.properNoun, OntolexOnt.Word);
    } else if (pos.startsWith("sustantivo femenino y masculino")) {
      pat = new PosAndType(LexinfoOnt.noun, OntolexOnt.Word);
      // TODO: add proper gender
    } else if (pos.startsWith("sustantivo femenino")) {
      pat = new PosAndType(LexinfoOnt.noun, OntolexOnt.Word);
      // TODO: add proper gender
    } else if (pos.startsWith("sustantivo masculino")) {
      pat = new PosAndType(LexinfoOnt.noun, OntolexOnt.Word);
      // TODO: add proper gender
    } else if (pos.startsWith("sustantivo")) {
      pat = new PosAndType(LexinfoOnt.noun, OntolexOnt.Word);
    } else if (pos.startsWith("adverbio")) {
      pat = new PosAndType(LexinfoOnt.adverb, OntolexOnt.Word);
    } else if (pos.startsWith("verbo transitivo")) {
      pat = new PosAndType(LexinfoOnt.verb, OntolexOnt.Word);
      // TODO: add proper syntactive frame
    } else if (pos.startsWith("verbo intransitivo")) {
      pat = new PosAndType(LexinfoOnt.verb, OntolexOnt.Word);
      // TODO: add proper syntactive frame
    } else if (pos.startsWith("verbo pronominal")) {
      pat = new PosAndType(LexinfoOnt.verb, OntolexOnt.Word);
      // TODO: add proper syntactive frame
    } else if (pos.startsWith("verbo impersonal")) {
      pat = new PosAndType(LexinfoOnt.verb, OntolexOnt.Word);
      // TODO: add proper syntactive frame
    } else if (pos.startsWith("verbo auxiliar")) {
      pat = new PosAndType(LexinfoOnt.verb, OntolexOnt.Word);
      // TODO: add proper syntactive frame
    } else if (pos.startsWith("verbo")) {
      pat = new PosAndType(LexinfoOnt.verb, OntolexOnt.Word);
    }

    // TODO handle extra information (genre, ...) from pos
    log.debug("Handling POS String: {} --> {} || in {}", pos, posResource(pat),
        currentPage.getName());
    // PosAndType pat = posAndTypeValueMap.get(pos);
    Resource typeR = typeResource(pat);
    initializeLexicalEntry(spos, posResource(pat), typeR);
  }

  private final static String posPatternString = "(?:verbo|sustantivo|adjetivo|adverbio)";
  private final static String glossWithPosValue = "(?:^\\s*(?:como\\s+)?(" + posPatternString + ")\\s*$|"
      + "^.*\\((" + posPatternString + ")\\)\\s*$|"
      + "^\\s*(" + posPatternString + "):.*$)";
  private Pattern glossWithPossPattern = Pattern.compile(glossWithPosValue);
  private Matcher glossWithPos = glossWithPossPattern.matcher("");

  @Override
  protected List<Resource> getLexicalEntryUsingGloss(Resource structuredGloss) {
    ArrayList<Resource> res = new ArrayList<>();
    // TODO: Should I take the sense Number into account, as it should be correctly processed by
    //  the extractor ?
    Statement s = structuredGloss.getProperty(RDF.value);
    if (null == s) {
      return res;
    }
    String gloss = s.getString();
    if (null == gloss) {
      return res;
    }
    glossWithPos.reset(gloss.trim());
    if (glossWithPos.matches()) {
      String pos = Stream.of(glossWithPos.group(1), glossWithPos.group(2), glossWithPos.group(3)).filter(
          Objects::nonNull).findFirst().orElse(null);
      addAllResourceOfPoS(res, getPosResource(pos));
    }
    return res;
  }

  private Resource getPosResource(String pos) {
    if ("verbo".equals(pos)) return LexinfoOnt.verb;
    else if ("sustantivo".equals(pos)) return LexinfoOnt.noun;
    else if ("adjetivo".equals(pos)) return LexinfoOnt.adjective;
    else if ("adverbio".equals(pos)) return LexinfoOnt.adverb;
    else return null;
  }

}

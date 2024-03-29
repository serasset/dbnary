package org.getalp.dbnary.languages.zho;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.getalp.dbnary.languages.AbstractWiktionaryExtractor;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.wiki.WikiPatterns;
import org.getalp.dbnary.wiki.WikiTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChineseRelatedWordsExtractor {


  private Logger log = LoggerFactory.getLogger(ChineseRelatedWordsExtractor.class);

  private IWiktionaryDataHandler delegate;


  public ChineseRelatedWordsExtractor(IWiktionaryDataHandler we) {
    this(we, (WiktionaryPageSource) null);
  }

  public ChineseRelatedWordsExtractor(IWiktionaryDataHandler we, WiktionaryPageSource wi) {
    // super(wi, locale, imageBaseURL, linkBaseURL);
    this.delegate = we;
  }


  public void parseRelatedWords(String relatedWords) {
    // parseRelatedWords(relatedWords);

  }


  protected final static String carPatternString;
  protected final static String macroOrLinkOrcarPatternString;

  static {
    // les caractères visible
    carPatternString = "(.)";

    // We should suppress multiline xml comments even if macros or line are to be on a single line.
    macroOrLinkOrcarPatternString = "(?:" + WikiPatterns.macroPatternString + ")|(?:"
        + WikiPatterns.linkPatternString + ")|(?:" + "(:*\\*)" // sub list
        + ")|(?:" + "^;([^:\\n\\r]*)" // Term definition
        + ")|(?:" + carPatternString + ")";
  }


  protected final static Pattern macroOrLinkOrcarPattern;
  protected final static Pattern carPattern;

  static {
    carPattern = Pattern.compile(carPatternString);
    macroOrLinkOrcarPattern =
        Pattern.compile(macroOrLinkOrcarPatternString, Pattern.DOTALL + Pattern.MULTILINE);
  }


  protected final static HashMap<String, String> relMarkerToRelName;

  static {
    relMarkerToRelName = new HashMap<String, String>(20);
    relMarkerToRelName.put("syn", "syn");
    relMarkerToRelName.put("ant", "ant");
    relMarkerToRelName.put("hypo", "hypo");
    relMarkerToRelName.put("hyper", "hyper");
    relMarkerToRelName.put("同义词", "syn");
    relMarkerToRelName.put("反义词", "ant");
    relMarkerToRelName.put("下位語", "hypo");
    relMarkerToRelName.put("上位語", "hyper");
    relMarkerToRelName.put("另一种表示", "alt");

  }


  protected static final int INIT = 1;
  protected static final int RELATION = 2;
  protected static final int VALUES = 3;

  private void extractRelatedWords(String relatedWords) {

    Matcher macroOrLinkOrcarMatcher = macroOrLinkOrcarPattern.matcher(relatedWords);
    int ETAT = INIT;

    String currentGlose = null;
    String currentNym = null, word = "";
    String usage = "";
    String currentRelation = "";

    while (macroOrLinkOrcarMatcher.find()) {

      String macro = macroOrLinkOrcarMatcher.group(1);
      String link = macroOrLinkOrcarMatcher.group(3);
      String star = macroOrLinkOrcarMatcher.group(5);
      String term = macroOrLinkOrcarMatcher.group(6);
      String car = macroOrLinkOrcarMatcher.group(7);

      switch (ETAT) {

        case INIT:
          if (macro != null) {
            log.debug("RELWORDS: Got {} macro while in INIT state. for page: {}", macro,
                this.delegate.currentPagename());
          } else if (link != null) {
            log.debug("RELWORDS: Unexpected link {} while in INIT state. for page: {}", link,
                this.delegate.currentPagename());
          } else if (star != null) {
            ETAT = RELATION;
          } else if (term != null) {
            currentGlose = term;
          } else if (car != null) {
            switch (car) {
              case ":":
                log.debug("Skipping ':' while in INIT state.");
                break;
              case "\n":
              case "\r":

                break;
              case ",":
                log.debug("Skipping ',' while in INIT state.");
                break;
              default:
                log.debug("Skipping {} while in INIT state.", car);
                break;
            }
          }

          break;

        case RELATION:
          if (macro != null) {
            currentRelation = macro;
          } else if (link != null) {
            // We have a link while we try to get a relation. It means that the link poits to a
            // related word for which the relation is not specified.
            // should we keep these words with an un-specified relation ?

          } else if (star != null) {

          } else if (term != null) {
            currentGlose = term;
            currentRelation = "";
            word = "";
            usage = "";
            ETAT = INIT;
          } else if (car != null) {
            if (car.equals(":")) {
              currentNym = currentRelation.trim();
              currentNym = AbstractWiktionaryExtractor.stripParentheses(currentNym);
              currentNym = relMarkerToRelName.get(currentNym);
              if (null == currentNym) {
                log.debug("RELWORDS: Unknown relation: {} in page {}", currentRelation,
                    this.delegate.currentPagename());
              }
              currentRelation = "";
              ETAT = VALUES;
            } else if (car.equals("\n") || car.equals("\r")) {

            } else if (car.equals(",")) {

            } else {
              currentRelation = currentRelation + car;
            }
          }

          break;
        case VALUES:
          if (macro != null) {
            if ("拼音".equals(macro)) {

              Map<String, String> argmap = WikiTool.parseArgs(macroOrLinkOrcarMatcher.group(2));
              word = argmap.get("1");
              argmap.remove("1");
              usage = argmap.toString();
              registerRelation(word, currentNym);
            } else {
              log.debug("RELWORDS: Got macro {} while in VALUE state in page {}", macro,
                  this.delegate.currentPagename());
            }
          } else if (link != null) {
            if (!isAnExternalLink(link)) {
              word = word + " " + ((macroOrLinkOrcarMatcher.group(4) == null) ? link
                  : macroOrLinkOrcarMatcher.group(4));
            }
          } else if (star != null) {

          } else if (term != null) {
            currentGlose = term;
            currentRelation = "";
            word = "";
            usage = "";
            currentNym = null;
            ETAT = INIT;
          } else if (car != null) {
            if (car.equals("\n") || car.equals("\r")) {
              usage = usage.trim();

              registerRelation(word, currentNym);
              currentNym = null;
              usage = "";
              word = "";
              ETAT = INIT;
            } else if (car.equals(",") || car.equals("、")) {
              usage = usage.trim();

              registerRelation(word, currentNym);
              usage = "";
              word = "";
            } else {
              usage = usage + car;
            }
          }
          break;
        default:
          log.error("Unexpected state number:" + ETAT);
          break;
      }


    }
  }


  private void registerRelation(String word, String currentNym) {
    if (word != null && word.length() != 0) {
      if (currentNym != null) {
        if ("alt".equals(currentNym)) {
          this.delegate.registerAlternateSpelling(word);
        } else {
          this.delegate.registerNymRelation(word.trim(), currentNym);
        }
      }
    }
  }

  private boolean isAnExternalLink(String link) {
    // TODO Auto-generated method stub
    return link.startsWith(":");
  }


}

package org.getalp.dbnary.languages.spa;

import info.bliki.wiki.filter.WikipediaParser;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.getalp.LangTools;
import org.getalp.dbnary.languages.AbstractGlossFilter;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.StructuredGloss;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.bliki.DbnaryWikiModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpanishTranslationExtractorWikiModel extends DbnaryWikiModel {

  private IWiktionaryDataHandler delegate;

  private Logger log = LoggerFactory.getLogger(SpanishTranslationExtractorWikiModel.class);
  private AbstractGlossFilter glossFilter;

  public SpanishTranslationExtractorWikiModel(IWiktionaryDataHandler we, Locale locale,
      String imageBaseURL, String linkBaseURL, AbstractGlossFilter glossFilter) {
    this(we, (WiktionaryPageSource) null, locale, imageBaseURL, linkBaseURL, glossFilter);
  }

  public SpanishTranslationExtractorWikiModel(IWiktionaryDataHandler we, WiktionaryPageSource wi,
      Locale locale, String imageBaseURL, String linkBaseURL, AbstractGlossFilter glossFilter) {
    super(wi, locale, imageBaseURL, linkBaseURL);
    this.delegate = we;
    this.glossFilter = glossFilter;
  }

  public void parseTranslationBlock(String block) {
    initialize();
    if (block == null) {
      return;
    }
    globalGloss = null;
    WikipediaParser.parse(block, this, true, null);
    initialize();
  }


  private final static String senseNumberOrRangeRegExp = "(?:[\\s\\d\\,\\-—–\\?]|&ndash;)+";
  private final static Pattern senseNumberOrRangePattern =
      Pattern.compile(senseNumberOrRangeRegExp);
  private final Matcher senseNumberOrRangeMatcher = senseNumberOrRangePattern.matcher("");
  private static Set<String> gender = new HashSet<>();

  static {
    gender.add("m");
    gender.add("f");
    gender.add("mf");
    gender.add("n");
    gender.add("c");
  }

  private static Set<String> pos = new HashSet<>();

  static {
    pos.add("adj");
    pos.add("adj.");
    pos.add("sust.");
    pos.add("sust");
    pos.add("verb.");
    pos.add("verb");
    pos.add("adj & sust");
    pos.add("adj. & sust.");
    pos.add("adj. y sust.");
    pos.add("sust. y adj.");
    pos.add("sust y adj");
    pos.add("adj y sust");
    pos.add("sust & verb");
    pos.add("sust. & verb.");
    pos.add("sust. y verb.");
    pos.add("verb & sust");
    pos.add("verb. & sust.");
    pos.add("verb. y sust.");
    pos.add("sust y verb");
  }

  StructuredGloss globalGloss = null;

  @Override
  public void setPageName(String pageTitle) {
    super.setPageName(pageTitle);
  }

  @Override
  public void substituteTemplateCall(String templateName, Map<String, String> parameterMap,
      Appendable writer) throws IOException {
    if ("t+".equals(templateName)) {
      // TODO : rank each translation by language
      String lang = LangTools.normalize(parameterMap.get("1"));
      int i = 2;
      String s = null;
      String usage = "", trans = null;
      StructuredGloss currentGloss = null;
      if (parameterMap.get("tr") != null) {
        usage = usage + "|tr=" + parameterMap.get("tr");
      }
      while (i != 31 && (s = parameterMap.get("" + i)) != null) {
        s = s.trim();
        senseNumberOrRangeMatcher.reset(s);
        if ("".equals(s)) {
          // Just ignore empty parameters
        } else if (s.matches("[,;]")) {
          if (usage.length() == 0) {
            usage = null;
          } else {
            usage = usage.substring(1);
          }
          if (null != trans) {
            delegate.registerTranslation(lang,
                delegate.createGlossResource(merge(currentGloss, globalGloss)), usage, trans);
          }
          trans = null;
          usage = "";
        } else if (senseNumberOrRangeMatcher.matches()) {
          // the current item is a senseNumber or range
          if (null != trans && null != currentGloss) {
            log.debug("Missing Comma after translation (was {}) when parsing a new gloss in {}",
                trans, delegate.currentPagename());
            if (usage.length() == 0) {
              usage = null;
            } else {
              usage = usage.substring(1);
            }
            delegate.registerTranslation(lang,
                delegate.createGlossResource(merge(currentGloss, globalGloss)), usage, trans);
            trans = null;
            usage = "";
          }
          currentGloss = glossFilter.extractGlossStructure(s);
        } else if (gender.contains(s)) {
          usage = usage + "|" + s;
        } else if ("p".equals(s)) {
          // plural
          usage = usage + "|" + s;
        } else if ("nota".equals(s)) {
          // nota
          i++;
          s = parameterMap.get("" + i);
          usage = usage + "|" + "nota=" + s;
        } else if (pos.contains(s)) {
          // Part Of Speech of target
          usage = usage + "|" + s;
        } else if ("tr".equals(s)) {
          // transcription
          i++;
          s = parameterMap.get("" + i);
          if (null != s && !"".equals(s)) {
            usage = usage + "|" + "tr=" + s;
          }
        } else if ("nl".equals(s)) {
          // ?
          i++;
          s = parameterMap.get("" + i);
          usage = usage + "|" + "nl=" + s;
        } else {
          // translation
          if (null != trans) {
            log.debug("Non null translation (was {}) when registering new translation {} in {}",
                trans, s, delegate.currentPagename());
          }
          trans = s;
        }
        i++;
      }
      if (null != trans) {
        if (usage.length() == 0) {
          usage = null;
        } else {
          usage = usage.substring(1);
        }
        delegate.registerTranslation(lang,
            delegate.createGlossResource(merge(currentGloss, globalGloss)), usage, trans);
      }
    } else if ("trad-arriba".equals(templateName)) {
      // TODO : extract the parameter (gloss and POS specification)
      if (log.isTraceEnabled() && !parameterMap.isEmpty()) {
        log.trace("trad-arriba with params:" + parameterMap + "||" + this.getPageName());
      }
      if (!parameterMap.isEmpty()) {
        String globalGlossValue = parameterMap.get("1");
        globalGloss = glossFilter.extractGlossStructure(globalGlossValue);
      } else {
        globalGloss = null;
      }
    } else if ("trad-centro".equals(templateName)) {
      // nop
    } else if ("trad-abajo".equals(templateName)) {
      globalGloss = null;
    } else if ("trad".equals(templateName)) {
      // Obsolete template that show a single translation
      String lang = parameterMap.get("1");
      String trans = parameterMap.get("2");
      String gloss = parameterMap.get("3");
      senseNumberOrRangeMatcher.reset(gloss);
      if (null != trans) {
        delegate.registerTranslation(lang,
            delegate.createGlossResource(
                merge(glossFilter.extractGlossStructure(gloss), globalGloss)), "", trans);
      }
    } else if ("l".equals(templateName)) {
      // Catch l template and expand it correctly as the template is now expanded before the
      // enclosing template
      int i = 2;
      StringBuffer text = new StringBuffer();
      String s;
      while (i <= 31 && (s = parameterMap.get("" + i)) != null) {
        s = s.trim();
        if (",".equals(s)) {
          text.append(",");
          // ignore next parameter which is the language
          i++;
        } else {
          text.append(s);
        }
        i++;
      }
      writer.append(text);
    } else {
      log.debug("Called template: {} while parsing translations of: {}", templateName,
          this.getPageName());
      // Just ignore the other template calls (uncomment to expand the template calls).
      // super.substituteTemplateCall(templateName, parameterMap, writer);
    }
  }

  private StructuredGloss merge(StructuredGloss localGloss, StructuredGloss globalGloss) {
    if (null == localGloss)
      return globalGloss;
    if (null == globalGloss)
      return localGloss;

    StructuredGloss result = new StructuredGloss();
    result.setSenseNumber(localGloss.getSenseNumber());
    if (null == localGloss.getSenseNumber())
      result.setSenseNumber(globalGloss.getSenseNumber());
    else {
      String localGlossValue = localGloss.getSenseNumber();
      String globalGlossValue = globalGloss.getSenseNumber();
      if (null != globalGlossValue && !localGlossValue.equals(globalGloss.getSenseNumber()))
        log.debug("incompatible senseNumbers : [" + localGloss.getSenseNumber() + "] vs ["
            + globalGloss.getSenseNumber() + "] in: " + this.getPageName());
    }

    result.setGloss(localGloss.getGloss());
    if (null == localGloss.getGloss())
      result.setGloss(globalGloss.getGloss());
    else if (null != globalGloss.getGloss())
      result.setGloss(globalGloss.getGloss() + "|" + localGloss.getGloss());
    return result;
  }
}

package org.getalp.dbnary.rus;

import info.bliki.wiki.filter.WikipediaParser;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.jena.rdf.model.Resource;
import org.getalp.LangTools;
import org.getalp.dbnary.AbstractGlossFilter;
import org.getalp.dbnary.IWiktionaryDataHandler;
import org.getalp.dbnary.WiktionaryIndex;
import org.getalp.dbnary.bliki.DbnaryWikiModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RussianTranslationExtractorWikiModel extends DbnaryWikiModel {

  private int rank;

  // static Set<String> ignoredTemplates = new TreeSet<String>();
  // static {
  // ignoredTemplates.add("Wikipedia");
  // ignoredTemplates.add("Incorrect");
  // }

  private IWiktionaryDataHandler delegate;

  private Logger log = LoggerFactory.getLogger(RussianTranslationExtractorWikiModel.class);
  private AbstractGlossFilter glossFilter;

  public RussianTranslationExtractorWikiModel(IWiktionaryDataHandler we, Locale locale,
      String imageBaseURL, String linkBaseURL, AbstractGlossFilter glossFilter) {
    this(we, (WiktionaryIndex) null, locale, imageBaseURL, linkBaseURL, glossFilter);
  }

  public RussianTranslationExtractorWikiModel(IWiktionaryDataHandler we, WiktionaryIndex wi,
      Locale locale, String imageBaseURL, String linkBaseURL, AbstractGlossFilter glossFilter) {
    super(wi, locale, imageBaseURL, linkBaseURL);
    this.delegate = we;
    this.glossFilter = glossFilter;
    this.rank = 1;
  }

  public void parseTranslationBlock(String block) {
    initialize();
    this.rank = 1;
    if (block == null) {
      return;
    }
    WikipediaParser.parse(block, this, true, null);
    initialize();
  }


  @Override
  public void substituteTemplateCall(String templateName, Map<String, String> parameterMap,
      Appendable writer) throws IOException {
    if ("перев-блок".equals(templateName)) {
      // This is a translation block
      // System.err.println("Template call to translation block");
      // System.err.println("Map is: " + parameterMap.toString());
      String g = parameterMap.get("1");
      Resource gloss = null;
      if (null != g) {
        g = g.trim();
        gloss = delegate.createGlossResource(glossFilter.extractGlossStructure(g), rank++);
      }
      for (Entry<String, String> kv : parameterMap.entrySet()) {
        if ("1".equals(kv.getKey())) {
          continue;
        }
        extractTranslations(gloss, LangTools.normalize(kv.getKey()), kv.getValue());
      }
    } else if ("помета".equals(templateName)) {
      writer.append("(").append(parameterMap.get("1")).append(")");
    } else {
      // Just ignore the other template calls (uncomment to expand the template calls).
      // super.substituteTemplateCall(templateName, parameterMap, writer);
      // As template calls are expanded BEFORE (since gwtwiki-3.20-SNAPSHOT) enclosing template,
      // we now expand by the source code to restore previous behaviour
      log.debug("Called macro: {} when expanding translation block in {}.", templateName,
          this.getPageName());
      writer.append("{{").append(templateName).append("}}"); // TODO: reconstruct template with all
      // args
    }
  }

  static Pattern parens = Pattern.compile("\\(([^\\)]*)\\)");
  static Pattern scripts = Pattern.compile("[^\\[\\]:,;]*:\\s*");

  private void extractTranslations(Resource gloss, String lang, String value) {
    // First black out commas that appear inside a pair of parenthesis
    // TODO: Keep usage information that may be found as a prefix: e.g. "de=несов.: [[verwenden]],
    // [[anwenden]], [[einsetzen]]; сов.: [[aufbrauchen]]"
    Matcher scriptMatcher = scripts.matcher(value);
    value = scriptMatcher.replaceAll("");
    value = blackoutCommas(value);
    String translations[] = value.split("[,;]");
    for (String translation : translations) {
      extractTranslation(gloss, lang, translation);
    }
  }

  private String blackoutCommas(String value) {
    Matcher m = parens.matcher(value);
    StringBuffer sb = new StringBuffer((int) (value.length() * 1.4));
    String inParens;
    while (m.find()) {
      inParens = m.group(1);
      inParens = inParens.replaceAll(";", "@@SEMICOLON@@");
      inParens = inParens.replaceAll(",", "@@COMMA@@");
      m.appendReplacement(sb, "(" + inParens + ")");
    }
    m.appendTail(sb);
    return sb.toString();
  }

  private String restoreCommas(String value) {
    value = value.replaceAll("@@SEMICOLON@@", ";");
    value = value.replaceAll("@@COMMA@@", ",");
    return value;
  }

  static Pattern linkPattern = Pattern.compile("\\[\\[([^\\]]*)\\]\\]");
  static Pattern linkWithTargetPattern = Pattern.compile("\\[\\[[^\\|]+\\|([^\\]]*)\\]\\]");
  static Pattern macroPattern = Pattern.compile("\\{\\{([^\\}]*)\\}\\}");

  private void extractTranslation(Resource gloss, String lang, String trans) {
    trans = restoreCommas(trans);
    Matcher macros = macroPattern.matcher(trans);
    String word = macros.replaceAll(""); // TODO: usages are now in macros
    Matcher linksWithPattern = linkWithTargetPattern.matcher(word);
    word = linksWithPattern.replaceAll("$1").trim();
    Matcher links = linkPattern.matcher(word);
    word = links.replaceAll("$1").trim();
    StringBuffer usage = new StringBuffer();
    StringBuffer w = new StringBuffer();
    Matcher m = parens.matcher(word);
    while (m.find()) {
      usage.append(m.group(0));
      usage.append(" ");
      m.appendReplacement(w, " ");
    }
    m.appendTail(w);
    word = w.toString().trim();
    if (usage.length() > 0) {
      usage.deleteCharAt(usage.length() - 1);
    }
    if (word != null && !word.equals("")) {
      delegate.registerTranslation(lang, gloss, usage.toString(), word);
    }
  }

}

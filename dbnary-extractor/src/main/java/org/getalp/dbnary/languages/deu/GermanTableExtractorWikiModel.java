package org.getalp.dbnary.languages.deu;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;

import info.bliki.wiki.template.ITemplateFunction;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.morphology.InflectedFormSet;
import org.getalp.dbnary.wiki.WikiText;
import org.getalp.dbnary.wiki.WikiText.Template;
import org.getalp.dbnary.wiki.WikiText.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class GermanTableExtractorWikiModel extends GermanDBnaryWikiModel {

  protected GermanTableExtractor germanTableExtractor;
  private Logger log = LoggerFactory.getLogger(GermanTableExtractorWikiModel.class);
  protected IWiktionaryDataHandler wdh;

  public GermanTableExtractorWikiModel(WiktionaryPageSource wi, Locale locale, String imageBaseURL,
      String linkBaseURL, IWiktionaryDataHandler wdh, GermanTableExtractor germanTableExtractor) {
    super(wi, locale, imageBaseURL, linkBaseURL);
    this.germanTableExtractor = germanTableExtractor;
    this.wdh = wdh;
  }

  @Override
  public void substituteTemplateCall(String templateName, Map<String, String> parameterMap,
      Appendable writer) throws IOException {
    if ("Flexlink".equals(templateName)) {
      // Just display the link name and drop the link...
      writer.append(parameterMap.get("1"));
    } else if ("Str subrev".equals(templateName)) {
      // subrev & rightc are extensively called and generate a very big Lua overhead
      writer.append(subrev(parameterMap));
    } else if ("Str rightc".equals(templateName)) {
      writer.append(rightc(parameterMap));
    } else if ("Str right".equals(templateName)) {
      writer.append(right(parameterMap));
    } else if ("Str len".equals(templateName)) {
      writer.append(Integer.toString(parameterMap.getOrDefault("1", "").length()));
    } else if ("Str crop".equals(templateName)) {
      writer.append(crop(parameterMap));
    } else if ("Literatur".equals(templateName)) {
      // nop : catch and ignore this template in the context of morphology tables.
    } else {
      // DONE: catch Str subc and Str subrev for drastic extraction time enhancement
      // log.trace("Expanding template call: {}", templateName);
      super.substituteTemplateCall(templateName, parameterMap, writer);
    }
  }

  private String crop(Map<String, String> parameterMap) {
    String s = parameterMap.getOrDefault("1", "");
    int cut = Integer.parseInt(parameterMap.getOrDefault("2", "0"));
    int end = s.length() - cut;
    if (0 <= end && end <= s.length()) {
      return s.substring(0, end);
    } else {
      return "";
    }
  }

  private String right(Map<String, String> parameterMap) {
    String text = parameterMap.getOrDefault("1", "").trim();
    String arg2 = parameterMap.get("2");
    int start = (null == arg2 || "".equals(arg2)) ? 0 : Integer.parseInt(arg2);
    if (start < 0) {
      return "";
    } else {
      return text.substring(start);
    }
  }

  private String rightc(Map<String, String> parameterMap) {
    String text = parameterMap.getOrDefault("1", "").trim();
    int posr = Integer.parseInt(parameterMap.getOrDefault("2", "1"));
    int start = text.length() - posr;
    if (start >= 0) {
      return text.substring(start);
    } else {
      return "";
    }
  }

  private String subrev(Map<String, String> parameterMap) {
    String rightc = rightc(parameterMap);
    int length = Integer.parseInt(parameterMap.getOrDefault("3", "1"));
    if (length <= rightc.length()) {
      return rightc.substring(0, length);
    } else {
      return "";
    }
  }


  Set<String> ignoredTemplates =
      Stream.of("Adjektivdeklination", "Verbkonjugation", "Adverbdeklination")
          .collect(collectingAndThen(toCollection(HashSet::new), Collections::unmodifiableSet));

  protected InflectedFormSet parseTables(String declinationTemplateCall) {

    if (log.isTraceEnabled()) {
      WikiText txt = new WikiText(getPageName(), declinationTemplateCall);
      for (Token token : txt.templatesOnUpperLevel()) {
        Template tmpl = (Template) token;
        if (!ignoredTemplates.contains(tmpl.getName())) {
          log.trace("MORPH template: {}", tmpl.getName());

          String tmplSource = tmpl.toString().replaceAll("\\n", "");
          log.trace("MORPH template call:  {} @ {}", tmplSource, getPageName());
        }
      }
    }

    String htmlCode = expandWikiCode(declinationTemplateCall);
    return germanTableExtractor.parseHTML(htmlCode, getPageName());
  }

  private static ITemplateFunction germanInvoke = new GermanInvoke();

  @Override
  public ITemplateFunction getTemplateFunction(String name) {
    if ("#invoke".equals(name))
      return germanInvoke;
    return getTemplateMap().get(name);
  }

}

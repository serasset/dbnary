package org.getalp.dbnary.languages.fra;

import info.bliki.wiki.filter.PlainTextConverter;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.bliki.DbnaryWikiModel;
import org.getalp.iso639.ISO639_3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FrenchDefinitionExtractorWikiModel extends DbnaryWikiModel {

  private Logger log = LoggerFactory.getLogger(FrenchDefinitionExtractorWikiModel.class);

  // static Set<String> ignoredTemplates = new TreeSet<String>();
  // static {
  // ignoredTemplates.add("Wikipedia");
  // ignoredTemplates.add("Incorrect");
  // }

  private IWiktionaryDataHandler delegate;


  public FrenchDefinitionExtractorWikiModel(IWiktionaryDataHandler we, Locale locale,
      String imageBaseURL, String linkBaseURL) {
    this(we, (WiktionaryPageSource) null, locale, imageBaseURL, linkBaseURL);
  }

  public FrenchDefinitionExtractorWikiModel(IWiktionaryDataHandler we, WiktionaryPageSource wi,
      Locale locale, String imageBaseURL, String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL);
    this.delegate = we;
  }

  public void parseDefinition(String definition, int defLevel) {
    // Render the definition to plain text, while ignoring the example template
    log.trace("extracting definitions in {}", this.getPageName());
    String def = null;
    try {
      def = render(new PlainTextConverter(), definition).trim();
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (null != def && !def.equals("")) {
      delegate.registerNewDefinition(def, defLevel);
    }
  }

  @Override
  public void substituteTemplateCall(String templateName, Map<String, String> parameterMap,
      Appendable writer) throws IOException {
    // Currently just expand the definition to get the full text.
    if (templateName.equals("nom langue") || templateName.endsWith(":nom langue")) {
      // intercept this template as it leads to a very inefficient Lua Script.
      String langCode = parameterMap.get("1").trim();
      String lang = ISO639_3.sharedInstance.getLanguageNameInFrench(langCode);
      if (null != lang) {
        writer.append(lang);
      }
    } else if ("sans balise".equals(templateName) || "sans_balise".equals(templateName)) {
      String t = parameterMap.get("1");
      if (null != t) {
        writer.append(t.replaceAll("<[^\\]]*>", "").replaceAll("'''?", ""));
      }
    } else if ("gsub".equals(templateName)) {
      String s = parameterMap.get("1");
      String pattern = parameterMap.get("2");
      String repl = parameterMap.get("3");
      if ("’".equals(pattern) && "'".equals(repl)) {
        writer.append(s.replaceAll(pattern, repl));
      } else {
        log.debug("gsub {} | {} | {}", parameterMap.get("1"), parameterMap.get("2"),
            parameterMap.get("3"));
        super.substituteTemplateCall(templateName, parameterMap, writer);
      }
    } else if ("str find".equals(templateName) || "str_find".equals(templateName)) {
      String s = parameterMap.get("1");
      String pattern = parameterMap.get("2");
      int i = s.trim().indexOf(pattern);
      if (-1 != i) {
        writer.append("").append(String.valueOf(i + 1));
      }
    } else if (templateName.contains("langues")) {
      // Generates (Linguistique) and registers the entry as a language name. Takes too long (?)
      writer.append("(Linguistique)");
    } else if ("pron".equals(templateName)) {
      // Ignore it as pronunciation is extracted independantly.
    } else {
      super.substituteTemplateCall(templateName, parameterMap, writer);
    }
  }

}

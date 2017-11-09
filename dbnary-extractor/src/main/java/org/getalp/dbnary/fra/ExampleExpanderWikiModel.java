package org.getalp.dbnary.fra;

import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.vocabulary.DCTerms;
import org.getalp.dbnary.WiktionaryIndex;
import org.getalp.dbnary.wiki.ExpandAllWikiModel;
import org.getalp.iso639.ISO639_3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleExpanderWikiModel extends ExpandAllWikiModel {

  static Set<String> ignoredTemplates = new HashSet<>();
  static Logger log = LoggerFactory.getLogger(ExampleExpanderWikiModel.class);

  static {
    ignoredTemplates.add("ébauche-exe");
  }

  private Map<Property, String> context;
  private ExpandAllWikiModel simpleExpander;

  public ExampleExpanderWikiModel(WiktionaryIndex wi, Locale locale, String imageBaseURL,
      String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL);
    simpleExpander = new ExpandAllWikiModel(wi, locale, imageBaseURL, linkBaseURL);
  }

  @Override
  public void setPageName(String pageTitle) {
    super.setPageName(pageTitle);
    simpleExpander.setPageName(pageTitle);
  }

  /**
   * Convert an example wiki code to plain text, while keeping track of all template calls and of
   * context definition (source, etc.).
   *
   * @param definition the wiki code
   * @param templates if not null, the method will add all called templates to the set.
   * @param context if not null, the method will add all contextual relation to the map.
   * @return the converted wiki code
   */
  public String expandExample(String definition, Set<String> templates,
      Map<Property, String> context) {
    // log.trace("extracting examples in {}", this.getPageName());
    this.context = context;
    return expandAll(definition, templates);
  }

  @Override
  public void substituteTemplateCall(String templateName, Map<String, String> parameterMap,
      Appendable writer) throws IOException {
    if (ignoredTemplates.contains(templateName)) {
      // NOP
    } else if ("source".equals(templateName)) {
      if (context != null) {
        String source = simpleExpander.expandAll(parameterMap.get("1"), this.templates);
        context.put(DCTerms.bibliographicCitation, source);
        parameterMap.remove("1");
        if (!parameterMap.isEmpty()) {
          log.debug("Non empty parameter map {} in {}", parameterMap, this.getPageName());
        }
      }
    } else if ("sans balise".equals(templateName) || "sans_balise".equals(templateName)) {
      String t = parameterMap.get("1");
      if (null != t) {
        writer.append(t.replaceAll("<[^\\]]*>", "").replaceAll("'''?", ""));
      }
    } else if (templateName.equals("nom langue") || templateName.endsWith(":nom langue")) {
      // intercept this template as it leads to a very inefficient Lua Script.
      String langCode = parameterMap.get("1").trim();
      String lang = ISO639_3.sharedInstance.getLanguageNameInFrench(langCode);
      if (null != lang) {
        writer.append(lang);
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
    } else {
      log.debug("Caught template call: {} --in-- {}", templateName, this.getPageName());
      super.substituteTemplateCall(templateName, parameterMap, writer);
    }
  }

  @Override
  public void addCategory(String categoryName, String sortKey) {
    log.debug("Called addCategory : " + categoryName);
    super.addCategory(categoryName, sortKey);
  }
}

package org.getalp.dbnary.languages.fra;

import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.bliki.ExpandAllWikiModel;
import org.getalp.iso639.ISO639_3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleExpanderWikiModel extends ExpandAllWikiModel {

  static Set<String> ignoredTemplates = new HashSet<>();
  static Logger log = LoggerFactory.getLogger(ExampleExpanderWikiModel.class);

  static {
    ignoredTemplates.add("ébauche-exe");
  }

  private Map<Property, RDFNode> context;
  private String shortEditionLanguage;
  private String shortSectionLanguage;
  private final ExpandAllWikiModel simpleExpander;

  public ExampleExpanderWikiModel(WiktionaryPageSource wi, Locale locale, String imageBaseURL,
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
      Map<Property, RDFNode> context) {
    return expandExample(definition, templates, context, null, null);
  }

  private static final String NOTE_SPLIT = "///NOTES///";
  public String expandExample(String definition, Set<String> templates,
      Map<Property, RDFNode> context, String shortEditionLanguage, String shortSectionLanguage) {
    // log.trace("extracting examples in {}", this.getPageName());
    this.context = context;
    this.shortEditionLanguage = shortEditionLanguage;
    this.shortSectionLanguage = shortSectionLanguage;
    String exampleText =  expandAll(definition, templates);
    String[] textAndNote = exampleText.split(NOTE_SPLIT);
    if (textAndNote.length > 1) {
      for (int i = 1; i < textAndNote.length; i++) {
        String note;
        if (textAndNote[i] != null && !"".equals(note = textAndNote[i].trim())) {
          context.put(SKOS.note, ResourceFactory.createLangLiteral(note, shortEditionLanguage));
        }
      }
    }
    return textAndNote[0];
  }

  @Override
  public void substituteTemplateCall(String templateName, Map<String, String> parameterMap,
      Appendable writer) throws IOException {
    if (ignoredTemplates.contains(templateName)) {
      // NOP
    } else if ("w".equals(templateName)) {
      String text = parameterMap.get("2");
      if (null == text) {
        text = parameterMap.get("1");
      }
      if (null == text) {
        text = getPageName();
      }
      writer.append(text.trim());
    } else if ("italique".equals(templateName)) {
      String text = parameterMap.get("1");
      if (null != text) {
        writer.append(text.trim());
      }
    } else if ("source".equals(templateName)) {
      if (context != null) {
        context.put(DCTerms.bibliographicCitation,
            rdfNode(parameterMap.get("1"), shortEditionLanguage));
      }
      parameterMap.remove("1");
      if (!parameterMap.isEmpty()) {
        log.trace("source uses unexpected parameters {} in {}", parameterMap, this.getPageName());
      }
    } else if ("sans balise".equals(templateName) || "sans_balise".equals(templateName)) {
      String t = parameterMap.get("1");
      if (null != t) {
        writer.append(t.replaceAll("<[^]]*>", "").replaceAll("'''?", ""));
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
        log.trace("gsub {} | {} | {}", parameterMap.get("1"), parameterMap.get("2"),
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
    } else if ("exemple".equals(templateName)) {
      String example = parameterMap.get("1");
      String lang = parameterMap.getOrDefault("lang", shortSectionLanguage);
      String translation = parameterMap.getOrDefault("2", parameterMap.get("sens"));
      String transcription = parameterMap.getOrDefault("3", parameterMap.get("tr"));
      String source = parameterMap.get("source");
      if (null != example) {
        writer.append(example);
      }
      if (context != null) {
        if (null != source)
          context.put(DCTerms.bibliographicCitation, rdfNode(source, lang));
        if (null != translation)
          context.put(RDF.value, rdfNode(translation, shortEditionLanguage));
        if (null != transcription)
          context.put(RDF.value, rdfNode(transcription, lang + "-Latn"));
      }
      if (parameterMap.get("lien") != null) {
        log.trace("Parameter <<lien>>={} in {}", parameterMap.get("lien"), getPageName());
      }
    } else if ("note".equals(templateName)) {
      writer.append(NOTE_SPLIT);
    } else {
      log.trace("Caught template call: {} --in-- {}", templateName, this.getPageName());
      super.substituteTemplateCall(templateName, parameterMap, writer);
    }
  }

  private Literal rdfNode(String value, String lang) {
    return rdfNode(value, lang, true);
  }

  private Literal rdfNode(String value, String lang, boolean expand) {
    return ResourceFactory.createLangLiteral(expand ? expandString(value) : value, lang);
  }

  private String expandString(String originalSource) {
    // Expand source to properly resolve links and cosmetic markups
    return simpleExpander.expandAll(originalSource, this.templates);
  }

  @Override
  public void addCategory(String categoryName, String sortKey) {
    log.trace("Called addCategory : " + categoryName);
    super.addCategory(categoryName, sortKey);
  }
}

package org.getalp.dbnary.languages.eng;

import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
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
    // ignoredTemplates.add("ébauche-exe");
  }

  private Set<Pair<Property, RDFNode>> context;
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

  private static final String NOTE_SPLIT = "///NOTES///";

  public String expandExample(String definition, Set<String> templates,
      Set<Pair<Property, RDFNode>> context, String shortEditionLanguage,
      String shortSectionLanguage) {
    this.context = context;
    this.shortEditionLanguage = shortEditionLanguage;
    this.shortSectionLanguage = shortSectionLanguage;
    String exampleText = expandAll(definition, templates);
    String[] textAndNote = exampleText.split(NOTE_SPLIT);
    if (textAndNote.length > 1) {
      for (int i = 1; i < textAndNote.length; i++) {
        String note;
        if (textAndNote[i] != null && !"".equals(note = textAndNote[i].trim())) {
          context.add(
              Pair.of(SKOS.note, ResourceFactory.createLangLiteral(note, shortEditionLanguage)));
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
    } else if ("ux".equals(templateName)) {
      // Simple usage example
      if (log.isTraceEnabled()) {
        String langCode = parameterMap.get("1");
        if (!langCode.equalsIgnoreCase(shortSectionLanguage)) {
          log.trace("UX Template: incoherent language code {} (expected {}) [{}]", langCode,
              shortSectionLanguage, getPageName());
        }
        // TODO: handle script code
        String scriptCode = parameterMap.get("sc");;
        if (null != scriptCode)
          log.trace("UX Template: unhanded script code {} [{}]", scriptCode, getPageName());
      }
      String text = parameterMap.get("2");
      String translation = parameterMap.getOrDefault("t",
          parameterMap.getOrDefault("translation", parameterMap.get("2")));
      String transliteration = parameterMap.getOrDefault("tr", parameterMap.get("transliteration"));
      if (context != null) {
        if (null != translation)
          context.add(Pair.of(RDF.value, rdfNode(translation, shortEditionLanguage)));
        if (null != transliteration)
          context.add(Pair.of(RDF.value, rdfNode(transliteration, shortSectionLanguage + "-Latn")));
      }
      writer.append(text.trim());
    } else if ("syn".equals(templateName)) {
      // HANDLE synonyms
    } else {
      log.trace("Template call: {} --in-- {}", templateName, this.getPageName());
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

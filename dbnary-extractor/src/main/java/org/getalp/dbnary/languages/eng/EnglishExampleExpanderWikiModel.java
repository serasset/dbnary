package org.getalp.dbnary.languages.eng;

import info.bliki.htmlcleaner.BaseToken;
import info.bliki.wiki.filter.ITextConverter;
import info.bliki.wiki.filter.PlainTextConverter;
import info.bliki.wiki.filter.WikipediaParser;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnglishExampleExpanderWikiModel extends EnglishWikiModel {

  static Set<String> ignoredTemplates = new HashSet<>();
  static Logger log = LoggerFactory.getLogger(EnglishExampleExpanderWikiModel.class);

  static {
    // ignoredTemplates.add("ébauche-exe");
  }

  private Set<Pair<Property, RDFNode>> context;
  private String shortEditionLanguage;
  private String shortSectionLanguage;
  private final ExpandAllWikiModel simpleExpander;

  public EnglishExampleExpanderWikiModel(WiktionaryPageSource wi, Locale locale,
      String imageBaseURL, String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL);
    simpleExpander = new ExpandAllWikiModel(wi, locale, imageBaseURL, linkBaseURL);
  }

  @Override
  public void setPageName(String pageTitle) {
    super.setPageName(pageTitle);
    simpleExpander.setPageName(pageTitle);
  }

  private static final String PASSAGE_OPEN = "///PASSAGE- ";
  private static final String PASSAGE_CLOSE = " -PASSAGE///";

  public void expandCitation(String definition, Set<Pair<Property, RDFNode>> context,
      String shortEditionLanguage, String shortSectionLanguage) {
    String text = render(definition, context, shortEditionLanguage, shortSectionLanguage);
    if (null != text && text.trim().length() > 0) {
      context.add(Pair.of(DCTerms.bibliographicCitation, rdfNode(text, shortSectionLanguage)));
    }
  }

  public String render(String definition, Set<Pair<Property, RDFNode>> context,
      String shortEditionLanguage, String shortSectionLanguage) {
    this.context = context;
    this.shortEditionLanguage = shortEditionLanguage;
    this.shortSectionLanguage = shortSectionLanguage;
    try {
      return render(new PlainTextConverter(), definition).trim();
    } catch (IOException e) {
      log.error("Error while rendering page.", e);
      // e.printStackTrace();
    }
    return "";
  }

  public void expandExample(String definition, Set<Pair<Property, RDFNode>> context,
      String shortEditionLanguage, String shortSectionLanguage) {
    String text = render(definition, context, shortEditionLanguage, shortSectionLanguage);
    if (null != text && text.trim().length() > 0) {
      context.add(Pair.of(RDF.value, rdfNode(text, shortSectionLanguage)));
    }
  }

  @Override
  public void substituteTemplateCall(String templateName, Map<String, String> parameterMap,
      Appendable writer) throws IOException {
    if (ignoredTemplates.contains(templateName)) {
      // NOP
    } else if ("ux".equals(templateName) || "ux-lite".equals(templateName)) {
      // Simple usage example
      if (log.isTraceEnabled()) {
        String langCode = parameterMap.get("1");
        if (!langCode.equalsIgnoreCase(shortSectionLanguage)) {
          log.trace("UX Template: incoherent language code {} (expected {}) [{}]", langCode,
              shortSectionLanguage, getPageName());
        }
        // TODO: handle script code
        String scriptCode = parameterMap.get("sc");;
        if (null != scriptCode) {
          log.trace("UX Template: unhanded script code {} [{}]", scriptCode, getPageName());
        }
      }
      String text = parameterMap.get("2");
      String translation = parameterMap.getOrDefault("t",
          parameterMap.getOrDefault("translation", parameterMap.get("3")));
      String transliteration = parameterMap.getOrDefault("tr", parameterMap.get("transliteration"));
      if (context != null) {
        if (null != text && text.trim().length() > 0) {
          context.add(Pair.of(RDF.value, rdfNode(text, shortSectionLanguage)));
        }
        if (null != translation) {
          context.add(Pair.of(RDF.value, rdfNode(translation, shortEditionLanguage)));
        }
        if (null != transliteration) {
          context.add(Pair.of(RDF.value, rdfNode(transliteration, shortSectionLanguage + "-Latn")));
        }
      }
    } else if ("syn".equals(templateName)) {
      // HANDLE synonyms
    } else if ("quote-book".equals(templateName) || "quote-journal".equals(templateName)) {
      String passage = parameterMap.getOrDefault("passage", parameterMap.get("text"));
      if (null != passage && !"".equals(passage.trim())) {
        parameterMap.remove("text");
        parameterMap.remove("passage");
      }
      StringBuilder str = new StringBuilder();
      super.substituteTemplateCall(templateName, parameterMap, str);
      if (context != null) {
        if (null != passage && passage.trim().length() > 0) {
          context.add(Pair.of(RDF.value, rdfNode(passage, shortSectionLanguage)));
        }
        String ref = StringUtils.strip(str.toString(), " \t\\x0B\f\n\r:");
        if (ref.length() > 0) {
          context.add(Pair.of(DCTerms.bibliographicCitation, rdfNode(ref, shortEditionLanguage)));
        }
      }
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

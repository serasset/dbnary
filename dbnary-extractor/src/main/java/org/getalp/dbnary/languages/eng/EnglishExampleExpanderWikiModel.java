package org.getalp.dbnary.languages.eng;

import info.bliki.wiki.filter.PlainTextConverter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
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
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.bliki.ExpandAllWikiModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnglishExampleExpanderWikiModel extends EnglishWikiModel {

  static Set<String> ignoredTemplates = new HashSet<>();
  static Logger log = LoggerFactory.getLogger(EnglishExampleExpanderWikiModel.class);

  static {
    ignoredTemplates.add("rfex");
    ignoredTemplates.add("rfquote");
    ignoredTemplates.add("rfquote-sense");
    ignoredTemplates.add("rfquotek");
  }

  private Set<Pair<Property, RDFNode>> context;
  private String shortEditionLanguage;
  private String shortSectionLanguage;
  private final ExpandAllWikiModel simpleExpander;

  private static final Map<String, String> nyms = new HashMap<>();

  static {
    nyms.put("syn", "syn");
    nyms.put("synonyms", "syn");
    nyms.put("syn-lite", "syn");
    nyms.put("antonyms", "ant");
    nyms.put("antonym", "ant");
    nyms.put("ant", "ant");
    nyms.put("hyponyms", "hypo");
    nyms.put("hypo", "hypo");
    nyms.put("hypernyms", "hyper");
    nyms.put("hyper", "hyper");
    nyms.put("holonyms", "holo");
    nyms.put("holo", "holo");
    nyms.put("hol", "holo");
    nyms.put("meronyms", "mero");
    nyms.put("mero", "mero");
    nyms.put("mer", "mero");
    nyms.put("comeronyms", "comero");
    nyms.put("troponyms", "tropo");
    nyms.put("coordinate terms", "cot");
    nyms.put("coordinate_terms", "cot");
    nyms.put("cot", "cot");
  }

  private final IWiktionaryDataHandler wdh;

  public EnglishExampleExpanderWikiModel(WiktionaryPageSource wi, Locale locale,
      String imageBaseURL, String linkBaseURL, IWiktionaryDataHandler wdh) {
    super(wi, locale, imageBaseURL, linkBaseURL);
    this.wdh = wdh;
    simpleExpander = new ExpandAllWikiModel(wi, locale, imageBaseURL, linkBaseURL);
  }

  @Override
  public void setPageName(String pageTitle) {
    super.setPageName(pageTitle);
    simpleExpander.setPageName(pageTitle);
  }

  public void expandCitation(String definition, Set<Pair<Property, RDFNode>> context,
      String shortEditionLanguage, String shortSectionLanguage) {
    String text = render(definition, context, shortEditionLanguage, shortSectionLanguage);
    String textWithoutErrors = text.replaceAll("TemplateParserError:LuaError", "");
    if (!textWithoutErrors.equals(text)) {
      log.debug("LuaError while expanding citation in {}", getPageName());
    }
    if (textWithoutErrors.trim().length() > 0) {
      addNodeToContext(context, DCTerms.bibliographicCitation,
          rdfNode(textWithoutErrors, shortSectionLanguage));
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
      addNodeToContext(context, RDF.value, rdfNode(text, shortSectionLanguage));
    }
  }

  @Override
  public void substituteTemplateCall(String templateName, Map<String, String> parameterMap,
      Appendable writer) throws IOException {
    if (ignoredTemplates.contains(templateName)) {
      // NOP
    } else if ("ux".equals(templateName) || "usex".equals(templateName) || "eg".equals(templateName)
        || "uxi".equals(templateName) || "ux-lite".equals(templateName)
        || "quote".equals(templateName)) {
      // Simple usage example
      if (log.isTraceEnabled()) {
        String langCode = parameterMap.getOrDefault("1", "").trim();
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
          addNodeToContext(context, RDF.value, rdfNode(text, shortSectionLanguage));
        }
        if (null != translation) {
          addNodeToContext(context, RDF.value, rdfNode(translation, shortEditionLanguage));
        }
        if (null != transliteration) {
          addNodeToContext(context, RDF.value,
              rdfNode(transliteration, shortSectionLanguage + "-Latn"));
        }
      }
    } else if (nyms.containsKey(templateName)) {
      // HANDLE synonyms
      // TODO: also take gloss template into account as it gives more info on the nym
      String nym = nyms.get(templateName);
      if (log.isTraceEnabled()) {
        String langCode = parameterMap.getOrDefault("1", "").trim();
        if (!langCode.equalsIgnoreCase(shortSectionLanguage)) {
          log.trace("NYM Template: incoherent language code {} (expected {}) [{}]", langCode,
              shortSectionLanguage, getPageName());
        }
      }
      String val;
      for (int i = 2; (val = parameterMap.get(String.valueOf(i))) != null; i++) {
        if (val.startsWith("Thesaurus:"))
          continue;
        wdh.registerNymRelationOnCurrentSense(val, nym);
      }
    } else if ("seeSynonyms".equals(templateName)) {
      // HANDLE synonyms in an external page
    } else if ("seeCites".equals(templateName) || "seeMoreCites".equals(templateName)
        || "seemoreCites".equals(templateName)) {
      // HANDLE Citations that are given in another page
    } else if ("quote-book".equals(templateName) || "quote-journal".equals(templateName)
        || "quote-text".equals(templateName) || "quote-video game".equals(templateName)
        || "quote-web".equals(templateName)) {
      String passage = parameterMap.getOrDefault("passage", parameterMap.get("text"));
      if (null != passage && !"".equals(passage.trim())) {
        parameterMap.remove("text");
        parameterMap.remove("passage");
      }
      StringBuilder str = new StringBuilder();
      super.substituteTemplateCall(templateName, parameterMap, str);
      if (context != null) {
        if (null != passage && passage.trim().length() > 0) {
          addNodeToContext(context, RDF.value, rdfNode(passage, shortSectionLanguage));
        }
        String ref = StringUtils.strip(str.toString(), " \t\\x0B\f\n\r:");
        if (ref.length() > 0) {
          addNodeToContext(context, DCTerms.bibliographicCitation,
              rdfNode(ref, shortEditionLanguage));
        }
      }
    } else if ("glossary".equals(templateName) || "glink".equals(templateName)) {
      String text = parameterMap.getOrDefault("2", parameterMap.get("1"));
      if (null != text) {
        writer.append(text);
      }
    } else if ("maintenance line".equals(templateName)) {
      // Just ignore maintenance lines
    } else if ("...".equals(templateName)) {
      writer.append(" […] ");
    } else if ("nb...".equals(templateName)) {
      writer.append("\u00A0[…]");
    } else {
      log.trace("Template call: {} --in-- {}", templateName, this.getPageName());
      super.substituteTemplateCall(templateName, parameterMap, writer);
    }
  }

  private Literal rdfNode(String value, String lang) {
    return rdfNode(value, lang, true);
  }

  private Literal rdfNode(String value, String lang, boolean expand) {
    String val = expand ? expandString(value) : value;
    if (null == val || val.trim().length() == 0)
      return null;
    return ResourceFactory.createLangLiteral(val, lang);
  }

  private String expandString(String originalSource) {
    // Expand source to properly resolve links and cosmetic markups
    String expanded = simpleExpander.expandAll(originalSource, this.templates);
    return expanded.replaceAll("TemplateParserError:LuaError", "");
  }

  @Override
  public void addCategory(String categoryName, String sortKey) {
    log.trace("Called addCategory : " + categoryName);
    super.addCategory(categoryName, sortKey);
  }

  private void addNodeToContext(Set<Pair<Property, RDFNode>> context, Property prop,
      Literal rdfNode) {
    if (null != rdfNode)
      context.add(Pair.of(prop, rdfNode));
  }
}

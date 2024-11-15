package org.getalp.dbnary.languages.swe;

import info.bliki.wiki.filter.ParsedPageName;
import info.bliki.wiki.model.WikiModelContentException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.bliki.ExpandAllWikiModel;
import org.getalp.dbnary.tools.TemplateTracker;
import org.getalp.dbnary.wiki.WikiText;
import org.getalp.dbnary.wiki.WikiText.InternalLink;
import org.getalp.dbnary.wiki.WikiText.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO inheriting SwedishWikiModel ?
public class ExampleExpanderWikiModel extends ExpandAllWikiModel {

  static Logger log = LoggerFactory.getLogger(ExampleExpanderWikiModel.class);
  static Set<String> ignoredTemplates = new HashSet<>();

  static {
    ignoredTemplates.add("stavning"); // spelling
    ignoredTemplates.add("homofoner"); // homophones
    ignoredTemplates.add("jämför"); // other similar concepts
    ignoredTemplates.add("seäven"); // other relevant concepts
    ignoredTemplates.add("diverse"); // other misc information
    ignoredTemplates.add("anagram"); // anagrams
    ignoredTemplates.add("konstr"); // common usage (TODO: use as examples ?)
    ignoredTemplates.add("etymologi"); // etymology (TODO: extract ?)
    ignoredTemplates.add("etymologi"); // etymology (TODO: extract ?)
    ignoredTemplates.add("grammatik"); // grammatical information (TODO: extract ?)
    ignoredTemplates.add("?"); // lexicographer note about lexical information
  }

  private Set<Pair<Property, RDFNode>> context;
  private final ExpandAllWikiModel simpleExpander;
  private final SwedishWikiModel swedishWikiModel;
  private final WiktionaryDataHandler wdh;
  private Resource target;
  private final TemplateTracker templateTracker = new TemplateTracker();

  public ExampleExpanderWikiModel(WiktionaryPageSource wi, WiktionaryDataHandler wdh) {
    this(wi, wdh, new Locale("sv"), "/IMG", "/LINK");
  }

  public ExampleExpanderWikiModel(WiktionaryPageSource wi, WiktionaryDataHandler wdh, Locale locale,
      String imageBaseURL, String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL);
    this.wdh = wdh;
    this.swedishWikiModel = new SwedishWikiModel(wi, locale, imageBaseURL, linkBaseURL);
    this.simpleExpander = new ExpandAllWikiModel(wi, locale, imageBaseURL, linkBaseURL) {
      @Override
      public String getRawWikiContent(ParsedPageName parsedPagename, Map<String, String> map)
          throws WikiModelContentException {
        return swedishWikiModel.getRawWikiContent(parsedPagename, map);
      }
    };
  }

  @Override
  public void setPageName(String pageTitle) {
    super.setPageName(pageTitle);
    simpleExpander.setPageName(pageTitle);
    swedishWikiModel.setPageName(pageTitle);
  }

  @Override
  public String getRawWikiContent(ParsedPageName parsedPagename, Map<String, String> map)
      throws WikiModelContentException {
    return swedishWikiModel.getRawWikiContent(parsedPagename, map);
  }

  /**
   * Convert an example wiki code to plain text, while keeping track of all template calls and of
   * context definition (source, etc.).
   *
   * @param example the wiki code
   * @param target the resource (current Sense or current LexEntry) on which to attach information.
   * @return the resource representing the extracted example
   */
  public Resource processDefinitionLine(String example, Resource target) {
    this.context = new HashSet<>();
    this.target = target;
    String exampleText = expandAll(example, templates).trim();
    if (!exampleText.isEmpty()) {
      Resource exNode = wdh.registerExample(exampleText, context);
      wdh.addTo(exNode, context);
      return exNode;
    } else {
      wdh.addTo(target, context);
    }
    return null;
  }

  public void processExampleTranslation(String translation, Resource example) {
    this.context = new HashSet<>();
    String exampleText = simpleExpander.expandAll(translation, this.templates);
    if (!exampleText.isEmpty()) {
      context.add(Pair.of(RDF.value,
          ResourceFactory.createLangLiteral(exampleText, wdh.getExtractedLanguage())));
      if (null != example)
        wdh.addTo(example, context);
    }
  }


  @Override
  public void substituteTemplateCall(String templateName, Map<String, String> parameterMap,
      Appendable writer) throws IOException {
    long startTime = 0;
    if (log.isTraceEnabled()) {
      startTime = System.nanoTime();
    }
    if (ignoredTemplates.contains(templateName)) {
      // NOP
    } else if ("citat".equals(templateName)) {
      String text = parameterMap.get("1");
      String translation = parameterMap.get("2");
      parameterMap.put("1", "");
      parameterMap.put("2", translation == null ? null : "");
      StringWriter citation = new StringWriter();
      super.substituteTemplateCall(templateName, parameterMap, citation);
      String ref = citation.toString().replaceAll("\n#:+[^\n]*", "").replaceAll(":$", "").trim();
      ref = simpleExpander.expandAll(ref, this.templates);
      text = simpleExpander.expandAll(text, this.templates);
      context
          .add(Pair.of(DCTerms.bibliographicCitation, rdfNode(ref, wdh.getCurrentEntryLanguage())));
      if (null != translation) {
        translation = simpleExpander.expandAll(translation, this.templates);
        context.add(Pair.of(RDF.value,
            ResourceFactory.createLangLiteral(translation, wdh.getExtractedLanguage())));
      }
      writer.append(text);
    } else if (WiktionaryDataHandler.isValidNym(templateName)) {
      extractNyms(templateName, parameterMap);
    } else if ("varianter".equals(templateName) || "smeknamn".equals(templateName)) {
      // TODO: extract variants
      log.trace("Variants are not extracted yet.");
    } else if ("användning".equals(templateName)) {
      // TODO: extract variants
      log.trace("användning (use ?) are not extracted yet.");
    } else if ("besläktade ord".equals(templateName)) {
      // TODO: extract related words
      log.trace("besläktade ord / seäven (related words ?) are not extracted yet.");
    } else if ("sammansättningar".equals(templateName) || "fraser".equals(templateName)) {
      // TODO: extract dérived terms and phrases
      log.trace("sammansättningar and (compounds ?) are not extracted yet.");
    } else {
      log.trace("DEFINITIONS - processing template: {} @ {}", templateName, this.getPageName());
      super.substituteTemplateCall(templateName, parameterMap, writer);
    }

    if (log.isTraceEnabled()) {
      long elapsedNanos = System.nanoTime() - startTime;
      templateTracker.incr(templateName, elapsedNanos);
    }
  }

  private void extractNyms(String nym, Map<String, String> parameterMap) {
    WikiText nymValues = new WikiText(parameterMap.get("1"));
    for (Token t : nymValues.links()) {
      if (t instanceof InternalLink) {
        InternalLink link = t.asInternalLink();
        String value = link.getTargetText();
        if (value != null) {
          String nymName = WiktionaryDataHandler.nymMarkerToNymName.get(nym);
          wdh.registerNymRelationToEntity(value, nymName, target);
        }
      }
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

  public void logTemplateTracker() {
    if (log.isTraceEnabled()) {
      templateTracker.trace(log, "Example Expander");
    }
  }
}

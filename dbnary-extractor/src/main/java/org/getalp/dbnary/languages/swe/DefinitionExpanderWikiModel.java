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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO inheriting SwedishWikiModel ?
public class DefinitionExpanderWikiModel extends ExpandAllWikiModel {

  static Logger log = LoggerFactory.getLogger(DefinitionExpanderWikiModel.class);

  private final ExpandAllWikiModel simpleExpander;
  private final SwedishWikiModel swedishWikiModel;

  public DefinitionExpanderWikiModel(WiktionaryPageSource wi) {
    this(wi, new Locale("sv"), "/IMG", "/LINK");
  }

  public DefinitionExpanderWikiModel(WiktionaryPageSource wi, Locale locale, String imageBaseURL,
      String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL);
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


  public String expandDefinition(String originalSource) {
    // Expand source to properly resolve links and cosmetic markups
    return simpleExpander.expandAll(originalSource, this.templates);
  }

  @Override
  public void substituteTemplateCall(String templateName, Map<String, String> parameterMap,
      Appendable writer) throws IOException {
    if ("böjning".equalsIgnoreCase(templateName)) {
      // Ignore inflection templates
      return;
    }
    super.substituteTemplateCall(templateName, parameterMap, writer);
  }
}

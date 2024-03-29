package org.getalp.dbnary.languages.cat;

import info.bliki.wiki.filter.ParsedPageName;
import info.bliki.wiki.model.WikiModelContentException;
import info.bliki.wiki.namespaces.INamespace.NamespaceCode;
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
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.bliki.ExpandAllWikiModel;
import org.getalp.iso639.ISO639_3;

/**
 * @author Arnaud Alet 13/07/2023
 */
public class ExpandAllWikiModelCat extends ExpandAllWikiModel {


  private Set<Pair<Property, RDFNode>> context;
  final WiktionaryDataHandler handler;
  private final ExpandAllWikiModel simpleExpander;

  public ExpandAllWikiModelCat(WiktionaryPageSource wi, Locale locale, String imageBaseURL,
      String linkBaseURL, final WiktionaryDataHandler handler) {
    super(wi, locale, imageBaseURL, linkBaseURL);
    this.handler = handler;
    simpleExpander = new ExpandAllWikiModel(wi, locale, imageBaseURL, linkBaseURL);
  }


  @Override
  public void substituteTemplateCall(String templateName, Map<String, String> parameterMap,
      Appendable writer) throws IOException {
    if (templateName.equals("ex-us") || templateName.equals("ex-cit")) {

      final String lang = parameterMap.get("1");

      writer.append(parameterMap.get("2"));

      if (null != parameterMap.get("ref")) {
        if (null == ISO639_3.sharedInstance.getLang(lang)) {
          logger.warn("Unknown language code in ex- template for {} : {}", this.getPageName(),
              lang);
        } else {
          // Sometimes the ref is given in extenso or inside a ref element. But the ref element
          // will not be rendered in plain text, so remove it.
          String ref = parameterMap.get("ref").replaceAll("<ref>|</ref>", "");
          context.add(Pair.of(DCTerms.bibliographicCitation, rdfNode(ref, lang)));
        }
      }
      if (null != parameterMap.get("3"))
        context.add(Pair.of(RDF.value, rdfNode(parameterMap.get("3"), "ca")));
      if (null != parameterMap.get("trad"))
        context.add(Pair.of(RDF.value, rdfNode(parameterMap.get("trad"), "ca")));
      if (null != parameterMap.get("t"))
        context.add(Pair.of(RDF.value, rdfNode(parameterMap.get("t"), "ca")));

      parameterMap.remove("ref");
      parameterMap.remove("1");
      parameterMap.remove("2");
      parameterMap.remove("3");
      parameterMap.remove("trad");
      parameterMap.remove("t");
      parameterMap.remove("inline");
      if (!parameterMap.isEmpty())
        logger.trace("Found complex ex template : " + parameterMap);
    } else if (!templateName.equals("forma-")) {
      super.substituteTemplateCall(templateName, parameterMap, writer);
    }
  }

  @Override
  public String getRawWikiContent(ParsedPageName parsedPagename, Map<String, String> map)
      throws WikiModelContentException {
    if (parsedPagename.namespace.isType(NamespaceCode.TEMPLATE_NAMESPACE_KEY)
        && parsedPagename.pagename.equals("la-pron")) {
      return "{{pronafi|la|/{{#invoke:la-pron|show|{{{1|{{PAGENAME}}}}}}}/|{{#if:{{{2|}}}|/{{#invoke:la-pron|show|{{{2}}}}}/}}}}\n";
    } // else if (parsedPagename.namespace.isType(NamespaceCode.MODULE_NAMESPACE_KEY)
      // && parsedPagename.pagename.equals("llengua/ordre")) {
      // return super.getRawWikiContent(parsedPagename, map).replaceAll("u\\(0x10_FFFF\\)",
      // "\"_\"");
    // }
    return super.getRawWikiContent(parsedPagename, map);
  }

  private Literal rdfNode(String value, String lang) {
    return rdfNode(value, lang, true);
  }

  private Literal rdfNode(String value, String lang, boolean expand) {
    return ResourceFactory.createLangLiteral(
        expand ? this.simpleExpander.expandAll(value, this.templates) : value, lang);
  }

  public void resetContext() {
    this.context = new HashSet<>();
  }

  public Set<Pair<Property, RDFNode>> getContext() {
    return this.context;
  }


  public String renderHtml(final String value) {
    return expandWikiCode(value);
  }
}

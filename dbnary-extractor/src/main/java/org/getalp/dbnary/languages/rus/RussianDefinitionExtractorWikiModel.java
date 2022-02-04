package org.getalp.dbnary.languages.rus;

import info.bliki.wiki.filter.ParsedPageName;
import info.bliki.wiki.filter.PlainTextConverter;
import info.bliki.wiki.model.WikiModelContentException;
import info.bliki.wiki.namespaces.INamespace;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.DCTerms;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.bliki.DbnaryWikiModel;
import org.getalp.dbnary.bliki.ExpandAllWikiModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RussianDefinitionExtractorWikiModel extends DbnaryWikiModel {

  // static Set<String> ignoredTemplates = new TreeSet<String>();
  // static {
  // ignoredTemplates.add("Wikipedia");
  // ignoredTemplates.add("Incorrect");
  // }

  protected static class Example {

    String value;
    Set<Pair<Property, RDFNode>> context = new HashSet<>();

    protected Example(String ex) {
      value = ex;
    }

    protected void add(Property p, RDFNode v) {
      context.add(Pair.of(p, v));
    }
  }

  private final ExpandAllWikiModel expander;

  private final IWiktionaryDataHandler delegate;
  private final Set<Example> currentExamples = new HashSet<>();
  private final Logger log = LoggerFactory.getLogger(RussianDefinitionExtractorWikiModel.class);

  public RussianDefinitionExtractorWikiModel(IWiktionaryDataHandler we, Locale locale,
      String imageBaseURL, String linkBaseURL) {
    this(we, null, locale, imageBaseURL, linkBaseURL);
  }

  public RussianDefinitionExtractorWikiModel(IWiktionaryDataHandler we, WiktionaryPageSource wi,
      Locale locale, String imageBaseURL, String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL);
    expander = new ExpandAllWikiModel(wi, locale, imageBaseURL, linkBaseURL);
    this.delegate = we;
  }

  @Override
  public void setPageName(String pageTitle) {
    super.setPageName(pageTitle);
    expander.setPageName(pageTitle);
  }

  public void parseDefinition(String definition, int defLevel) {
    // Render the definition to plain text, while ignoring the example template
    currentExamples.clear();
    String def = null;
    try {
      def = render(new PlainTextConverter(), definition).trim();
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (null != def && !def.equals("")) {
      delegate.registerNewDefinition(def, defLevel);
      if (!currentExamples.isEmpty()) {
        for (Example example : currentExamples) {
          delegate.registerExample(example.value, example.context);
        }
      }
    }
  }

  @Override
  public void substituteTemplateCall(String templateName, Map<String, String> parameterMap,
      Appendable writer) throws IOException {
    if ("пример".equals(templateName)) {
      // This is an example of usage of the definition.
      // DONE: add this example in the extracted data.
      if (parameterMap.containsKey("текст")) {
        // Call with named parameters
        // {{пример|текст=|перевод=|автор=|титул=|ответственный=|издание=|перев=|дата=|источник=}}
        String ex = expander.expandAll(parameterMap.get("текст"), null);
        if (null != ex && ex.length() != 0) {
          Example example = new Example(ex);
          parameterMap.remove("текст");
          example.add(DCTerms.bibliographicCitation, formatMap(parameterMap));
          currentExamples.add(example);
        }
      } else if (parameterMap.containsKey("1")) {
        // Call with positional parameters
        // {{пример|текст|автор|титул|дата|}}
        String ex = expander.expandAll(parameterMap.get("1"), null);
        if (null != ex && ex.length() != 0) {
          Example example = new Example(ex);
          parameterMap.remove("1");
          example.add(DCTerms.bibliographicCitation, formatMap(parameterMap));
          currentExamples.add(example);
        }
      }
    } else {
      // Do not ignore the other template calls.
      // log.debug("Called macro: {} when expanding definition block in {}.", templateName,
      // this.getPageName());
      super.substituteTemplateCall(templateName, parameterMap, writer);
    }
  }

  private RDFNode formatMap(Map<String, String> parameterMap) {
    StringBuilder b = new StringBuilder();
    for (Map.Entry<String, String> entry : parameterMap.entrySet()) {
      b.append(entry.getKey()).append("=").append(entry.getValue()).append("|");
    }
    b.setLength(b.length() - 1);
    return ResourceFactory.createLangLiteral(b.toString(), "ru");
  }

  @Override
  public String getRawWikiContent(ParsedPageName parsedPagename, Map<String, String> map)
      throws WikiModelContentException {
    ParsedPageName fixedPageName = parsedPagename;
    if (parsedPagename.namespace.isType(INamespace.NamespaceCode.MODULE_NAMESPACE_KEY)
        && parsedPagename.pagename.startsWith("Module:")) {
      fixedPageName = new ParsedPageName(parsedPagename.namespace,
          parsedPagename.pagename.substring(7), parsedPagename.valid);
    }
    return super.getRawWikiContent(fixedPageName, map);
  }
}

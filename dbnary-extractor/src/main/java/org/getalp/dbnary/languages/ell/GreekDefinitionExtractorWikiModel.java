package org.getalp.dbnary.languages.ell;

import info.bliki.wiki.filter.ParsedPageName;
import info.bliki.wiki.filter.PlainTextConverter;
import info.bliki.wiki.model.WikiModelContentException;
import info.bliki.wiki.namespaces.INamespace.NamespaceCode;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.bliki.ExpandAllWikiModel;
import org.getalp.iso639.ISO639_3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GreekDefinitionExtractorWikiModel extends ExpandAllWikiModel {

  private Logger log = LoggerFactory.getLogger(GreekDefinitionExtractorWikiModel.class);

  // static Set<String> ignoredTemplates = new TreeSet<String>();
  // static {
  // ignoredTemplates.add("Wikipedia");
  // ignoredTemplates.add("Incorrect");
  // }

  private IWiktionaryDataHandler delegate;


  public GreekDefinitionExtractorWikiModel(IWiktionaryDataHandler we, Locale locale,
      String imageBaseURL, String linkBaseURL) {
    this(we, (WiktionaryPageSource) null, locale, imageBaseURL, linkBaseURL);
  }

  public GreekDefinitionExtractorWikiModel(IWiktionaryDataHandler we, WiktionaryPageSource wi,
      Locale locale, String imageBaseURL, String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL);
    this.delegate = we;
  }

  public void parseDefinition(String definition, int defLevel) {
    // Render the definition to plain text, while ignoring the example template
    String def = expandAll(definition, null).trim();
    if (!def.equals("")) {
      delegate.registerNewDefinition(def, defLevel);
    }
  }

  @Override
  public void substituteTemplateCall(String templateName, Map<String, String> parameterMap,
      Appendable writer) throws IOException {
    // Currently just expand the definition to get the full text.
    super.substituteTemplateCall(templateName, parameterMap, writer);
  }

  @Override
  public String getRawWikiContent(ParsedPageName parsedPagename, Map<String, String> map)
      throws WikiModelContentException {
    // Patch Module:labels/data as the passed labels are incorrectly trimed by bliki.
    String rawText = super.getRawWikiContent(parsedPagename, map);
    if (parsedPagename.namespace.isType(NamespaceCode.MODULE_NAMESPACE_KEY) &&
        "labels".equals(parsedPagename.pagename) && rawText != null) {
      rawText = rawText.replace("local alias = ''",
          "label = mw.text.trim(label)\n\tlocal alias = ''");
    }
    return rawText;
  }
}

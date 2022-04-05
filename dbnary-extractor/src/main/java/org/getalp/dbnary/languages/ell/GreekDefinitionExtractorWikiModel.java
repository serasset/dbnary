package org.getalp.dbnary.languages.ell;

import info.bliki.wiki.filter.ParsedPageName;
import info.bliki.wiki.model.WikiModelContentException;
import info.bliki.wiki.namespaces.INamespace.NamespaceCode;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.bliki.ExpandAllWikiModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GreekDefinitionExtractorWikiModel extends ExpandAllWikiModel {

  private final Logger log = LoggerFactory.getLogger(GreekDefinitionExtractorWikiModel.class);

  // static Set<String> ignoredTemplates = new TreeSet<String>();
  // static {
  // ignoredTemplates.add("Wikipedia");
  // ignoredTemplates.add("Incorrect");
  // }

  private final IWiktionaryDataHandler delegate;


  public GreekDefinitionExtractorWikiModel(IWiktionaryDataHandler we, Locale locale,
      String imageBaseURL, String linkBaseURL) {
    this(we, null, locale, imageBaseURL, linkBaseURL);
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

  static String patched_ετ = null;

  /* Old code as the template (and others) has been fixed in wiktionary
  @Override

  public String getRawWikiContent(ParsedPageName parsedPagename, Map<String, String> map)
      throws WikiModelContentException {
    // Patch Module:labels/data as the passed labels are incorrectly trimed by bliki.
    if (parsedPagename.namespace.isType(NamespaceCode.TEMPLATE_NAMESPACE_KEY)
        && "ετ".equals(parsedPagename.pagename)) {
      // Patch the template that introduces spaces and newline that play havoc with bliki and Lua
      if (patched_ετ == null) {
        String rawText = super.getRawWikiContent(parsedPagename, map);
        if (null != rawText)
          patched_ετ =
              rawText.replaceAll("}\n\\|", "}<!--\n-->|").replaceAll("-->\n\\|", "\n-->\\|");
      }
      return patched_ετ;
    } else {
      return super.getRawWikiContent(parsedPagename, map);
    }
  }
     */
}

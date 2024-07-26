package org.getalp.dbnary.languages.pol;

import info.bliki.wiki.filter.ParsedPageName;
import info.bliki.wiki.filter.PlainTextConverter;
import info.bliki.wiki.model.WikiModelContentException;
import info.bliki.wiki.namespaces.INamespace.NamespaceCode;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.languages.commons.ModulesPatcherWikiModel;
import org.getalp.dbnary.wiki.WikiTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefinitionExpanderWikiModel extends ModulesPatcherWikiModel {
  private static final Logger log = LoggerFactory.getLogger(DefinitionExpanderWikiModel.class);

  static Set<String> ignoredTemplates = new HashSet<>();

  static {
    ignoredTemplates.add("wikipedia");
  }

  public DefinitionExpanderWikiModel(Locale locale, String imageBaseURL, String linkBaseURL) {
    super(locale, imageBaseURL, linkBaseURL);
  }

  public DefinitionExpanderWikiModel(WiktionaryPageSource wi, Locale locale, String imageBaseURL,
      String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL);
  }

  public String expandAll(String definition, Set<String> templates) {
    String def = WikiTool.removeReferencesIn(definition);
    this.templates = templates;
    try {
      return render(new PlainTextConverter(), def).trim();
    } catch (IOException e) {
      log.error("Error while rendering page.", e);
      // e.printStackTrace();
    }
    return null;
  }

  @Override
  public void substituteTemplateCall(String templateName, Map<String, String> parameterMap,
      Appendable writer) throws IOException {
    if (ignoredTemplates.contains(templateName)) {
      // nop
    } else if ("skrót".equals(templateName)) {
      writer.append("(").append(parameterMap.get("2")).append(")");
    } else if ("reg-pl".equals(templateName) || "gw-pl".equals(templateName)) {
      // This template shows that there is a regionalism or dialectal word sense
      // This template leads to a systematic Lua error when called in definitions.
      logger.trace("{} called with {}", templateName, parameterMap.entrySet().stream()
          .map(e -> e.getKey() + ":" + e.getValue()).collect(Collectors.joining()));
      writer.append("(").append(parameterMap.get("1")).append(")");
      if (null != parameterMap.get("2"))
        writer.append(" ").append(parameterMap.get("2"));
    } else {
      logger.trace("Calling {}", templateName);
      super.substituteTemplateCall(templateName, parameterMap, writer);
    }
  }

  @Override
  public String getRawWikiContent(ParsedPageName parsedPagename, Map<String, String> map)
      throws WikiModelContentException {
    if (parsedPagename.namespace.isType(NamespaceCode.MODULE_NAMESPACE_KEY)
        && parsedPagename.pagename.equals("NKJP")) {
      return getAndPatchModule(parsedPagename, map,
          t -> t.replace("local function hasNilOrEmptyArg( ... )\n",
              "local function hasNilOrEmptyArg( ... )\n"
                  + "\tlocal arg = { n = select('#', ...); ... }"));
    }
    return super.getRawWikiContent(parsedPagename, map);
  }
}

package org.getalp.dbnary.swe;

import info.bliki.wiki.filter.ParsedPageName;
import info.bliki.wiki.model.WikiModelContentException;
import info.bliki.wiki.namespaces.INamespace.NamespaceCode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.getalp.dbnary.WiktionaryIndex;
import org.getalp.dbnary.morphology.InflectedFormSet;
import org.getalp.dbnary.wiki.WikiText;
import org.getalp.dbnary.wiki.WikiText.Template;
import org.getalp.dbnary.wiki.WikiText.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SwedishTableExtractorWikiModel extends SwedishWikiModel {

  private Logger log = LoggerFactory.getLogger(SwedishTableExtractorWikiModel.class);

  public SwedishTableExtractorWikiModel(WiktionaryIndex wi, String imageBaseURL,
      String linkBaseURL) {
    super(wi, new Locale("sv"), imageBaseURL, linkBaseURL);
  }

  protected InflectedFormSet parseTables(String declinationTemplateCall) {

    if (log.isDebugEnabled()) {
      WikiText txt = new WikiText(declinationTemplateCall);
      for (Token token : txt.templatesOnUpperLevel()) {
        Template tmpl = (Template) token;
        // if (!ignoredTemplates.contains(tmpl.getName())) {
        log.debug("MORPH template: {}", tmpl.getName());

        String tmplSource = tmpl.toString().replaceAll("\\n", "");
        log.debug("MORPH template call:  {} @ {}", tmplSource, getPageName());
        // }
      }
    }

    SwedishTableExtractor tableExtractor = new SwedishTableExtractor(this.getPageName());
    String htmlCode = expandWikiCode(declinationTemplateCall);
    log.trace(htmlCode);
    return tableExtractor.parseHTML(htmlCode);
  }

  @Override
  public String getRawWikiContent(ParsedPageName parsedPagename, Map<String, String> map)
      throws WikiModelContentException {
    String result;
    // There modules uses the obsolete arg magik variable which is not supported anymore by lua > 5.1
    if (parsedPagename.namespace.isType(NamespaceCode.MODULE_NAMESPACE_KEY) && parsedPagename.pagename.equals("grammar-table")) {
      result = loadModuleResource("grammar-table");
    } else {
      result = super.getRawWikiContent(parsedPagename, map);
    }
    if (log.isDebugEnabled()) {
      if (result.contains("(...)"))
        log.debug("{} contains a vararg. Check if it's use is correct.", parsedPagename.fullPagename());
    }
    return result;
  }

  private String loadModuleResource(String name) {
    return loadResource(resourceNameFromModuleName(name));
  }

  private String loadResource(String name) {
    if (name == null) {
      return null;
    }
    if (log.isDebugEnabled()) {
      log.error("loading "+name);
    }
    try (InputStream is = getClass().getResourceAsStream(name)) {
      return is == null ? null : IOUtils.toString(is, StandardCharsets.UTF_8);
    } catch (IOException e) {
      logger.error("error loading "+name, e);
      throw new RuntimeException(e);
    }
  }

  private String resourceNameFromModuleName(String name) {
    return name + ".lua" ;
  }


}

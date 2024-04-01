package org.getalp.dbnary.languages.eng;

import info.bliki.wiki.filter.ParsedPageName;
import info.bliki.wiki.filter.PlainTextConverter;
import info.bliki.wiki.model.WikiModelContentException;
import info.bliki.wiki.namespaces.INamespace.NamespaceCode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.bliki.DbnaryWikiModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnglishPronunciationExtractorWikiModel extends EnglishWikiModel {

  private final Logger log = LoggerFactory.getLogger(EnglishPronunciationExtractorWikiModel.class);

  private final IWiktionaryDataHandler delegate;


  public EnglishPronunciationExtractorWikiModel(IWiktionaryDataHandler we, Locale locale,
      String imageBaseURL, String linkBaseURL) {
    this(we, null, locale, imageBaseURL, linkBaseURL);
  }

  public EnglishPronunciationExtractorWikiModel(IWiktionaryDataHandler we, WiktionaryPageSource wi,
      Locale locale, String imageBaseURL, String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL);
    this.delegate = we;
  }

  private static final Pattern narrowDownIPAPattern = Pattern.compile("IPA\\(key\\):([^\n]*)");
  private static final Pattern pronunciationPattern = Pattern.compile("/[^/]*/|\\[[^\\]]*\\]");

  public void parsePronunciation(String pronTemplate) {
    // Render the pronunciation template to plain text then extract the computed pronunciation
    log.trace("Parsing pronunciation : ||| {} ||| in {}", pronTemplate.toString(),
        delegate.currentPagename());
    // Do not parse pronunciation for vietnamese entry as the pronunciation code is not
    // compatible with our Lua version and systematically raises an error
    if ("vi".equals(delegate.getCurrentEntryLanguage()))
      return;
    String expandedPron = null;
    try {
      expandedPron = render(new PlainTextConverter(), pronTemplate).trim();
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (null != expandedPron && !expandedPron.equals("")) {
      Scanner s = new Scanner(expandedPron);
      s.findAll(narrowDownIPAPattern).forEach(ipa -> {
        Scanner pronScanner = new Scanner(ipa.group(1));
        pronScanner.findAll(pronunciationPattern).forEach(mr -> {
          String pron = mr.group(0);
          delegate.registerPronunciation(pron, delegate.getCurrentEntryLanguage() + "-fonipa");
        });
      });

    }
  }

  @Override
  public void substituteTemplateCall(String templateName, Map<String, String> parameterMap,
      Appendable writer) throws IOException {
    super.substituteTemplateCall(templateName, parameterMap, writer);
  }

  @Override
  public String getRawWikiContent(ParsedPageName parsedPagename, Map<String, String> map)
      throws WikiModelContentException {
    String pageContent = super.getRawWikiContent(parsedPagename, map);
    // Patch the ko-phon module which does not work in bliki
    if (pageContent != null) {
      if (parsedPagename.namespace.isType(NamespaceCode.MODULE_NAMESPACE_KEY)
          && parsedPagename.pagename.equals("ko-pron")) {
        pageContent = pageContent.replace(
            "return tostring(html_ul) .. tostring(html_table) .. require(\"Module:TemplateStyles\")(\"Template:ko-IPA/style.css\")",
            "return tostring(html_ul)").replace(
                "return tostring(html_ul) .. require(\"Module:TemplateStyles\")(\"Template:ko-IPA/style.css\")",
                "return tostring(html_ul)");
      } else if (parsedPagename.namespace.isType(NamespaceCode.MODULE_NAMESPACE_KEY)
          && parsedPagename.pagename.equals("audio")) {
        pageContent = pageContent.replace("return stylesheet .. text .. categories",
            "return text .. categories");
      }
    }
    return pageContent;
  }
}

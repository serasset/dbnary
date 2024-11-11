package org.getalp.dbnary.languages.commons;

import info.bliki.wiki.filter.ParsedPageName;
import info.bliki.wiki.model.WikiModelContentException;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.bliki.DbnaryWikiModel;

public class ModulesPatcherWikiModel extends DbnaryWikiModel {

  public ModulesPatcherWikiModel(Locale locale, String imageBaseURL, String linkBaseURL) {
    super(locale, imageBaseURL, linkBaseURL);
  }

  public ModulesPatcherWikiModel(WiktionaryPageSource wi, Locale locale, String imageBaseURL,
      String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL);
  }

  @SafeVarargs
  protected final String getAndPatchModule(ParsedPageName parsedPagename, Map<String, String> map,
      Function<String, String>... patchers) throws WikiModelContentException {
    String content = getContent(parsedPagename, map);
    if (content == null) {
      return null;
    }
    content = patchModule(parsedPagename.pagename, content, patchers);
    return content;
  }

  protected String getContent(ParsedPageName parsedPagename, Map<String, String> map)
      throws WikiModelContentException {
    return super.getRawWikiContent(parsedPagename, map);
  }

  protected String patchModule(String pagename, String content,
      Function<String, String>... patchers) {
    int patchnum = 0;
    for (Function<String, String> patcher : patchers) {
      String patchedContent = patcher.apply(content);
      if (logger.isDebugEnabled()) {
        boolean patched = !patchedContent.equals(content);
        if (patched) {
          logger.debug("Module:{} has been patched ({}).", pagename, ++patchnum);
        } else {
          logger.warn("Module:{} could not be patched! ({}) Check current implementation.",
              pagename, ++patchnum);
        }
      }
      content = patchedContent;
    }
    return content;
  }
}


package org.getalp.dbnary.bliki;

import info.bliki.wiki.filter.HTMLConverter;
import info.bliki.wiki.filter.ParsedPageName;
import info.bliki.wiki.model.Configuration;
import info.bliki.wiki.model.WikiModel;
import info.bliki.wiki.model.WikiModelContentException;
import info.bliki.wiki.namespaces.INamespace.NamespaceCode;
import info.bliki.wiki.tags.HTMLTag;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.apache.commons.text.WordUtils;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.tools.CounterSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbnaryWikiModel extends WikiModel {

  private static final Logger log = LoggerFactory.getLogger(DbnaryWikiModel.class);

  protected WiktionaryPageSource wi;

  public DbnaryWikiModel(Locale locale, String imageBaseURL, String linkBaseURL) {
    this(null, locale, imageBaseURL, linkBaseURL);
  }

  public DbnaryWikiModel(WiktionaryPageSource wi, Locale locale, String imageBaseURL,
      String linkBaseURL) {
    super(new Configuration(), locale, imageBaseURL, linkBaseURL);
    this.wi = wi;
    // This tag is used at least in French Morphology templates.
    this.addTokenTag("bdi", new HTMLTag("bdi"));
    this.addTokenTag("templatestyles", new HTMLTag("templatestyles"));
  }

  protected String expandWikiCode(String wikicode) {
    try {
      return render(new HTMLConverter(), wikicode);
    } catch (IOException e) {
      log.warn("WikiCode Expansion led to Exception in {}", getPageName());
      log.debug("   ---> {}", e.getMessage(), e);
    }
    return null;
  }

  protected CounterSet trace = new CounterSet();

  public void displayGlobalTrace(String msg) {
    trace.traceCounters(log, msg);
  }

  @Override
  public String getRawWikiContent(ParsedPageName parsedPagename, Map<String, String> map)
      throws WikiModelContentException {
    return getContentPlain(parsedPagename, map);
  }

  private String getContentPlain(ParsedPageName parsedPagename, Map<String, String> map)
      throws WikiModelContentException {
    log.trace("resolving {} in {}", parsedPagename.fullPagename(), this.getPageName());
    String result = super.getRawWikiContent(parsedPagename, map);
    if (result != null) {
      // found magic word template
      return result;
    }

    // Fix a bug in some wiktionary where a lua script import "Module:page" by specifying
    // the namepace, while the wiktionary edition uses a localized namespace.
    if (parsedPagename.namespace.isType(NamespaceCode.MODULE_NAMESPACE_KEY)
        && (parsedPagename.pagename.startsWith("Module:")
            || parsedPagename.pagename.startsWith("module:"))) {
      parsedPagename = new ParsedPageName(parsedPagename.namespace,
          parsedPagename.pagename.substring(7), parsedPagename.valid);
    }

    if (log.isTraceEnabled())
      trace.incr(parsedPagename.fullPagename());

    if (null != wi) {
      String rawText = wi.getTextOfPageWithRedirects(parsedPagename.fullPagename());
      String name;
      if (null == rawText
          && !(name = parsedPagename.pagename.trim()).equals(parsedPagename.pagename)) {
        rawText = wi.getTextOfPageWithRedirects(parsedPagename.namespace + ":" + name);
      }
      if (parsedPagename.namespace.isType(NamespaceCode.TEMPLATE_NAMESPACE_KEY)) {
        rawText = prepareForTransclusion(rawText);
      }
      if (null == rawText
          && parsedPagename.namespace.isType(NamespaceCode.MEDIAWIKI_NAMESPACE_KEY)) {
        // In MediaWiki, try to capitalize the pagename
        rawText = wi.getTextOfPageWithRedirects(parsedPagename.namespace + ":"
            + WordUtils.capitalize(parsedPagename.pagename, new char[] {}));
      }
      // TODO: should I try with: name = encodeTitleToUrl(articleName, true);

      return rawText;
    }

    log.debug("getRawWikiContent return null for {} in {}", parsedPagename.fullPagename(),
        this.getPageName());
    return null;
  }

  /**
   * Retrieves the content of a module or template and applies patching functions to it. This method
   * is useful for fixing known issues in Lua modules before they are processed.
   *
   * @param parsedPagename the parsed page name
   * @param map the parameter map
   * @param patchers variable number of patching functions to apply to the content
   * @return the patched content, or null if content cannot be retrieved
   * @throws WikiModelContentException if there's an error retrieving the content
   */
  @SafeVarargs
  protected final String getAndPatchModule(ParsedPageName parsedPagename, Map<String, String> map,
      Function<String, String>... patchers) throws WikiModelContentException {
    String content = getContentPlain(parsedPagename, map);
    if (content == null) {
      return null;
    }
    content = patchModule(parsedPagename.pagename, content, patchers);
    return content;
  }

  /**
   * Applies a series of patching functions to module content. Logs debug information about
   * successful and failed patches.
   *
   * @param pagename the name of the page being patched
   * @param content the original content
   * @param patchers variable number of patching functions
   * @return the patched content
   */
  @SafeVarargs
  protected final String patchModule(String pagename, String content,
      Function<String, String>... patchers) {
    int patchnum = 0;
    for (Function<String, String> patcher : patchers) {
      ++patchnum;
      String patchedContent = patcher.apply(content);
      if (log.isDebugEnabled()) {
        boolean patched = !patchedContent.equals(content);
        if (patched) {
          log.debug("Module:{} has been patched ({}).", pagename, patchnum);
        } else {
          log.warn("Module:{} could not be patched! ({}) Check current implementation.", pagename,
              patchnum);
        }
      }
      if (patchedContent != null) {
        content = patchedContent;
      }
    }
    return content;
  }

  public String prepareForTransclusion(String rawWikiText) {
    if (null == rawWikiText) {
      return null;
    }

    int noIncludeOffset = rawWikiText.indexOf("<noinclude>");
    if (-1 != noIncludeOffset) {
      int noIncludeEndOffset = rawWikiText.indexOf("</noinclude>", noIncludeOffset);
      if (-1 != noIncludeEndOffset) {
        return prepareForTransclusion(rawWikiText.substring(0, noIncludeOffset)
            + rawWikiText.substring(noIncludeEndOffset + "</noinclude>".length()));
      }
    }
    int onlyIncludeOffset = rawWikiText.indexOf("<onlyinclude>");
    if (-1 != onlyIncludeOffset) {
      int onlyIncludeEndOffset = rawWikiText.indexOf("</onlyinclude>", onlyIncludeOffset);
      if (-1 != onlyIncludeEndOffset) {
        return rawWikiText.substring(onlyIncludeOffset + "<onlyinclude>".length(),
            onlyIncludeEndOffset);
      }
    }
    int includeOnlyOffset = rawWikiText.indexOf("<includeonly>");
    if (-1 != includeOnlyOffset) {
      int includeOnlyEndOffset = rawWikiText.indexOf("</includeonly>", noIncludeOffset);
      if (-1 != includeOnlyEndOffset) {
        String removeTags = rawWikiText.substring(0, includeOnlyOffset)
            + rawWikiText.substring(includeOnlyOffset + "<includeonly>".length(),
                includeOnlyEndOffset)
            + rawWikiText.substring(includeOnlyEndOffset + "</includeonly>".length());
        return prepareForTransclusion(removeTags);
      }
    }
    return rawWikiText.replaceAll("\\{\\{safesubst:", "{{").replaceAll("\\{\\{subst:", "{{");
  }

  private final Pattern HAS_WIKI_CONTENT = Pattern.compile("\\{\\{|<nowiki|\\[|^[#:*=]");

  // This method is only called by preprocess and, when called with a non wiki text, generates
  // a html string with <p> while the php preprocess will not. To avoid some bugs appearing in
  // English template, we catch calls with simple (non wiki, non html) values and return the
  // original string directly
  @Override
  public String render(String rawWikiText) throws IOException {
    if (HAS_WIKI_CONTENT.matcher(rawWikiText).find()) {
      return super.render(rawWikiText);
    } else {
      return rawWikiText;
    }
  }
}

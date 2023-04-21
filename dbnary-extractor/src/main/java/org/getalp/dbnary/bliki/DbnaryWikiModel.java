package org.getalp.dbnary.bliki;

import info.bliki.extensions.scribunto.engine.ScribuntoEngine;
import info.bliki.extensions.scribunto.engine.lua.CompiledScriptCache;
import info.bliki.extensions.scribunto.engine.lua.ScribuntoLuaEngine;
import info.bliki.wiki.filter.HTMLConverter;
import info.bliki.wiki.filter.ParsedPageName;
import info.bliki.wiki.model.Configuration;
import info.bliki.wiki.model.WikiModel;
import info.bliki.wiki.model.WikiModelContentException;
import info.bliki.wiki.namespaces.INamespace;
import info.bliki.wiki.namespaces.INamespace.NamespaceCode;
import info.bliki.wiki.tags.HTMLTag;
import java.io.IOException;
import java.io.StringReader;
import java.util.Locale;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.text.WordUtils;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.tools.CounterSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class DbnaryWikiModel extends WikiModel {

  private static final Logger log = LoggerFactory.getLogger(DbnaryWikiModel.class);

  protected WiktionaryPageSource wi;
  private CompiledScriptCache compiledScriptCache = new CompiledScriptCache();
  private ScribuntoEngine fScribuntoEngine = null;

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

  private static DocumentBuilder docBuilder = null;
  private static InputSource docSource = null;

  // get the DOM representation of the HTML code corresponding
  // to the wikicode given in arguments
  public Document wikicodeToHtmlDOM(String wikicode) {
    if (docBuilder == null) {
      try {
        docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      } catch (ParserConfigurationException e) {
        System.err.println("got a ParserConfigurationException in the DBnaryWikiModel class.");
        return null;
      }

      docSource = new InputSource();
    }

    String html = expandWikiCode(wikicode);

    docSource.setCharacterStream(new StringReader("<div>" + html + "</div>"));

    Document doc = null;

    try {
      doc = docBuilder.parse(docSource);
    } catch (SAXException e) {
      log.error("Unable to parse template call in DBnaryWikiModel.");
    } catch (IOException e) {
      log.error("got IOException in DBnaryWikiModel ‽");
    }

    return doc;
  }

  protected String expandWikiCode(String wikicode) {
    try {
      return render(new HTMLConverter(), wikicode);
    } catch (IOException e) {
      log.warn("WikiCode Expansion led to Exception in {}", getPageName());
      e.printStackTrace();
    }
    return null;
  }

  protected CounterSet trace = new CounterSet();

  public void displayGlobalTrace() {
    trace.traceCounters(log);
  }

  public void displayGlobalTrace(String msg) {
    trace.traceCounters(log, msg);
  }

  @Override
  public String getRawWikiContent(ParsedPageName parsedPagename, Map<String, String> map)
      throws WikiModelContentException {
    log.trace("resolving {} in {}", parsedPagename.fullPagename(), this.getPageName());
    String result = super.getRawWikiContent(parsedPagename, map);
    if (result != null) {
      // found magic word template
      return result;
    }

    // Fix a bug in some wiktionary where a lua script import "Module:page" by specifying
    // the namepace, while the wiktionary edition uses a localized namespace.
    if (parsedPagename.namespace.isType(INamespace.NamespaceCode.MODULE_NAMESPACE_KEY)
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

  @Override
  public ScribuntoEngine createScribuntoEngine() {
    if (null == fScribuntoEngine) {
      Logger scribuntoLog = LoggerFactory.getLogger(ScribuntoEngine.class);
      fScribuntoEngine = new ScribuntoLuaEngine(this, compiledScriptCache,
          scribuntoLog.isDebugEnabled());
    }
    return fScribuntoEngine;
  }
}

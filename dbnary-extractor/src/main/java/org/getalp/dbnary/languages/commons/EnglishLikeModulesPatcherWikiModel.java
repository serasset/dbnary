package org.getalp.dbnary.languages.commons;

import info.bliki.wiki.filter.ParsedPageName;
import info.bliki.wiki.model.WikiModelContentException;
import info.bliki.wiki.namespaces.INamespace.NamespaceCode;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.bliki.DbnaryWikiModel;

public class EnglishLikeModulesPatcherWikiModel extends DbnaryWikiModel {

  public static final Pattern UNPACK_PATTERN = Pattern.compile("(unpack\\([^),]+)\\)");

  public EnglishLikeModulesPatcherWikiModel(Locale locale, String imageBaseURL,
      String linkBaseURL) {
    super(locale, imageBaseURL, linkBaseURL);
  }

  public EnglishLikeModulesPatcherWikiModel(WiktionaryPageSource wi,
      Locale locale, String imageBaseURL, String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL);
  }

  @Override
  public String getRawWikiContent(ParsedPageName parsedPagename, Map<String, String> map)
      throws WikiModelContentException {
    // This allow the handling of the Chinese wikimodel that uses capitalized module names
    String pagename = parsedPagename.pagename.toLowerCase();
    if (parsedPagename.namespace.isType(NamespaceCode.MODULE_NAMESPACE_KEY)) {
      if (pagename.startsWith("place")) {
        return getAndPatchModule(parsedPagename, map,
            t -> UNPACK_PATTERN.matcher(t).replaceAll(m -> m.group(1) + ", 1, 3)"));
      } else if (pagename.equals("utilities")) {
        // utilities uses the nasty nowiki tag hack. So patch the function using it to always return
        // 0
        return getAndPatchModule(parsedPagename, map,
            t -> t.replaceAll("function export.get_current_section\\(\\)",
                "function export.get_current_section()\n\t\tlocal test = 1\n"
                    + "\t\tif test then return 0 end"),
            t -> t.replaceAll("return export",
                "-- This function is redefined to avoid too much time taken in calculus we do not need.\n"
                    + "function export.format_categories(categories, lang, sort_key, sort_base, force_output, sc)\n"
                    + "return \"\"\n" + "end\n" + "\n" + "return export"));
      } else if (pagename.equals("hrkt-translit/data/ja")) {
        // Hrkt-translit/data/ja does not exists, and Lua takes care of this, but will generate
        // an annoying error message. So we just return an empty table
        return "return {}";
      } else if (pagename.equals("ko-pron")) {
        return getAndPatchModule(parsedPagename, map, t -> t.replace(
            "return tostring(html_ul) .. tostring(html_table) .. require(\"Module:TemplateStyles\")(\"Template:ko-IPA/style.css\")",
            "return tostring(html_ul)").replace(
            "return tostring(html_ul) .. require(\"Module:TemplateStyles\")(\"Template:ko-IPA/style.css\")",
            "return tostring(html_ul)"));
      } else if (pagename.equals("audio")) {
        return getAndPatchModule(parsedPagename, map,
            t -> t.replace("return stylesheet .. text .. categories", "return text .. categories"));
      } else if (pagename.equals("table")) {
        return getAndPatchModule(parsedPagename, map,
            t -> t.replace("function export.append(...)\n" + "\tlocal ret, n = {}, 0\n",
                "function export.append(...)\n" + "\tlocal arg = { n = select('#', ...); ... }\n"
                    + "\tlocal ret, n = {}, 0\n"));
      } else if (pagename.equals("languages")) {
        return getAndPatchModule(parsedPagename, map,
            t -> t.replace("function export.addDefaultTypes(data, regular, ...)\n",
                "function export.addDefaultTypes(data, regular, ...)\n"
                    + "\tlocal arg = { n = select('#', ...); ... }\n"));
      } else if (pagename.equals("scripts")) {
        return getAndPatchModule(parsedPagename, map,
            t -> t.replace("function Script:hasType(...)\n",
                "function Script:hasType(...)\n" + "\tlocal arg = { n = select('#', ...); ... }"));
      }
    }
    return super.getRawWikiContent(parsedPagename, map);
  }

  @SafeVarargs
  protected final String getAndPatchModule(ParsedPageName parsedPagename, Map<String, String> map,
      Function<String, String>... patchers) throws WikiModelContentException {
    String content = super.getRawWikiContent(parsedPagename, map);
    if (null == content)
      return null;
    int patchnum = 0;
    for (Function<String, String> patcher : patchers) {
      String patchedContent = patcher.apply(content);
      if (logger.isDebugEnabled()) {
        boolean patched = !patchedContent.equals(content);
        if (patched)
          logger.debug("Module:{} has been patched ({}).", parsedPagename.pagename, ++patchnum);
        else
          logger.warn("Module:{} could not be patched! ({}) Check current implementation.",
              parsedPagename.pagename, ++patchnum);
      }
      content = patchedContent;
    }
    return content;
  }
}

package org.getalp.dbnary.languages.eng;

import info.bliki.wiki.filter.ParsedPageName;
import info.bliki.wiki.model.WikiModelContentException;
import info.bliki.wiki.namespaces.INamespace.NamespaceCode;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.bliki.DbnaryWikiModel;
import org.getalp.iso639.ISO639_3;

public class EnglishWikiModel extends DbnaryWikiModel {

  public EnglishWikiModel(WiktionaryPageSource wi, Locale locale, String imageBaseURL,
      String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL);
  }

  @Override
  public void substituteTemplateCall(String templateName, Map<String, String> parameterMap,
      Appendable writer) throws IOException {
    if (!templateName.startsWith("tracking")) {
      if (templateName.equals("check deprecated lang param usage")) {
        writer.append(parameterMap.get("1"));
      } else if ("str left".equals(templateName)) {
        try {
          int l = Integer.parseInt(parameterMap.get("2"));
          String val = parameterMap.getOrDefault("1", "");
          writer.append(val.substring(0, Math.min(l, val.length())));
        } catch (NumberFormatException e) {
          // nop
        }
      } else if ("catlangname".equals(templateName) || "cln".equals(templateName)
          || "categorize".equals(templateName) || "C".equals(templateName)
          || "topics".equals(templateName)) {
        // Just ignore this template
      } else if ("langname".equals(templateName)) {
        String langname = ISO639_3.sharedInstance.getLanguageNameInEnglish(parameterMap.get("1"));
        if (null != langname)
          writer.append(langname);
        else
          super.substituteTemplateCall(templateName, parameterMap, writer);
      } else if ("ja-pron".equals(templateName)) {
        Map<String, String> simplifiedMap = new LinkedHashMap<>(parameterMap.size());
        // Remove all references arguments to the template
        for (Entry<String, String> e : parameterMap.entrySet()) {
          if (!e.getKey().endsWith("_ref"))
            simplifiedMap.put(e.getKey(), e.getValue());
        }
        super.substituteTemplateCall(templateName, simplifiedMap, writer);
      } else {
        super.substituteTemplateCall(templateName, parameterMap, writer);
      }
    }
  }

  public static final Pattern UNPACK_PATTERN = Pattern.compile("(unpack\\([^),]+)\\)");

  @Override
  public String getRawWikiContent(ParsedPageName parsedPagename, Map<String, String> map)
      throws WikiModelContentException {
    if (parsedPagename.namespace.isType(NamespaceCode.TEMPLATE_NAMESPACE_KEY)
        && parsedPagename.pagename.equals("langname")) {
      // give a langname implementation without safesubst
      return "{{#invoke:languages/templates|getByCode|{{{1}}}|getCanonicalName}}";
    } else if (parsedPagename.namespace.isType(NamespaceCode.MODULE_NAMESPACE_KEY)
        && parsedPagename.pagename.equals("Jpan-sortkey")) {
      // Jpan sortkey uses a hack that is not possible in our setting and generates a Lua error
      // As sortkey is not essential in our setting, just return a stub
      return getAndPatchModule(parsedPagename, map, t -> t.replaceAll(
          "tonumber\\(mw.getCurrentFrame\\(\\):extensionTag\\('nowiki', ''\\):match'\\(\\[%dA-F\\]\\+\\)', 16\\)",
          "0"));
    } else if (parsedPagename.namespace.isType(NamespaceCode.MODULE_NAMESPACE_KEY)) {
      if (parsedPagename.pagename.startsWith("place")) {
        return getAndPatchModule(parsedPagename, map,
            t -> UNPACK_PATTERN.matcher(t).replaceAll(m -> m.group(1) + ", 1, 3)"));
      } else if (parsedPagename.pagename.equals("utilities")) {
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
      } else if (parsedPagename.pagename.equals("Hrkt-translit/data/ja")) {
        // Hrkt-translit/data/ja does not exists, and Lua takes care of this, but will generate
        // an annoying error message. So we just return an empty table
        return "return {}";
      } else if (parsedPagename.pagename.equals("ko-pron")) {
        return getAndPatchModule(parsedPagename, map, t -> t.replace(
            "return tostring(html_ul) .. tostring(html_table) .. require(\"Module:TemplateStyles\")(\"Template:ko-IPA/style.css\")",
            "return tostring(html_ul)").replace(
                "return tostring(html_ul) .. require(\"Module:TemplateStyles\")(\"Template:ko-IPA/style.css\")",
                "return tostring(html_ul)"));
      } else if (parsedPagename.pagename.equals("audio")) {
        return getAndPatchModule(parsedPagename, map,
            t -> t.replace("return stylesheet .. text .. categories", "return text .. categories"));
      } else if (parsedPagename.pagename.equals("table")) {
        return getAndPatchModule(parsedPagename, map,
            t -> t.replace("function export.append(...)\n" + "\tlocal ret, n = {}, 0\n",
                "function export.append(...)\n" + "\tlocal arg = { n = select('#', ...); ... }\n"
                    + "\tlocal ret, n = {}, 0\n"));
      } else if (parsedPagename.pagename.equals("languages")) {
        return getAndPatchModule(parsedPagename, map,
            t -> t.replace("function export.addDefaultTypes(data, regular, ...)\n",
                "function export.addDefaultTypes(data, regular, ...)\n"
                    + "\tlocal arg = { n = select('#', ...); ... }\n"));
      } else if (parsedPagename.pagename.equals("scripts")) {
        return getAndPatchModule(parsedPagename, map,
            t -> t.replace("function Script:hasType(...)\n",
                "function Script:hasType(...)\n" + "\tlocal arg = { n = select('#', ...); ... }"));
      }
    }
    return super.getRawWikiContent(parsedPagename, map);
  }

  @SafeVarargs
  private String getAndPatchModule(ParsedPageName parsedPagename, Map<String, String> map,
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

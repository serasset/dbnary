package org.getalp.dbnary.languages.eng;

import info.bliki.wiki.filter.ParsedPageName;
import info.bliki.wiki.model.WikiModelContentException;
import info.bliki.wiki.namespaces.INamespace.NamespaceCode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.io.IOUtils;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.languages.commons.EnglishLikeModulesPatcherWikiModel;
import org.getalp.iso639.ISO639_3;

public class EnglishWikiModel extends EnglishLikeModulesPatcherWikiModel {

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
        // Just ignore these templates
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
    } else if (parsedPagename.namespace.isType(NamespaceCode.MODULE_NAMESPACE_KEY)
        && parsedPagename.pagename.equals("parameters/track")) {
      // December 2024: Module:parameters now uses the traceback that is only available in debug
      // mode avoid an error while compiling the module as debug is not available in our execution
      // environment — patch it
      // July 2025: now defined in Module/parameters/track
      return getAndPatchModule(parsedPagename, map,
          t -> t.replaceAll("local\\s+traceback\\s*=\\s*debug.traceback\n", //
              "local function traceback() \n" //
                  + " return \"\"\n" //
                  + "end\n"));
    } else if (parsedPagename.namespace.isType(NamespaceCode.MODULE_NAMESPACE_KEY)
        && parsedPagename.pagename.equals("table/getUnprotectedMetatable")) {
      // December 2024: Module:table/getUnprotectedMetatable queries debug which is nil
      // in our environment — patch it
      return getAndPatchModule(parsedPagename, map,
          t -> t.replaceAll("local _getmetatable = debug.getmetatable\n", //
              "local _getmetatable = nil\n"));
    } else if (parsedPagename.namespace.isType(NamespaceCode.MODULE_NAMESPACE_KEY)
        && parsedPagename.pagename.equals("checkparams")) {
      String resource = loadStubResource("checkparams.lua");
      return null == resource ? super.getRawWikiContent(parsedPagename, map) : resource;
    }
    return super.getRawWikiContent(parsedPagename, map);
  }

  private String loadStubResource(String name) {
    logger.debug("loading stub page from resources: {}", name);
    try (InputStream is = getClass().getResourceAsStream(name)) {
      return is == null ? null : IOUtils.toString(is, StandardCharsets.UTF_8);
    } catch (IOException e) {
      logger.error("Could not substitute stub page '{}' defaulting to normal page.", name, e);
      return null;
    }
  }
}

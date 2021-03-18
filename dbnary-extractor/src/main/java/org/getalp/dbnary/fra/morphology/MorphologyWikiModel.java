package org.getalp.dbnary.fra.morphology;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import org.getalp.dbnary.WiktionaryIndex;
import org.getalp.dbnary.bliki.DbnaryWikiModel;
import org.getalp.iso639.ISO639_3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract wiki model that handles the transclusion of inflection and conjugation tables for the
 * French language edition.
 */
public abstract class MorphologyWikiModel extends DbnaryWikiModel {

  private static final Logger log = LoggerFactory.getLogger(MorphologyWikiModel.class);


  public MorphologyWikiModel(WiktionaryIndex wi, Locale locale, String imageBaseURL,
      String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL);
  }

  public static String sansBalises(String t) {
    if (null != t) {
      return t.replaceAll("<[^\\>]*>", "").replaceAll("'''?", "");
    } else {
      return "";
    }
  }

  @Override
  public void substituteTemplateCall(String templateName, Map<String, String> parameterMap,
      Appendable writer) throws IOException {
    // Currently just expand the definition to get the full text.
    if (templateName.equals("nom langue") || templateName.endsWith(":nom langue")) {
      // intercept this template as it leeds to a very inefficient Lua Script.
      String langCode = parameterMap.get("1").trim();
      String lang = ISO639_3.sharedInstance.getLanguageNameInFrench(langCode);
      if (null != lang) {
        writer.append(lang);
      }
    } else if ("sans balise".equals(templateName) || "sans_balise".equals(templateName)) {
      String t = parameterMap.get("1");
      if (null != t) {
        writer.append(sansBalises(t));
      }
    } else if ("gsub".equals(templateName)) {
      String s = parameterMap.get("1");
      String pattern = parameterMap.get("2");
      String repl = parameterMap.get("3");
      if (null == pattern && null == repl) {
        writer.append(s);
      } else if ("’".equals(pattern) && "'".equals(repl)) {
        writer.append(s.replaceAll(pattern, repl));
      } else if ("s$".equals(pattern) && null == repl) {
        writer.append(s.replaceAll(pattern, ""));
      } else {
        // log.trace("gsub {} | {} | {}", parameterMap.get("1"), parameterMap.get("2"),
        // parameterMap.get("3"));
        super.substituteTemplateCall(templateName, parameterMap, writer);
      }
    } else if ("str find".equals(templateName) || "str_find".equals(templateName)) {
      String s = parameterMap.get("1");
      String pattern = parameterMap.get("2");
      int i = s.trim().indexOf(pattern);
      if (-1 != i) {
        writer.append("").append(String.valueOf(i + 1));
      }
    } else if (templateName.equals("pron")) {
      // catch this template call as it resolves in a non useful link.
      writer.append("\\").append(parameterMap.get("1")).append("\\");
    } else if (templateName.equals("pron-brut")) {
      // catch this template call as it resolves into a non usefull link.
      if (null == parameterMap.get("1"))
        writer.append("—");
      else
        writer.append(parameterMap.get("1"));
    } else if (templateName.equals("param1ou2")) {
      // This template generates incomplete links xxx]] ou [[xxx which breaks the bliki parser
      writer.append(parameterMap.get("1")).append(" ou ").append(parameterMap.get("2"));
    } else {
      // log.trace("substituting template {} with parameters [[{}]]", templateName,
      // parameterMap.entrySet().stream().map(e -> "<" + e.getKey() + " --> " + e.getValue() + ">")
      // .collect(Collectors.joining(", ")));
      super.substituteTemplateCall(templateName, parameterMap, writer);
    }
  }
}

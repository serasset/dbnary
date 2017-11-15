package org.getalp.dbnary.eng;

import info.bliki.wiki.filter.PlainTextConverter;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import org.getalp.dbnary.DbnaryWikiModel;
import org.getalp.dbnary.IWiktionaryDataHandler;
import org.getalp.dbnary.WiktionaryIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnglishDefinitionExtractorWikiModel extends DbnaryWikiModel {

  private Logger log = LoggerFactory.getLogger(EnglishDefinitionExtractorWikiModel.class);

  // static Set<String> ignoredTemplates = new TreeSet<String>();
  // static {
  // ignoredTemplates.add("Wikipedia");
  // ignoredTemplates.add("Incorrect");
  // }

  private IWiktionaryDataHandler delegate;


  public EnglishDefinitionExtractorWikiModel(IWiktionaryDataHandler we, Locale locale,
      String imageBaseURL, String linkBaseURL) {
    this(we, (WiktionaryIndex) null, locale, imageBaseURL, linkBaseURL);
  }

  public EnglishDefinitionExtractorWikiModel(IWiktionaryDataHandler we, WiktionaryIndex wi,
      Locale locale, String imageBaseURL, String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL);
    this.delegate = we;
  }

  public void parseDefinition(String definition, int defLevel) {
    // Render the definition to plain text, while ignoring the example template
    // log.trace("extracting definitions in {}", this.getPageName());
    String def = null;
    try {
      def = render(new PlainTextConverter(), definition).trim();
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (null != def && !def.equals("")) {
      delegate.registerNewDefinition(def, defLevel);
    }
  }

  @Override
  public void substituteTemplateCall(String templateName, Map<String, String> parameterMap,
      Appendable writer) throws IOException {
    // Currently just expand the definition to get the full text.
    if (templateName.equals("label") || templateName.equals("lb") || templateName.equals("lbl")) {
      // intercept this template as it leads to a very inefficient Lua Script.
      writer.append("(");
      writer.append(parameterMap.get("2"));
      for (int i = 3; i < 9; i++) {
        String p = parameterMap.get(Integer.toString(i));
        // TODO: correctly handle comma in label construction
        if (null != p) {
          writer.append(", ");
          writer.append(p);
        }
      }
      writer.append(") ");
    } else if (templateName.equals("context") || templateName.equals("cx")) {
      log.debug("Obsolete Context template in {}", this.getPageName());
    } else if (templateName.equals("l") || templateName.equals("link") || templateName.equals("m")
        || templateName.equals("mention")) {
      String l = parameterMap.get("3");
      if (null == l) {
        l = parameterMap.get("2");
      }
      writer.append(l);
    } else if (templateName.endsWith(" of")) {
      log.debug("Ignoring template {} in definition of {}", templateName, this.getPageName());
    } else if (templateName.equals("categorize") || templateName.equals("catlangname")
        || templateName.equals("catlangcode")) {
      // ignore
    } else if (templateName.equals("given name")) {
      writer.append(givenName(parameterMap));
    } else {
      super.substituteTemplateCall(templateName, parameterMap, writer);
    }
  }

  private String givenName(Map<String, String> parameterMap) {
    String gender = parameterMap.getOrDefault("1",
        parameterMap.getOrDefault("gender", ""));
    String article = parameterMap.get("A");
    String or = parameterMap.get("or");
    String diminutive = parameterMap.get("diminutive");
    // TODO: there is sometimes the origin of the given name (e.g. a Japanese male given name)
    StringBuilder result = new StringBuilder();
    if (null == article) {
      result.append("A ");
    } else {
      result.append(article).append(" ");
    }
    if (null != diminutive) {
      result.append("diminutive of the ");
    }
    result.append(gender).append(" ");
    if (null != or) {
      result.append("or ").append(or).append(" ");
    }
    result.append("given name");
    if (null != diminutive) {
      result.append(" ").append(diminutive);
    }
    return result.toString();
  }

}

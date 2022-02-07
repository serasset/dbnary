package org.getalp.dbnary.languages.eng;

import info.bliki.wiki.filter.PlainTextConverter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.bliki.DbnaryWikiModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnglishDefinitionExtractorWikiModel extends DbnaryWikiModel {

  private final Logger log = LoggerFactory.getLogger(EnglishDefinitionExtractorWikiModel.class);

  // static Set<String> ignoredTemplates = new TreeSet<String>();
  // static {
  // ignoredTemplates.add("Wikipedia");
  // ignoredTemplates.add("Incorrect");
  // }

  private final IWiktionaryDataHandler delegate;


  public EnglishDefinitionExtractorWikiModel(IWiktionaryDataHandler we, Locale locale,
      String imageBaseURL, String linkBaseURL) {
    this(we, null, locale, imageBaseURL, linkBaseURL);
  }

  public EnglishDefinitionExtractorWikiModel(IWiktionaryDataHandler we, WiktionaryPageSource wi,
      Locale locale, String imageBaseURL, String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL);
    this.delegate = we;
  }

  public void parseDefinition(String definition, int defLevel) {
    // Render the definition to plain text, while ignoring the example template
    // log.trace("extracting definitions in {}", this.getPageName());
    log.debug("Parsing definition : ||| {} ||| in {}", definition, delegate.currentPagename());
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

  private static HashSet<String> ignoredTemplates = new HashSet<>();
  static {
    ignoredTemplates.add("categorize");
    ignoredTemplates.add("catlangname");
    ignoredTemplates.add("catlangcode");
    ignoredTemplates.add("senseid"); // Check if this template could be used to identify the sense
    ignoredTemplates.add("rfex");
    ignoredTemplates.add("rfd-sense");
    ignoredTemplates.add("attention");
    ignoredTemplates.add("attn");
    ignoredTemplates.add("rfclarify");
    ignoredTemplates.add("rfquote");
    ignoredTemplates.add("rfquotek");
    ignoredTemplates.add("rfv-sense");
    ignoredTemplates.add("rfc-sense");
    ignoredTemplates.add("rfquote-sense");
    ignoredTemplates.add("rfdef");
    ignoredTemplates.add("tea room sense");
    ignoredTemplates.add("rfd-redundant");
    ignoredTemplates.add("wikipedia");
    ignoredTemplates.add("wp");
    ignoredTemplates.add("slim-wikipedia");
    ignoredTemplates.add("swp");
    ignoredTemplates.add("slim-wp");
  }

  @SuppressWarnings("StatementWithEmptyBody")
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
    } else if (templateName.equals("glossary")) {
      String text = parameterMap.get("2");
      if (null == text)
        text = parameterMap.get("1");
      writer.append(text);
    } else if (templateName.equals("gloss")) {
      String text = parameterMap.get("gloss");
      if (null == text)
        text = parameterMap.get("1");
      writer.append("(").append(text).append(")");
    } else if (templateName.equals("check deprecated lang param usage")) {
      writer.append(parameterMap.get("1"));
    } else if (templateName.equals("context") || templateName.equals("cx")) {
      log.debug("Obsolete Context template in {}", this.getPageName());
    } else if (templateName.equals("l") || templateName.equals("link") || templateName.equals("m")
        || templateName.equals("mention")) {
      String l = parameterMap.get("3");
      if (null == l) {
        l = parameterMap.get("2");
      }
      writer.append(l);
    } else if (templateName.equals("synonym of") || templateName.equals("ellipsis of")
        || templateName.equals("initialism of") || templateName.equals("init of")
        || templateName.equals("acronym of")) {
      // TODO: handle synonym of by creating the appropriate synonymy relation.
      // catch and expand synonym of template before it is caught by next condition.
      super.substituteTemplateCall(templateName, parameterMap, writer);
    } else if (templateName.endsWith(" of")) {
      log.debug("Ignoring template {} in definition of {}", templateName, this.getPageName());
    } else if (ignoredTemplates.contains(templateName)) {

    } else if (templateName.equals("given name")) {
      writer.append(givenName(parameterMap));
    } else if (templateName.equals("quote-book")) {
      // TODO: example cannot be registered while transcluding as the lexical sense is not available
      // yet.
      // StringWriter quotation = new StringWriter();
      // super.substituteTemplateCall(templateName, parameterMap, quotation);
      // delegate.registerExample(quotation.toString(), null);
    } else if (templateName.equals("non-gloss definition") || templateName.equals("n-g")
        || templateName.equals("ngd") || templateName.equals("non-gloss")
        || templateName.equals("non gloss")) {
      String def = parameterMap.getOrDefault("1", "");
      writer.append(def);
    } else {
      // log.debug("BEGIN >>> Subtituting template {} in page {}", templateName,
      // delegate.currentLexEntry());
      super.substituteTemplateCall(templateName, parameterMap, writer);
      // log.debug("END <<< Subtituting template {} in page {}", templateName,
      // delegate.currentLexEntry());
    }
  }

  private String givenName(Map<String, String> parameterMap) {
    String gender = parameterMap.getOrDefault("1", parameterMap.getOrDefault("gender", ""));
    String article = parameterMap.get("A");
    if (null != article && article.length() == 0) {
      article = null;
    }
    String or = parameterMap.get("or");
    String dimtype = parameterMap.get("dimtype");
    ArrayList<String> equivalents = listArgs(parameterMap, "eq");
    ArrayList<String> diminutives = listArgs(parameterMap, "dim");
    if (diminutives.size() == 0) {
      diminutives = listArgs(parameterMap, "diminutive");
    }
    // TODO: there is sometimes the origin of the given name (e.g. a Japanese male given name)
    StringBuilder result = new StringBuilder();
    if (null == article) {
      result.append("A ");
    } else {
      result.append(article).append(" ");
    }
    if (diminutives.size() > 0) {
      if (null != dimtype) {
        result.append(dimtype);
        result.append(" ");
      }
      result.append("diminutive of the ");
    }
    result.append(gender).append(" ");
    if (null != or) {
      result.append("or ").append(or).append(" ");
    }
    result.append("given name");
    if (diminutives.size() > 1) {
      result.append("s");
    }
    appendList(result, diminutives, " ", "");
    appendList(result, equivalents, ", equivalent to English ", "");
    return result.toString();
  }

  private void appendList(StringBuilder res, ArrayList<String> list, String before, String after) {
    if (list.size() > 0) {
      res.append(before);
      res.append(list.get(0));
      for (int i = 1; i < list.size(); i++) {
        if (i == list.size() - 1) {
          res.append(" or ");
        } else {
          res.append(", ");
        }
        res.append(list.get(i));
      }
      res.append(after);
    }
  }

  private ArrayList<String> listArgs(Map<String, String> args, String arg) {
    ArrayList<String> res = new ArrayList<>();
    String eq = args.get(arg);
    int i = 2;
    while (null != eq) {
      res.add(eq);
      eq = args.get(arg + i);
      i++;
    }
    return res;
  }

}

package org.getalp.dbnary.languages.zho;

import java.util.Map.Entry;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.wiki.WikiText;
import org.getalp.dbnary.wiki.WikiText.Template;
import org.getalp.dbnary.wiki.WikiText.Token;
import org.getalp.dbnary.wiki.WikiText.WikiContent;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class ChinesePronunciationExtractorWikiModel extends ChineseDbnaryWikiModel {

  private static Logger log = LoggerFactory.getLogger(ChinesePronunciationExtractorWikiModel.class);

  private IWiktionaryDataHandler delegate;

  private HashMap<String, String> dialectCodeList = new HashMap<>();

  {
    //dialectCodeList.put("北方話", "zh-cnp-fonipa");
    dialectCodeList.put("粵語", "yue-fonipa");
    dialectCodeList.put("客家語", "hak-fonipa");
    dialectCodeList.put("閩東語", "cdo-fonipa");
    dialectCodeList.put("閩南語", "nan-fonipa");
    dialectCodeList.put("吳語", "wuu-fonipa");
    dialectCodeList.put("閩北語", "mnp-fonipa");
    dialectCodeList.put("晉語", "cjy-fonipa");
    dialectCodeList.put("贛語", "gan-fonipa");
    dialectCodeList.put("普通話", "cmn-fonipa");
    dialectCodeList.put("粤语", "yue-fonipa");
    dialectCodeList.put("客家语", "hak-fonipa");
    dialectCodeList.put("闽东语", "cdo-fonipa");
    dialectCodeList.put("闽南语", "nan-fonipa");
    dialectCodeList.put("吴语", "wuu-fonipa");
    dialectCodeList.put("闽北语", "mnp-fonipa");
    dialectCodeList.put("晋语", "cjy-fonipa");
    dialectCodeList.put("赣语", "gan-fonipa");
    dialectCodeList.put("普通话", "cmn-fonipa");
  }

  public ChinesePronunciationExtractorWikiModel(IWiktionaryDataHandler wdh, WiktionaryPageSource wi,
      Locale locale,
      String imageBaseURL, String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL);
    this.delegate = wdh;
  }

  private static String toZhPronCall(Template t) {
    // Build {{zh-phon|...}}
    StringBuilder code = new StringBuilder();
    code.append("{{zh-pron|");
    for (Entry<String, WikiContent> arg : t.getArgs().entrySet()) {
      // 國       = <!-- {{國音| }} -->                    m
      // | 粵       = <!-- {{粵音| }} -->                  c
      // | 平話字   =
      // | 台湾方言音符号 =
      // | 白話字   =
      // | 臺羅拼音 =
      // | 滬       =
      // | 客       =
      // | 四縣     =
      // | 寧       =
      if ("國".equals(arg.getKey())) {
        // concat all args of the 國音 template
        StringBuilder pinyinBuilder = new StringBuilder();
        arg.getValue().templates().stream().map(Token::asTemplate)
            .filter(pt -> pt.getName().equals("國音")||pt.getName().equals("国音")).findFirst().ifPresent(pt -> pt.getParsedArgs().forEach((key, val) -> {
              if (key.matches("\\d+")) {
                pinyinBuilder.append(val);
              }
            }));
        String pinyin = pinyinBuilder.toString();
        if (pinyin.length() > 0) {
          code.append("m=");
          code.append(pinyin);
          code.append("|");
        }
      } else if ("粵".equals(arg.getKey())) {
        // WARN, here, append a space after the numbers
        StringBuilder pinyinBuilder = new StringBuilder();
        arg.getValue().templates().stream().map(Token::asTemplate).filter(template -> template.getName().equals("粵音")||template.getName().equals("粵音/空")||template.getName().equals("粤音")||template.getName().equals("粤音/空")).findFirst().ifPresent(template -> template.getParsedArgs().forEach((key, val) -> {
         if(key.matches("\\d+")){
            pinyinBuilder.append(val);
         }
         if(val.matches("\\d+"))
           pinyinBuilder.append(" ");
        }));
        String pinyin = pinyinBuilder.toString();
        if(pinyin.length()>0){
          code.append("c=");
          code.append(pinyin);
          code.append("|");
        }
      } else {

      }
      // append the cat (should we change the category ?)
    }
    code.append("cat=n}}");
    return code.toString();
  }

  public void parsePronunciation(String templateCall) {
    // TODO: get the pinyin and register with language zh-Latn, yue-Latn
    if (templateCall.contains("{{汉语读音") || templateCall.contains("{{漢語讀音")) {
      WikiText text = new WikiText(templateCall);
      StringBuilder translatedCallBuilder = new StringBuilder();
      text.templates().stream().map(Token::asTemplate).filter(t ->
              (t.getName().equals("汉语读音") || t.getName().equals("漢語讀音")))
          .forEach(t ->
            translatedCallBuilder.append(toZhPronCall(t)));
      templateCall = translatedCallBuilder.toString();
    }
    String html = expandWikiCode(templateCall);

    //System.out.println(html);
    Document doc = Jsoup.parse(html);
    if (null == doc) {
      return;
    }
    Elements phonetics = doc.getElementsByTag("span");
    for (Element phonetic : phonetics) {
      if (phonetic.className().equals("IPA")) {
        Elements parents = phonetic.parents();
        Element dialectList = null;
        for (int i = 0; i < parents.size(); i++) {
          if (parents.get(i).tagName().equals("li")) {
            dialectList = parents.get(i);
          }
        }
        if (dialectList == null) {
          log.debug("Extract pronunciation: can't find the location of the Phonetic Alphabet");
        }
        String dialectName = dialectList.child(0).text();
        String dialectCode = dialectCodeList.get(dialectName);
        if (dialectCode == null) {
          log.trace("Extract pronunciation: There is no corresponding code for this dialect: "
              + dialectName);
          delegate.registerPronunciation(phonetic.text(), "zh-fonipa");
        } else {
          delegate.registerPronunciation(phonetic.text(), dialectCode);
        }
      }
    }


  }

}

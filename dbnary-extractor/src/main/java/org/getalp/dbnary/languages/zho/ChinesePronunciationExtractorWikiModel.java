package org.getalp.dbnary.languages.zho;

import java.io.IOException;
import java.util.IllformedLocaleException;
import java.util.Locale.Builder;
import java.util.Map;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChinesePronunciationExtractorWikiModel extends ChineseDbnaryWikiModel {
  private static final Logger log =
      LoggerFactory.getLogger(ChinesePronunciationExtractorWikiModel.class);
  private final IWiktionaryDataHandler delegate;
  private static final HashMap<String, String> dialectCodeList = new HashMap<>();
  static {
    dialectCodeList.put("官話", "zh");
    dialectCodeList.put("北方話", "zh");
    dialectCodeList.put("粵語", "yue");
    dialectCodeList.put("客家語", "hak");
    dialectCodeList.put("閩南語", "nan");
    dialectCodeList.put("閩東語", "cdo");
    dialectCodeList.put("吳語", "wuu");
    dialectCodeList.put("閩北語", "mnp");
    dialectCodeList.put("晉語", "cjy");
    dialectCodeList.put("贛語", "gan");
    dialectCodeList.put("湘語", "hsn");
  }
  private final HashMap<String, String> phonemicSystemList = new HashMap<>();
  {
    phonemicSystemList.put("汉语拼音", "Latn");
    phonemicSystemList.put("國際音標", "fonipa");// IPA
    phonemicSystemList.put("四川話拼音", "Latn");// Sichuanese Pinyin
    phonemicSystemList.put("四川话拉丁化新文字", "Latn");// Scuanxua Ladinxua Sin Wenz
    phonemicSystemList.put("國語羅馬字", "Latn-GR");// Gwoyeu Romatzyh
    phonemicSystemList.put("威式拼音", "Latn-WG");// Wade–Giles
    phonemicSystemList.put("客家話拼音", "Latn");// Hakka Pinyin
    phonemicSystemList.put("客家語拼音", "Latn");// Hakka Pinyin
    phonemicSystemList.put("平話字", "Latn-BUC");// Bàng-uâ-cê
    phonemicSystemList.put("廣州話拼音", "Latn");// Cantonese Pinyin
    phonemicSystemList.put("廣東拼音", "Latn");// Cantonese Pinyin
    phonemicSystemList.put("拼音", "Latn");// Pinyin
    phonemicSystemList.put("普實台文", "Latn-PSDB");// Phofsit Daibuun
    phonemicSystemList.put("模仿白話字", "Latn-POJ");// Pe̍h-uē- 台语罗马字 白话字 台語羅馬字 教会罗马字
    phonemicSystemList.put("白話字", "Latn-POJ");
    phonemicSystemList.put("粵拼", "Jyutping");// Jyutping
    phonemicSystemList.put("維基詞典", "Latn-Wiki");// Wiktionary
    phonemicSystemList.put("耶魯粵拼", "Latn-YaleRomanization"); // Yale romanization of Cantonese
    phonemicSystemList.put("臺羅拼音", "Latn");// The official romanization system for Taiwanese Hokkien
                                           // in Taiwanese Hokkien
    phonemicSystemList.put("西里爾字母", "Cyrl");// Cyrillic
    phonemicSystemList.put("通用拼音", "Latn");// a romanization system for Mandarin Chinese used in
                                           // Taiwan
    phonemicSystemList.put("注音", "Bopo");// Bopomofo zhuyin
    phonemicSystemList.put("臺灣話", "TW");// Here is one location name, in order to adapt the method
                                        // parsePronunciation2
  }
  private static final HashMap<String, String> locationList = new HashMap<>();
  static {
    locationList.put("福建", "CN-FJ"); // Fujian
    locationList.put("Beijing dialect", "CN-BJ"); // Beijing
    locationList.put("北京方言", "CN-BJ"); // Beijing
    locationList.put("北京話", "CN-BJ"); // Beijing
    locationList.put("北四縣話", "ignored");
    locationList.put("南四縣話", "ignored");
    locationList.put("南昌", "CN-JX"); // Nanchang
    locationList.put("台山話", "CN-GD"); // Taishan is a sub district of Guangdong region
    locationList.put("四縣話", "ignored");
    locationList.put("太原話", "CN-SX"); // Taiyuan
    locationList.put("廣州話", "CN-GD"); // Guangzhou
    locationList.put("建甌", "CN-FJ"); // Jianou in Fujian region
    locationList.put("成都話", "CN-SC"); // Chengdu
    locationList.put("東干語", "ignored");// dng
    locationList.put("梅州話", "CN-GD"); // Meizhou is a sub district of Guangdong region
    locationList.put("潮州話", "CN-GD"); // Chaozhou is a sub district of Guangdong region
    locationList.put("福州話", "CN-FJ"); // Fuzhou (capitale du Fujian)
    locationList.put("長沙話", "CN-HN"); // Changsha (capitale du Hunan)
    locationList.put("非標準或方言", "ignored");
    locationList.put("上海話", "CN-SH");
    locationList.put("官話", "ignored");
    locationList.put("台山話，台城", "CN-GD"); // Taishan is a sub district of Guangdong region
  }

  public ChinesePronunciationExtractorWikiModel(IWiktionaryDataHandler wdh, WiktionaryPageSource wi,
      Locale locale, String imageBaseURL, String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL);
    this.delegate = wdh;
  }

  private String toZhPronCall(Template t) {
    // Build {{zh-phon|...}}
    StringBuilder code = new StringBuilder();
    code.append("{{zh-pron|");
    for (Entry<String, WikiContent> arg : t.getArgs().entrySet()) {
      // 國 = <!-- {{國音| }} --> m
      // | 粵 = <!-- {{粵音| }} --> c
      // | 漢語拼音 =
      // | 平話字 = This template is not actually used
      // | 台湾方言音符号 = This template is not actually used
      // | 白話字 =
      // | 臺羅拼音 =
      // | 滬 =
      // | 客 = This template is not actually used
      // | 四縣 = This template is not actually used
      // | 寧 = This template is not actually used
      if ("國".equals(arg.getKey())) {
        // concat all args of the 國音 template
        StringBuilder pinyinBuilder = new StringBuilder();
        arg.getValue().templates().stream().map(Token::asTemplate)
            .filter(pt -> pt.getName().equals("國音") || pt.getName().equals("国音")).findFirst()
            .ifPresent(pt -> pt.getParsedArgs().forEach((key, val) -> {
              if (key.matches("\\d+")) {
                pinyinBuilder.append(val);
                pinyinBuilder.append(" ");
              }
              if (key.contains("注") && pinyinBuilder.length() != 0) {
                if (pinyinBuilder.charAt(pinyinBuilder.length() - 1) == ' ') {
                  pinyinBuilder.deleteCharAt(pinyinBuilder.length() - 1);
                }
                pinyinBuilder.append(",");
              }
            }));
        String pinyin = pinyinBuilder.toString();
        if (pinyin.length() > 0) {
          while (pinyin.charAt(pinyin.length() - 1) == ' '
              || pinyin.charAt(pinyin.length() - 1) == ',') {
            pinyin = pinyin.substring(0, pinyin.length() - 1);
          }
          code.append("m=");
          code.append(pinyin);
          code.append("|");
        }
      } else if ("粵".equals(arg.getKey())) {
        // WARN, here, append a space after the numbers
        StringBuilder pinyinBuilder = new StringBuilder();
        arg.getValue().templates().stream().map(Token::asTemplate)
            .filter(template -> template.getName().equals("粵音") || template.getName().equals("粵音/空")
                || template.getName().equals("粤音") || template.getName().equals("粤音/空"))
            .findFirst().ifPresent(template -> template.getParsedArgs().forEach((key, val) -> {
              if (key.matches("\\d+")) {
                pinyinBuilder.append(val);
              }
              if (val.matches("\\d+"))
                pinyinBuilder.append(" ");
            }));
        String pinyin = pinyinBuilder.toString();
        if (pinyin.length() > 0) {
          code.append("c=");
          pinyin =
              pinyin.charAt(pinyin.length() - 1) == ' ' ? pinyin.substring(0, pinyin.length() - 1)
                  : pinyin;
          code.append(pinyin);
          code.append("|");
        }
      } else if ("漢語拼音".equals(arg.getKey())) {
        String pinyin = arg.getValue().toString();
        if (pinyin.length() > 0) {
          code.append("m=");
          code.append(pinyin);
          code.append("|");
        }
      } else if ("滬".equals(arg.getKey())) {
        String htmlInformContainPhonetic = expandWikiCode(arg.getValue().toString());
        Document doc = Jsoup.parse(htmlInformContainPhonetic);
        Elements wuPhonetics = doc.getElementsByClass("IPA");
        for (Element wuPhonetic : wuPhonetics) {
          delegate.registerPronunciation(wuPhonetic.text(), "wuu-fonipa");
        }
      } else if ("臺羅拼音".equals(arg.getKey())) {
        delegate.registerPronunciation(arg.getValue().toString(), "nan-Latn");
      } else if ("白話字".equals(arg.getKey())) {
        delegate.registerPronunciation(arg.getValue().toString(), "nan-POJ");
      } else {
        log.trace("Extract pronunciation：this template: " + arg.getKey() + " was ignored");
      }
    }
    code.append("cat=n}}");
    return code.toString();
  }

  public void parsePronunciation2(String templateCall) {
    WikiText text = new WikiText(templateCall);
    WikiContent content = text.content();
    for (Token t : content.wikiTokens()) {
      if (t instanceof WikiText.ListItem) {
        String phonemicSystemName = null;
        String phonemicSystemCode;
        String Tag;
        WikiContent smallContent = ((WikiText.ListItem) t).getContent();
        for (Token smallT : smallContent.wikiTokens()) {
          if (smallT instanceof WikiText.InternalLink) {
            phonemicSystemName = ((WikiText.InternalLink) smallT).getLinkText();
          }
        }
        if (phonemicSystemName == null) {
          phonemicSystemName =
              t.getText().substring(1, t.getText().length() - 1).split("：")[0].trim();
        }
        phonemicSystemCode = phonemicSystemList.get(phonemicSystemName);
        if (phonemicSystemCode.equals("Jyutping")) {
          Tag = "yue-Jyutping";
        } else {
          Tag = "zh-" + phonemicSystemCode;
        }
        String pronunciation = t.getText().split("：")[1];
        delegate.registerPronunciation(pronunciation, Tag);
      }
    }
  }

  static Locale.Builder localeBuilder = new Builder();

  public void parsePronunciation(String templateCall) {
    if (templateCall.contains("* [[")) {
      parsePronunciation2(templateCall);
    } else {
      if (templateCall.contains("{{汉语读音") || templateCall.contains("{{漢語讀音")) {
        WikiText text = new WikiText(templateCall);
        StringBuilder translatedCallBuilder = new StringBuilder();
        text.templates().stream().map(Token::asTemplate)
            .filter(t -> (t.getName().equals("汉语读音") || t.getName().equals("漢語讀音")))
            .forEach(t -> translatedCallBuilder.append(toZhPronCall(t)));
        templateCall = translatedCallBuilder.toString();
      }
      String html = expandWikiCode(templateCall);
      Document doc = Jsoup.parse(html);
      Elements divs = doc.getElementsByTag("div");
      Element usefulParte = null;
      for (Element div : divs) {
        if (div.hasClass("vshide"))
          usefulParte = div;
      }
      if (usefulParte != null) {
        Elements lists = usefulParte.getElementsByTag("li");
        for (Element list : lists) {
          if (isLastLi(list)) {
            String languageTypeCode = getLanguageType(list);
            String phonemicSystemCode = getPhonemicSystem(list);
            String locationName = getLocationName(list);
            String tag = getPronunciationTag(languageTypeCode, phonemicSystemCode, locationName);
            String pronunciation = getPronunciation(list);
            try {
              // Check the language using localeBuilder. It will throw an exception if incorrect
              localeBuilder.setLanguageTag(tag);
              delegate.registerPronunciation(pronunciation, tag);
            } catch (IllformedLocaleException e) {
              // Just ignore
              log.trace("Illformed Locale ignored: {} || {}", tag, getPageName());
            }
          }
        }
      }
    }
  }

  public static boolean isLastLi(Element li) {
    Elements children = li.children();
    for (Element child : children) {
      if (child.tagName().equals("li") || child.tagName().equals("ul")) {
        return false;
      }
    }
    return true;
  }

  public static String getLanguageType(Element li) {
    Elements parents = li.parents();
    Element dialectList = null;
    for (Element parent : parents) {
      if (parent.tagName().equals("li")) {
        dialectList = parent;
      }
    }
    if (null == dialectList)
      return null;
    return dialectCodeList.get(dialectList.child(0).text());
  }

  public static String getLocationName(Element li) {
    Element parent = li.parent();
    // Element location = parent.previousElementSibling().text().equals("+")?
    // parent.previousElementSibling().previousElementSibling():parent.previousElementSibling();
    Element location = parent != null ? parent.previousElementSibling() : null;
    if (null == location)
      return "";
    if (location.tagName().equals("sup")) {
      location = location.previousElementSibling();
    }
    if (null == location)
      return "";
    Pattern locationPattern = Pattern.compile("\\((.*)\\)");
    String locationText = location.text();
    Matcher locationMatcher = locationPattern.matcher(locationText);
    String locationName = null;
    if (locationMatcher.find()) {
      locationName = locationMatcher.group(1);
    }
    StringBuilder locationCode = new StringBuilder(15);
    if (null == locationName)
      return "";
    if (locationName.contains("福建:")) {
      locationCode.append("Fujian");
    } else if (locationName.contains("官話,")) {
      if (locationName.contains("Mainland") || locationName.contains("大陸")) {
        locationCode.append("Mainland");
      }
      if (locationName.contains("Taiwan") || locationName.contains("臺灣")
          || locationName.contains("台灣")) {
        locationCode.append("TW");
      } else {
        locationCode.append("ignored");
      }
    } else if (locationList.get(locationName) != null) {
      String NameCode = locationList.get(locationName);
      locationCode.append(NameCode);
    } else {
      locationCode.append("ignored");
    }
    return String.valueOf(locationCode);
  }

  public String getPhonemicSystem(Element li) {
    String phonemicSystemName;
    if (li.text().contains("：")) {
      phonemicSystemName = li.text().split("：")[0];
    } else if (li.text().contains(":")) {
      phonemicSystemName = li.text().split(":")[0];
    } else {
      log.trace("Extraction : can't find the phonemic System Name");
      return "ignored";
    }
    if (phonemicSystemName.contains(" ")) {
      phonemicSystemName = phonemicSystemName.split(" ")[0];
    }
    return phonemicSystemList.get(phonemicSystemName);
  }

  public String getPronunciationTag(String languageType, String phonemicSystem,
      String locationName) {
    StringBuilder pronunciationTag = new StringBuilder(30);
    if (languageType != null && !languageType.equals("ignored")) {
      pronunciationTag.append(languageType);
    } else {
      pronunciationTag.append("zh");
    }
    if (phonemicSystem != null && !phonemicSystem.equals("ignored")) {
      pronunciationTag.append("-").append(phonemicSystem);
    } else {
      pronunciationTag.append("-unKnownSystem");
    }
    if (locationName != null && !locationName.equals("ignored")) {
      pronunciationTag.append("-").append(locationName);
    }

    return String.valueOf(pronunciationTag);
  }

  public String getPronunciation(Element li) {
    String pronunciation;
    if (li.text().contains("：")) {
      pronunciation = li.text().split("：")[1];
    } else if (li.text().contains(":")) {
      pronunciation = li.text().split(":")[1];
    } else {
      log.debug("Extract pronunciation : there is no pronunciation");
      pronunciation = null;
    }
    return pronunciation;
  }
}

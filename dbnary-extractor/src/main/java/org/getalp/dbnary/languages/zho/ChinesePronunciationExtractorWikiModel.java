package org.getalp.dbnary.languages.zho;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Locale;

public class ChinesePronunciationExtractorWikiModel extends ChineseDbnaryWikiModel {
  private static Logger log = LoggerFactory.getLogger(ChinesePronunciationExtractorWikiModel.class);

  private IWiktionaryDataHandler delegate;

  private HashMap<String, String> dialectCodeList = new HashMap<String, String>();
    {
      //dialectCodeList.put("北方話", "zh-cnp-fonipa");
      dialectCodeList.put("粵語", "zh-yue-fonipa");
      dialectCodeList.put("客家語", "zh-hak-fonipa");
      dialectCodeList.put("閩東語", "zh-cdo-fonipa");
      dialectCodeList.put("閩南語", "zh-nan-fonipa");
      dialectCodeList.put("吳語", "zh-wuu-fonipa");
      dialectCodeList.put("閩北語","zh-mnp-fonipa");
      dialectCodeList.put("晉語","zh-cjy-fonipa");
      dialectCodeList.put("贛語","zh-gan-fonipa");
      dialectCodeList.put("普通話","zh-cmn-fonipa");
      dialectCodeList.put("粤语", "zh-yue-fonipa");
      dialectCodeList.put("客家语", "zh-hak-fonipa");
      dialectCodeList.put("闽东语", "zh-cdo-fonipa");
      dialectCodeList.put("闽南语", "zh-nan-fonipa");
      dialectCodeList.put("吴语", "zh-wuu-fonipa");
      dialectCodeList.put("闽北语","zh-mnp-fonipa");
      dialectCodeList.put("晋语","zh-cjy-fonipa");
      dialectCodeList.put("赣语","zh-gan-fonipa");
      dialectCodeList.put("普通话","zh-cmn-fonipa");
    }

  public ChinesePronunciationExtractorWikiModel(IWiktionaryDataHandler wdh, WiktionaryPageSource wi, Locale locale,
                                                String imageBaseURL, String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL);
    this.delegate=wdh;
  }
  public void parsePronunciation(String templateCall) {
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
        if(dialectCode==null){
          log.trace("Extract pronunciation: There is no corresponding code for this dialect: "+dialectName);
          delegate.registerPronunciation(phonetic.text(),"zh-fonipa");
        }
        else
          delegate.registerPronunciation(phonetic.text(),dialectCode);
    }
  }

  }




}

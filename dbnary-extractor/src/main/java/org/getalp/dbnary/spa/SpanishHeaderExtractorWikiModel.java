package org.getalp.dbnary.spa;

import info.bliki.wiki.filter.WikipediaParser;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import org.getalp.dbnary.IWiktionaryDataHandler;
import org.getalp.dbnary.WiktionaryIndex;
import org.getalp.dbnary.bliki.DbnaryWikiModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpanishHeaderExtractorWikiModel extends DbnaryWikiModel {

  private IWiktionaryDataHandler delegate;

  private Logger log = LoggerFactory.getLogger(SpanishHeaderExtractorWikiModel.class);

  public SpanishHeaderExtractorWikiModel(IWiktionaryDataHandler we, Locale locale,
      String imageBaseURL, String linkBaseURL) {
    this(we, null, locale, imageBaseURL, linkBaseURL);
  }

  public SpanishHeaderExtractorWikiModel(IWiktionaryDataHandler we, WiktionaryIndex wi,
      Locale locale, String imageBaseURL, String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL);
    this.delegate = we;
  }

  public void parseHeaderBlock(String block) {
    initialize();
    if (block == null) {
      return;
    }
    log.trace("[{}] HEADER = {}", getPageName(), block);
    WikipediaParser.parse(block, this, true, null);
    initialize();
  }


  static final String[] pronunciationVariants =
      new String[] {"s", "c", "ll", "y", "yc", "ys", "lls", "llc"};


  private boolean isApi(String s) {
    if (s == null) {
      return true;
    }
    s = s.toLowerCase().trim();
    return s.equals("-") || s.equals("afi") || s.equals("");
  }

  private static final String[] PRON_GRAF_IGNORABLE_ARGS = {"leng", "lang", "división",
      "longitud_silábica", "ls", "número_letras", "nl", "dnota", "parónimo", "p", "homófono", "h",
      "halt", "hnum", "hnúm", "htr", "hnota", "acentuación", "ac"};

  private static final String[] ALT_PREFIXES = {"", "2", "3", "4", "5", "6"};

  @Override
  // TODO: handle pronunciation that use the pron-graf template.
  public void substituteTemplateCall(String templateName, Map<String, String> parameterMap,
      Appendable writer) throws IOException {
    if ("pronunciación".equalsIgnoreCase(templateName)) {
      String p1;
      if ((p1 = parameterMap.get("1")) != null && p1.length() > 0) {
        if (isApi(parameterMap.get("2"))) {
          if (!p1.equals("-") && !p1.equals("&nbsp;")) {
            delegate.registerPronunciation(p1, "es-ipa");
          }
          parameterMap.remove("1");
          parameterMap.remove("2");
          // DONE: maybe register alternate pronunciations
          for (String p : pronunciationVariants) {
            if (parameterMap.get(p) != null) {
              delegate.registerPronunciation(parameterMap.get(p), "es-" + p + "-ipa");
              parameterMap.remove(p);
            }
          }
          parameterMap.remove("leng");
          parameterMap.remove("lang");
          if (parameterMap.size() != 0) {
            log.debug("Remaining pronunciations : {} in {}", parameterMap, this.getPageName());
          }
        } else {
          log.debug("Unknown pronunciation transcription {} in {}", parameterMap.get("2"),
              this.getPageName());
        }
      }
    } else if ("pron-graf".equalsIgnoreCase(templateName)) {
      normalizeFirstParam(parameterMap);
      for (String s : ALT_PREFIXES)
        extractPhoneticRep(s + "fone", parameterMap);


      parameterMap.remove("leng");
      parameterMap.remove("lang");
      for (String s : PRON_GRAF_IGNORABLE_ARGS)
        parameterMap.remove(s);
      if (parameterMap.size() != 0) {
        log.debug("Remaining args in pron-graf : {} in {}", parameterMap, getPageName());
      }
    }
  }

  private void extractPhoneticRep(String foneKey, Map<String, String> parameterMap) {
    String fone = parameterMap.get(foneKey);
    if (fone != null && fone.length() > 0) {
      if (!fone.equals("-") && !fone.equals("&nbsp;")) {
        delegate.registerPronunciation(fone, "es-ipa");
      }
      parameterMap.remove(foneKey);
      int i = 2;
      while (true) {
        String fonei = parameterMap.get(foneKey + i);
        if (null == fonei)
          break;
        if (!"".equals(fonei) && !fone.equals("-") && !fone.equals("&nbsp;")) {
          delegate.registerPronunciation(fonei, "es-ipa");
        }
        parameterMap.remove(foneKey + i);
        i++;
      }
    }

  }

  private void normalizeFirstParam(Map<String, String> parameterMap) {
    String fone = parameterMap.get("1");
    if (null != fone) {
      if (null != parameterMap.get("fone")) {
        log.warn("fone arg and first args are both given in pron-graf template in {}",
            getPageName());
        // Use the fone parameter value as it is usually the correct one.
        fone = parameterMap.get("fone");
      }
      parameterMap.remove("1");
      parameterMap.put("fone", fone);
    }
  }

}

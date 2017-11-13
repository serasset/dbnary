package org.getalp.dbnary.zho;

import info.bliki.wiki.filter.WikipediaParser;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.jena.rdf.model.Resource;
import org.getalp.dbnary.AbstractGlossFilter;
import org.getalp.dbnary.DbnaryWikiModel;
import org.getalp.dbnary.IWiktionaryDataHandler;
import org.getalp.dbnary.WiktionaryIndex;
import org.getalp.iso639.ISO639_3;

public class ChineseTranslationExtractorWikiModel extends DbnaryWikiModel {

  private final AbstractGlossFilter glossFilter;
  private IWiktionaryDataHandler delegate;
  private int rank = 1;

  public ChineseTranslationExtractorWikiModel(IWiktionaryDataHandler we, Locale locale,
      String imageBaseURL, String linkBaseURL, AbstractGlossFilter glossFilter) {
    this(we, (WiktionaryIndex) null, locale, imageBaseURL, linkBaseURL, glossFilter);
  }

  public ChineseTranslationExtractorWikiModel(IWiktionaryDataHandler we, WiktionaryIndex wi,
      Locale locale, String imageBaseURL, String linkBaseURL, AbstractGlossFilter glossFilter) {
    super(wi, locale, imageBaseURL, linkBaseURL);
    this.delegate = we;
    this.glossFilter = glossFilter;
  }

  public void parseTranslationBlock(String block) {
    initialize();
    this.rank = 1;
    if (block == null) {
      return;
    }
    WikipediaParser.parse(block, this, true, null);
    initialize();
  }

  private Resource currentGloss = null;

  @Override
  public void substituteTemplateCall(String templateName,
      Map<String, String> parameterMap, Appendable writer)
      throws IOException {
    if ("trad".equals(templateName)) {
      // Trad macro contains a set of translations with no usage note.
      String lang = normalizeLang(parameterMap.get("1"));
      for (Entry<String, String> kv : parameterMap.entrySet()) {
        if ("1".equals(kv.getKey())) {
          continue;
        }
        delegate.registerTranslation(lang, currentGloss, null, kv.getValue());
      }
    } else if ("xlatio".equals(templateName) || "trad-".equals(templateName)) {
      // xlatio and trad- macro contains a translation and a transcription.
      String lang = normalizeLang(parameterMap.get("1"));
      delegate
          .registerTranslation(lang, currentGloss, parameterMap.get("3"), parameterMap.get("2"));
    } else if ("t".equals(templateName) || "t+".equals(templateName)) {
      // t macro contains a translation, a transcription and an usage note.
      String lang = normalizeLang(parameterMap.get("1"));
      String usage = parameterMap.get("3");
      if (null == usage) {
        usage = "";
      }
      String transcription = parameterMap.get("tr");
      if (null == transcription) {
        transcription = parameterMap.get("4");
      }
      if (null == transcription) {
        transcription = "";
      }

      if (!transcription.equals("")) {
        usage = "(" + transcription + "), " + usage;
      }
      delegate.registerTranslation(lang, currentGloss, usage, parameterMap.get("2"));
    } else if ("tradini".equals(templateName)) {
      String g = parameterMap.get("1");
      if (null != g) {
        g = g.trim();
        currentGloss = delegate.createGlossResource(glossFilter.extractGlossStructure(g), rank++);
      } else {
        currentGloss = null;
      }
    } else if ("tradini-checar".equals(templateName)) {
      currentGloss = null;
    } else if ("tradmeio".equals(templateName)) {
      // nop
    } else if ("tradfim".equals(templateName)) {
      currentGloss = null;
    } else if (ISO639_3.sharedInstance.getIdCode(templateName) != null) {
      // This is a template for the name of a language, just ignore it...
    } else {

    }
  }

  private String normalizeLang(String lang) {
    String normLangCode;
    if ((normLangCode = ISO639_3.sharedInstance.getIdCode(lang)) != null) {
      lang = normLangCode;
    }
    return lang;
  }

}

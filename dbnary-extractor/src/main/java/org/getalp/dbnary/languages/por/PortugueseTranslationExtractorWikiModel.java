package org.getalp.dbnary.languages.por;

import info.bliki.wiki.filter.WikipediaParser;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.jena.rdf.model.Resource;
import org.getalp.LangTools;
import org.getalp.dbnary.languages.AbstractGlossFilter;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.bliki.DbnaryWikiModel;

public class PortugueseTranslationExtractorWikiModel extends DbnaryWikiModel {

  private IWiktionaryDataHandler delegate;
  private int rank;

  public PortugueseTranslationExtractorWikiModel(IWiktionaryDataHandler we, Locale locale,
      String imageBaseURL, String linkBaseURL) {
    this(we, (WiktionaryPageSource) null, locale, imageBaseURL, linkBaseURL);
  }

  public PortugueseTranslationExtractorWikiModel(IWiktionaryDataHandler we, WiktionaryPageSource wi,
      Locale locale, String imageBaseURL, String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL);
    this.delegate = we;
  }

  public void parseTranslationBlock(String block) {
    initialize();
    rank = 1;
    if (block == null) {
      return;
    }
    WikipediaParser.parse(block, this, true, null);
    initialize();
  }

  private static final HashSet<String> transMacroWithNotes = new HashSet<>();

  static {
    transMacroWithNotes.add("xlatio");
    transMacroWithNotes.add("trad-");

  }

  private Resource currentGloss = null;

  @Override
  public void substituteTemplateCall(String templateName, Map<String, String> parameterMap,
      Appendable writer) throws IOException {
    if ("trad".equals(templateName)) {
      // Trad macro contains a set of translations with no usage note.
      String lang = LangTools.normalize(parameterMap.get("1"));
      for (Entry<String, String> kv : parameterMap.entrySet()) {
        if ("1".equals(kv.getKey())) {
          continue;
        }
        delegate.registerTranslation(lang, currentGloss, null, kv.getValue());
      }
    } else if ("xlatio".equals(templateName) || "trad-".equals(templateName)) {
      // xlatio and trad- macro contains a translation and a transcription.
      String lang = LangTools.normalize(parameterMap.get("1"));
      // if (null != parameterMap.get("4")) System.err.println("map has 4 params in " +
      // this.getImageBaseURL() +": " + parameterMap);
      delegate.registerTranslation(lang, currentGloss, parameterMap.get("3"),
          parameterMap.get("2"));
    } else if ("t".equals(templateName) || "t+".equals(templateName)) {
      // t macro contains a translation, a transcription and an usage note.
      String lang = LangTools.normalize(parameterMap.get("1"));
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
        currentGloss = delegate.createGlossResource(g, rank++);
      }
    } else if ("tradini-checar".equals(templateName)) {
      currentGloss = null;
    } else if ("tradmeio".equals(templateName)) {
      // nop
    } else if ("tradfim".equals(templateName)) {
      currentGloss = null;
    } else if (LangTools.normalize(templateName) != null) {
      // This is a template for the name of a language, just ignore it...
    } else {
      // System.err.println("Called template: " + templateName + " while parsing translations of: "
      // + this.getImageBaseURL());
      // Just ignore the other template calls (uncomment to expand the template calls).
      // super.substituteTemplateCall(templateName, parameterMap, writer);
    }
  }
}

package org.getalp.dbnary.languages.rus;

import info.bliki.wiki.filter.WikipediaParser;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.bliki.DbnaryWikiModel;

public class RussianMorphoExtractorWikiModel extends DbnaryWikiModel {

  static Set<String> russianPOS = new TreeSet<>();

  static {
    russianPOS.add("сущ");
    russianPOS.add("прил");
    russianPOS.add("прич");
    russianPOS.add("деепр");
    russianPOS.add("гл");
    russianPOS.add("adv");
    russianPOS.add("мест");
    russianPOS.add("числ");
    russianPOS.add("Фам");
    russianPOS.add("interj");
    russianPOS.add("suffix ");
    russianPOS.add("conj ");
    russianPOS.add("prep ");
    russianPOS.add("part ");
    russianPOS.add("predic "); // predicative ?
    russianPOS.add("intro "); // introductory word ?
    russianPOS.add("onomatop "); // introductory word ?
  }

  private IWiktionaryDataHandler delegate;
  private boolean hasAPOS = false;

  public RussianMorphoExtractorWikiModel(IWiktionaryDataHandler we, Locale locale,
      String imageBaseURL, String linkBaseURL) {
    this(we, (WiktionaryPageSource) null, locale, imageBaseURL, linkBaseURL);
  }

  public RussianMorphoExtractorWikiModel(IWiktionaryDataHandler we, WiktionaryPageSource wi,
      Locale locale, String imageBaseURL, String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL);
    this.delegate = we;
  }

  public boolean parseMorphoBlock(String block) {
    initialize();
    if (block == null) {
      return false;
    }
    WikipediaParser.parse(block, this, true, null);
    initialize();
    boolean r = hasAPOS;
    hasAPOS = false;
    return r;
  }

  @Override
  public void substituteTemplateCall(String templateName, Map<String, String> parameterMap,
      Appendable writer) throws IOException {
    String pos = getPOS(templateName);
    if (null != pos) {
      // This is a macro specifying the part Of Speech
      // TODO: extract other morphological information ?
      hasAPOS = true;
      delegate.initializeLexicalEntry(pos);
    } else {
      // Just ignore the other template calls (uncomment to expand the template calls).
      // super.substituteTemplateCall(templateName, parameterMap, writer);
    }
  }

  private String getPOS(String templateName) {
    for (String p : russianPOS) {
      if (templateName.startsWith(p)) {
        return p.trim();
      }
    }
    return null;
  }

}

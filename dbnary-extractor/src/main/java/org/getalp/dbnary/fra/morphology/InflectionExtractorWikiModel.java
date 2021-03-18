package org.getalp.dbnary.fra;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.getalp.dbnary.IWiktionaryDataHandler;
import org.getalp.dbnary.WiktionaryIndex;
import org.getalp.ontolex.model.LexicalForm;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * This wiki model handles the extraction of inflection tables that are to be found at the beginning
 * of adjectival and nominal lexical entries.
 *
 * @author serasset
 */
public class InflectionExtractorWikiModel extends MorphologyWikiModel {

  private static Logger log = LoggerFactory.getLogger(InflectionExtractorWikiModel.class);

  private WiktionaryDataHandler delegate;

  public InflectionExtractorWikiModel(IWiktionaryDataHandler wdh, WiktionaryIndex wi, Locale locale,
      String imageBaseURL, String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL);
    if (wdh instanceof WiktionaryDataHandler)
      this.delegate = (WiktionaryDataHandler) wdh;
    else
      throw new RuntimeException(
          "Invalid delegate class, expecting French Wiktionary Data Handler");
  }

  public void parseOtherForm(String templateCall, List<String> context) {
    log.trace("Extracting other forms in {}", this.getPageName());
    log.trace("Other form template call : {}", templateCall);
    log.trace("Global context = [[{}]]", String.join("|", context));

    String html = expandWikiCode(templateCall);

    if (html == null) {
      return; // failing silently: error message already given.
    }

    Document doc = Jsoup.parse(html);
    if (null == doc) {
      return;
    }
    Elements tables = doc.select("table");

    FrenchAccordsTableExtractor declinationExtractor =
        new FrenchAccordsTableExtractor(this.getPageName(), "fr", context);

    for (Element table : tables) {
      Set<LexicalForm> forms = declinationExtractor.parseTable(table);
      registerAllForms(forms);
    }
  }

  private void registerAllForms(Set<LexicalForm> forms) {
    for (LexicalForm form : forms) {
      delegate.addLexicalForm(form);
    }
  }

}

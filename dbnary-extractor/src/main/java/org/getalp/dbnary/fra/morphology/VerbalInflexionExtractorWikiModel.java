package org.getalp.dbnary.fra.morphology;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.getalp.dbnary.IWiktionaryDataHandler;
import org.getalp.dbnary.WiktionaryIndex;
import org.getalp.dbnary.fra.WiktionaryDataHandler;
import org.getalp.dbnary.morphology.RefactoredTableExtractor;
import org.getalp.model.ontolex.LexicalForm;
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
public class VerbalInflexionExtractorWikiModel extends MorphologyWikiModel {
  // TODODODODODODODODOOD : change the way pronounciations are rendered, add an span with a class
  // and use it to detect pronunciations.
  private static Logger log = LoggerFactory.getLogger(VerbalInflexionExtractorWikiModel.class);

  private WiktionaryDataHandler delegate;

  public VerbalInflexionExtractorWikiModel(IWiktionaryDataHandler wdh, WiktionaryIndex wi,
      Locale locale, String imageBaseURL, String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL);
    if (wdh instanceof WiktionaryDataHandler)
      this.delegate = (WiktionaryDataHandler) wdh;
    else
      throw new RuntimeException(
          "Invalid delegate class, expecting French Wiktionary Data Handler");
  }

  private final static String FrenchConjugationPagePrefix = "Conjugaison:français/";

  public void extractConjugations(List<String> context) {
    String conjugationPageContent =
        wi.getTextOfPage(FrenchConjugationPagePrefix + delegate.currentLexEntry());

    if (conjugationPageContent == null) {
      log.debug("No conjugation page for '" + delegate.currentLexEntry() + "'");
      return;
    }
    log.trace("Extracting conjugation page in {}", this.getPageName());
    log.trace(" --> Global context = [[{}]]", String.join("|", context));

    String html = expandWikiCode(conjugationPageContent);
    if (html == null) {
      return; // failing silently: error message already given.
    }

    Document doc = Jsoup.parse(html);
    if (null == doc) {
      return;
    }

    // TODO: get groupe, auxiliary verb and modèle de conjugaison.

    Elements sectionTitles = doc.select("h3");

    for (Element h3 : sectionTitles) {
      String sectionTitle = h3.text().trim();

      if ("Futur simple et conditionnel présent avant 1976 (et prononciation du XIXe siècle)"
          .equals(sectionTitle)) {
        log.debug("Ignoring inflections from pré-1976 tables in {}", this.getPageName());
        continue;
      }
      List<String> sectionContext = (new ArrayList<>(context));
      sectionContext.add(sectionTitle);
      RefactoredTableExtractor verbalTableExtractor;
      if ("Modes impersonnels".equals(sectionTitle) || "Impératif".equals(sectionTitle)) {
        verbalTableExtractor =
            new ImpersonalMoodTableExtractor(this.getPageName(), "fr", sectionContext);
      } else {
        verbalTableExtractor =
            new StandardMoodTableExtractor(this.getPageName(), "fr", sectionContext);
      }
      Element sectionContent = h3.nextElementSibling();
      // get the tables that do not contain any embedded tables
      Elements tables = sectionContent.select("table");
      tables.forEach(t -> registerAllForms(verbalTableExtractor.parseTable(t)));
    }
  }

  private void registerAllForms(Set<LexicalForm> forms) {
    for (LexicalForm form : forms) {
      delegate.addLexicalForm(form);
    }
  }

}

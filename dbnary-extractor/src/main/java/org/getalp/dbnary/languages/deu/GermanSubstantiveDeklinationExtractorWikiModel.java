package org.getalp.dbnary.languages.deu;

import java.util.List;
import java.util.Locale;
import org.apache.commons.lang3.tuple.Pair;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.languages.deu.GermanInflectionData.Genre;
import org.getalp.dbnary.morphology.InflectedFormSet;
import org.getalp.dbnary.wiki.WikiText.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GermanSubstantiveDeklinationExtractorWikiModel extends GermanTableExtractorWikiModel {

  private final Logger log =
      LoggerFactory.getLogger(GermanSubstantiveDeklinationExtractorWikiModel.class);

  public GermanSubstantiveDeklinationExtractorWikiModel(IWiktionaryDataHandler wdh,
      WiktionaryPageSource wi, Locale locale, String imageBaseURL, String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL, wdh,
        new GermanSubstantiveDeklinationTableExtractor());
  }

  private static void setGenus(GermanInflectionData infl, Genre g, String argnum) {
    if (infl.genre == Genre.NOTHING && infl.note.contains("number:" + argnum)) {
      infl.genre = g;
    }
  }

  private static final List<Pair<String, String>> genusArgs =
      List.of(Pair.of("Genus", null), Pair.of("Genus 1", "1"), Pair.of("Genus 2", "2"),
          Pair.of("Genus 3", "3"), Pair.of("Genus 4", "4"));

  @Override
  public void postProcessForms(Template template, InflectedFormSet forms) {
    if (template.getName().trim().equals("Deutsch Substantiv Übersicht")) {
      boolean hasGenus = false;
      for (Pair<String, String> arg : genusArgs) {
        String genus = template.getParsedArg(arg.getLeft());
        if (null != genus) {
          hasGenus = true;
          genus = genus.trim();
          switch (genus) {
            case "m":
              forms.forEach(
                  e -> setGenus((GermanInflectionData) e.getKey(), Genre.MASCULIN, arg.getRight()));
              break;
            case "f":
              forms.forEach(
                  e -> setGenus((GermanInflectionData) e.getKey(), Genre.FEMININ, arg.getRight()));
              break;
            case "n":
              forms.forEach(
                  e -> setGenus((GermanInflectionData) e.getKey(), Genre.NEUTRUM, arg.getRight()));
              break;
            default:
              log.debug("MORPH: unknown Genus {} in {}", genus, getPageName());
              break;
          }
        }
      }
      if (!hasGenus) {
        log.debug("MORPH: no Genus in {} || {}", template.getText(), getPageName());
      }

      super.postProcessForms(template, forms);
    } else if (template.getName().trim().equals("Deutsch Vorname Übersicht m")) {
      forms.forEach(e -> setGenus((GermanInflectionData) e.getKey(), Genre.MASCULIN, null));
    }
  }

}

package org.getalp.dbnary.languages.swe;

import java.util.Map.Entry;
import java.util.Set;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.morphology.InflectedFormSet;
import org.getalp.dbnary.morphology.InflectionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SwedishMorphologyExtractor {
  private Logger log = LoggerFactory.getLogger(SwedishMorphologyExtractor.class);

  private final IWiktionaryDataHandler wdh;
  protected final SwedishTableExtractorWikiModel tableExtractor;

  public SwedishMorphologyExtractor(IWiktionaryDataHandler wdh, WiktionaryPageSource wi) {
    this.wdh = wdh;
    tableExtractor = new SwedishTableExtractorWikiModel(wi, "/${Bild}", "/${Titel}");
  }

  public void extractMorphologicalData(String wikiCode, String pageName) {
    tableExtractor.setPageName(pageName);
    InflectedFormSet forms = tableExtractor.parseTables(wikiCode);
    registerAllForms(forms);
  }

  private void registerAllForms(InflectedFormSet forms) {
    for (Entry<InflectionData, Set<String>> form : forms) {
      wdh.registerInflection(form.getKey(), form.getValue());
    }
  }
}

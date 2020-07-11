package org.getalp.dbnary.deu;

import java.util.HashSet;
import java.util.Set;
import org.getalp.dbnary.morphology.TableExtractor;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class GermanTableExtractor extends TableExtractor {
  private Logger log = LoggerFactory.getLogger(GermanTableExtractor.class);


  public GermanTableExtractor(String currentEntry) {
    super(currentEntry);
  }

  @Override
  protected boolean shouldIgnoreCurrentH2(Element elt) {
    return !elt.text().contains("Deutsch");
  }

  @Override
  protected Set<String> getInflectedForms(Element cell) {
    // there are cells with <br> and commas to separate different values: split them
    // get rid of spurious html-formatting (<nbsp> <small> <i> etc.)
    Set<String> forms = new HashSet<String>();
    Elements anchors = cell.select("a");

    if (anchors.isEmpty()) {
      String cellText = cell.html();
      // check for <br>
      Elements linebreaks = cell.select("br");
      if (!linebreaks.isEmpty()) {
        log.debug("cell contains <br> : {}", cell.html());
        // replace <br> by ","
        cellText = cellText.replaceAll("<br/?>", ",");
      }
      cellText = cellText.replaceAll("&nbsp;", " ");
      cellText = cellText.replaceAll("</?small>", "");
      cellText = cellText.replaceAll("</?i>", "");
      cellText = cellText.replaceAll("</?strong.*?>", "");

      String[] atomicForms = cellText.split("[,;]");
      for (int i = 0; i < atomicForms.length; i++) {
        String trimmedText = atomicForms[i].trim();
        // log.debug(" was split into : {}", trimmedText);
        if (!trimmedText.isEmpty()) {
          forms.add(trimmedText);
        }
      }
    } else {
      for (Element anchor : anchors) {
        forms.add(anchor.text());
      }
    }
    for (String form : forms) {
      if (form.contains(",")) {
        log.trace("Comma found in morphological value: {}", form);
      }
    }
    return forms;
  }
}

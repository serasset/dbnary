package org.getalp.dbnary.languages.fra;

import org.getalp.dbnary.wiki.WikiEventsSequence;
import org.getalp.dbnary.wiki.WikiText;
import java.util.ArrayList;
import java.util.List;


/**
 * example SymbolsObject: relationShip: INHERITANCE , template: {{étyl | frm | word }}
 */
public class FSymbols {

  public String relationShip;
  public List<WikiText.Template> templates;


  public FSymbols(String relationShip) {
    this.relationShip = relationShip;
    this.templates = new ArrayList<>();
  }

  public void setRelationShip(String relationShip) {
    this.relationShip = relationShip;
  }

  public void addTemplate(WikiText.Template template) {
    this.templates.add(template);
  }

  public void addTemplates(WikiEventsSequence s) {
    s.iterator().forEachRemaining(e -> {
      if (e instanceof WikiText.Template) {
        addTemplate((WikiText.Template) e);
      }
    });
  }

  public WikiText.Template getNextTemplate(WikiText.Template t) {
    if (templates.indexOf(t) < templates.size() - 1) {
      return templates.get(templates.indexOf(t) + 1);
    } else {
      return null;
    }
  }
}

package org.getalp.dbnary.languages.deu;

import info.bliki.wiki.filter.PlainTextConverter;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GermanDefinitionExpander extends GermanDBnaryWikiModel {
  private static final Logger log = LoggerFactory.getLogger(GermanDefinitionExpander.class);
  protected Set<String> templates = null;

  public GermanDefinitionExpander(WiktionaryPageSource wi) {
    super(wi, Locale.GERMAN, "/images", "/link");
  }

  /**
   * Convert a wiki code to plain text, while keeping track of all template calls.
   *
   * @param definition the wiki code
   * @param templates if not null, the method will add all called templates to the set.
   * @return the expanding resulting string
   */
  public String expandAll(String definition, Set<String> templates) {
    this.templates = templates;
    try {
      return render(new PlainTextConverter(), definition).trim();
    } catch (IOException e) {
      log.error("Error while rendering page.", e);
      // e.printStackTrace();
    }
    return null;
  }

  @Override
  public void substituteTemplateCall(String templateName, Map<String, String> parameterMap,
      Appendable writer) throws IOException {
    if (templates != null) {
      templates.add(templateName);
    }
    super.substituteTemplateCall(templateName, parameterMap, writer);
  }

}

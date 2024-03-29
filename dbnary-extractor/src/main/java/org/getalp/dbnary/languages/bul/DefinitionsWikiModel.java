package org.getalp.dbnary.languages.bul;

import info.bliki.wiki.filter.PlainTextConverter;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.bliki.DbnaryWikiModel;

public class DefinitionsWikiModel extends DbnaryWikiModel {

  private Set<String> usedTemplates = null;

  public DefinitionsWikiModel(Locale locale, String imageBaseURL, String linkBaseURL) {
    this((WiktionaryPageSource) null, locale, imageBaseURL, linkBaseURL);
  }

  public DefinitionsWikiModel(WiktionaryPageSource wi, Locale locale, String imageBaseURL,
      String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL);
  }

  public DefinitionsWikiModel(WiktionaryPageSource wi, Locale locale, String imageBaseURL,
      String linkBaseURL, Set<String> usedTemplates) {
    super(wi, locale, imageBaseURL, linkBaseURL);
    this.usedTemplates = usedTemplates;
  }

  /**
   * Convert a wiki code to plain text, while keeping track of all template calls.
   *
   * @param definition the wiki code
   * @return a String of plain text
   */
  public String expandAll(String definition) {
    try {
      return render(new PlainTextConverter(), definition).trim();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  public Set<String> getTemplates() {
    return usedTemplates;
  }

  @Override
  public void substituteTemplateCall(String templateName, Map<String, String> parameterMap,
      Appendable writer) throws IOException {
    if (usedTemplates != null) {
      usedTemplates.add(templateName);
    }
    if ("Noun".equalsIgnoreCase(templateName)) {
      return;
    }
    if ("Adverb".equalsIgnoreCase(templateName)) {
      return;
    }
    if ("Verb".equalsIgnoreCase(templateName)) {
      return;
    }
    if ("Adjective".equalsIgnoreCase(templateName)) {
      return;
    }

    // TODO: void Noun, and other templates.
    super.substituteTemplateCall(templateName, parameterMap, writer);
  }

}

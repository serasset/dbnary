package org.getalp.dbnary.languages.swe;

import info.bliki.wiki.filter.PlainTextConverter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.tools.TemplateTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO inheriting SwedishWikiModel ?
public class DefinitionExpanderWikiModel extends SwedishWikiModel {

  private static Logger log = LoggerFactory.getLogger(DefinitionExpanderWikiModel.class);
  private final TemplateTracker templateTracker = new TemplateTracker();
  private Set<String> ignoreTemplates = new HashSet<>(Arrays.asList("böjning", "?", "avledning"));

  public DefinitionExpanderWikiModel(WiktionaryPageSource wi) {
    this(wi, new Locale("sv"), "/IMG", "/LINK");
  }

  public DefinitionExpanderWikiModel(WiktionaryPageSource wi, Locale locale, String imageBaseURL,
      String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL);
  }

  @Override
  public void setPageName(String pageTitle) {
    super.setPageName(pageTitle);
  }

  public String expandDefinition(String originalSource) {
    // Expand source to properly resolve links and cosmetic markups
    try {
      return render(new PlainTextConverter(), originalSource).trim();
    } catch (IOException e) {
      log.error("Error while rendering page.", e);
    }
    return null;
  }

  @Override
  public void substituteTemplateCall(String templateName, Map<String, String> parameterMap,
      Appendable writer) throws IOException {
    long startTime = 0;
    if (log.isTraceEnabled()) {
      startTime = System.nanoTime();
    }
    // TODO: template gammalstavning links old spelling to new versions
    if (ignoreTemplates.contains(templateName.toLowerCase())) {
      // Ignore inflection templates
    } else if ("tagg".equals(templateName)) {
      String topics = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9").stream()
          .map(key -> Optional.ofNullable(parameterMap.get(key))).flatMap(Optional::stream)
          .collect(Collectors.joining(", "));
      if (!topics.isEmpty()) {
        writer.append("(").append(topics).append(") ");
      }
    } else {
      super.substituteTemplateCall(templateName, parameterMap, writer);
    }
    if (log.isTraceEnabled()) {
      long elapsedNanos = System.nanoTime() - startTime;
      templateTracker.incr(templateName, elapsedNanos);
    }
  }

  public void logTemplateTracker() {
    if (log.isTraceEnabled()) {
      templateTracker.trace(log, "Definition Expander");
    }
  }
}

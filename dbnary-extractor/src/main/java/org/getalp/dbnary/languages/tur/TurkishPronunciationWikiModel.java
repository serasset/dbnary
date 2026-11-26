package org.getalp.dbnary.languages.tur;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.getalp.dbnary.api.WiktionaryPageSource;

/**
 * Expands pronunciation templates computed by Lua modules ({{tr-IPA}} -> Modül:tr-IPA) and returns
 * the resulting transcriptions.
 *
 * <p>
 * Create it once per extractor (the Lua engine is costly to initialize) and set its page name
 * whenever the extractor changes page, as the modules default to the page name.
 */
public class TurkishPronunciationWikiModel extends TurkishModulesPatcherWikiModel {

  /** Each transcription is rendered by Modül:IPA as a span of class IPA. */
  private static final Pattern IPA_SPAN = Pattern.compile("<span class=\"IPA\">([^<]+)</span>");

  public TurkishPronunciationWikiModel(WiktionaryPageSource wi, Locale locale, String imageBaseURL, String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL);
  }

  /** The transcriptions produced by a template call such as "{{tr-IPA|fa'sulye|fasülye}}". */
  public List<String> pronunciations(String templateCall) {
    String html = expandWikiCode(templateCall);
    if (null == html) {
      return List.of();
    }
    Matcher m = IPA_SPAN.matcher(html);
    return m.results().map(r -> r.group(1).trim()).filter(p -> !p.isEmpty()).toList();
  }
}

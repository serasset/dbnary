package org.getalp.dbnary.ita;

import info.bliki.wiki.filter.PlainTextConverter;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import org.getalp.dbnary.IWiktionaryDataHandler;
import org.getalp.dbnary.WiktionaryIndex;
import org.getalp.dbnary.bliki.DbnaryWikiModel;

public class ItalianExampleExtractorWikiModel extends DbnaryWikiModel {

  private IWiktionaryDataHandler delegate;

  public ItalianExampleExtractorWikiModel(IWiktionaryDataHandler dataHandler, WiktionaryIndex wi,
      Locale locale, String imageBaseURL, String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL);
    this.delegate = dataHandler;
  }

  public void parseExample(String example) {
    // Render the definition to plain text, while ignoring the example template
    String ex = null;
    try {
      ex = render(new PlainTextConverter(), example).trim();
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (null != ex && !ex.equals("")) {
      delegate.registerExample(ex, null);
    }
  }

  @Override
  public void substituteTemplateCall(String templateName, Map<String, String> parameterMap,
      Appendable writer) throws IOException {
    // Currently just expand the definition to get the full text.
    super.substituteTemplateCall(templateName, parameterMap, writer);
  }

}

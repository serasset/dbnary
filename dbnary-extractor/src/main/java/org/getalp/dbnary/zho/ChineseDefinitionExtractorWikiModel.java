package org.getalp.dbnary.zho;

import info.bliki.wiki.filter.PlainTextConverter;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import org.getalp.dbnary.DbnaryWikiModel;
import org.getalp.dbnary.IWiktionaryDataHandler;
import org.getalp.dbnary.WiktionaryIndex;

public class ChineseDefinitionExtractorWikiModel extends DbnaryWikiModel {

  private IWiktionaryDataHandler delegate;


  public ChineseDefinitionExtractorWikiModel(IWiktionaryDataHandler we, Locale locale,
      String imageBaseURL, String linkBaseURL) {
    this(we, (WiktionaryIndex) null, locale, imageBaseURL, linkBaseURL);
  }

  public ChineseDefinitionExtractorWikiModel(IWiktionaryDataHandler we, WiktionaryIndex wi,
      Locale locale, String imageBaseURL, String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL);
    this.delegate = we;
  }

  public void parseDefinition(String definition) {
    // Render the definition to plain text, while ignoring the example template
    String def = null;
    try {
      def = render(new PlainTextConverter(), definition).trim();
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (null != def && !def.equals("")) {
      delegate.registerNewDefinition(def);
    }
  }

  @Override
  public void substituteTemplateCall(String templateName, Map<String, String> parameterMap,
      Appendable writer) throws IOException {
    // Currently just expand the definition to get the full text.
    super.substituteTemplateCall(templateName, parameterMap, writer);
  }

}


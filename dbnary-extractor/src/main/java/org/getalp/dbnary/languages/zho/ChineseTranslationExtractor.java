package org.getalp.dbnary.languages.zho;

import java.util.List;
import java.util.Locale;
import org.apache.jena.rdf.model.Resource;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.languages.AbstractGlossFilter;
import org.getalp.dbnary.wiki.WikiText;
import org.getalp.dbnary.wiki.WikiText.InternalLink;
import org.getalp.dbnary.wiki.WikiText.ListItem;
import org.getalp.dbnary.wiki.WikiText.Template;
import org.getalp.dbnary.wiki.WikiText.Text;
import org.getalp.dbnary.wiki.WikiText.Token;
import org.getalp.dbnary.wiki.WikiText.WikiContent;
import org.getalp.iso639.ISO639_3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChineseTranslationExtractor {
  private final AbstractGlossFilter glossFilter;
  private IWiktionaryDataHandler delegate;
  private int rank = 1;
  private Logger log = LoggerFactory.getLogger(ChineseTranslationExtractor.class);

  public ChineseTranslationExtractor(IWiktionaryDataHandler we, AbstractGlossFilter glossFilter) {
    this.delegate = we;
    this.glossFilter = glossFilter;
  }

  private Resource currentGloss = null;

  public void parseTranslationBlock(String block) {
    WikiText text = new WikiText(block);
    extractTranslationsBlocks(text.content());
  }

  private void extractTranslationsBlocks(WikiContent text) {
    for (Token t : text.wikiTokens()) {
      if (t instanceof Template) {
        extractGloss(t);
      } else if (t instanceof ListItem) {
        extractTranslationLine(t.asListItem().getContent());
      } else {
        log.debug("Translation: Unexpected WikiToken {} in {}", t.getClass().getName(),
            delegate.currentPagename());
      }
    }
  }

  private void extractGloss(Token t) {
    Template tmpl = t.asTemplate();
    if (isTheTopOfTranslation(tmpl.getName())) {
      // Extract gloss
      String g = tmpl.getParsedArg("1");
      if (null != g) {
        g = g.trim();
        currentGloss = delegate.createGlossResource(glossFilter.extractGlossStructure(g), rank++);
      } else {
        currentGloss = null;
      }
    } else if (isTheBottomOfTranslation(tmpl.getName())) {
      currentGloss = null;
    } else if (isTheMiddleOfTranslation(tmpl.getName())) {
      // Just ignore
    } else {
      log.debug("Translation: Unknown Template {} in {}", tmpl.getName(),
          delegate.currentPagename());
    }
  }

  private static boolean isTheTopOfTranslation(String name) {
    return name.equals("trans-top") || name.equals("翻译-顶") || name.equals("翻譯-頂");
  }

  private static boolean isTheMiddleOfTranslation(String name) {
    return name.equals("trans-mid") || name.equals("翻译-中") || name.equals("翻譯-中");
  }

  private static boolean isTheBottomOfTranslation(String name) {
    return name.equals("trans-bottom") || name.equals("翻译-底") || name.equals("翻譯-底");
  }

  private void extractTranslationLine(WikiContent content) {
    List<Token> tokens = content.tokens();
    if (tokens.size() == 0)
      return;
    removeIrrelevantToken(tokens, 0);
    Token firstToken = tokens.get(0);
    String langFromFirstToken = getLanguageNameFromFirstToken(tokens, firstToken);
    tokens.remove(0);
    registerTransFromTheRestOfTokens(tokens, langFromFirstToken);
  }

  private void removeIrrelevantToken(List<Token> tokens, int location) {
    if (tokens.get(location).getText().equals(" ") || tokens.get(location).getText().equals(": ")
        || tokens.get(location).getText().equals("：")) {
      tokens.remove(location);
    }
  }

  private String getLanguageNameFromFirstToken(List<Token> tokens, Token firstToken) {
    String langFromFirstToken = null;
    if (firstToken instanceof Template) {

      langFromFirstToken = (!firstToken.asTemplate().getName().equals("langname"))
          ? firstToken.asTemplate().getName().toLowerCase(Locale.ROOT)
          : firstToken.asTemplate().getParsedArg("1");
      removeIrrelevantToken(tokens, 1);
    } else if (firstToken instanceof Text) {
      String languageIntroduction = firstToken.asText().toString();
      langFromFirstToken = languageIntroduction.split(":|：")[0].trim();
    } else {
      log.debug("Translation Line: Unexpected first token " + firstToken.getText() + " {} in {}",
          firstToken.getClass().toString(), delegate.currentPagename());
    }
    return langFromFirstToken;
  }

  private void registerTransFromTheRestOfTokens(List<Token> tokens, String langFromFirstToken) {
    String langCode;
    for (Token t : tokens) {
      if (t instanceof Text)
        continue;
      if (t instanceof Template) {
        Template tmpl = t.asTemplate();
        if (tmpl.getName().equals("t") || tmpl.getName().equals("t+") || tmpl.getName().equals("tt")
            || tmpl.getName().equals("t-") || tmpl.getName().equals("tt-")
            || tmpl.getName().equals("l")) {
          langCode = normalizeLang(tmpl.getParsedArg("1").trim());
          String usage = tmpl.getParsedArg("3");
          if (null == usage) {
            usage = "";
          }
          String transcription = tmpl.getParsedArg("tr");
          if (null == transcription) {
            transcription = tmpl.getParsedArg("4");
          }
          if (null == transcription) {
            transcription = "";
          }
          if (!transcription.equals("")) {
            usage = "(" + transcription + "), " + usage;
          }
          String word = tmpl.getParsedArg("2");
          delegate.registerTranslation(langCode, currentGloss, usage, word);
        }
      } else if (t instanceof InternalLink) {
        if (langFromFirstToken == null) {
          log.debug("Translation Line: can't identify the language of the text: " + t.getText());
        }
        langCode = normalizeLang(langFromFirstToken);
        delegate.registerTranslation(langCode, currentGloss, null,
            t.asInternalLink().getTargetText());
      } else {
        log.debug("Translation Line: Unexpected token " + t.getText() + " {} in {}",
            t.getClass().toString(), delegate.currentPagename());
      }
    }
  }

  private String normalizeLang(String lang) {
    String normLangCode;

    if ((normLangCode = ISO639_3.sharedInstance.getIdCode(lang.trim())) != null) {
      lang = normLangCode;
    } else if (normLangCode == null) {
      log.debug("Translation line: can't find the code of the language: " + lang);
    }
    return lang;
  }
}

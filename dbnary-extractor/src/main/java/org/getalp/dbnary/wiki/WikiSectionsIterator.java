package org.getalp.dbnary.wiki;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Created by serasset on 28/01/16.
 */
public class WikiSectionsIterator implements Iterator<WikiSection> {

  int level;
  WikiText.WikiContent content;
  Iterator<WikiText.Token> baseIterator;
  WikiText.Token currentToken = null;

  public WikiSectionsIterator(WikiText.WikiContent content, int level) {
    this.content = content;
    this.level = level;
    this.baseIterator = content.tokens().iterator();
    init();
    advanceToNextHeading();
  }

  // My model 1 primitives...
  public void init() {
    if (baseIterator.hasNext()) {
      currentToken = baseIterator.next();
    }
  }

  public boolean eof() {
    return currentToken == null;
  }

  public void advance() {
    if (!baseIterator.hasNext()) {
      currentToken = null;
    } else {
      currentToken = baseIterator.next();
    }
  }

  // Collect next element and prepare for the following
  // sequence is of type XXXXXHCCCCCCCCChXXXHCCC where X is to be discarded, H is a header of level
  // n and h is a higher level header
  // init state is ^currentToken
  // final state is ^currentToken
  private void advanceToNextHeading() {
    while (!eof() && !isOpeningHeading(currentToken)) {
      advance();
    }
    // either at eof or on an opening heading
  }

  public boolean isOpeningHeading(WikiText.Token tok) {
    return tok instanceof WikiText.Heading && ((WikiText.Heading) tok).getLevel() == level;
  }

  public boolean isClosingHeading(WikiText.Token tok) {
    return tok instanceof WikiText.Heading && ((WikiText.Heading) tok).getLevel() <= level;
  }

  @Override
  public boolean hasNext() {
    return !eof();
  }

  @Override
  public WikiSection next() {
    if (eof()) {
      throw new NoSuchElementException("No remaining section in this wikitext.");
    }
    WikiText.Heading sectionHeading = (WikiText.Heading) currentToken;
    WikiText.WikiContent sectionContent =
        content.getWikiText().new WikiContent(currentToken.offset.end);
    advance();
    while (!eof() && !isClosingHeading(currentToken)) {
      sectionContent.addToken(currentToken);
      advance();
    }
    if (eof()) {
      sectionContent.setEndOffset(content.getWikiText().getEndOffset());
    } else {
      sectionContent.setEndOffset(currentToken.offset.start);
    }
    advanceToNextHeading();
    return new WikiSection(sectionHeading, sectionContent);
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}

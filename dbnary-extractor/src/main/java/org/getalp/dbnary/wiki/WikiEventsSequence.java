package org.getalp.dbnary.wiki;

import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.getalp.dbnary.wiki.WikiText.Token;

/**
 * Created by serasset on 28/01/16.
 */
public class WikiEventsSequence implements Iterable<WikiText.Token> {

  private WikiText.WikiContent content;
  private WikiEventFilter filter;

  public WikiEventsSequence(WikiText.WikiContent content, WikiEventFilter filter) {
    this.content = content;
    this.filter = filter;
  }

  @Override
  public Iterator<WikiText.Token> iterator() {
    return new WikiEventIterator(content, filter);
  }

  public Stream<Token> stream() {
    return StreamSupport.stream(this.spliterator(), false);
  }

  public WikiEventsSequence and(WikiEventFilter filter) {
    this.filter = new WikiEventFilterConjunction(this.filter, filter);
    return this;
  }

  public WikiEventsSequence or(WikiEventFilter filter) {
    this.filter = new WikiEventFilterDisjunction(this.filter, filter);
    return this;
  }

}

package org.getalp.dbnary.wiki;

import java.util.Iterator;
import java.util.Stack;
import org.getalp.dbnary.wiki.WikiText.Heading;
import org.getalp.dbnary.wiki.WikiText.ListItem;
import org.getalp.dbnary.wiki.WikiText.Token;

/**
 * Created by serasset on 28/01/16.
 */
public class WikiEventIterator implements Iterator<WikiText.Token> {

  private final WikiEventFilter filter;
  private final WikiText.WikiContent content;
  private final Stack<Iterator<Token>> iterators = new Stack<>();
  private WikiText.Token nextToken = null;

  public WikiEventIterator(WikiText.WikiContent content, WikiEventFilter filter) {
    this.content = content;
    this.filter = filter;
    this.iterators.push(content.tokens().iterator());
    advance();
  }

  private void advance() {
    nextToken = nextTokenToReturn();
  }

  private Token nextTokenToReturn() {
    if (iterators.empty())
      return null;
    Iterator<Token> currentIterator = iterators.peek();
    if (!currentIterator.hasNext()) {
      iterators.pop();
      return nextTokenToReturn();
    }
    Token t = currentIterator.next();
    switch (filter.apply(t)) {
      case VOID:
        return nextTokenToReturn();
      case ENTER:
        if (t instanceof ListItem) {
          ListItem li = (ListItem) t;
          iterators.push(li.getContent().tokens().iterator());
          return nextTokenToReturn();
        } else if (t instanceof Heading) {
          Heading h = (Heading) t;
          iterators.push(h.getContent().tokens().iterator());
          return nextTokenToReturn();
        } else {
          // treat an incorrect ENTER action as a VOID
          return nextTokenToReturn();
        }
      case KEEP:
        return t;
      default:
        return nextTokenToReturn();
    }
  }


  @Override
  public boolean hasNext() {
    return null != nextToken;
  }

  @Override
  public WikiText.Token next() {
    WikiText.Token t = nextToken;
    advance();
    return t;
  }

}

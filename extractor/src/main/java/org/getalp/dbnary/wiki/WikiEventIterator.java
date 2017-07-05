package org.getalp.dbnary.wiki;

import java.util.Iterator;

/**
 * Created by serasset on 28/01/16.
 */
public class WikiEventIterator implements Iterator<WikiText.Token> {

    WikiEventFilter filter;
    WikiText.WikiContent content;
    Iterator<WikiText.Token> baseIterator;
    WikiText.Token nextToken = null;

    public WikiEventIterator(WikiText.WikiContent content, WikiEventFilter filter) {
        this.content = content;
        this.filter = filter;
        this.baseIterator = content.tokens().iterator();
        advance();
    }

    private void advance() {
        nextToken = null;
        while (baseIterator.hasNext() && !filter.apply(nextToken = baseIterator.next())) ;
        if (null != nextToken && !baseIterator.hasNext() && !filter.apply(nextToken))
            nextToken = null;
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

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}

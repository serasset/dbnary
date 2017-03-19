package org.getalp.dbnary.wiki;

import java.util.Iterator;

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

}

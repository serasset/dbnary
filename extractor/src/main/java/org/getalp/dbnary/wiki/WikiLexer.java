package org.getalp.dbnary.wiki;

import java.util.Iterator;

/**
 * Created by serasset on 28/01/16.
 */

/**
 * A sequence of wiki tokens conforming to a given lexer and discarding all data that is not
 * matched by the lexer
 */
public class WikiTokensSequence implements Iterable<WikiText.Token> {

    private WikiText.WikiContent content;
    private WikiEventLexer lexer;

    public WikiTokensSequence(WikiText.WikiContent content, WikiEventLexer lexer) {
        this.content = content;
        this.lexer = lexer;
    }

    @Override
    public Iterator<WikiText.Token> iterator() {
        return new WikiEventIterator(content, lexer);
    }

}

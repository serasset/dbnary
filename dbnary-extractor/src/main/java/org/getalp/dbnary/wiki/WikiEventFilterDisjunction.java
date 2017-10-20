package org.getalp.dbnary.wiki;

public class WikiEventFilterDisjunction implements WikiEventFilter {

    private WikiEventFilter rhs;
    private WikiEventFilter lhs;

    public WikiEventFilterDisjunction(WikiEventFilter rhs, WikiEventFilter lhs) {
        this.rhs = rhs;
        this.lhs = lhs;
    }

    @Override
    public boolean apply(WikiText.Token tok) {
        return rhs.apply(tok) || lhs.apply(tok);
    }
}

package org.getalp.dbnary.wiki;

import static org.getalp.dbnary.wiki.WikiEventFilter.Action.VOID;

public class WikiEventFilterConjunction implements WikiEventFilter {

  private WikiEventFilter rhs;
  private WikiEventFilter lhs;

  public WikiEventFilterConjunction(WikiEventFilter rhs, WikiEventFilter lhs) {
    this.rhs = rhs;
    this.lhs = lhs;
  }

  @Override
  public Action apply(WikiText.Token tok) {
    if (rhs.apply(tok) == VOID) {
      return VOID;
    } else {
      return lhs.apply(tok);
    }
  }
}

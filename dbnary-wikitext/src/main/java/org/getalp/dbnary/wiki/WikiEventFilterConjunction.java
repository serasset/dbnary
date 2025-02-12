package org.getalp.dbnary.wiki;

import static org.getalp.dbnary.wiki.WikiEventFilter.Action.KEEP;

public class WikiEventFilterConjunction implements WikiEventFilter {

  private WikiEventFilter rhs;
  private WikiEventFilter lhs;

  public WikiEventFilterConjunction(WikiEventFilter rhs, WikiEventFilter lhs) {
    this.rhs = rhs;
    this.lhs = lhs;
  }

  @Override
  public Action apply(WikiText.Token tok) {
    Action leftAction;
    if ((leftAction = rhs.apply(tok)) != KEEP) {
      return leftAction;
    } else {
      return lhs.apply(tok);
    }
  }
}

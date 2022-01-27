package org.getalp.dbnary.wiki;

public class WikiEventFilterDisjunction implements WikiEventFilter {

  private WikiEventFilter rhs;
  private WikiEventFilter lhs;

  public WikiEventFilterDisjunction(WikiEventFilter rhs, WikiEventFilter lhs) {
    this.rhs = rhs;
    this.lhs = lhs;
  }

  @Override
  public Action apply(WikiText.Token tok) {
    Action rhsA = rhs.apply(tok);
    if (rhsA == Action.VOID) {
      return lhs.apply(tok);
    } else {
      return rhsA;
    }
  }
}

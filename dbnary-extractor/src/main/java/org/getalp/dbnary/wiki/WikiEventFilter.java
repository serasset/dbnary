package org.getalp.dbnary.wiki;

/**
 * Created by serasset on 01/02/16.
 */
public interface WikiEventFilter {

  enum Action {
    KEEP, ENTER, VOID
  }

  /**
   * returns true if tok should be kept in the event sequence.
   *
   * @param tok the token to be considered
   * @return a boolean
   */
  Action apply(WikiText.Token tok);
}

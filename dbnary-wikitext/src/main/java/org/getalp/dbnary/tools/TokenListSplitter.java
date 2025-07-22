package org.getalp.dbnary.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import org.apache.commons.lang3.tuple.Pair;
import org.getalp.dbnary.wiki.WikiText.Token;

public class TokenListSplitter {

  public static List<Pair<Token, List<Token>>> split(List<Token> tokens, Predicate<Token> predicate) {
    List<Pair<Token, List<Token>>> splits = new ArrayList<>();
    List<Token> currentSplit = new ArrayList<>();
    for (Token t : tokens) {
      if (predicate.test(t)) {
        currentSplit = new ArrayList<>();
        splits.add(Pair.of(t, currentSplit));
      } else {
        currentSplit.add(t);
      }
    }
    return splits;
  }

  public static <R> List<Pair<R, List<Token>>> splitAndProcessToken(List<Token> tokens, Function<Token, R> predicate) {
    List<Pair<R, List<Token>>> splits = new ArrayList<>();
    List<Token> currentSplit = new ArrayList<>();
    for (Token t : tokens) {
      R r = predicate.apply(t);
      if (r != null) {
        currentSplit = new ArrayList<>();
        splits.add(Pair.of(r, currentSplit));
      } else {
        currentSplit.add(t);
      }
    }
    return splits;
  }

}

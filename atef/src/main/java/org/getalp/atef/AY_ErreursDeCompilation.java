package org.getalp.atef;

import org.antlr.v4.runtime.*;
import java.util.*;

public class AY_ErreursDeCompilation extends BaseErrorListener {
  @Override
  public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int lig, int col,
      String msg, RecognitionException e) {
    System.out.println(" ----- AY_ErreursDeCompilation: ERREUR: -----");
    List<String> stack = ((Parser) recognizer).getRuleInvocationStack();
    Collections.reverse(stack);
    System.out.println(" pile des règles: " + stack);
    System.out.println(" ligne=" + lig + ", colonne=" + col + " " + offendingSymbol + ": " + msg);
    System.out.println(" ----- AY_ErreursDeCompilation: ERREUR. -----");
  }
}

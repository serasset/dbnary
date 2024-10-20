package org.getalp.wiktionary;

public class WiktionaryIndexerException extends Exception {

  public WiktionaryIndexerException(String string, Exception ex) {
    super(string, ex);
  }

  public WiktionaryIndexerException(Exception ex) {
    super(ex);
  }

  public WiktionaryIndexerException(String message) {
    super(message);
  }
}

package org.getalp.dbnary.cli.utils;

public class DBnaryCommandLineException extends RuntimeException {

  public DBnaryCommandLineException() {}

  public DBnaryCommandLineException(String message) {
    super(message);
  }

  public DBnaryCommandLineException(String message, Throwable cause) {
    super(message, cause);
  }

  public DBnaryCommandLineException(Throwable cause) {
    super(cause);
  }

  public DBnaryCommandLineException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}

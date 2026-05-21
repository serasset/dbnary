package org.getalp.dbnary.enhancer.disambiguation;

import java.io.Serial;

public class InvalidContextException extends Exception {

  /**
   *
   */
  @Serial
  private static final long serialVersionUID = 6781528336910789591L;

  public InvalidContextException() {
    super();
  }

  public InvalidContextException(String message) {
    super(message);
  }

  public InvalidContextException(Throwable cause) {
    super(cause);
  }

  public InvalidContextException(String message, Throwable cause) {
    super(message, cause);
  }

  public InvalidContextException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}

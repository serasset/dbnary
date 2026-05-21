package org.getalp.dbnary.enhancer.disambiguation;

import java.io.Serial;

public class InvalidEntryException extends Exception {


  @Serial
  private static final long serialVersionUID = -3093043044636990444L;

  public InvalidEntryException() {
    super();
  }

  public InvalidEntryException(String message) {
    super(message);
  }

  public InvalidEntryException(Throwable cause) {
    super(cause);
  }

  public InvalidEntryException(String message, Throwable cause) {
    super(message, cause);
  }

  public InvalidEntryException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}

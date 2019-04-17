package org.getalp.dbnary.wiki;

import java.util.ArrayList;
import java.util.function.Function;

/**
 * Created by serasset on 17/02/17.
 */
public interface WikiSequenceFiltering {

  abstract class Action {

  }

  class Content extends Action {

    final Function<WikiText.Token, ArrayList<WikiText.Token>> getter;

    private Content() {
      getter = null;
    }

    public Content(Function<WikiText.Token, ArrayList<WikiText.Token>> getContent) {
      getter = getContent;
    }
  }

  class KeepAsis extends Action {

  }

  class OpenContentClose extends Content {

    private OpenContentClose() {
      super();
    }

    public OpenContentClose(Function<WikiText.Token, ArrayList<WikiText.Token>> getContent) {
      super(getContent);
    }
  }

  class Void extends Action {

  }

  class Atomize extends Action {

  }


}

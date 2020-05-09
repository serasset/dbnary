package org.getalp.dbnary.wiki;

import java.util.Iterator;

/**
 * Created by serasset on 28/01/16.
 */
public class WikiSectionsSequence implements Iterable<WikiText.WikiSection> {

  private final int level;
  private WikiText.WikiContent content;

  public WikiSectionsSequence(WikiText.WikiContent content, int level) {
    this.content = content;
    this.level = level;
  }

  @Override
  public Iterator<WikiText.WikiSection> iterator() {
    return new WikiText.LevelBasedWikiSectionsIterator(content, level);
  }

}

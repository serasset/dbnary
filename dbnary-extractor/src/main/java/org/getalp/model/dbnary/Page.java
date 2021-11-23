package org.getalp.model.dbnary;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;
import org.getalp.LangTools;
import org.getalp.model.ontolex.LexicalEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Page implements AcceptTranslation {
  private static final Logger log = LoggerFactory.getLogger(Page.class);
  protected String language;
  protected String name;
  protected Stack<LexicalEntry> entries = new Stack<>();

  public Page(String language, String name) {
    this.language = LangTools.getCode(language);
    if (this.language == null) {
      log.warn("Unknown language {} while parsing page {}", language, name);
      this.language = language;
    }
    this.name = name;
  }

  public String getShortLanguageCode() {
    String shortCode = LangTools.getPart1OrId(language);
    return null != shortCode ? shortCode : language;
  }

  public String getLongLanguageCode() {
    return language;
  }

  public String getName() {
    return name;
  }

  public LexicalEntry newEntry(String pos) {
    return this.newEntry(this.getName(), pos);
  }

  public LexicalEntry newEntry(String name, String pos) {
    LexicalEntry le = new LexicalEntry(name, pos, entries.size());
    entries.add(le);
    return le;
  }

  public int nbEntries() {
    return entries.size();
  }


}

package org.getalp.model.dbnary;

import java.util.Stack;
import org.getalp.LangTools;
import org.getalp.model.ontolex.LexicalEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Page implements AcceptTranslation {
  private static final Logger log = LoggerFactory.getLogger(Page.class);
  protected final String language;
  protected final String name;
  protected Stack<LexicalEntry> entries = new Stack<>();

  public Page(String language, String name) {
    String normalizedLanguage = LangTools.getCode(language);
    if (normalizedLanguage == null) {
      log.debug("Unknown language {} while parsing page {}", language, name);
      normalizedLanguage = language;
    }
    this.language = normalizedLanguage;
    this.name = name;
  }

  public String getShortLanguageCode() {
    String shortCode = LangTools.getShortCode(language);
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
    return this.newEntry(name, pos, this.language);
  }

  public LexicalEntry newEntry(String name, String pos, String language) {
    LexicalEntry le = new LexicalEntry(name, pos, entries.size());
    entries.add(le);
    return le;
  }

  public int nbEntries() {
    return entries.size();
  }

  public String getWiktionaryURI() {
    return "http://" + getShortLanguageCode() + ".wiktionary.org/wiki/" + name;
  }
}

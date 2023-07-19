package org.getalp.dbnary.tools;

import org.getalp.dbnary.wiki.WikiText;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author Arnaud Alet 13/07/2023
 */
public class PageIterator implements Iterator<WikiText.Token> {

  protected final List<WikiText.Token> page;
  protected final Set<String> skippedTemplate;
  protected int cursor;
  protected int size;

  public PageIterator(final List<WikiText.Token> page) {
    this(page, Set.of());
  }

  public PageIterator(final List<WikiText.Token> page, final Set<String> skippedTemplate) {
    this.page = page;
    this.skippedTemplate = skippedTemplate;
    this.cursor = 0;
    this.size = this.page.size();
  }

  public int getCursor() {
    return this.cursor;
  }

  /**
   * @return the last value of the next() or nextTemplate()
   */
  public WikiText.Token get() {
    return this.page.get(this.cursor - 1);
  }

  protected WikiText.Token getAndIncrement() {
    return this.page.get(this.cursor++);
  }

  protected WikiText.Token pageOffsetGet(final int cursor) {
    return this.page.get(this.cursor + cursor);
  }

  protected WikiText.Token pageGetNext() {
    return this.page.get(this.cursor);
  }

  @Override
  public WikiText.Token next() {
    while (pageGetNext().getText().trim().isBlank() || (pageGetNext() instanceof WikiText.Template
        && this.skippedTemplate.contains(pageGetNext().asTemplate().getName())))
      this.cursor++;
    return getAndIncrement();
  }


  public WikiText.Template nextTemplate() {
    while (!(next() instanceof WikiText.Template));
    return get().asTemplate();
  }

  public WikiText.Token shadowNext() {
    int shadowCursor = 0;
    while (pageOffsetGet(shadowCursor).getText().trim().isBlank()
        || (pageOffsetGet(shadowCursor) instanceof WikiText.Template
            && this.skippedTemplate.contains(pageOffsetGet(shadowCursor).asTemplate().getName())))
      shadowCursor++;
    return pageOffsetGet(shadowCursor);
  }

  public WikiText.Template shadowNextTemplate() {
    int shadowCursor = 0;
    while (!(pageOffsetGet(shadowCursor) instanceof WikiText.Template)
        || this.skippedTemplate.contains(pageOffsetGet(shadowCursor).asTemplate().getName()))
      shadowCursor++;
    return pageOffsetGet(shadowCursor).asTemplate();
  }

  public void skip(final int length) {
    this.cursor += length;
  }

  @Override
  public boolean hasNext() {
    int shadowCursor = 0;
    while (this.cursor + shadowCursor < this.size && (pageOffsetGet(shadowCursor).getText().trim()
        .isBlank()
        || (pageOffsetGet(shadowCursor) instanceof WikiText.Template
            && this.skippedTemplate.contains(pageOffsetGet(shadowCursor).asTemplate().getName()))))
      shadowCursor++;
    return this.cursor + shadowCursor < this.size;
  }

  public boolean hasNextTemplate() {
    int shadowCursor = 0;
    while (this.cursor + shadowCursor < this.size
        && (!(pageOffsetGet(shadowCursor) instanceof WikiText.Template)
            || this.skippedTemplate.contains(pageOffsetGet(shadowCursor).asTemplate().getName())))
      shadowCursor++;
    return this.cursor + shadowCursor < this.size;
  }

  public int findNextTemplate(final String name) {
    int shadowCursor = 0;
    while (this.cursor + shadowCursor < this.page.size()
        && pageOffsetGet(shadowCursor) instanceof WikiText.Template
        && !pageOffsetGet(shadowCursor).asTemplate().getName().equals(name))
      shadowCursor++;
    return this.cursor + shadowCursor < this.size ? shadowCursor : -1;
  }

  public WikiText.Template goToNextTemplate(final String name) {
    WikiText.Template template = null;
    while (this.hasNextTemplate() && !(template = this.nextTemplate()).getName().equals(name));
    if (template == null)
      return null;
    return template.getName().equals(name) ? template : null;
  }

  public List<WikiText.Token> remaining() {
    return this.page.subList(this.cursor, this.size);
  }

  public static PageIterator of(final String content) {
    return of(content, Set.of());
  }

  public static PageIterator of(final String content, final Set<String> skippedTemplate) {
    WikiText page = new WikiText("PageIterator parser.", content);
    WikiText.WikiDocument doc = page.asStructuredDocument();

    return new PageIterator(doc.getContent().tokens(), skippedTemplate);
  }

  public static PageIterator of(final List<WikiText.Token> tokens) {
    return of(tokens, Set.of());
  }

  public static PageIterator of(final List<WikiText.Token> tokens,
      final Set<String> skippedTemplate) {
    return new PageIterator(tokens, skippedTemplate);
  }

  public static PageIterator of(final WikiText.WikiSection section) {
    return of(section.getContent().tokens(), Set.of());
  }

  public static PageIterator of(final WikiText.WikiSection section,
      final Set<String> skippedTemplate) {
    return of(section.getContent().tokens(), skippedTemplate);
  }

  public boolean isSection() {
    return this.get() instanceof WikiText.WikiSection;
  }

  public boolean isTemplate() {
    return this.get() instanceof WikiText.Template;
  }

  public boolean isNextASection() {
    return this.shadowNext() instanceof WikiText.WikiSection;
  }

  public boolean isNextATemplate() {
    return this.shadowNext() instanceof WikiText.Template;
  }

  @Override
  public String toString() {
    PageIterator newIt = this.cloneIt();
    StringBuilder builder = new StringBuilder();
    builder.append("Cursor : ").append(this.cursor).append("\n{\n");
    while (newIt.hasNext())
      builder.append(newIt.getCursor() == cursor ? " -> " : "    ").append(newIt.cursor)
          .append(": ").append(newIt.next()).append("\n");
    builder.append("}");
    return builder.toString();
  }

  public PageIterator cloneIt() {
    return new PageIterator(page, skippedTemplate);
  }
}

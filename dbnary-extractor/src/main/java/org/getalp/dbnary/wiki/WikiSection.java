package org.getalp.dbnary.wiki;

public class WikiSection {

  private WikiText.Heading header;
  private WikiText.WikiContent content;

  public WikiSection(WikiText.Heading heading, WikiText.WikiContent content) {
    this.header = heading;
    this.content = content;
  }

  public WikiText.Heading getHeader() {
    return header;
  }

  public WikiText.WikiContent getContent() {
    return content;
  }
}

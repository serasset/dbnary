package org.getalp.dbnary.wiki.visit;

import org.getalp.dbnary.wiki.WikiText.ExternalLink;
import org.getalp.dbnary.wiki.WikiText.HTMLComment;
import org.getalp.dbnary.wiki.WikiText.Heading;
import org.getalp.dbnary.wiki.WikiText.Indentation;
import org.getalp.dbnary.wiki.WikiText.InternalLink;
import org.getalp.dbnary.wiki.WikiText.Item;
import org.getalp.dbnary.wiki.WikiText.ListItem;
import org.getalp.dbnary.wiki.WikiText.NumberedListItem;
import org.getalp.dbnary.wiki.WikiText.Template;
import org.getalp.dbnary.wiki.WikiText.Text;
import org.getalp.dbnary.wiki.WikiText.Token;
import org.getalp.dbnary.wiki.WikiText.WikiContent;
import org.getalp.dbnary.wiki.WikiText.WikiDocument;
import org.getalp.dbnary.wiki.WikiText.WikiSection;

public abstract class BaseWikiTextVisitor implements Visitor<Void> {

  @Override
  public Void visit(ExternalLink externalLink) {
    externalLink.getTarget().accept(this);
    if (null != externalLink.getLinkContent())
      externalLink.getLinkContent().accept(this);
    return null;
  }

  @Override
  public Void visit(InternalLink internalLink) {
    internalLink.getTarget().accept(this);
    if (null != internalLink.getLinkContent())
      internalLink.getLinkContent().accept(this);
    return null;
  }

  @Override
  public Void visit(Heading heading) {
    heading.getContent().accept(this);
    return null;
  }

  @Override
  public Void visit(Indentation indentation) {
    indentation.getContent().accept(this);
    return null;
  }

  @Override
  public Void visit(Item item) {
    item.getContent().accept(this);
    return null;
  }

  @Override
  public Void visit(ListItem listItem) {
    listItem.getContent().accept(this);
    return null;
  }

  @Override
  public Void visit(NumberedListItem numlistItem) {
    numlistItem.getContent().accept(this);
    return null;
  }

  @Override
  public Void visit(Template template) {
    for (WikiContent value : template.getArgs().values()) {
      value.accept(this);
    }
    return null;
  }

  @Override
  public Void visit(WikiContent content) {
    for (Token token : content.tokens()) {
      token.accept(this);
    }
    return null;
  }

  @Override
  public Void visit(Text text) {
    return null;
  }

  @Override
  public Void visit(HTMLComment htmlComment) {
    return null;
  }

  @Override
  public Void visit(WikiSection section) {
    section.getHeading().accept(this);
    for (Token token : section.getContent().tokens()) {
      token.accept(this);
    }
    return null;
  }

  @Override
  public Void visit(WikiDocument doc) {
    for (Token token : doc.getContent().tokens()) {
      token.accept(this);
    }
    return null;
  }
}

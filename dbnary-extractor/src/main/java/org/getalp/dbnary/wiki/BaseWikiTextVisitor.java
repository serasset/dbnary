package org.getalp.dbnary.wiki;

import org.getalp.dbnary.wiki.WikiText.ExternalLink;
import org.getalp.dbnary.wiki.WikiText.HTMLComment;
import org.getalp.dbnary.wiki.WikiText.Heading;
import org.getalp.dbnary.wiki.WikiText.Indentation;
import org.getalp.dbnary.wiki.WikiText.InternalLink;
import org.getalp.dbnary.wiki.WikiText.ListItem;
import org.getalp.dbnary.wiki.WikiText.Template;
import org.getalp.dbnary.wiki.WikiText.Text;
import org.getalp.dbnary.wiki.WikiText.Token;
import org.getalp.dbnary.wiki.WikiText.WikiContent;

public abstract class BaseWikiTextVisitor implements Visitor {

  @Override
  public void visit(ExternalLink externalLink) {
    externalLink.target.accept(this);
    if (null != externalLink.text)
      externalLink.text.accept(this);
  }

  @Override
  public void visit(InternalLink internalLink) {
    internalLink.target.accept(this);
    if (null != internalLink.text)
      internalLink.text.accept(this);
  }

  @Override
  public void visit(Heading heading) {
    heading.content.accept(this);
  }

  @Override
  public void visit(Indentation indentation) {
    indentation.content.accept(this);
  }

  @Override
  public void visit(ListItem listItem) {
    listItem.content.accept(this);
  }

  @Override
  public void visit(Template template) {
    for (WikiContent value : template.getArgs().values()) {
      value.accept(this);
    }
  }

  @Override
  public void visit(WikiContent content) {
    for (Token token : content.tokens()) {
      token.accept(this);
    }
  }

  @Override
  public void visit(Text text) {}

  @Override
  public void visit(HTMLComment htmlComment) {}
}

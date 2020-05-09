package org.getalp.dbnary.wiki;

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
import org.getalp.dbnary.wiki.WikiText.WikiContent;
import org.getalp.dbnary.wiki.WikiText.WikiDocument;
import org.getalp.dbnary.wiki.WikiText.WikiSection;

public interface Visitor {
  void visit(ExternalLink externalLink);

  void visit(InternalLink internalLink);

  void visit(Heading heading);

  void visit(Indentation indentation);

  void visit(Item item);

  void visit(ListItem listItem);

  void visit(NumberedListItem listItem);

  void visit(Template template);

  void visit(WikiContent content);

  void visit(Text text);

  void visit(HTMLComment htmlComment);

  void visit(WikiSection section);

  void visit(WikiDocument section);
}

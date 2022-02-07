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
import org.getalp.dbnary.wiki.WikiText.WikiContent;
import org.getalp.dbnary.wiki.WikiText.WikiDocument;
import org.getalp.dbnary.wiki.WikiText.WikiSection;

public interface Visitor<T> {
  T visit(ExternalLink externalLink);

  T visit(InternalLink internalLink);

  T visit(Heading heading);

  T visit(Indentation indentation);

  T visit(Item item);

  T visit(ListItem listItem);

  T visit(NumberedListItem listItem);

  T visit(Template template);

  T visit(WikiContent content);

  T visit(Text text);

  T visit(HTMLComment htmlComment);

  T visit(WikiSection section);

  T visit(WikiDocument section);
}

package org.getalp.dbnary.wiki;

import org.getalp.dbnary.wiki.WikiText.*;

public interface Visitor {
  void visit(ExternalLink externalLink);

  void visit(InternalLink internalLink);

  void visit(Heading heading);

  void visit(Indentation indentation);

  void visit(ListItem listItem);

  void visit(Template template);

  void visit(WikiContent content);

  void visit(Text text);

  void visit(HTMLComment htmlComment);
}

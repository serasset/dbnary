package org.getalp.dbnary.languages.eng;

import java.util.stream.Stream;
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
import org.getalp.dbnary.wiki.visit.Visitor;

public class EnglishTranslationTemplateStream implements Visitor<Stream<Template>> {

  @Override
  public Stream<Template> visit(ExternalLink externalLink) {
    return Stream.empty();
  }

  @Override
  public Stream<Template> visit(InternalLink internalLink) {
    return Stream.empty();
  }

  @Override
  public Stream<Template> visit(Heading heading) {
    return Stream.empty();
  }

  @Override
  public Stream<Template> visit(Indentation indentation) {
    return indentation.getContent().accept(this);
  }

  @Override
  public Stream<Template> visit(Item item) {
    return item.getContent().accept(this);
  }

  @Override
  public Stream<Template> visit(ListItem listItem) {
    return listItem.getContent().accept(this);
  }

  @Override
  public Stream<Template> visit(NumberedListItem listItem) {
    return listItem.getContent().accept(this);
  }

  @Override
  public Stream<Template> visit(Template template) {
    if ("multitrans".equals(template.getName())) {
      return template.getArgs().get("data").accept(this);
    } else {
      return Stream.of(template);
    }
  }

  @Override
  public Stream<Template> visit(WikiContent content) {
    return content.tokens().stream().flatMap(t -> t.accept(this));
  }

  @Override
  public Stream<Template> visit(Text text) {
    return Stream.empty();
  }

  @Override
  public Stream<Template> visit(HTMLComment htmlComment) {
    return Stream.empty();
  }

  @Override
  public Stream<Template> visit(WikiSection section) {
    return section.getContent().accept(this);
  }

  @Override
  public Stream<Template> visit(WikiDocument section) {
    return section.getContent().accept(this);
  }
}

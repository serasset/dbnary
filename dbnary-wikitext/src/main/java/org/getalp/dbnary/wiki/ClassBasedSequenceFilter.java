package org.getalp.dbnary.wiki;

import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import org.getalp.dbnary.wiki.WikiSequenceFiltering.Action;
import org.getalp.dbnary.wiki.WikiSequenceFiltering.Atomize;
import org.getalp.dbnary.wiki.WikiSequenceFiltering.Content;
import org.getalp.dbnary.wiki.WikiSequenceFiltering.KeepAsis;
import org.getalp.dbnary.wiki.WikiSequenceFiltering.OpenContentClose;
import org.getalp.dbnary.wiki.WikiText.ExternalLink;
import org.getalp.dbnary.wiki.WikiText.HTMLComment;
import org.getalp.dbnary.wiki.WikiText.Heading;
import org.getalp.dbnary.wiki.WikiText.Indentation;
import org.getalp.dbnary.wiki.WikiText.IndentedItem;
import org.getalp.dbnary.wiki.WikiText.InternalLink;
import org.getalp.dbnary.wiki.WikiText.Item;
import org.getalp.dbnary.wiki.WikiText.Link;
import org.getalp.dbnary.wiki.WikiText.ListItem;
import org.getalp.dbnary.wiki.WikiText.NumberedListItem;
import org.getalp.dbnary.wiki.WikiText.Template;
import org.getalp.dbnary.wiki.WikiText.Text;
import org.getalp.dbnary.wiki.WikiText.Token;
import org.getalp.dbnary.wiki.WikiText.WikiContent;

/**
 * Created by serasset on 27/02/17.
 */
public class ClassBasedSequenceFilter implements Function<Token, Action> {

  private HashMap<Class, Action> actions = new HashMap<>();

  /**
   * Creates a default Class based filter that :
   * <ul>
   * <li>atomizes all, but</li>
   * <li>keep textual content</li>
   * <li>void HTML Comments and no wiki</li>
   * </ul>
   */
  public ClassBasedSequenceFilter() {
    super();
    this.atomizeAll().sourceText().voidHTMLComment().voidNowiki().openCloseHeading()
        .openCloseIndentedItem();
  }

  public ClassBasedSequenceFilter clearAction() {
    actions.clear();
    return this;
  }

  // ======================
  // Atomizing content
  // ======================

  public ClassBasedSequenceFilter atomizeTemplates() {
    actions.put(Template.class, new Atomize());
    return this;
  }

  public ClassBasedSequenceFilter atomizeInternalLink() {
    actions.put(InternalLink.class, new Atomize());
    return this;
  }

  public ClassBasedSequenceFilter atomizeLink() {
    actions.put(Link.class, new Atomize());
    return this;
  }

  public ClassBasedSequenceFilter atomizeExternalLink() {
    actions.put(ExternalLink.class, new Atomize());
    return this;
  }

  public ClassBasedSequenceFilter atomizeHTMLComment() {
    actions.put(HTMLComment.class, new Atomize());
    return this;
  }

  public ClassBasedSequenceFilter atomizeIndentedItem() {
    actions.put(IndentedItem.class, new Atomize());
    return this;
  }

  public ClassBasedSequenceFilter atomizeIndentation() {
    actions.put(Indentation.class, new Atomize());
    return this;
  }

  public ClassBasedSequenceFilter atomizeItem() {
    actions.put(Item.class, new Atomize());
    return this;
  }

  public ClassBasedSequenceFilter atomizeNumberedListItem() {
    actions.put(NumberedListItem.class, new Atomize());
    return this;
  }

  public ClassBasedSequenceFilter atomizeListItem() {
    actions.put(ListItem.class, new Atomize());
    return this;
  }

  public ClassBasedSequenceFilter atomizeHeading() {
    actions.put(Heading.class, new Atomize());
    return this;
  }

  public ClassBasedSequenceFilter atomizeNowiki() {
    // TODO: implement nowiki handling
    return this;
  }

  public ClassBasedSequenceFilter atomizeText() {
    actions.put(Text.class, new Atomize());
    return this;
  }

  public ClassBasedSequenceFilter atomizeAll() {
    this.atomizeExternalLink().atomizeHTMLComment().atomizeInternalLink().atomizeNowiki()
        .atomizeTemplates().atomizeIndentedItem().atomizeHeading().atomizeText();
    return this;
  }

  // ======================
  // Voiding content
  // ======================

  public ClassBasedSequenceFilter voidTemplates() {
    actions.put(Template.class, new WikiSequenceFiltering.Void());
    return this;
  }

  public ClassBasedSequenceFilter voidInternalLink() {
    actions.put(InternalLink.class, new WikiSequenceFiltering.Void());
    return this;
  }

  public ClassBasedSequenceFilter voidLink() {
    actions.put(Link.class, new WikiSequenceFiltering.Void());
    return this;
  }

  public ClassBasedSequenceFilter voidExternalLink() {
    actions.put(ExternalLink.class, new WikiSequenceFiltering.Void());
    return this;
  }

  public ClassBasedSequenceFilter voidHTMLComment() {
    actions.put(HTMLComment.class, new WikiSequenceFiltering.Void());
    return this;
  }

  public ClassBasedSequenceFilter voidIndentedItem() {
    actions.put(IndentedItem.class, new WikiSequenceFiltering.Void());
    return this;
  }

  public ClassBasedSequenceFilter voidIndentation() {
    actions.put(Indentation.class, new WikiSequenceFiltering.Void());
    return this;
  }

  public ClassBasedSequenceFilter voidItem() {
    actions.put(Item.class, new WikiSequenceFiltering.Void());
    return this;
  }

  public ClassBasedSequenceFilter voidNumberedListItem() {
    actions.put(NumberedListItem.class, new WikiSequenceFiltering.Void());
    return this;
  }

  public ClassBasedSequenceFilter voidListItem() {
    actions.put(ListItem.class, new WikiSequenceFiltering.Void());
    return this;
  }

  public ClassBasedSequenceFilter voidHeading() {
    actions.put(Heading.class, new WikiSequenceFiltering.Void());
    return this;
  }

  public ClassBasedSequenceFilter voidNowiki() {
    // TODO: implement nowiki handling
    return this;
  }

  public ClassBasedSequenceFilter voidText() {
    actions.put(Text.class, new WikiSequenceFiltering.Void());
    return this;
  }

  public ClassBasedSequenceFilter voidAll() {
    this.voidExternalLink().voidHTMLComment().voidInternalLink().voidNowiki().voidTemplates()
        .voidIndentedItem().voidHeading().voidText();
    return this;
  }

  public static List<Token> collectAllParameterContent(Token t) {
    if (t instanceof Template) {
      Template tt = (Template) t;
      return tt.getContent().tokens();
    } else {
      throw new RuntimeException("Cannot collect parameter contents on a non Template token");
    }
  }

  public static List<Token> getTarget(Token t) {
    if (t instanceof InternalLink) {
      InternalLink il = (InternalLink) t;
      return il.getTarget().tokens();
    } else if (t instanceof ExternalLink) {
      ExternalLink el = (ExternalLink) t;
      return el.getTarget().tokens();
    } else {
      throw new RuntimeException("Cannot collect parameter contents on a non Link token");
    }
  }

  public static List<Token> getContent(Token t) {
    if (t instanceof Heading) {
      Heading h = (Heading) t;
      return h.getContent().tokens();
    } else if (t instanceof IndentedItem) {
      return t.asIndentedItem().getContent().tokens();
    } else if (t instanceof InternalLink) {
      InternalLink li = (InternalLink) t;
      List<Token> toks = li.getLink().tokens();
      WikiContent suf = li.getSuffix();
      if (null != suf) {
        toks.addAll(suf.tokens());
      }
      return toks;
    } else if (t instanceof ExternalLink) {
      ExternalLink li = (ExternalLink) t;
      return li.getLink().tokens();
    } else {
      throw new RuntimeException("Cannot collect parameter contents on a content less token");
    }
  }

  // ======================
  // Keeping content only
  // ======================

  public ClassBasedSequenceFilter keepContentOfTemplates() {
    actions.put(Template.class, new Content(ClassBasedSequenceFilter::collectAllParameterContent));
    return this;
  }

  public ClassBasedSequenceFilter keepContentOfTemplates(
      Function<Token, List<Token>> contentGetter) {
    actions.put(Template.class, new Content(contentGetter));
    return this;
  }

  public ClassBasedSequenceFilter keepTargetOfInternalLink() {
    actions.put(InternalLink.class, new Content(ClassBasedSequenceFilter::getTarget));
    return this;
  }

  public ClassBasedSequenceFilter keepTargetOfLink() {
    actions.put(Link.class, new Content(ClassBasedSequenceFilter::getTarget));
    return this;
  }

  public ClassBasedSequenceFilter keepTargetOfExternalLink() {
    actions.put(ExternalLink.class, new Content(ClassBasedSequenceFilter::getTarget));
    return this;
  }

  public ClassBasedSequenceFilter keepContentOfInternalLink() {
    actions.put(InternalLink.class, new Content(ClassBasedSequenceFilter::getContent));
    return this;
  }

  public ClassBasedSequenceFilter keepContentOfLink() {
    actions.put(Link.class, new Content(ClassBasedSequenceFilter::getContent));
    return this;
  }

  public ClassBasedSequenceFilter keepContentOfExternalLink() {
    actions.put(ExternalLink.class, new Content(ClassBasedSequenceFilter::getContent));
    return this;
  }

  // public ClassBasedSequenceFilter keepContentOfHTMLComment() {
  // actions.put(WikiText.HTMLComment.class, new WikiSequenceFiltering.Content());
  // return this;
  // }

  public ClassBasedSequenceFilter keepContentOfIndentedItem() {
    actions.put(IndentedItem.class, new Content(ClassBasedSequenceFilter::getContent));
    return this;
  }

  public ClassBasedSequenceFilter keepContentOfListItem() {
    actions.put(ListItem.class, new Content(ClassBasedSequenceFilter::getContent));
    return this;
  }

  public ClassBasedSequenceFilter keepContentOfIndentation() {
    actions.put(Indentation.class, new Content(ClassBasedSequenceFilter::getContent));
    return this;
  }

  public ClassBasedSequenceFilter keepContentOfItem() {
    actions.put(Item.class, new Content(ClassBasedSequenceFilter::getContent));
    return this;
  }

  public ClassBasedSequenceFilter keepContentOfNumberedListItem() {
    actions.put(NumberedListItem.class, new Content(ClassBasedSequenceFilter::getContent));
    return this;
  }

  public ClassBasedSequenceFilter keepContentOfHeading() {
    actions.put(Heading.class, new Content(ClassBasedSequenceFilter::getContent));
    return this;
  }

  public ClassBasedSequenceFilter keepContentOfNowiki() {
    // TODO: implement nowiki handling
    return this;
  }

  // public ClassBasedSequenceFilter keepContentOfText() {
  // actions.put(WikiText.Text.class, new WikiSequenceFiltering.Content());
  // return this;
  // }

  public ClassBasedSequenceFilter keepContentOfAll() {
    this.keepTargetOfExternalLink().keepTargetOfInternalLink().keepContentOfNowiki()
        .keepContentOfTemplates().keepContentOfIndentedItem().keepContentOfHeading().sourceText();
    return this;
  }


  // ======================
  // Open/content/close content
  // ======================
  public ClassBasedSequenceFilter openCloseTemplates() {
    actions.put(Template.class,
        new OpenContentClose(ClassBasedSequenceFilter::collectAllParameterContent));
    return this;
  }

  public ClassBasedSequenceFilter openCloseInternalLink() {
    actions.put(InternalLink.class, new OpenContentClose(ClassBasedSequenceFilter::getTarget));
    return this;
  }

  public ClassBasedSequenceFilter openCloseLink() {
    actions.put(Link.class, new OpenContentClose(ClassBasedSequenceFilter::getTarget));
    return this;
  }

  public ClassBasedSequenceFilter openCloseExternalLink() {
    actions.put(ExternalLink.class, new OpenContentClose(ClassBasedSequenceFilter::getTarget));
    return this;
  }

  // public ClassBasedSequenceFilter openCloseHTMLComment() {
  // actions.put(WikiText.HTMLComment.class, new WikiSequenceFiltering.OpenContentClose());
  // return this;
  // }

  public ClassBasedSequenceFilter openCloseIndentedItem() {
    actions.put(IndentedItem.class, new OpenContentClose(ClassBasedSequenceFilter::getContent));
    return this;
  }

  public ClassBasedSequenceFilter openCloseListItem() {
    actions.put(ListItem.class, new OpenContentClose(ClassBasedSequenceFilter::getContent));
    return this;
  }

  public ClassBasedSequenceFilter openCloseIndentation() {
    actions.put(Indentation.class, new OpenContentClose(ClassBasedSequenceFilter::getContent));
    return this;
  }

  public ClassBasedSequenceFilter openCloseItem() {
    actions.put(Item.class, new OpenContentClose(ClassBasedSequenceFilter::getContent));
    return this;
  }

  public ClassBasedSequenceFilter openCloseNumberedListItem() {
    actions.put(NumberedListItem.class, new OpenContentClose(ClassBasedSequenceFilter::getContent));
    return this;
  }

  public ClassBasedSequenceFilter openCloseHeading() {
    actions.put(Heading.class, new OpenContentClose(ClassBasedSequenceFilter::getContent));
    return this;
  }

  public ClassBasedSequenceFilter openCloseNowiki() {
    // TODO: implement nowiki handling
    return this;
  }

  // public ClassBasedSequenceFilter openCloseText() {
  // actions.put(WikiText.Text.class, new
  // WikiSequenceFiltering.OpenContentClose(ClassBasedSequenceFilter::getContent));
  // return this;
  // }

  public ClassBasedSequenceFilter openCloseAll() {
    this.openCloseExternalLink().openCloseInternalLink().openCloseNowiki().openCloseTemplates()
        .openCloseIndentedItem().openCloseHeading().sourceText();
    return this;
  }

  // ======================
  // Keeping source as is
  // ======================

  public ClassBasedSequenceFilter sourceTemplates() {
    actions.put(Template.class, new KeepAsis());
    return this;
  }

  public ClassBasedSequenceFilter sourceInternalLink() {
    actions.put(InternalLink.class, new KeepAsis());
    return this;
  }

  public ClassBasedSequenceFilter sourceLink() {
    actions.put(Link.class, new KeepAsis());
    return this;
  }

  public ClassBasedSequenceFilter sourceExternalLink() {
    actions.put(ExternalLink.class, new KeepAsis());
    return this;
  }

  public ClassBasedSequenceFilter sourceHTMLComment() {
    actions.put(HTMLComment.class, new KeepAsis());
    return this;
  }

  public ClassBasedSequenceFilter sourceIndentedItem() {
    actions.put(IndentedItem.class, new KeepAsis());
    return this;
  }

  public ClassBasedSequenceFilter sourceListItem() {
    actions.put(ListItem.class, new KeepAsis());
    return this;
  }

  public ClassBasedSequenceFilter sourceIndentation() {
    actions.put(Indentation.class, new KeepAsis());
    return this;
  }

  public ClassBasedSequenceFilter sourceItem() {
    actions.put(Item.class, new KeepAsis());
    return this;
  }

  public ClassBasedSequenceFilter sourceNumberedListItem() {
    actions.put(NumberedListItem.class, new KeepAsis());
    return this;
  }

  public ClassBasedSequenceFilter sourceHeading() {
    actions.put(Heading.class, new KeepAsis());
    return this;
  }

  public ClassBasedSequenceFilter sourceNowiki() {
    // TODO: implement nowiki handling
    return this;
  }

  public ClassBasedSequenceFilter sourceText() {
    actions.put(Text.class, new KeepAsis());
    return this;
  }

  public ClassBasedSequenceFilter sourceAll() {
    this.sourceExternalLink().sourceHTMLComment().sourceInternalLink().sourceNowiki()
        .sourceTemplates().sourceIndentedItem().sourceHeading().sourceText();
    return this;
  }


  @Override
  public Action apply(Token tok) {
    Class clazz = tok.getClass();
    Action action = null;
    while (clazz != Token.class && (action = actions.get(clazz)) == null) {
      clazz = clazz.getSuperclass();
    }
    return (action == null) ? new WikiSequenceFiltering.Void() : action;
  }
}

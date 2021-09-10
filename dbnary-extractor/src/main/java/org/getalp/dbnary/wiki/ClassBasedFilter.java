package org.getalp.dbnary.wiki;

import static org.getalp.dbnary.wiki.WikiEventFilter.Action.ENTER;
import static org.getalp.dbnary.wiki.WikiEventFilter.Action.KEEP;
import static org.getalp.dbnary.wiki.WikiEventFilter.Action.VOID;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by serasset on 01/02/16.
 */
// TODO: try to uniformize the Sequence filtering machinery with this EventFilter
// Eventually, do rely on stream/filter functions instead...
public class ClassBasedFilter implements WikiEventFilter {

  private HashSet<Class> classesToKeep = new HashSet<Class>();
  private HashSet<Class> classesToEnter = new HashSet<Class>();

  public ClassBasedFilter() {
    super();
  }

  public ClassBasedFilter(Set<Class> allowedClasses) {
    super();
    this.classesToKeep.addAll(allowedClasses);
  }

  public ClassBasedFilter(Set<Class> allowedClasses, Set<Class> goIntoClasses) {
    super();
    this.classesToKeep.addAll(allowedClasses);
    this.enterAll();
  }

  public ClassBasedFilter allowTemplates() {
    classesToKeep.add(WikiText.Template.class);
    return this;
  }

  public ClassBasedFilter allowInternalLink() {
    classesToKeep.add(WikiText.InternalLink.class);
    return this;
  }

  public ClassBasedFilter allowLink() {
    classesToKeep.add(WikiText.Link.class);
    return this;
  }

  public ClassBasedFilter allowExternalLink() {
    classesToKeep.add(WikiText.ExternalLink.class);
    return this;
  }

  public ClassBasedFilter allowHTMLComment() {
    classesToKeep.add(WikiText.HTMLComment.class);
    return this;
  }

  public ClassBasedFilter allowIndentedItem() {
    classesToKeep.add(WikiText.IndentedItem.class);
    return this;
  }

  public ClassBasedFilter allowListItem() {
    classesToKeep.add(WikiText.ListItem.class);
    return this;
  }

  public ClassBasedFilter allowNumberedListItem() {
    classesToKeep.add(WikiText.NumberedListItem.class);
    return this;
  }

  public ClassBasedFilter allowIndentation() {
    classesToKeep.add(WikiText.Indentation.class);
    return this;
  }

  public ClassBasedFilter allowItem() {
    classesToKeep.add(WikiText.Item.class);
    return this;
  }

  public ClassBasedFilter allowHeading() {
    classesToKeep.add(WikiText.Heading.class);
    return this;
  }

  public ClassBasedFilter allowNowiki() {
    // TODO: implement nowiki handling
    return this;
  }

  public ClassBasedFilter allowText() {
    classesToKeep.add(WikiText.Text.class);
    return this;
  }

  public ClassBasedFilter allowSections() {
    classesToKeep.add(WikiText.WikiSection.class);
    return this;
  }

  public ClassBasedFilter allowAll() {
    this.allowExternalLink().allowHTMLComment().allowInternalLink().allowNowiki().allowTemplates()
        .allowIndentedItem().allowHeading().allowText();
    return this;
  }

  public ClassBasedFilter denyTemplates() {
    classesToKeep.remove(WikiText.Template.class);
    return this;
  }

  public ClassBasedFilter denyInternalLink() {
    classesToKeep.remove(WikiText.InternalLink.class);
    return this;
  }

  public ClassBasedFilter denyExternalLink() {
    classesToKeep.remove(WikiText.ExternalLink.class);
    return this;
  }

  public ClassBasedFilter denyIndentedItem() {
    classesToKeep.remove(WikiText.IndentedItem.class);
    return this;
  }

  public ClassBasedFilter denyListItem() {
    classesToKeep.remove(WikiText.ListItem.class);
    return this;
  }

  public ClassBasedFilter denyIndentation() {
    classesToKeep.remove(WikiText.Indentation.class);
    return this;
  }

  public ClassBasedFilter denyItem() {
    classesToKeep.remove(WikiText.Item.class);
    return this;
  }

  public ClassBasedFilter denyNumberedListItem() {
    classesToKeep.remove(WikiText.NumberedListItem.class);
    return this;
  }

  public ClassBasedFilter denyHeading() {
    classesToKeep.remove(WikiText.Heading.class);
    return this;
  }

  public ClassBasedFilter denyHTMLComment() {
    classesToKeep.remove(WikiText.HTMLComment.class);
    return this;
  }

  public ClassBasedFilter denyNowiki() {
    // TODO: implement nowiki handling
    return this;
  }

  public ClassBasedFilter denyText() {
    classesToKeep.remove(WikiText.Text.class);
    return this;
  }

  public ClassBasedFilter denySection() {
    classesToKeep.remove(WikiText.WikiSection.class);
    return this;
  }


  public ClassBasedFilter denyAll() {
    classesToKeep.clear();
    return this;
  }

  public ClassBasedFilter enterIndentedItem() {
    classesToEnter.add(WikiText.IndentedItem.class);
    return this;
  }

  public ClassBasedFilter enterHeadings() {
    classesToEnter.add(WikiText.Heading.class);
    return this;
  }

  public ClassBasedFilter enterSections() {
    classesToEnter.add(WikiText.WikiSection.class);
    return this;
  }

  public ClassBasedFilter enterAll() {
    this.enterIndentedItem().enterHeadings().enterSections();
    return this;
  }

  @Override
  public Action apply(WikiText.Token tok) {
    for (Class allowedClass : classesToKeep) {
      if (allowedClass.isInstance(tok)) {
        return KEEP;
      }
    }
    for (Class allowedClass : classesToEnter) {
      if (allowedClass.isInstance(tok)) {
        return ENTER;
      }
    }
    return VOID;
  }
}

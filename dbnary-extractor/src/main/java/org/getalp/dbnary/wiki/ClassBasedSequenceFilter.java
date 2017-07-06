package org.getalp.dbnary.wiki;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Created by serasset on 27/02/17.
 */
public class ClassBasedSequenceFilter implements Function<WikiText.Token, WikiSequenceFiltering.Action> {

    private HashMap<Class, WikiSequenceFiltering.Action> actions = new HashMap<>();

    /**
     * Creates a default Class baed filter that :
     * <ul>
     * <li>atomizes all, but</li>
     * <li>keep textual content</li>
     * <li>void HTML Comments and no wiki</li>
     * </ul>
     */
    public ClassBasedSequenceFilter() {
        super();
        this.atomizeAll().sourceText().voidHTMLComment().voidNowiki().openCloseHeading().openCloseListItem();
    }

    public ClassBasedSequenceFilter clearAction() {
        actions.clear();
        return this;
    }

    // ======================
    // Atomizing content
    // ======================

    public ClassBasedSequenceFilter atomizeTemplates() {
        actions.put(WikiText.Template.class,  new WikiSequenceFiltering.Atomize());
        return this;
    }

    public ClassBasedSequenceFilter atomizeInternalLink() {
        actions.put(WikiText.InternalLink.class, new WikiSequenceFiltering.Atomize());
        return this;
    }

    public ClassBasedSequenceFilter atomizeLink() {
        actions.put(WikiText.Link.class, new WikiSequenceFiltering.Atomize());
        return this;
    }

    public ClassBasedSequenceFilter atomizeExternalLink() {
        actions.put(WikiText.ExternalLink.class, new WikiSequenceFiltering.Atomize());
        return this;
    }

    public ClassBasedSequenceFilter atomizeHTMLComment() {
        actions.put(WikiText.HTMLComment.class, new WikiSequenceFiltering.Atomize());
        return this;
    }

    public ClassBasedSequenceFilter atomizeListItem() {
        actions.put(WikiText.ListItem.class, new WikiSequenceFiltering.Atomize());
        return this;
    }

    public ClassBasedSequenceFilter atomizeHeading() {
        actions.put(WikiText.Heading.class, new WikiSequenceFiltering.Atomize());
        return this;
    }

    public ClassBasedSequenceFilter atomizeNowiki() {
        // TODO: implement nowiki handling
        return this;
    }

    public ClassBasedSequenceFilter atomizeText() {
        actions.put(WikiText.Text.class, new WikiSequenceFiltering.Atomize());
        return this;
    }

    public ClassBasedSequenceFilter atomizeAll() {
        this.atomizeExternalLink().atomizeHTMLComment().atomizeInternalLink().atomizeNowiki().atomizeTemplates().
                atomizeListItem().atomizeHeading().atomizeText();
        return this;
    }

    // ======================
    // Voiding content
    // ======================

    public ClassBasedSequenceFilter voidTemplates() {
        actions.put(WikiText.Template.class,new WikiSequenceFiltering.Void());
        return this;
    }

    public ClassBasedSequenceFilter voidInternalLink() {
        actions.put(WikiText.InternalLink.class,new WikiSequenceFiltering.Void());
        return this;
    }

    public ClassBasedSequenceFilter voidLink() {
        actions.put(WikiText.Link.class,new WikiSequenceFiltering.Void());
        return this;
    }

    public ClassBasedSequenceFilter voidExternalLink() {
        actions.put(WikiText.ExternalLink.class,new WikiSequenceFiltering.Void());
        return this;
    }

    public ClassBasedSequenceFilter voidHTMLComment() {
        actions.put(WikiText.HTMLComment.class,new WikiSequenceFiltering.Void());
        return this;
    }

    public ClassBasedSequenceFilter voidListItem() {
        actions.put(WikiText.ListItem.class,new WikiSequenceFiltering.Void());
        return this;
    }

    public ClassBasedSequenceFilter voidHeading() {
        actions.put(WikiText.Heading.class,new WikiSequenceFiltering.Void());
        return this;
    }

    public ClassBasedSequenceFilter voidNowiki() {
        // TODO: implement nowiki handling
        return this;
    }

    public ClassBasedSequenceFilter voidText() {
        actions.put(WikiText.Text.class,new WikiSequenceFiltering.Void());
        return this;
    }

    public ClassBasedSequenceFilter voidAll() {
        this.voidExternalLink().voidHTMLComment().voidInternalLink().voidNowiki().voidTemplates().
                voidListItem().voidHeading().voidText();
        return this;
    }

    public static ArrayList<WikiText.Token> collectAllParameterContent(WikiText.Token t) {
        if (t instanceof WikiText.Template) {
            WikiText.Template tt = (WikiText.Template) t;
            return tt.getContent().tokens();
        } else
            throw new RuntimeException("Cannot collect parameter contents on a non Template token");
    }

    public static ArrayList<WikiText.Token> getTarget(WikiText.Token t) {
        if (t instanceof WikiText.InternalLink) {
            WikiText.InternalLink il = (WikiText.InternalLink) t;
            return il.getTarget().tokens();
        } else if (t instanceof WikiText.ExternalLink) {
            WikiText.ExternalLink el = (WikiText.ExternalLink) t;
            return ((WikiText.ExternalLink) t).getTarget().tokens();
        } else
            throw new RuntimeException("Cannot collect parameter contents on a non Link token");
    }

    public static ArrayList<WikiText.Token> getContent(WikiText.Token t) {
        if (t instanceof WikiText.Heading) {
            WikiText.Heading h = (WikiText.Heading) t;
            return h.getContent().tokens();
        } else if (t instanceof WikiText.ListItem) {
            WikiText.ListItem li = (WikiText.ListItem) t;
            return li.getContent().tokens();
        } else if (t instanceof WikiText.Indentation) {
            WikiText.Indentation li = (WikiText.Indentation) t;
            return li.getContent().tokens();
        } else
            throw new RuntimeException("Cannot collect parameter contents on a non Link token");
    }

    // ======================
    //  Keeping content only
    // ======================

    public ClassBasedSequenceFilter keepContentOfTemplates() {
        actions.put(WikiText.Template.class, new WikiSequenceFiltering.Content(ClassBasedSequenceFilter::collectAllParameterContent));
        return this;
    }

    public ClassBasedSequenceFilter keepContentOfInternalLink() {
        actions.put(WikiText.InternalLink.class, new WikiSequenceFiltering.Content(ClassBasedSequenceFilter::getTarget));
        return this;
    }

    public ClassBasedSequenceFilter keepContentOfLink() {
        actions.put(WikiText.Link.class, new WikiSequenceFiltering.Content(ClassBasedSequenceFilter::getTarget));
        return this;
    }

    public ClassBasedSequenceFilter keepContentOfExternalLink() {
        actions.put(WikiText.ExternalLink.class, new WikiSequenceFiltering.Content(ClassBasedSequenceFilter::getTarget));
        return this;
    }

//    public ClassBasedSequenceFilter keepContentOfHTMLComment() {
//        actions.put(WikiText.HTMLComment.class, new WikiSequenceFiltering.Content());
//        return this;
//    }

    public ClassBasedSequenceFilter keepContentOfListItem() {
        actions.put(WikiText.ListItem.class, new WikiSequenceFiltering.Content(ClassBasedSequenceFilter::getContent));
        return this;
    }

    public ClassBasedSequenceFilter keepContentOfHeading() {
        actions.put(WikiText.Heading.class, new WikiSequenceFiltering.Content(ClassBasedSequenceFilter::getContent));
        return this;
    }

    public ClassBasedSequenceFilter keepContentOfNowiki() {
        // TODO: implement nowiki handling
        return this;
    }

//    public ClassBasedSequenceFilter keepContentOfText() {
//        actions.put(WikiText.Text.class, new WikiSequenceFiltering.Content());
//        return this;
//    }

    public ClassBasedSequenceFilter keepContentOfAll() {
        this.keepContentOfExternalLink().keepContentOfInternalLink().keepContentOfNowiki().keepContentOfTemplates().
                keepContentOfListItem().keepContentOfHeading().sourceText();
        return this;
    }


    // ======================
    // Open/content/close content
    // ======================
    public ClassBasedSequenceFilter openCloseTemplates() {
        actions.put(WikiText.Template.class, new WikiSequenceFiltering.OpenContentClose(ClassBasedSequenceFilter::collectAllParameterContent));
        return this;
    }

    public ClassBasedSequenceFilter openCloseInternalLink() {
        actions.put(WikiText.InternalLink.class, new WikiSequenceFiltering.OpenContentClose(ClassBasedSequenceFilter::getTarget));
        return this;
    }

    public ClassBasedSequenceFilter openCloseLink() {
        actions.put(WikiText.Link.class, new WikiSequenceFiltering.OpenContentClose(ClassBasedSequenceFilter::getTarget));
        return this;
    }

    public ClassBasedSequenceFilter openCloseExternalLink() {
        actions.put(WikiText.ExternalLink.class, new WikiSequenceFiltering.OpenContentClose(ClassBasedSequenceFilter::getTarget));
        return this;
    }

//    public ClassBasedSequenceFilter openCloseHTMLComment() {
//        actions.put(WikiText.HTMLComment.class, new WikiSequenceFiltering.OpenContentClose());
//        return this;
//    }

    public ClassBasedSequenceFilter openCloseListItem() {
        actions.put(WikiText.ListItem.class, new WikiSequenceFiltering.OpenContentClose(ClassBasedSequenceFilter::getContent));
        return this;
    }

    public ClassBasedSequenceFilter openCloseHeading() {
        actions.put(WikiText.Heading.class, new WikiSequenceFiltering.OpenContentClose(ClassBasedSequenceFilter::getContent));
        return this;
    }

    public ClassBasedSequenceFilter openCloseNowiki() {
        // TODO: implement nowiki handling
        return this;
    }

//    public ClassBasedSequenceFilter openCloseText() {
//        actions.put(WikiText.Text.class, new WikiSequenceFiltering.OpenContentClose(ClassBasedSequenceFilter::getContent));
//        return this;
//    }

    public ClassBasedSequenceFilter openCloseAll() {
        this.openCloseExternalLink().openCloseInternalLink().openCloseNowiki().openCloseTemplates().
                openCloseListItem().openCloseHeading().sourceText();
        return this;
    }

    // ======================
    // Keeping source as is
    // ======================

    public ClassBasedSequenceFilter sourceTemplates() {
        actions.put(WikiText.Template.class, new WikiSequenceFiltering.KeepAsis());
        return this;
    }

    public ClassBasedSequenceFilter sourceInternalLink() {
        actions.put(WikiText.InternalLink.class, new WikiSequenceFiltering.KeepAsis());
        return this;
    }

    public ClassBasedSequenceFilter sourceLink() {
        actions.put(WikiText.Link.class, new WikiSequenceFiltering.KeepAsis());
        return this;
    }

    public ClassBasedSequenceFilter sourceExternalLink() {
        actions.put(WikiText.ExternalLink.class, new WikiSequenceFiltering.KeepAsis());
        return this;
    }

    public ClassBasedSequenceFilter sourceHTMLComment() {
        actions.put(WikiText.HTMLComment.class, new WikiSequenceFiltering.KeepAsis());
        return this;
    }

    public ClassBasedSequenceFilter sourceListItem() {
        actions.put(WikiText.ListItem.class, new WikiSequenceFiltering.KeepAsis());
        return this;
    }

    public ClassBasedSequenceFilter sourceHeading() {
        actions.put(WikiText.Heading.class, new WikiSequenceFiltering.KeepAsis());
        return this;
    }

    public ClassBasedSequenceFilter sourceNowiki() {
        // TODO: implement nowiki handling
        return this;
    }

    public ClassBasedSequenceFilter sourceText() {
        actions.put(WikiText.Text.class, new WikiSequenceFiltering.KeepAsis());
        return this;
    }

    public ClassBasedSequenceFilter sourceAll() {
        this.sourceExternalLink().sourceHTMLComment().sourceInternalLink().sourceNowiki().sourceTemplates().
                sourceListItem().sourceHeading().sourceText();
        return this;
    }


    @Override
    public WikiSequenceFiltering.Action apply(WikiText.Token tok) {
        Class clazz = tok.getClass();
        WikiSequenceFiltering.Action action = null;
        while (clazz != WikiText.Token.class && (action = actions.get(clazz)) == null)
            clazz = clazz.getSuperclass();
        return (action == null) ? new WikiSequenceFiltering.Void() : action;
    }
}

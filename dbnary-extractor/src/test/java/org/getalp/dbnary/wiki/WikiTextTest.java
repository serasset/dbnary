package org.getalp.dbnary.wiki;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.*;

public class WikiTextTest {

    @Test
    public void testParse() throws Exception {
        String test = "text {{f1|x=ccc|z={{toto}}}} text <!-- [[un lien caché]] {{ --> encore [[lien#bidule]] [[Category:English|text]] [http://kaiko.getalp.org/about-dbnary about DBnary]   [[lien|]]";
        WikiText text = new WikiText(test);

        assert !text.wikiTokens().isEmpty();
        assertEquals(6, text.wikiTokens().size());
        assertTrue(text.wikiTokens().get(0) instanceof WikiText.Template);
        assertTrue(text.wikiTokens().get(1) instanceof WikiText.HTMLComment);
        assertTrue(text.wikiTokens().get(2) instanceof WikiText.InternalLink);
        assertTrue(text.wikiTokens().get(3) instanceof WikiText.InternalLink);
        WikiText.InternalLink l = (WikiText.InternalLink) text.wikiTokens().get(3);
        assertEquals("Category:English", l.target.toString());
        assertEquals("text", l.text.toString());
        assertEquals("text", l.getLinkText());
        assertEquals("Category:English", l.getFullTargetText());

        WikiText.InternalLink l1 = (WikiText.InternalLink) text.wikiTokens().get(2);
        assertEquals("lien#bidule", l1.getLinkText());
        assertEquals("lien#bidule", l1.getFullTargetText());
        assertEquals("lien", l1.getTargetText());
        assertTrue(l1.hasAnchor());
        assertEquals("bidule", l1.getAnchorText());

        assertTrue(text.wikiTokens().get(4) instanceof WikiText.ExternalLink);
        WikiText.ExternalLink l2 = (WikiText.ExternalLink) text.wikiTokens().get(4);
        assertEquals("http://kaiko.getalp.org/about-dbnary", l2.target.toString());
        assertEquals("about DBnary", l2.text.toString());

        assertTrue(text.wikiTokens().get(5) instanceof WikiText.InternalLink);

    }

    @Test
    public void testParseWithTextTokens() throws Exception {
        String test = "text {{f1|x=ccc|z={{toto}}}} text <!-- [[un lien caché]] {{ --> encore [[lien]] [[Category:English|text]] [http://kaiko.getalp.org/about-dbnary about DBnary]   [[lien|]]";
        WikiText text = new WikiText(test);

        ArrayList<? extends WikiText.Token> toks = text.tokens();
        assert !toks.isEmpty();
        assertEquals(12, toks.size());
        assertTrue(toks.get(0) instanceof WikiText.Text);
        assertTrue(toks.get(1) instanceof WikiText.Template);
        assertTrue(toks.get(2) instanceof WikiText.Text);
        assertTrue(toks.get(3) instanceof WikiText.HTMLComment);
        assertTrue(toks.get(4) instanceof WikiText.Text);
        assertTrue(toks.get(5) instanceof WikiText.InternalLink);

        WikiText.Text t = (WikiText.Text) toks.get(0);
        assertEquals("text ", t.getText());

        WikiText.InternalLink l = (WikiText.InternalLink) toks.get(7);
        assertEquals("Category:English", l.target.toString());
        assertEquals("text", l.text.toString());
        assertEquals("text", l.getLinkText());
        assertEquals("Category:English", l.getFullTargetText());

        WikiText.InternalLink l1 = (WikiText.InternalLink) toks.get(5);
        assertEquals("lien", l1.getLinkText());
        assertEquals("lien", l1.getFullTargetText());

        assertTrue(toks.get(9) instanceof WikiText.ExternalLink);
        WikiText.ExternalLink l2 = (WikiText.ExternalLink) toks.get(9);
        assertEquals("http://kaiko.getalp.org/about-dbnary", l2.target.toString());
        assertEquals("about DBnary", l2.text.toString());

        assertTrue(toks.get(11) instanceof WikiText.InternalLink);

        text = new WikiText(null, test, 5, 35);
        toks = text.tokens();
        assert !toks.isEmpty();
        assertTrue(toks.get(0) instanceof WikiText.Template);
        assertTrue(toks.get(1) instanceof WikiText.Text);

    }

    @Test
    public void testParseOnlyOneTemplate() throws Exception {
        String test = "{{en-noun|head=[[araneomorph]] {{vern|funnel-web spider|pedia=1}}}}";
        WikiText text = new WikiText(test);

        assertNotNull(text.wikiTokens());
        assertFalse(text.wikiTokens().isEmpty());
        assertTrue(text.wikiTokens().get(0) instanceof WikiText.Template);
        assertEquals(1, text.wikiTokens().size());

    }

    @Test
    public void testParseWithBoundaries() throws Exception {
        String test = "{{f1|x=[[ccc]]}} text <!-- [[un lien caché]] {{ --> encore [[lien]] {{f2|[[text]]|]   [[lien|}}";
        WikiText text = new WikiText(null, test, 17, 90);


        assert !text.wikiTokens().isEmpty();
        // 1 HTMLComment, 1 Internal Link, 1 InternalLink (as Template should be ignored as it remains unclosed in the offset.
        assertEquals(3, text.wikiTokens().size());
        assertTrue(text.wikiTokens().get(0) instanceof WikiText.HTMLComment);
        assertTrue(text.wikiTokens().get(1) instanceof WikiText.InternalLink);
        assertTrue(text.wikiTokens().get(2) instanceof WikiText.InternalLink);
        WikiText.InternalLink l = (WikiText.InternalLink) text.wikiTokens().get(2);
        assertEquals("text", l.target.toString());

    }

    @Test
    public void testParseWithUnclosedTemplate() throws Exception {
        String test = "{{en-noun|head=[[araneomorph]] {{vern|funnel-web spider|pedia=1}}";
        WikiText text = new WikiText(test);

        assertNotNull(text.wikiTokens());
        assertFalse(text.wikiTokens().isEmpty());
        // The template is not closed, hence it is considered as a text and the first valid token in a link.
        assertTrue(text.wikiTokens().get(0) instanceof WikiText.InternalLink);
        assertEquals(2, text.wikiTokens().size());

    }

    @Test
    public void testWikiTextIterator() throws Exception {
        String test = "{{en-noun}} text [[link]] text {{template}} text ";
        WikiText text = new WikiText(test);

        WikiEventsSequence s = text.templates();
        Iterator<WikiText.Token> it = s.iterator();

        assertTrue(it.hasNext());
        assertTrue(it.next() instanceof WikiText.Template);
        assertTrue(it.hasNext());
        assertTrue(it.next() instanceof WikiText.Template);
        assertFalse(it.hasNext());

        ClassBasedFilter filter = new ClassBasedFilter();
        filter.allowInternalLink();
        s = text.filteredTokens(filter);
        it = s.iterator();

        assertTrue(it.hasNext());
        assertTrue(it.next() instanceof WikiText.InternalLink);
        assertFalse(it.hasNext());

    }

    @Test
    public void testWikiTextIterator2() throws Exception {
        String test = "{{en-noun}} text [[link]] text {{template}} text [[toto]]";
        WikiText text = new WikiText(test);

        ClassBasedFilter filter = new ClassBasedFilter();
        filter.allowInternalLink().allowTemplates();
        WikiEventsSequence s = text.filteredTokens(filter);
        Iterator<WikiText.Token> it = s.iterator();

        assertTrue(it.hasNext());
        assertTrue(it.next() instanceof WikiText.Template);
        assertTrue(it.hasNext());
        assertTrue(it.next() instanceof WikiText.InternalLink);
        assertTrue(it.hasNext());
        assertTrue(it.next() instanceof WikiText.Template);
        assertTrue(it.hasNext());
        assertTrue(it.next() instanceof WikiText.InternalLink);
        assertFalse(it.hasNext());
    }

    @Test
    public void testWikiTextIteratorWithEmbedding() throws Exception {
        String test = "{{en-noun|text [[link]]}} text {{template}} text [[toto]]";
        WikiText text = new WikiText(test);

        ClassBasedFilter filter = new ClassBasedFilter();
        filter.allowInternalLink().allowTemplates();
        WikiEventsSequence s = text.filteredTokens(filter);
        Iterator<WikiText.Token> it = s.iterator();

        assertTrue(it.hasNext());
        assertTrue(it.next() instanceof WikiText.Template);
        // Internal Link is hidden inside the Template
        assertTrue(it.hasNext());
        assertTrue(it.next() instanceof WikiText.Template);
        assertTrue(it.hasNext());
        assertTrue(it.next() instanceof WikiText.InternalLink);
        assertFalse(it.hasNext());
    }


    @Test
    public void testList() throws Exception {
        String test = "* {{l|en|thalamus}}\n" +
                "* {{l|en|hypothalamus}} * toto\n" +
                "* {{l|en|prethalamus}}\n" +
                ": toto\n" +
                ": titi";
        WikiText text = new WikiText(test);

        ClassBasedFilter filter = new ClassBasedFilter();
        filter.allowInternalLink().allowTemplates().allowListItem();
        WikiEventsSequence s = text.filteredTokens(filter);
        Iterator<WikiText.Token> it = s.iterator();

        assertTrue(it.hasNext());
        assertTrue(it.next() instanceof WikiText.ListItem);
        assertTrue(it.hasNext());
        assertTrue(it.next() instanceof WikiText.ListItem);
        assertTrue(it.hasNext());
        assertTrue(it.next() instanceof WikiText.ListItem);
        assertTrue(it.hasNext());
        WikiText.Token t;
        assertTrue((t = it.next()) instanceof WikiText.Indentation);
        assertEquals(" toto", ((WikiText.Indentation) t).getContent().toString());
        assertTrue((t = it.next()) instanceof WikiText.Indentation);
        assertEquals(" titi", ((WikiText.Indentation) t).getContent().toString());
        assertFalse(it.hasNext());

    }

    @Test
    public void testParseTemplateArgs() throws Exception {
        String test = "{{en-noun|head=[[araneomorph]] {{vern|funnel-web spider|pedia=1}}|v1|xx=vxx|v2}}";
        WikiText text = new WikiText(test);

        assertNotNull(text.wikiTokens());
        assertFalse(text.wikiTokens().isEmpty());
        WikiText.Template tmpl = (WikiText.Template) text.wikiTokens().get(0);

        assertEquals("en-noun", tmpl.getName());
        Map<String, String> args = tmpl.getParsedArgs();
        assertEquals(4, args.size());
        assertEquals("v1", args.get("1"));
        assertEquals("v2", args.get("2"));
        assertEquals("vxx", args.get("xx"));
        assertEquals("[[araneomorph]] {{vern|funnel-web spider|pedia=1}}", args.get("head"));

        test = "{{test|notakey {{x|toto=titi}}|thisisakey={{x|toto=titi}}}}";
        text = new WikiText(test);

        ArrayList<WikiText.Token> wt = text.wikiTokens();
        assertNotNull(wt);
        assertFalse(wt.isEmpty());
        assertEquals(1, wt.size());
        tmpl = (WikiText.Template) text.wikiTokens().get(0);

        Map<String, WikiText.WikiContent> tmplargs = tmpl.getArgs();
        assertEquals(2, tmplargs.size());
        assertNotNull(tmplargs.get("1"));
        assertNotNull(tmplargs.get("thisisakey"));
    }

    @Test
    public void testExternalLinks() throws Exception {
        String test = "[http://this.is.a link] [1,2] [sms:thisonealso] [http://this.one.also]]";
        WikiText text = new WikiText(test);

        assertNotNull(text.wikiTokens());
        assertFalse(text.wikiTokens().isEmpty());
        assertTrue(text.wikiTokens().get(0) instanceof WikiText.ExternalLink);
        WikiText.ExternalLink el = (WikiText.ExternalLink) text.wikiTokens().get(0);
        assertEquals("http://this.is.a", el.target.toString());
        assertEquals("link", el.text.toString());

        // [1,2] is not a link
        assertTrue(text.wikiTokens().get(1) instanceof WikiText.ExternalLink);
        el = (WikiText.ExternalLink) text.wikiTokens().get(1);
        assertEquals("sms:thisonealso", el.target.toString());
        assertNull(el.text);

        assertTrue(text.wikiTokens().get(2) instanceof WikiText.ExternalLink);
        el = (WikiText.ExternalLink) text.wikiTokens().get(2);
        assertEquals("http://this.one.also", el.target.toString());
        assertNull(el.text);

    }

    @Test
    public void testInternalLinks() throws Exception {
        String test = "[[Help]] [[Help|text]] [[Help]]s [[Help|text]]s";
        WikiText text = new WikiText(test);

        assertNotNull(text.wikiTokens());
        assertFalse(text.wikiTokens().isEmpty());
        assertTrue(text.wikiTokens().get(0) instanceof WikiText.InternalLink);
        WikiText.InternalLink el = (WikiText.InternalLink) text.wikiTokens().get(0);
        assertEquals("Help", el.getFullTargetText().toString());
        assertEquals("Help", el.getLinkText().toString());

        assertTrue(text.wikiTokens().get(1) instanceof WikiText.InternalLink);
        el = (WikiText.InternalLink) text.wikiTokens().get(1);
        assertEquals("Help", el.getFullTargetText().toString());
        assertEquals("text", el.getLinkText().toString());

        assertTrue(text.wikiTokens().get(2) instanceof WikiText.InternalLink);
        el = (WikiText.InternalLink) text.wikiTokens().get(2);
        assertEquals("Help", el.getFullTargetText().toString());
        assertEquals("Helps", el.getLinkText().toString());

        assertTrue(text.wikiTokens().get(3) instanceof WikiText.InternalLink);
        el = (WikiText.InternalLink) text.wikiTokens().get(3);
        assertEquals("Help", el.getFullTargetText().toString());
        assertEquals("texts", el.getLinkText().toString());

    }

    @Test
    public void Heading() throws Exception {
        String test = "=== Simple Heading ===\n" +
                "=== Heading level 2 ==\n" +
                "=== Heading level 3 ====\n" +
                " === not an heading ===\n" +
                "=== this one neither === \n" +
                "=== nor this one \n" +
                "= No Heading 1 =\n" +
                "===end===";
        WikiText text = new WikiText(test);

        assertNotNull(text.wikiTokens());
        assertFalse(text.wikiTokens().isEmpty());
        assertTrue(text.wikiTokens().get(0) instanceof WikiText.Heading);
        WikiText.Heading el = (WikiText.Heading) text.wikiTokens().get(0);
        assertEquals(" Simple Heading ", el.content.toString());
        assertEquals(3, el.getLevel());

        assertTrue(text.wikiTokens().get(1) instanceof WikiText.Heading);
        el = (WikiText.Heading) text.wikiTokens().get(1);
        assertEquals("= Heading level 2 ", el.content.toString());
        assertEquals(2, el.getLevel());

        assertTrue(text.wikiTokens().get(2) instanceof WikiText.Heading);
        el = (WikiText.Heading) text.wikiTokens().get(2);
        assertEquals(" Heading level 3 =", el.content.toString());
        assertEquals(3, el.getLevel());

        assertTrue(text.wikiTokens().get(3) instanceof WikiText.Heading);
        el = (WikiText.Heading) text.wikiTokens().get(3);
        assertEquals("end", el.content.toString());
        assertEquals(3, el.getLevel());

    }

/*    @Test
    public void testRegex() {
        Matcher m = Pattern.compile("(?<CH>={2,6}$)|(?<OH>^={2,6})", Pattern.MULTILINE).matcher("=== toto ===\nr === titi ===");

        assertTrue(m.find());
        assertEquals("===", m.group("OH"));
        assertTrue(m.find());
        assertEquals("===", m.group("CH"));


        assertTrue(m.find(0));
        assertEquals("===", m.group("OH"));
        System.out.println("[" + m.start() + "," + m.end() + "] " + m.group());

        assertTrue(m.find(3));
        System.out.println("[" + m.start() + "," + m.end() + "] " + m.group());
    }*/

    /*    @Test
    public void testLookup() throws Exception {
        String test = "bonjour <!-- -->";
        WikiText text = new WikiText(test);
        assertNotNull(text.peekString(0, "bon"));
        assertNotNull(text.peekString(0, "b"));
        assertNotNull(text.peekString(0, ""));
        assertNotNull(text.peekString(0, "bonjour <!--"));
        assertNotNull(text.peekString(1, "onjour <!--"));
        assertNotNull(text.peekString(8, "<!--"));
    }
*/
}
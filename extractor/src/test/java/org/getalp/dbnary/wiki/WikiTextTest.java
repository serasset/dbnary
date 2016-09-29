package org.getalp.dbnary.wiki;

import org.junit.Test;

import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.*;

public class WikiTextTest {

    @Test
    public void testParse() throws Exception {
        String test = "text {{f1|x=ccc|z={{toto}}}} text <!-- [[un lien caché]] {{ --> encore [[lien]] [[Category:English|text]] [http://kaiko.getalp.org/about-dbnary about DBnary]   [[lien|]]";
        WikiText text = new WikiText(test);

        assert !text.tokens().isEmpty();
        assertEquals(6, text.tokens().size());
        assertTrue(text.tokens().get(0) instanceof WikiText.Template);
        assertTrue(text.tokens().get(1) instanceof WikiText.HTMLComment);
        assertTrue(text.tokens().get(2) instanceof WikiText.InternalLink);
        assertTrue(text.tokens().get(3) instanceof WikiText.InternalLink);
        WikiText.InternalLink l = (WikiText.InternalLink) text.tokens().get(3);
        assertEquals("Category:English", l.target.toString());
        assertEquals("text", l.text.toString());
        assertEquals("text", l.getLinkText());
        assertEquals("Category:English", l.getTarget());

        WikiText.InternalLink l1 = (WikiText.InternalLink) text.tokens().get(2);
        assertEquals("lien", l1.getLinkText());
        assertEquals("lien", l1.getTarget());

        assertTrue(text.tokens().get(4) instanceof WikiText.ExternalLink);
        WikiText.ExternalLink l2 = (WikiText.ExternalLink) text.tokens().get(4);
        assertEquals("http://kaiko.getalp.org/about-dbnary", l2.target.toString());
        assertEquals("about DBnary", l2.text.toString());

        assertTrue(text.tokens().get(5) instanceof WikiText.InternalLink);

    }

    @Test
    public void testParseOnlyOneTemplate() throws Exception {
        String test = "{{en-noun|head=[[araneomorph]] {{vern|funnel-web spider|pedia=1}}}}";
        WikiText text = new WikiText(test);

        assertNotNull(text.tokens());
        assertFalse(text.tokens().isEmpty());
        assertTrue(text.tokens().get(0) instanceof WikiText.Template);
        assertEquals(1, text.tokens().size());

    }

    @Test
    public void testParseWithBoundaries() throws Exception {
        String test = "{{f1|x=[[ccc]]}} text <!-- [[un lien caché]] {{ --> encore [[lien]] {{f2|[[text]]|]   [[lien|}}";
        WikiText text = new WikiText(test, 17, 90);


        assert !text.tokens().isEmpty();
        // 1 HTMLComment, 1 Internal Link, 1 InternalLink (as Template should be ignored as it remains unclosed in the offset.
        assertEquals(3, text.tokens().size());
        assertTrue(text.tokens().get(0) instanceof WikiText.HTMLComment);
        assertTrue(text.tokens().get(1) instanceof WikiText.InternalLink);
        assertTrue(text.tokens().get(2) instanceof WikiText.InternalLink);
        WikiText.InternalLink l = (WikiText.InternalLink) text.tokens().get(2);
        assertEquals("text", l.target.toString());

    }

    @Test
    public void testParseWithUnclosedTemplate() throws Exception {
        String test = "{{en-noun|head=[[araneomorph]] {{vern|funnel-web spider|pedia=1}}";
        WikiText text = new WikiText(test);

        assertNotNull(text.tokens());
        assertFalse(text.tokens().isEmpty());
        // The template is not closed, hence it is considered as a text and the first valid token in a link.
        assertTrue(text.tokens().get(0) instanceof WikiText.InternalLink);
        assertEquals(2, text.tokens().size());

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
                "* {{l|en|hypothalamus}}\n" +
                "* {{l|en|prethalamus}}";
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
        assertFalse(it.hasNext());


    }

    @Test
    public void testParseTemplateArgs() throws Exception {
        String test = "{{en-noun|head=[[araneomorph]] {{vern|funnel-web spider|pedia=1}}|v1|xx=vxx|v2}}";
        WikiText text = new WikiText(test);

        assertNotNull(text.tokens());
        assertFalse(text.tokens().isEmpty());
        WikiText.Template tmpl = (WikiText.Template) text.tokens().get(0);

        assertEquals("en-noun", tmpl.getName());
        Map<String, String> args = tmpl.getParsedArgs();
        assertEquals(4, args.size());
        assertEquals("v1", args.get("1"));
        assertEquals("v2", args.get("2"));
        assertEquals("vxx", args.get("xx"));
        assertEquals("[[araneomorph]] {{vern|funnel-web spider|pedia=1}}", args.get("head"));


    }

    @Test
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
}
package org.getalp.dbnary.wiki;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;
import org.getalp.dbnary.wiki.WikiText.NumberedListItem;
import org.getalp.dbnary.wiki.WikiText.Template;
import org.getalp.dbnary.wiki.WikiText.WikiContent;
import org.junit.Test;

public class WikiTextTest {

  @Test
  public void testParse() throws Exception {
    String test =
        "text {{f1|x=ccc|z={{toto}}}} text <!-- [[un lien caché]] {{ --> encore [[lien#bidule]] [[Category:English|text]] [http://kaiko.getalp.org/about-dbnary about DBnary]   [[lien|]]";
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
    String test =
        "text {{f1|x=ccc|z={{toto}}}} text <!-- [[un lien caché]] {{ --> encore [[lien]] [[Category:English|text]] [http://kaiko.getalp.org/about-dbnary about DBnary]   [[lien|]]";
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
    String test =
        "{{f1|x=[[ccc]]}} text <!-- [[un lien caché]] {{ --> encore [[lien]] {{f2|[[text]]|]   [[lien|}}";
    WikiText text = new WikiText(null, test, 17, 90);

    assert !text.wikiTokens().isEmpty();
    // 1 HTMLComment, 1 Internal Link, 1 InternalLink (as Template should be ignored as it remains
    // unclosed in the offset.
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
    // The template is not closed, hence it is considered as a text and the first valid token in a
    // link.
    assertTrue(text.wikiTokens().get(0) instanceof WikiText.InternalLink);
    assertEquals(2, text.wikiTokens().size());

  }

  @Test
  public void testWikiTextIterator() throws Exception {
    String test = "{{en-noun}} text [[link]] text {{template}} text ";
    WikiText text = new WikiText(test);

    WikiEventsSequence s = text.templatesOnUpperLevel();
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
    String test = "* {{l|en|thalamus}}\n" + "* {{l|en|hypothalamus}} * toto\n"
        + "* {{l|en|prethalamus}}\n" + ": toto\n" + ": titi";
    WikiText text = new WikiText(test);

    ClassBasedFilter filter = new ClassBasedFilter();
    filter.allowIndentedItem();
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
    String test =
        "{{en-noun|head=[[araneomorph]] {{vern|funnel-web spider|pedia=1}}|v1|xx=vxx|v2}}";
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
  public void testHeading() throws Exception {
    String test =
        "=== Simple Heading ===\n" + "=== Heading level 2 ==\n" + "=== Heading level 3 ====\n"
            + " === not an heading ===\n" + "=== this one neither === \n" + "=== nor this one \n"
            + "= No Heading 1 =\n" + "===end===\n" + "==Heading 2==\n" + "===Heading 2.1===";
    WikiText text = new WikiText(test);

    assertNotNull(text.wikiTokens());
    assertFalse(text.wikiTokens().isEmpty());
    assertTrue(text.wikiTokens().get(0) instanceof WikiText.Heading);
    WikiText.Heading el = (WikiText.Heading) text.wikiTokens().get(0);
    assertEquals(" Simple Heading ", el.getContent().toString());
    assertEquals(3, el.getLevel());

    assertTrue(text.wikiTokens().get(1) instanceof WikiText.Heading);
    el = (WikiText.Heading) text.wikiTokens().get(1);
    assertEquals("= Heading level 2 ", el.getContent().toString());
    assertEquals(2, el.getLevel());

    assertTrue(text.wikiTokens().get(2) instanceof WikiText.Heading);
    el = (WikiText.Heading) text.wikiTokens().get(2);
    assertEquals(" Heading level 3 =", el.getContent().toString());
    assertEquals(3, el.getLevel());

    assertTrue(text.wikiTokens().get(3) instanceof WikiText.Heading);
    el = (WikiText.Heading) text.wikiTokens().get(3);
    assertEquals("end", el.getContent().toString());
    assertEquals(3, el.getLevel());

    int nbH = 0;
    for (WikiText.Token tok : text.headers()) {
      assertTrue(tok instanceof WikiText.Heading);
      nbH++;
    }
    assertEquals(6, nbH);

    nbH = 0;
    for (WikiText.Token tok : text.headers(2)) {
      assertTrue(tok instanceof WikiText.Heading);
      nbH++;
    }
    assertEquals(2, nbH);

    nbH = 0;
    for (WikiText.Token tok : text.headers(3)) {
      assertTrue(tok instanceof WikiText.Heading);
      nbH++;
    }
    assertEquals(4, nbH);

    nbH = 0;
    for (WikiText.Token tok : text.headersMatching(Pattern.compile("\\s*Heading.*"))) {
      assertTrue(tok instanceof WikiText.Heading);
      nbH++;
    }
    assertEquals(3, nbH); // 3 Headers with title beginning by "Heading", === Heading level 3 ====,
    // ==Heading 2==, ===Heading 2.1===
    // === Heading level 2 == is a level 2 heading starting with = (hence it does not match...)

    nbH = 0;
    for (WikiText.Token tok : text.headersMatching(3, Pattern.compile("\\s*Heading.*"))) {
      assertTrue(tok instanceof WikiText.Heading);
      nbH++;
    }
    assertEquals(2, nbH); // 3 Headers with title beginning by "Heading", === Heading level 3 ====,
    // ==Heading 2==, ===Heading 2.1===
    // === Heading level 2 == is a level 2 heading starting with = (hence it does not match...)

  }

  @Test
  public void testSections() {
    String test = "==H2==\n" + "===H3.1===\n" + "{{test}}\n" + "text\n" + "===H3.2===\n"
        + "[[link]]\n" + "==H2.2==\n" + "{{test}}\n" + "===H3.3===\n" + "{{test}}\n" + "[[link]]";

    WikiText t = new WikiText(test);

    for (WikiText.WikiSection s : t.sections(2)) {
      assertTrue(s.getHeading().getContent().toString().startsWith("H2"));
      for (WikiText.Token tok : s.getContent().tokens()) {
        System.out.println("tok=" + tok.toString());
      }
      System.out.println("---------");
    }
    System.out.println("+++++++++");

    for (WikiText.WikiSection s : t.sections(3)) {
      System.out.println(s.getContent());
      System.out.println("---------");
    }

  }

  @Test
  public void testWikiTextEndOfFileHandling() throws Exception {
    WikiText text = new WikiText("{{en-noun}}");
    assertEquals("Bad handling of end of file for correct template", 1, text.wikiTokens().size());
    assertTrue("Bad handling of end of file for correct template",
        text.wikiTokens().get(0) instanceof Template);

    text = new WikiText("{{en-noun}");
    assertEquals("Bad handling of end of file for correct template", 0, text.wikiTokens().size());

    text = new WikiText("{{en-noun");
    assertEquals("Bad handling of end of file for correct template", 0, text.wikiTokens().size());

    text = new WikiText("* {{en-noun}}");
    assertEquals("Bad handling of end of file for correct list item with correct template", 1,
        text.wikiTokens().size());

    text = new WikiText("# {{en-noun}}");
    assertEquals("Bad handling of end of file for correct numbered list item with correct template",
        1, text.wikiTokens().size());
    assertTrue("Bad handling of end of file for correct numbered list item with correct template",
        text.wikiTokens().get(0) instanceof NumberedListItem);
    WikiContent content = text.wikiTokens().get(0).asNumberedListItem().getContent();
    assertEquals("Bad handling of end of file for correct numbered list item with correct template",
        1, content.wikiTokens().size());
    assertTrue("Bad handling of end of file for correct numbered list item with correct template",
        content.wikiTokens().get(0) instanceof Template);

    text = new WikiText("# {{en-noun}");
    assertEquals("Bad handling of end of file for correct numbered list item with bad template", 1,
        text.wikiTokens().size());
    assertTrue("Bad handling of end of file for correct numbered list item with correct template",
        text.wikiTokens().get(0) instanceof NumberedListItem);
    content = text.wikiTokens().get(0).asNumberedListItem().getContent();
    assertEquals("Bad handling of end of file for correct numbered list item with correct template",
        0, content.wikiTokens().size());

  }

  @Test
  public void testWikiTextTemplatesWithNewLines() throws Exception {
    WikiText text = new WikiText("{{en-noun\n}}");
    assertEquals("Bad handling of end of file for correct template", 1, text.wikiTokens().size());
    assertTrue("Bad handling of end of file for correct template",
        text.wikiTokens().get(0) instanceof Template);

    text = new WikiText("{{en-noun\n}");
    assertEquals("Bad handling of end of file for correct template", 0, text.wikiTokens().size());

    text = new WikiText("{{en-noun\n");
    assertEquals("Bad handling of end of file for correct template", 0, text.wikiTokens().size());

    text = new WikiText("* {{en-noun\n}}");
    assertEquals("Bad handling of end of file for correct list item with correct template", 1,
        text.wikiTokens().size());

    text = new WikiText("# {{en-noun\n}}");
    assertEquals("Bad handling of end of file for correct numbered list item with correct template",
        1, text.wikiTokens().size());
    assertTrue("Bad handling of end of file for correct numbered list item with correct template",
        text.wikiTokens().get(0) instanceof NumberedListItem);
    WikiContent content = text.wikiTokens().get(0).asNumberedListItem().getContent();
    assertEquals("Bad handling of end of file for correct numbered list item with correct template",
        1, content.wikiTokens().size());
    assertTrue("Bad handling of end of file for correct numbered list item with correct template",
        content.wikiTokens().get(0) instanceof Template);

    text = new WikiText("# {{en-noun\n}");
    assertEquals("Bad handling of end of file for correct numbered list item with bad template", 1,
        text.wikiTokens().size());
    assertTrue("Bad handling of end of file for correct numbered list item with correct template",
        text.wikiTokens().get(0) instanceof NumberedListItem);
    content = text.wikiTokens().get(0).asNumberedListItem().getContent();
    assertEquals("Bad handling of end of file for correct numbered list item with correct template",
        0, content.wikiTokens().size());

  }
  /*
   * @Test public void testRegex() { Matcher m = Pattern.compile("(?<CH>={2,6}$)|(?<OH>^={2,6})",
   * Pattern.MULTILINE).matcher("=== toto ===\nr === titi ===");
   * 
   * assertTrue(m.find()); assertEquals("===", m.group("OH")); assertTrue(m.find());
   * assertEquals("===", m.group("CH"));
   * 
   * 
   * assertTrue(m.find(0)); assertEquals("===", m.group("OH")); System.out.println("[" + m.start() +
   * "," + m.end() + "] " + m.group());
   * 
   * assertTrue(m.find(3)); System.out.println("[" + m.start() + "," + m.end() + "] " + m.group());
   * }
   */

  /*
   * @Test public void testLookup() throws Exception { String test = "bonjour <!-- -->"; WikiText
   * text = new WikiText(test); assertNotNull(text.peekString(0, "bon"));
   * assertNotNull(text.peekString(0, "b")); assertNotNull(text.peekString(0, ""));
   * assertNotNull(text.peekString(0, "bonjour <!--")); assertNotNull(text.peekString(1,
   * "onjour <!--")); assertNotNull(text.peekString(8, "<!--")); }
   */
}

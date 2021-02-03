package org.getalp.dbnary.wiki;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.getalp.dbnary.wiki.WikiText.ExternalLink;
import org.getalp.dbnary.wiki.WikiText.Heading;
import org.getalp.dbnary.wiki.WikiText.IndentedItem;
import org.getalp.dbnary.wiki.WikiText.InternalLink;
import org.getalp.dbnary.wiki.WikiText.NumberedListItem;
import org.getalp.dbnary.wiki.WikiText.Template;
import org.getalp.dbnary.wiki.WikiText.Text;
import org.getalp.dbnary.wiki.WikiText.Token;
import org.getalp.dbnary.wiki.WikiText.WikiContent;
import org.junit.Test;

public class WikiTextTest {

  @Test
  public void testParse() throws Exception {
    String test =
        "text {{f1|x=ccc|z={{toto}}}} text <!-- [[un lien caché]] {{ --> encore [[lien#bidule]] [[Category:English|text]] [http://kaiko.getalp.org/about-dbnary about DBnary]   [[lien|]]";
    WikiText text = new WikiText(test);

    assert !text.wikiTokens().isEmpty();
    assertEquals(6, text.wikiTokensWithHtmlComments().size());
    assertEquals(5, text.wikiTokens().size());
    assertTrue(text.wikiTokens().get(0) instanceof WikiText.Template);
    assertTrue(text.wikiTokens().get(1) instanceof WikiText.InternalLink);
    assertTrue(text.wikiTokens().get(2) instanceof WikiText.InternalLink);
    assertTrue(text.wikiTokensWithHtmlComments().get(1) instanceof WikiText.HTMLComment);


    WikiText.InternalLink l = (WikiText.InternalLink) text.wikiTokens().get(2);
    assertEquals("Category:English", l.target.toString());
    assertEquals("text", l.text.toString());
    assertEquals("text", l.getLinkText());
    assertEquals("Category:English", l.getFullTargetText());

    WikiText.InternalLink l1 = (WikiText.InternalLink) text.wikiTokens().get(1);
    assertEquals("lien#bidule", l1.getLinkText());
    assertEquals("lien#bidule", l1.getFullTargetText());
    assertEquals("lien", l1.getTargetText());
    assertTrue(l1.hasAnchor());
    assertEquals("bidule", l1.getAnchorText());

    assertTrue(text.wikiTokens().get(3) instanceof WikiText.ExternalLink);
    WikiText.ExternalLink l2 = (WikiText.ExternalLink) text.wikiTokens().get(3);
    assertEquals("http://kaiko.getalp.org/about-dbnary", l2.target.toString());
    assertEquals("about DBnary", l2.text.toString());

    assertTrue(text.wikiTokens().get(4) instanceof WikiText.InternalLink);

  }

  @Test
  public void testParseWithTextTokens() throws Exception {
    String test =
        "text {{f1|x=ccc|z={{toto}}}} text <!-- [[un lien caché]] {{ --> encore [[lien]] [[Category:English|text]] [http://kaiko.getalp.org/about-dbnary about DBnary]   [[lien|]]";
    WikiText text = new WikiText(test);

    List<? extends WikiText.Token> toks = text.tokens();
    assert !toks.isEmpty();
    assertEquals(11, toks.size()); // htmlComments is not given
    assertTrue(toks.get(0) instanceof WikiText.Text);
    assertTrue(toks.get(1) instanceof WikiText.Template);
    assertTrue(toks.get(2) instanceof WikiText.Text);
    // assertTrue(toks.get(3) instanceof WikiText.HTMLComment);
    assertTrue(toks.get(3) instanceof WikiText.Text);
    assertTrue(toks.get(4) instanceof WikiText.InternalLink);

    WikiText.Text t = (WikiText.Text) toks.get(0);
    assertEquals("text ", t.getText());

    WikiText.InternalLink l = (WikiText.InternalLink) toks.get(6);
    assertEquals("Category:English", l.target.toString());
    assertEquals("text", l.text.toString());
    assertEquals("text", l.getLinkText());
    assertEquals("Category:English", l.getFullTargetText());

    WikiText.InternalLink l1 = (WikiText.InternalLink) toks.get(4);
    assertEquals("lien", l1.getLinkText());
    assertEquals("lien", l1.getFullTargetText());

    assertTrue(toks.get(8) instanceof WikiText.ExternalLink);
    WikiText.ExternalLink l2 = (WikiText.ExternalLink) toks.get(8);
    assertEquals("http://kaiko.getalp.org/about-dbnary", l2.target.toString());
    assertEquals("about DBnary", l2.text.toString());

    assertTrue(toks.get(10) instanceof WikiText.InternalLink);

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
    assertEquals(3, text.wikiTokensWithHtmlComments().size());
    assertTrue(text.wikiTokensWithHtmlComments().get(0) instanceof WikiText.HTMLComment);
    assertTrue(text.wikiTokensWithHtmlComments().get(1) instanceof WikiText.InternalLink);
    assertTrue(text.wikiTokensWithHtmlComments().get(2) instanceof WikiText.InternalLink);
    WikiText.InternalLink l = (WikiText.InternalLink) text.wikiTokensWithHtmlComments().get(2);
    assertEquals("text", l.target.toString());

    assertEquals(2, text.wikiTokens().size());
    assertTrue(text.wikiTokens().get(0) instanceof WikiText.InternalLink);
    assertTrue(text.wikiTokens().get(1) instanceof WikiText.InternalLink);

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

    List<Token> wt = text.wikiTokens();
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
    assertEquals("[[Help]]", el.getText());

    assertTrue(text.wikiTokens().get(1) instanceof WikiText.InternalLink);
    el = (WikiText.InternalLink) text.wikiTokens().get(1);
    assertEquals("Help", el.getFullTargetText().toString());
    assertEquals("text", el.getLinkText().toString());
    assertEquals("[[Help|text]]", el.getText());

    assertTrue(text.wikiTokens().get(2) instanceof WikiText.InternalLink);
    el = (WikiText.InternalLink) text.wikiTokens().get(2);
    assertEquals("Help", el.getFullTargetText().toString());
    assertEquals("Helps", el.getLinkText().toString());
    assertEquals("[[Help]]s", el.getText());

    assertTrue(text.wikiTokens().get(3) instanceof WikiText.InternalLink);
    el = (WikiText.InternalLink) text.wikiTokens().get(3);
    assertEquals("Help", el.getFullTargetText().toString());
    assertEquals("texts", el.getLinkText().toString());
    assertEquals("[[Help|text]]s", el.getText());

  }

  @Test
  public void testHeading() throws Exception {
    String test = "=== Simple Heading ===\n" + "=== Heading level 2 ==\n"
        + "=== Heading level 3 ====\n" + " === not an heading ===\n"
        + "=== spaces are accepted after equals === \n" + "=== nor this one \n"
        + "= No Heading 1 =\n" + "===end===\n" + "==Heading 2==\n" + "===Heading 2.1===";
    WikiText text = new WikiText(test);

    assertNotNull(text.wikiTokens());
    assertFalse(text.wikiTokens().isEmpty());
    assertEquals("Should have parsed 7 headings", 7, text.wikiTokens().size());

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
    assertEquals(" spaces are accepted after equals ", el.getContent().toString());
    assertEquals(3, el.getLevel());

    assertTrue(text.wikiTokens().get(4) instanceof WikiText.Heading);
    el = (WikiText.Heading) text.wikiTokens().get(4);
    assertEquals("end", el.getContent().toString());
    assertEquals(3, el.getLevel());

    int nbH = 0;
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
    assertEquals(5, nbH);

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

  @Test
  public void testInternalLinkWithBracket() throws Exception {
    WikiText text =
        new WikiText("utiliser [[Titres non pris en charge/Crochet gauche|[]]. Unicode : U+005B. ");
    assertEquals("Internal link with bracket should be parsed entirely", 1,
        text.wikiTokens().size());
    assertTrue("The parsed item should be an internal link",
        text.wikiTokens().get(0) instanceof InternalLink);
    assertEquals("The parsed link title should be a square bracket", "[",
        text.wikiTokens().get(0).asInternalLink().getLinkText());

  }

  @Test
  public void testInternalLinkWithValidExternalLink() throws Exception {
    // Valid internal link with valid external link inside : (external link should not be
    // interpreted)
    WikiText text = new WikiText("[[Internal Link|[http://mediawiki.org/|dbnary]]]");
    assertEquals("Internal link with bracket should be parsed entirely", 1,
        text.wikiTokens().size());
    assertTrue("The parsed item should be an internal link",
        text.wikiTokens().get(0) instanceof InternalLink);
    assertEquals("The parsed link title should be a square bracket",
        "[http://mediawiki.org/|dbnary]", text.wikiTokens().get(0).asInternalLink().getLinkText());

  }

  @Test
  public void testInternalLinkWithInvalidExternalLink() throws Exception {
    // Valid internal link with invalid external link inside :
    WikiText text = new WikiText("[[Internal Link|[toto]]]");
    assertEquals("Internal link with bracket should be parsed entirely", 1,
        text.wikiTokens().size());
    assertTrue("The parsed item should be an internal link",
        text.wikiTokens().get(0) instanceof InternalLink);
    assertEquals("The parsed link title should be a square bracket", "[toto",
        text.wikiTokens().get(0).asInternalLink().getLinkText());

  }

  @Test
  public void testInternalLinkWithOpeningBracket() throws Exception {
    // Valid internal link with opening square bracket inside :
    WikiText text = new WikiText("[[Internal Link|[]]");
    assertEquals("Internal link with bracket should be parsed entirely", 1,
        text.wikiTokens().size());
    assertTrue("The parsed item should be an internal link",
        text.wikiTokens().get(0) instanceof InternalLink);
    assertEquals("The parsed link title should be a square bracket", "[",
        text.wikiTokens().get(0).asInternalLink().getLinkText());

  }

  @Test
  public void testValideExternalLinkClosedByDoubleBrackets() throws Exception {
    // Invalid internal link with valid external link inside :
    WikiText text = new WikiText("[http://kaiko.getalp.org/ dbnary]]");
    assertEquals("External link with extra bracket should be parsed entirely", 1,
        text.wikiTokens().size());
    assertTrue("The parsed item should be an internal link",
        text.wikiTokens().get(0) instanceof ExternalLink);
    assertEquals("The parsed link title should be a square bracket", "dbnary",
        text.wikiTokens().get(0).asExternalLink().getLinkText());
  }

  @Test
  public void testValidExternalLinkOpenedByDoubleBrackets() throws Exception {
    // Invalid internal link with valid external link inside :
    WikiText text = new WikiText("[[http://kaiko.getalp.org/ dbnary]");
    assertEquals("External link with extra bracket should be parsed entirely", 1,
        text.wikiTokens().size());
    assertTrue("The parsed item should be an internal link",
        text.wikiTokens().get(0) instanceof ExternalLink);
    assertEquals("The parsed link title should be a square bracket", "dbnary",
        text.wikiTokens().get(0).asExternalLink().getLinkText());
  }

  @Test
  public void testInternalLinkWithValidUnclosedExternalLink() throws Exception {
    // Invalid internal link with valid external link inside :
    WikiText text = new WikiText("[[Internal Link|[http://kaiko.getalp.org/ dbnary]]");
    assertEquals("Internal link with bracket should be parsed entirely", 1,
        text.wikiTokens().size());
    assertTrue("The parsed item should be an internal link",
        text.wikiTokens().get(0) instanceof InternalLink);
    assertEquals("The parsed link title should be a square bracket",
        "[http://kaiko.getalp.org/ dbnary",
        text.wikiTokens().get(0).asInternalLink().getLinkText());

    text = new WikiText("[[Internal Link|before]after]]\n");
    assertEquals("Internal link with closing bracket should be parsed entirely", 1,
        text.wikiTokens().size());
    assertTrue("The parsed item should be an internal link",
        text.wikiTokens().get(0) instanceof InternalLink);
    assertEquals("The parsed link title should be a square bracket", "before]after",
        text.wikiTokens().get(0).asInternalLink().getLinkText());


    // Invalid internal link with valid external link inside :
    // [[Internal Link|[http://kaiko.getalp.org/|dbnary] [[Internal Link|[1]
    //
    // Invalid internal link with invalid external link inside :
    // [[Internal Link|[toto]] [toto
    //
    // Invalid internal link with invalid external link inside :
    // [[Internal Link|[toto] [[Internal Link|[toto]
    //
    // Invalid internal link with opening square bracket inside :
    // [[Internal Link|[] [[Internal Link|[]
    //
    // Testing internal links priorities :
    // [[First Link|[[Second Link]]]] [[First Link|Second Link]] (The deepest link is interpreted
    // the other is text)
    //
    // Testing internal links priorities :
    // [[First Link|[[Second Link]]] [[First Link|Second Link] (Same here)
    //
    // Testing internal links priorities :
    // [[First Link|[[Second Link]] [[First Link|Second Link (Deepest link is closed the rest is
    // text)
  }

  @Test
  public void testWikiTextIncoherentLinkInTemplate() throws Exception {
    WikiText text = new WikiText("{{template|var=[[toto] \n" + "titi}}");
    assertEquals("Incoherent template should be parsed", 1, text.wikiTokens().size());
    assertTrue("The first Template has not been correctly parsed",
        text.wikiTokens().get(0) instanceof Template);
  }

  @Test
  public void headersMayHaveCommentsBetweenClosingEqualsAndNewLine() {
    WikiText text = new WikiText("==== {{Übersetzungen}} ==== <!--\n"
        + "Hinter der jeweiligen Übersetzung diese bitte auch mit dem \"Heimatwiktionary\" (der Muttersprache des Wortes) verlinken. Hinter der jeweiligen Übersetzung bitte auf in eckigen Klammern auf die zugehörige Bedeutung verweisen.\n"
        + "Überzählige Sprachen am besten mit den Pfeilen auskommentieren oder einfach löschen. -->\n");
    assertEquals("Header should be correctly parsed.", 1, text.wikiTokens().size());
    assertTrue("The element should be a Heading", text.wikiTokens().get(0) instanceof Heading);

    // Same without final newline
    text = new WikiText("==== {{Übersetzungen}} ==== <!--\n"
        + "Hinter der jeweiligen Übersetzung diese bitte auch mit dem \"Heimatwiktionary\" (der Muttersprache des Wortes) verlinken. Hinter der jeweiligen Übersetzung bitte auf in eckigen Klammern auf die zugehörige Bedeutung verweisen.\n"
        + "Überzählige Sprachen am besten mit den Pfeilen auskommentieren oder einfach löschen. -->");
    assertEquals("Header should be correctly parsed.", 1, text.wikiTokens().size());
    assertTrue("The element should be a Heading", text.wikiTokens().get(0) instanceof Heading);

    // more than one xml comment should be correctly handled
    text = new WikiText("==== {{Übersetzungen}} ==== <!-- test1 -->  <!--comment2-->\n");
    assertEquals("Header should be correctly parsed.", 1, text.wikiTokens().size());
    assertTrue("The element should be a Heading", text.wikiTokens().get(0) instanceof Heading);

    // The parser should not go past the correct closing of comments
    text = new WikiText(
        "==== Title1 ==== <!-- test1 -->\n" + "\n" + "==== Title2 ==== <!-- Comment 2 -->\n");
    assertEquals("Headers should be correctly parsed.", 2, text.wikiTokens().size());
    assertTrue("The first element should be a Heading",
        text.wikiTokens().get(0) instanceof Heading);
    assertEquals("Header Text should be correct.", " Title1 ",
        text.wikiTokens().get(0).asHeading().getContent().toString());
    assertTrue("The second element should be a Heading",
        text.wikiTokens().get(1) instanceof Heading);
    assertEquals("Header Text should be correct.", " Title2 ",
        text.wikiTokens().get(1).asHeading().getContent().toString());

  }

  @Test
  public void templateHasPriorityOverLink() {
    WikiText text = new WikiText(
        "[https://dl.ndl.go.jp/info:ndljp/pid/969161 {{lj|[言](げん)[泉](せん) : [日](にっ)[本](ぽん)[大](だい)[辞](じ)[典](てん). [第](だい)[3](さん)[巻](かん)}}]\n");
    assertEquals("Incoherent Internal Link should not be detected", 1, text.wikiTokens().size());
    assertTrue("The element should be an External Link",
        text.wikiTokens().get(0) instanceof ExternalLink);
    ExternalLink externalLink = text.wikiTokens().get(0).asExternalLink();
    assertEquals("Internal Link should contain a template", 1,
        externalLink.getLink().wikiTokens().size());
    assertTrue("Internal Link should contain a template",
        externalLink.getLink().wikiTokens().get(0) instanceof Template);
  }

  @Test
  public void noBracketOrTokenInsideInternalLinks() {
    WikiText text = new WikiText("[[Test]]\n");
    assertEquals("Correct internal link should be detected", 1, text.wikiTokens().size());
    assertTrue("The element should be an Internal Link",
        text.wikiTokens().get(0) instanceof InternalLink);
    assertEquals("The element target text should be \"Test\"", "Test",
        text.wikiTokens().get(0).asInternalLink().getTargetText());

    String[] testCases = {"[[Test}]]\n", "[[{Test]]\n", "[[{{Test]]\n"};
    for (String testCase : testCases) {
      text = new WikiText(testCase);
      assertEquals("Incorrect internal link should not be detected", 0, text.wikiTokens().size());
    }

    // In this case, MediaWiki does not create a link, but if template is {{SITENAME}}, it does...
    // So, here, we chose to accept templates inside internal links
    text = new WikiText("[[{{qualifier|definite}}]]");
    assertEquals("There is a Internal link in this sample.", 1, text.wikiTokens().size());
    assertTrue("The element should be an Internal link",
        text.wikiTokens().get(0) instanceof InternalLink);
    InternalLink link = text.wikiTokens().get(0).asInternalLink();
    assertEquals("There is a Internal link in this sample.", 1, link.getLink().wikiTokens().size());
    assertTrue("The contained element should be a Template",
        link.getLink().wikiTokens().get(0) instanceof Template);
    assertEquals("The template name should be \"qualifier\"", "qualifier",
        link.getLink().wikiTokens().get(0).asTemplate().getName());
  }

  @Test
  public void incorrectTokensShouldNotChangeTreeGeometry() {
    WikiText text = new WikiText(" Text is not a token\n" + "# A male [[sheep]], a [[ram]].\n"
        + "# The head of a [[hammer]], and particularly of a steam-driven hammer.\n"
        + "#* Note that this external link is incorrect {{FakeTemplate|toto|arg=  \n"
        + "#* This Indentation should be a child of root\n" + "#*: This one also.\n");
    assertEquals("All Indentations should be attach to the root.", 5, text.wikiTokens().size());
    assertTrue(text.wikiTokens().stream().allMatch(t -> (t instanceof IndentedItem)));

    // Same with two nested templates
    text = new WikiText(" Text is not a token\n" + "# A male [[sheep]], a [[ram]].\n"
        + "# The head of a [[hammer]], and particularly of a steam-driven hammer.\n"
        + "#* Note that this external link is incorrect {{FakeTemplate|toto|arg=  \n"
        + "#* This Indentation also contains an {{incorrect template]] and should be a child of root\n"
        + "#*: This one also.\n");
    assertEquals("All Indentations should be attach to the root.", 5, text.wikiTokens().size());
    assertTrue(text.wikiTokens().stream().allMatch(t -> (t instanceof IndentedItem)));

    text = new WikiText("=== Text With incorrect {{Template|   ===\n" + "# A male [[sheep]], a [[ram]].\n"
        + "# The head of a [[hammer]], and particularly of a steam-driven hammer.\n"
        + "#* Note that this external link is incorrect {{FakeTemplate|toto|arg=  \n"
        + "#* This Indentation should be a child of root\n" + "#*: This one also.\n");
    assertEquals("The Heading should be correctly closed.", 6, text.wikiTokens().size());
    assertTrue(text.wikiTokens().get(0) instanceof Heading);
    assertTrue(text.wikiTokens().get(1) instanceof IndentedItem);
    assertTrue(text.wikiTokens().get(2) instanceof IndentedItem);
    assertTrue(text.wikiTokens().get(3) instanceof IndentedItem);
    assertTrue(text.wikiTokens().get(4) instanceof IndentedItem);

  }

  @Test
  public void externalLinksAreNotNested() {
    WikiText text =
        new WikiText("[http://kaiko.getalp.org/ dbnary [http://kaiko.getalp.org/ dbnary2] text]\n");
    assertEquals("External Links cannot be nested.", 2, text.tokens().size());
    assertTrue("The first link gets precedence.", text.tokens().get(0) instanceof ExternalLink);
    assertEquals("The first link ends after second link start.",
        "dbnary [http://kaiko.getalp.org/ dbnary2",
        text.tokens().get(0).asExternalLink().getLinkText());
    assertTrue("The rest is text", text.tokens().get(1) instanceof Text);
    assertEquals("The remaining is incorrect", " text]\n", text.tokens().get(1).asText().getText());
  }

  @Test
  public void htmlCommentsShouldNotAppearInTokenValues() {
    WikiText text = new WikiText(
        "<!-- Just an html comment-->\n" + "=== A <!--  and another in title --> Title ===\n"
            + "{{NAME|<!-- And another one -->arg1}}");
    assertEquals("Heading and Template should be correctly parsed.", 2, text.wikiTokens().size());
    assertTrue("The first element is a Heading.", text.wikiTokens().get(0) instanceof Heading);
    WikiContent titleContent = text.wikiTokens().get(0).asHeading().getContent();
    assertEquals("The heading content begins with \" A \"", " A ",
        titleContent.tokens().get(0).asText().getText());
    assertEquals("The heading content ends with \" Title \"", " Title ",
        titleContent.tokens().get(1).asText().getText());
    assertTrue("The second element is a Template", text.wikiTokens().get(1) instanceof Template);
    Template tmpl = text.wikiTokens().get(1).asTemplate();
    assertEquals("The template name is incorrect", "NAME", tmpl.getName());
    assertEquals("The template first argument is incorrect", "arg1", tmpl.getParsedArgs().get("1"));

    System.out.println(text.content().getText());

  }
}

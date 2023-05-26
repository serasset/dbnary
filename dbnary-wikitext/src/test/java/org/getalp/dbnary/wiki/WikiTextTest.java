package org.getalp.dbnary.wiki;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
  public void testParse() {
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
    assertEquals("text", l.linkTextContent.toString());
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
    assertEquals("about DBnary", l2.linkTextContent.toString());

    assertTrue(text.wikiTokens().get(4) instanceof WikiText.InternalLink);

  }

  @Test
  public void testParseWithTextTokens() {
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
    assertEquals("text", l.linkTextContent.toString());
    assertEquals("text", l.getLinkText());
    assertEquals("Category:English", l.getFullTargetText());

    WikiText.InternalLink l1 = (WikiText.InternalLink) toks.get(4);
    assertEquals("lien", l1.getLinkText());
    assertEquals("lien", l1.getFullTargetText());

    assertTrue(toks.get(8) instanceof WikiText.ExternalLink);
    WikiText.ExternalLink l2 = (WikiText.ExternalLink) toks.get(8);
    assertEquals("http://kaiko.getalp.org/about-dbnary", l2.target.toString());
    assertEquals("about DBnary", l2.linkTextContent.toString());

    assertTrue(toks.get(10) instanceof WikiText.InternalLink);

    text = new WikiText(null, test, 5, 35);
    toks = text.tokens();
    assert !toks.isEmpty();
    assertTrue(toks.get(0) instanceof WikiText.Template);
    assertTrue(toks.get(1) instanceof WikiText.Text);

  }

  @Test
  public void testParseOnlyOneTemplate() {
    String test = "{{en-noun|head=[[araneomorph]] {{vern|funnel-web spider|pedia=1}}}}";
    WikiText text = new WikiText(test);

    assertNotNull(text.wikiTokens());
    assertFalse(text.wikiTokens().isEmpty());
    assertTrue(text.wikiTokens().get(0) instanceof WikiText.Template);
    assertEquals(1, text.wikiTokens().size());

  }

  @Test
  public void testParseWithBoundaries() {
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
  public void testParseWithUnclosedTemplate() {
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
  public void testWikiTextIterator() {
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
  public void testWikiTextIterator2() {
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
  public void testWikiTextIteratorWithEmbedding() {
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
  public void testList() {
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
  public void testParseTemplateArgs() {
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
  public void testExternalLinks() {
    String test = "[http://this.is.a link] [1,2] [sms:thisonealso] [http://this.one.also]]";
    WikiText text = new WikiText(test);

    assertNotNull(text.wikiTokens());
    assertFalse(text.wikiTokens().isEmpty());
    assertTrue(text.wikiTokens().get(0) instanceof WikiText.ExternalLink);
    WikiText.ExternalLink el = (WikiText.ExternalLink) text.wikiTokens().get(0);
    assertEquals("http://this.is.a", el.target.toString());
    assertEquals("link", el.linkTextContent.toString());

    // [1,2] is not a link
    assertTrue(text.wikiTokens().get(1) instanceof WikiText.ExternalLink);
    el = (WikiText.ExternalLink) text.wikiTokens().get(1);
    assertEquals("sms:thisonealso", el.target.toString());
    assertNull(el.linkTextContent);

    assertTrue(text.wikiTokens().get(2) instanceof WikiText.ExternalLink);
    el = (WikiText.ExternalLink) text.wikiTokens().get(2);
    assertEquals("http://this.one.also", el.target.toString());
    assertNull(el.linkTextContent);

  }

  @Test
  public void testInternalLinks() {
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
  public void testHeading() {
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
  public void testWikiTextEndOfFileHandling() {
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
  public void testWikiTextTemplatesWithNewLines() {
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
  public void testInternalLinkWithBracket() {
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
  public void testInternalLinkWithValidExternalLink() {
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
  public void testInternalLinkWithInvalidExternalLink() {
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
  public void testInternalLinkWithOpeningBracket() {
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
  public void testInternalLinkPriority() {
    WikiText text = new WikiText("[[principle]], [[cause], [[origin]]");
    assertEquals("There should be 2 recognized internal links", 2, text.wikiTokens().size());
    assertTrue("The first item should be an internal link",
        text.wikiTokens().get(0) instanceof InternalLink);
    assertEquals("The parsed link title should be a square bracket", "principle",
        text.wikiTokens().get(0).asInternalLink().getLinkText());
    assertTrue("The second item should be an internal link",
        text.wikiTokens().get(1) instanceof InternalLink);
    assertEquals("The parsed link title should be a square bracket", "origin",
        text.wikiTokens().get(1).asInternalLink().getLinkText());

    text = new WikiText(":* {{en}}: [[principle]], [[cause], [[origin]]\n");
    assertEquals("There should be 1 IndentedItem", 1, text.wikiTokens().size());
    assertTrue("The first item should be an IndentedItem",
        text.wikiTokens().get(0) instanceof IndentedItem);
    WikiContent content = text.wikiTokens().get(0).asIndentedItem().getContent();
    assertEquals("There should be 2 recognized internal links inside the IndentedItem", 3,
        content.wikiTokens().size());
    assertTrue("The first item should be a template",
        content.wikiTokens().get(0) instanceof Template);
    assertEquals("The parsed link title should be a square bracket", "en",
        content.wikiTokens().get(0).asTemplate().getName());
    assertTrue("The first item should be an internal link",
        content.wikiTokens().get(1) instanceof InternalLink);
    assertEquals("The parsed link title should be a square bracket", "principle",
        content.wikiTokens().get(1).asInternalLink().getLinkText());
    assertTrue("The second item should be an internal link",
        content.wikiTokens().get(2) instanceof InternalLink);
    assertEquals("The parsed link title should be a square bracket", "origin",
        content.wikiTokens().get(2).asInternalLink().getLinkText());

    text = new WikiText("[[toto, [[titi]] ]]");
    assertEquals("There should be 1 recognized internal links", 1, text.wikiTokens().size());
    assertTrue("The first item should be an internal link",
        text.wikiTokens().get(0) instanceof InternalLink);
    assertEquals("The parsed link title should be the internal most prioritary one.", "titi",
        text.wikiTokens().get(0).asInternalLink().getLinkText());

    text = new WikiText("[[toto\n" + "*: test\n" + "]]\n");
    assertEquals("There should be 1 recognized indented item", 1, text.wikiTokens().size());
    assertTrue("The first item should be an internal link",
        text.wikiTokens().get(0) instanceof IndentedItem);
    assertEquals("The indented item should be correctly created.", "*: test",
        text.wikiTokens().get(0).asIndentedItem().getText());
  }

  @Test
  public void testInternalLinkHasLowerPriorityThanIndentedItemOnNewlines() {
    WikiText text = new WikiText("*: [[toto\n" + "]]\n");
    assertEquals("There should be 1 recognized indented item", 1, text.wikiTokens().size());
    assertTrue("The first item should be an indented item",
        text.wikiTokens().get(0) instanceof IndentedItem);
    assertEquals("The indented item should be correctly created.", "*: [[toto",
        text.wikiTokens().get(0).asIndentedItem().getText());
  }

  @Test
  public void testInternalLinkHasLowerPriorityThanClosingHeader() {
    WikiText text = new WikiText("== test [[titi ==\n" + "]]\n");
    assertEquals("There should be 1 recognized Heading", 1, text.wikiTokens().size());
    assertTrue("The first item should be an internal link",
        text.wikiTokens().get(0) instanceof Heading);
    assertEquals("The indented item should be correctly created.", " test [[titi ",
        text.wikiTokens().get(0).asHeading().getContent().getText());
  }

  @Test
  public void testValideExternalLinkClosedByDoubleBrackets() {
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
  public void testValidExternalLinkOpenedByDoubleBrackets() {
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
  public void testInternalLinkWithValidUnclosedExternalLink() {
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
  public void testWikiTextIncoherentLinkInTemplate() {
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

    text = new WikiText(
        "=== Text With incorrect {{Template|   ===\n" + "# A male [[sheep]], a [[ram]].\n"
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

    // System.out.println(text.content().getText());

  }

  @Test
  public void testVeryLongWikiText() {
    WikiText text = new WikiText("\n"
        + "{{Haftanın kelimesi|29|2007}}\n"
        + "[[Resim:Menschliches auge.jpg|küçükresim|120px|{{mânâ|anatomi}} Bir insanın '''gözü''']]\n"
        + "[[Resim:Side table drawer.jpg|küçükresim|120px|{{mânâ|mobilya}} Bir masanın '''gözü''']]\n"
        + "===Köken===\n"
        + ":{{devralınan|tr|ota|گوز|ç=göz}}.\n"
        + "\n"
        + "===Söyleniş===\n"
        + "* {{IPA|dil=tr|/ˈɟøz/}}\n"
        + "* {{h|dil=tr||göz}}\n"
        + "\n"
        + "===Ad===\n"
        + "{{tr-ad}}\n"
        + "\n"
        + "# [[bakış]], [[görüş]]\n"
        + "#: {{misal|tr|Bu sefer alacaklı '''gözüyle''' baktım.}}\n"
        + "# [[boşluk]], [[delik]]\n"
        + "#: {{misal|tr|İğnenin '''gözü'''. Köprünün '''gözleri''' karış karış kazılmıştır.|Sâit Fâik Abasıyanık|S. F. Abasıyanık}}\n"
        + "# [[bölüm]], [[hane]]\n"
        + "#: {{misal|tr|Dama tahtasında altmış dört '''göz''' vardır.}}\n"
        + "# [[gönül]] [[bağlantı]]sı, [[ilgi]], [[sevgi]]\n"
        + "#: {{misal|tr|'''Göze''' girmek. '''Gözden''' düşmek.}}\n"
        + "# [[kıskançlık]] [[veya]] [[hayranlık]]la [[bakılmak|bakıldığında]] [[bir şey]]e [[kötülük]] [[vermek|verdiğine]] [[inanılmak|inanılan]] [[uğursuzluk]]\n"
        + "# [[nazar]]\n"
        + "#: {{misal|tr|İnsanı '''gözle''' yiyip bitirirler.|Ömer Seyfettin|Ö. Seyfettin}}\n"
        + "# [[terazi]] [[kefe]]si\n"
        + "# {{t|dil=tr|anatomi}} [[basar]], [[görme organı]]\n"
        + "#: {{misal|tr|'''Gözü''' iki numara miyop.}}\n"
        + "# {{t|dil=tr|bitki anatomisi}} [[ağaç|ağacın]] [[tomurcuk]] [[vermek|veren]] [[yer]]lerinden [[her biri]]\n"
        + "#: {{misal|tr|'''Göz''' aşısı.}}\n"
        + "# {{t|dil=tr|coğrafya|hidroloji}} [[su]]yun [[toprak]]tan [[kaynamak|kaynadığı]] [[yer]], [[kaynak]]\n"
        + "#: {{misal|tr|Asıl felaket bu pınara sırt çevirmek, bu pınarın '''gözlerine''' taş tıkamak değil de ne olurdu?|Tarık Buğra|T. Buğra}}\n"
        + "# {{mecaz|dil=tr}} [[bazı|bâzı]] [[deyim]]lerde [[görme]] [[ve]] [[bakma]]\n"
        + "#: {{misal|tr|'''Göz''' önünde. '''Gözden''' geçirmek. '''Gözden''' kaybolmak. '''Gözü''' keskin.}}\n"
        + "# {{t|dil=tr|mimarlık}} [[oda]]\n"
        + "#: {{misal|tr|Şu fakir mahallede bir '''göz''' evim olsaydı. Nasıl sevinç içinde çıkardım şu yokuşu.|Ziya Osman Saba|Z. O. Saba}}\n"
        + "# {{t|dil=tr|mobilya}} [[çekmece]]\n"
        + "#: {{misal|tr|Masanın '''gözleri''' kilitliydi.}}\n"
        + "# {{t|dil=tr|tıp}} [[bazı]] [[yara]]ların [[uç]] [[bölüm]]ü\n"
        + "#: {{misal|tr|Çıbanın '''gözü''' kocaman olmuştu.}}\n"
        + "\n"
        + "====Çekimleme====\n"
        + "{{tr-ad-tablo}}\n"
        + "\n"
        + "====Üst kavramlar====\n"
        + "* {{mana|ağacın bir bölümü}} [[ağaç]]\n"
        + "* {{mana|anatomi}} [[duyu organı]]\n"
        + "* {{mana|çekmece}} [[mobilya]]\n"
        + "\n"
        + "====Alt kavramlar====\n"
        + "* [[birleşik göz]], [[petek göz]]\n"
        + "\n"
        + "====Atasözleri====\n"
        + "{{sütun|2|başlık=<u>göz</u> kelimesinin atasözleri|\n"
        + "* [[Ağaran baş, ağlayan göz gizlenmez]]\n"
        + "* [[Ağız büzülür, göz süzülür, ille burun, ille burun]]\n"
        + "* [[Ağrılarda göz ağrısı, her kişinin öz ağrısı]]\n"
        + "* [[Bir başa bir göz yeter]]\n"
        + "* [[El terazi, göz mizan]]\n"
        + "* [[Fazla mal göz çıkarmaz]]\n"
        + "* [[Göz gördüğünü ister]]\n"
        + "* [[Kul istedi bir göz, Allah verdi iki göz]]\n"
        + "}}\n"
        + "\n"
        + "====Deyimler====\n"
        + "{{sütun|2|başlık=<u>göz</u> kelimesinin deyimleri|\n"
        + "* [[açıkgöz]]\n"
        + "* [[ağ gözü]]\n"
        + "* [[ağ gözü açıklığı]]\n"
        + "* [[cam göz]]\n"
        + "* [[çıplak göz]]\n"
        + "* [[dört göz bir evlat için]]\n"
        + "* [[dünyaya gözlerini açmak]]\n"
        + "* [[dünyaya gözlerini kapamak]]\n"
        + "* [[göz açamamak]]\n"
        + "* [[göz açtırmamak]]\n"
        + "* [[göz alıcı güzellik]]\n"
        + "* [[göz ardı etmek]]\n"
        + "* [[göz aşısı]]\n"
        + "* [[göz atmak]]\n"
        + "* [[göz bebeği]]\n"
        + "* [[göz boyamak]]\n"
        + "* [[göz dönmesi]]\n"
        + "* [[göz erimi]]\n"
        + "* [[göz göre göre]]\n"
        + "* [[göz göze gelmek]]\n"
        + "* [[göz hakkı]]\n"
        + "* [[göz kırpmak]]\n"
        + "* [[göz kulak olmak]]\n"
        + "* [[göz küresi]]\n"
        + "* [[göz önünde]]\n"
        + "* [[göz pekliği]]\n"
        + "* [[göz rengi]]\n"
        + "* [[göz rengi farklılığı]]\n"
        + "* [[göz suyu]]\n"
        + "* [[göz süzme]]\n"
        + "* [[göz süzmek]]\n"
        + "* [[göz yummak]]\n"
        + "* [[gözdağı vermek]]\n"
        + "* [[gözden düşmek]]\n"
        + "* [[gözden kaçmak]]\n"
        + "* [[gözden uzak]]\n"
        + "* [[göze batan]]\n"
        + "* [[göze batan]]\n"
        + "* [[göze çarpan]]\n"
        + "* [[göze girmek]]\n"
        + "* [[gözleri açılmak]]\n"
        + "* [[gözleri çakmak çakmak olmak]]\n"
        + "* [[gözleri dolu dolu olmak]]\n"
        + "* [[gözleri fal taşı gibi açılmak]]\n"
        + "* [[gözleri fıldır fıldır etmek]]\n"
        + "* [[gözleri yollarda kalmak]]\n"
        + "* [[gözleri yuvasından fırlamak]]\n"
        + "* [[gözlerinde şimşekler çakmak]]\n"
        + "* [[gözlerine inanamamak]]\n"
        + "* [[gözü açık]]\n"
        + "* [[gözü dışarıda]]\n"
        + "* [[gözü dönmek]]\n"
        + "* [[gözü gibi bakmak]]\n"
        + "* [[gözü olmamak]]\n"
        + "* [[gözü toprağa bakmak]]\n"
        + "* [[gözü uyku tutmamak]]\n"
        + "* [[gözü yükseklerde olmak]]\n"
        + "* [[gözü yüksekte]]\n"
        + "* [[gözüm görmesin]]\n"
        + "* [[gözünde tütmek]]\n"
        + "* [[gözüne çarpmak]]\n"
        + "* [[gözünü bağlamak]]\n"
        + "* [[gözünü karartmak]]\n"
        + "* [[gözünü korkutmak]]\n"
        + "* [[gözünün içine bakmak]]\n"
        + "* [[gözünün önünden geçmek]]\n"
        + "* [[gözyaşı]]\n"
        + "* [[kötü göz]]\n"
        + "* [[para gözlü]]\n"
        + "}}\n"
        + "\n"
        + "====Türetilmiş kavramlar====\n"
        + "{{sütun|2|başlık=<u>göz</u> kelimesinin türetilmiş kavramlar|\n"
        + "* [[göz bebeği]], \n"
        + "* [[göz kapağı]], \n"
        + "* [[göz tansiyonu]], \n"
        + "* [[gözce]], \n"
        + "* [[gözcü]], \n"
        + "* [[gözcük]], \n"
        + "* [[gözde]], \n"
        + "* [[göze]], \n"
        + "* [[gözenek]], \n"
        + "* [[gözetim]], \n"
        + "* [[gözgü]], \n"
        + "* [[gözken]], \n"
        + "* [[gözle]], \n"
        + "* [[gözlem]], \n"
        + "* [[gözleme]], \n"
        + "* [[gözlü]], \n"
        + "* [[gözlük]], \n"
        + "* [[gözsüz]], \n"
        + "* [[güzel|gözel]]\n"
        + "}}\n"
        + "\n"
        + "====Çeviriler====\n"
        + "{{Üst|alaka, ilgi, sevgi|tip=çeviriler}}\n"
        + "* Aari dili: {{ç|aiw|}}\n"
        + "* Abhazca: {{ç|ab|}}\n"
        + "* Afarca: {{ç|aa|}}\n"
        + "* Afrikanca: {{ç|af|}}\n"
        + "* Ainu dili: {{ç|ain|}}\n"
        + "* Aiton dili: {{ç|aio|}}\n"
        + "* Akadca: {{ç|akk|}}\n"
        + "* Almanca: {{ç|de|Zuneigung|f}}\n"
        + "*: Bavyeraca: {{ç|bar|}}\n"
        + "*: İsviçre Almancası: {{ç|gsw|}}\n"
        + "*: Yahudi Almancası: {{ç|yi|}}\n"
        + "* Altayca:\n"
        + "*: Güney Altai: {{ç|alt|}}\n"
        + "*: Kuzey Altayca: {{ç|atv|}}\n"
        + "* Amharca: {{ç|am|}}\n"
        + "* Amorice:\n"
        + "*: Ugaritçe: {{ç|uga|}}\n"
        + "* Aragonca: {{ç|an|}}\n"
        + "* Aramice:\n"
        + "*: Arami: {{ç|arc||tr=|sc=Syrc}}\n"
        + "*: İbrani: {{ç|arc||tr=|sc=Hebr}}\n"
        + "* Arapça: {{ç|ar|}}\n"
        + "*: Cezayir Arapçası: {{ç|arq||tr=aḡrum}}\n"
        + "*: Hicaz Arapçası: {{ç|acw||tr=}}\n"
        + "*: Fas Arapçası: {{ç|ary||tr=}}\n"
        + "*: Irak Arapçası: {{ç|acm||tr=}}\n"
        + "*: Kuzey Levantin Arapçası: {{ç|apc||tr=}}\n"
        + "*: Mısır Arapçası: {{ç|arz||tr=fi|sc=Arab}}\n"
        + "* Argobaca: {{ç|agj||tr=}}\n"
        + "* Arnavutça: {{ç|sq|}}\n"
        + "* Asturyasça: {{ç|ast|}}\n"
        + "* Aşağı Almanca: {{ç|nds|}}\n"
        + "*: Alman Aşağı Almancası: {{ç|nds-de|}}\n"
        + "*:: Plautdieç: {{ç|pdt|}}\n"
        + "*: Hollanda Aşağı Almancası: {{ç|nds-nl|}}\n"
        + "* Aşo dili: {{ç|csh|}}\n"
        + "* Avarca: {{ç|av|}}\n"
        + "* Aymaraca: {{ç|ay|}}\n"
        + "* Azerice: {{ç|az|}}\n"
        + "* Balinezce: {{ç|ban|}}\n"
        + "* Bambara dili: {{ç|bm|}}\n"
        + "* Baskça: {{ç|eu|}}\n"
        + "* Başkurtça: {{ç|ba|}}\n"
        + "* Batsça: {{ç|bbl|}}\n"
        + "* Beluçça: {{ç|bal|}}\n"
        + "* Bengalce: {{ç|bn|}}\n"
        + "* Beyaz Rusça: {{ç|be|}}\n"
        + "* Bikolca:\n"
        + "*: Albay Bikol: {{ç|bhk|}}\n"
        + "*: Merkezî Bikol: {{ç|bcl|}}\n"
        + "*: Pandan Bikol: {{ç|cts|}}\n"
        + "*: Rinkonada Bikol: {{ç|bto|}}\n"
        + "* Birmanca: {{ç|my|}}\n"
        + "* Bretonca: {{ç|br|}}\n"
        + "* Budukça: {{ç|bdk|}}\n"
        + "* Bulgarca: {{ç|bg|}}\n"
        + "* Calo dili: {{ç|rmq|}}\n"
        + "* Cingpo dili: {{ç|kac|}}\n"
        + "* Çeçence: {{ç|ce|}}\n"
        + "* Çekçe: {{ç|cs|}}\n"
        + "* Çerkesçe:\n"
        + "*: Batı Çerkesçesi: {{ç|ady|}}\n"
        + "*: Doğu Çerkesçesi: {{ç|kbd|}}\n"
        + "* Çerokice: {{ç|chr|}}\n"
        + "* Çevaca: {{ç|ny|}}\n"
        + "* Çikasav dili: {{ç|cic|}}\n"
        + "* Çince: {{ç|zh|}}\n"
        + "*: Dungan: {{ç|dng|}}\n"
        + "*: Hakka: {{ç|hak||tr=}}\n"
        + "*: Mandarin: {{ç|cmn||tr=}}\n"
        + "*: Min Bei: {{ç|mnp|}}\n"
        + "*: Min Dong: {{ç|cdo||tr=}}\n"
        + "*: Min Nan: {{ç|nan||tr=}}\n"
        + "*: Wu: {{ç|wuu||tr=}}\n"
        + "*: Yue: {{ç|yue||tr=}}\n"
        + "* Çuvaşça: {{ç|cv|}}\n"
        + "* Dagbanice: {{ç|dag|}}\n"
        + "* Dalmaçyaca: {{ç|dlm|}}\n"
        + "* Danca: {{ç|da|}}\n"
        + "* Dolganca: {{ç|dlg|}}\n"
        + "* Endonezce: {{ç|id|}}\n"
        + "* Ermenice: {{ç|hy|}}\n"
        + "*: Batı Ermenicesi: {{ç|hyw|}}\n"
        + "*: Doğu Ermenicesi: {{ç|hye|}}\n"
        + "*: Eski Ermenice: {{ç|xcl|}}\n"
        + "*: Orta Ermenice: {{ç|axm|}}\n"
        + "* Ersya dili: {{ç|myv|}}\n"
        + "* Eski Norsça: {{ç|non|}}\n"
        + "* Eski Slavca:\n"
        + "*: Eski Doğu Slavcası: {{ç|orv|}}\n"
        + "*: Eski Kilise Slavcası: {{ç|cu|}}\n"
        + "* Esperanto: {{ç|eo|}}\n"
        + "* Estonca: {{ç|et|}}\n"
        + "* Evenkice: {{ç|evn|}}\n"
        + "* Ewece: {{ç|ee|}}\n"
        + "* Farfarca: {{ç|gur|}}\n"
        + "* Faroece: {{ç|fo|}}\n"
        + "* Farsça: {{ç|fa|}}\n"
        + "*: Orta Farsça: {{ç|pal|}}\n"
        + "* Felemenkçe: {{ç|nl|}}\n"
        + "* Fijice: {{ç|fj|}}\n"
        + "* Fince: {{ç|fi|}}\n"
        + "* Frankça: {{ç|frk|}}\n"
        + "* Fransızca: {{ç|fr|}}\n"
        + "* Frigce: {{ç|xpg|}}\n"
        + "* Friuli dili: {{ç|fur|}}\n"
        + "* Frizce:\n"
        + "*: Batı Frizce: {{ç|fy|}}\n"
        + "*: Kuzey Frizce: {{ç|frr|}}\n"
        + "*: Sater Frizcesi: {{ç|stq|}}\n"
        + "* Fulanice:\n"
        + "*: Adlam: {{ç|ff|}}\n"
        + "*: Latin: {{ç|ff|}}\n"
        + "* Furlanca: {{ç|fur|}}\n"
        + "* Gagavuzca: {{ç|gag|}}\n"
        + "* Galce: {{ç|cy|}}\n"
        + "* Galiçyaca: {{ç|gl|}}\n"
        + "* Gilyakça: {{ç|niv|}}\n"
        + "* Gotça: {{ç|got|}}\n"
        + "* Grönlandca: {{ç|kl|}}\n"
        + "* Guanarice: {{ç|gn|}}\n"
        + "* Gucaratça: {{ç|gu|}}\n"
        + "* Gürcüce: {{ç|ka|}}\n"
        + "* Haiti Kreolü: {{ç|ht|}}\n"
        + "* Hausaca: {{ç|ha|}}\n"
        + "* Hawaii dili: {{ç|haw|}}\n"
        + "* Hınalık dili: {{ç|kjj|}}\n"
        + "* Hintçe: {{ç|hi|}}\n"
        + "* Hititçe: {{ç|hit|}}\n"
        + "* Hmong dili: {{ç|hmn|}}\n"
        + "* Hunzib dili: {{ç|huz|}}\n"
        + "* İbranice: {{ç|he|}}\n"
        + "* İdo dili: {{ç|io|}}\n"
        + "* İngilizce: {{ç|en|love}}\n"
        + "*: Eski İngilizce: {{ç|ang|}}\n"
        + "*: İskoç İngilizcesi: {{ç|sco|}}\n"
        + "*: Orta İngilizce: {{ç|enm|}}\n"
        + "* İnterlingua: {{ç|ia|}}\n"
        + "* İnuitçe:\n"
        + "*: Doğu Kanada İnuitçesi: {{ç|iu|}}\n"
        + "* İrlandaca: {{ç|ga|}}\n"
        + "*: Eski İrlandaca: {{ç|sga|}}\n"
        + "* İskoçça: {{ç|gd|}}\n"
        + "* İspanyolca: {{ç|es|}}\n"
        + "*: Yahudi İspanyolcası: {{ç|lad|}}\n"
        + "* İstrioça: {{ç|ist|}}\n"
        + "* İsveççe: {{ç|sv|}}\n"
        + "* İtalyanca: {{ç|it|}}\n"
        + "* İzlandaca: {{ç|is|}}\n"
        + "* Japonca: {{ç|ja|}}\n"
        + "* Kabardeyce: {{ç|kbd|}}\n"
        + "* Kalmukça: {{ç|xal|}}\n"
        + "* Kapampangan dili: {{ç|pam|}}\n"
        + "* Kannada dili: {{ç|kn|}}\n"
        + "* Karaçay-Balkarca: {{ç|krc|}}\n"
        + "* Karakalpakça: {{ç|kaa|}}\n"
        + "* Karayca: {{ç|kdr|}}\n"
        + "* Karipúna Fransız Kreolü: {{ç|kmv|}}\n"
        + "* Kaşupça: {{ç|csb|}}\n"
        + "* Katalanca: {{ç|ca|}}\n"
        + "* Kazakça: {{ç|kk|}}\n"
        + "* Keçuva dili: {{ç|qu|}}\n"
        + "* Kernevekçe: {{ç|kw|}}\n"
        + "* Kıptîce: {{ç|cop|}}\n"
        + "* Kırgızca: {{ç|ky|}}\n"
        + "* Kikuyu dili: {{ç|ki|}}\n"
        + "* Kmer dili: {{ç|km|}}\n"
        + "* Komi dili: {{ç|kv|}}\n"
        + "* Komorca: {{ç|bnt|}}\n"
        + "* Konkani dili: {{ç|kok|}}\n"
        + "* Korece: {{ç|ko|}}\n"
        + "* Korsikaca: {{ç|co|}}\n"
        + "* Koryakça: {{ç|kpy|}}\n"
        + "* Krice: {{ç|cr|}}\n"
        + "*: Ova Kricesi: {{ç|crk|}}\n"
        + "* Kuçean dili: {{ç|txb|}}\n"
        + "* Kumukça: {{ç|kum|}}\n"
        + "* Kürtçe: {{ç|ku|}}\n"
        + "*: Kuzey Kürtçe: {{ç|kmr|}}\n"
        + "*: Lekçe: {{ç|lki|}}\n"
        + "*: Orta Kürtçe: {{ç|ckb|}}\n"
        + "* Ladince: {{ç|lld|}}\n"
        + "* Laoca: {{ç|lo|}}\n"
        + "* Laponca:\n"
        + "*: Akkala: {{ç|sia|}}\n"
        + "*: Güney Laponca: {{ç|sma|}}\n"
        + "*: İnari: {{ç|smn|}}\n"
        + "*: Kemi: {{ç|sjk|}}\n"
        + "*: Kildin: {{ç|sjd|}}\n"
        + "*: Kuzey Laponca: {{ç|sme|}}\n"
        + "*: Lule: {{ç|smj|}}\n"
        + "*: Pite: {{ç|sje|}}\n"
        + "*: Skolt: {{ç|sms|}}\n"
        + "*: Ter: {{ç|sjt|}}\n"
        + "*: Ume: {{ç|sju|}}\n"
        + "* Latgalce: {{ç|ltg|}}\n"
        + "* Latince: {{ç|la|}}\n"
        + "* Lehçe: {{ç|pl|}}\n"
        + "* Letonca: {{ç|lv|}}\n"
        + "* Lezgice: {{ç|lez|}}\n"
        + "* Ligurya dili: {{ç|lij|}}\n"
        + "* Limburgca: {{ç|li|}}\n"
        + "* Lingala: {{ç|ln|}}\n"
        + "* Litvanca: {{ç|lt|}}\n"
        + "* Livonca: {{ç|liv|}}\n"
        + "* Lombardça: {{ç|lmo|}}\n"
        + "* Luhyaca: {{ç|luy|}}\n"
        + "* Luvice:\n"
        + "*: Çivi yazısı: {{ç|xlu|}}\n"
        + "*: Hiyeroglif: {{ç|hlu|}}\n"
        + "* Lüksemburgca: {{ç|lb|}}\n"
        + "* Maasai dili: {{ç|mas|}}\n"
        + "* Macarca: {{ç|hu|}}\n"
        + "* Magindanao dili: {{ç|mdh|}}\n"
        + "* Makedonca: {{ç|mk|}}\n"
        + "* Malayalam dili: {{ç|ml|}}\n"
        + "* Malayca: {{ç|ms|}}\n"
        + "*: Ambonez Malayca: {{ç|abs|}}\n"
        + "* Malgaşça: {{ç|mg|}}\n"
        + "* Maltaca: {{ç|mt|}}\n"
        + "* Mançuca: {{ç|mnc|}}\n"
        + "* Manksça {{ç|gv|}}\n"
        + "* Maorice: {{ç|mi|}}\n"
        + "* Mapudungun dili: {{ç|arn|}}\n"
        + "* Maranao dili: {{ç|mrw|}}\n"
        + "* Marathi dili: {{ç|mr|}}\n"
        + "* Mari dili: {{ç|chm|}}\n"
        + "* Massachusett dili: {{ç|wam|}}\n"
        + "* Mayaca:\n"
        + "*: Yukatek Mayacası: {{ç|yua|}}\n"
        + "* Mbyá Guaraní dili: {{ç|gun|}}\n"
        + "* Merkezî Sierra Miwok dili: {{ç|csm|}}\n"
        + "* Mısırca: {{ç|egy|}}\n"
        + "* Moğolca:\n"
        + "*: Kiril: {{ç|mn|}}\n"
        + "*: Latin: {{ç|mn|}}\n"
        + "* Mòkeno dili: {{ç|mhn|}}\n"
        + "* Mokşa dili: {{ç|mdf|}}\n"
        + "* Moldovaca: {{ç|mo|}}\n"
        + "* Munsee dili: {{ç|umu|}}\n"
        + "* Mwani dili: {{ç|wmw|}}\n"
        + "* Nahuatl dili: {{ç|nah|}}\n"
        + "*: Klâsik Nahuatl dili: {{ç|nci|}}\n"
        + "* Nandi dili: {{ç|kln|}}\n"
        + "* Napolice: {{ç|nap|}}\n"
        + "* Nauruca: {{ç|na|}}\n"
        + "* Navahoca: {{ç|nv|}}\n"
        + "* Nenetsçe: {{ç|yrk|}}\n"
        + "* Nepalce: {{ç|ne|}}\n"
        + "* Nogayca: {{ç|nog|}}\n"
        + "* Normanca: {{ç|nrf|}}\n"
        + "* Norveççe: {{ç|no|}}\n"
        + "*: Bokmål: {{ç|nb|}}\n"
        + "*: Nynorsk: {{ç|nn|}}\n"
        + "* Novial: {{ç|nov|}}\n"
        + "* Ojibvaca: {{ç|oji|}}\n"
        + "* Oksitanca: {{ç|oc|}}\n"
        + "* Orta Atlas Tamazit dili: {{ç|tzm|}}\n"
        + "* Osetçe: {{ç|os|}}\n"
        + "* Övdalca: {{ç|ovd|}}\n"
        + "* Özbekçe: {{ç|uz|}}\n"
        + "* Pali dili: {{ç|pi|}}\n"
        + "* Papiamento: {{ç|pap|}}\n"
        + "* Pencapça: {{ç|pa|}}\n"
        + "*: Batı Pencapça: {{ç|pnb|}}\n"
        + "* Permyakça: {{ç|koi|}}\n"
        + "* Peştuca: {{ç|ps|}}\n"
        + "* Piemontça: {{ç|pms|}}\n"
        + "* Polapça: {{ç|pox|}}\n"
        + "* Portekizce: {{ç|pt|}}\n"
        + "* Rapa Nui dili: {{ç|rap|}}\n"
        + "* Rohingyaca: {{ç|rhg|}}\n"
        + "* Romanca: {{ç|rom|}}\n"
        + "* Romanşça: {{ç|ro|}}\n"
        + "* Ruanda dili: {{ç|rw|}}\n"
        + "* Rumence: {{ç|ro|}}\n"
        + "* Rusça: {{ç|ru|}}\n"
        + "* Rusince: {{ç|rue|}}\n"
        + "* Sabuanca: {{ç|ceb|}}\n"
        + "* Samoaca: {{ç|sm|}}\n"
        + "* Samogitçe: {{ç|sgs|}}\n"
        + "* Sango dili: {{ç|sg|}}\n"
        + "* Sanskritçe: {{ç|sa|}}\n"
        + "* Santalice: {{ç|sat|}}\n"
        + "* Sardunyaca: {{ç|sc|}}\n"
        + "*: Kampidanez Sardunyacası: {{ç|sro|}}\n"
        + "* Savahili dili: {{ç|sw|}}\n"
        + "* Seylanca: {{ç|si|}}\n"
        + "* Sırp-Hırvatça:\n"
        + "*: Kiril: {{ç|sh|}}\n"
        + "*: Latin: {{ç|sh|}}\n"
        + "* Sicilyaca: {{ç|scn|}}\n"
        + "* Sidama dili: {{ç|sid|}}\n"
        + "* Slovakça: {{ç|sk|}}\n"
        + "* Slovence: {{ç|sl|}}\n"
        + "* Somalice: {{ç|so|}}\n"
        + "* Sorbca:\n"
        + "*: Aşağı Sorbca: {{ç|dsb|}}\n"
        + "*: Yukarı Sorbca: {{ç|hsb|}}\n"
        + "* Sotho dili: {{ç|st|}}\n"
        + "* Sunda dili: {{ç|su|genep}}\n"
        + "* Supikçe: {{ç|ems|}}\n"
        + "* Sümerce: {{ç|sux|}}\n"
        + "* Süryanice: {{ç|syc|}}\n"
        + "*: Neo-Süryanice: {{ç|aii||tr=}}\n"
        + "* Savahili: {{ç|sw|}}\n"
        + "* Svanca: {{ç|sva|}}\n"
        + "* Şan dili: {{ç|shn|}}\n"
        + "* Şilha dili: {{ç|shi|}}\n"
        + "* Şona dili: {{ç|sn|}}\n"
        + "* Şorca: {{ç|cjs|}} \n"
        + "* Tabasaranca: {{ç|tab|}}\n"
        + "* Tacikçe: {{ç|tg|}}\n"
        + "* Tagalogca: {{ç|tl|}}\n"
        + "* Tai Lü dili: {{ç|khb|}}\n"
        + "* Tamilce: {{ç|ta|}}\n"
        + "* Tarifit: {{ç|rif|}}\n"
        + "* Tatarca: {{ç|tt|}}\n"
        + "*: Kırım Tatarcası: {{ç|crh|}}\n"
        + "* Tayca: {{ç|th|}}\n"
        + "*: Kuzey Tayca: {{ç|nod|}}\n"
        + "* Telugu dili: {{ç|te|}}\n"
        + "* Tesmence: {{ç|tk|}}\n"
        + "* Tetum: {{ç|tet|}}\n"
        + "* Tibetçe:\n"
        + "*: Klâsik Tibetçe: {{ç|xct|}}\n"
        + "*: Lhasa Tibetçesi: {{ç|bo|}}\n"
        + "* Tigrinya dili: {{ç|ti|}}\n"
        + "* Tok Pisin dili: {{ç|tpi|}}\n"
        + "* Tsonga dili: {{ç|ts|}}\n"
        + "* Tutelo dili: {{ç|tta|}}\n"
        + "* Tupinambá: {{ç|tpn|}}\n"
        + "* Tuvaca: {{ç|tyv|}}\n"
        + "* Türkçe:\n"
        + "*: Eski Türkçe: {{ç|otk|}}\n"
        + "*: Osmanlı Türkçesi: {{ç|ota|گوز}}\n"
        + "* Türkmence: {{ç|tk|}}\n"
        + "* Tzotzilce: {{ç|tzo|}}\n"
        + "* Udmurtça: {{ç|udm|}}\n"
        + "* Ukraynaca: {{ç|uk|}}\n"
        + "* Ulahça: {{ç|rup|}}\n"
        + "* Umbundu dili: {{ç|umb|}}\n"
        + "* Urduca: {{ç|ur|}}\n"
        + "* Uygurca: {{ç|ug|}}\n"
        + "* Valonca: {{ç|wa|}}\n"
        + "* Venedikçe: {{ç|vec|}}\n"
        + "* Vepsçe: {{ç|vep|}}\n"
        + "* Vietnamca: {{ç|vi|}}\n"
        + "* Vilamov dili: {{ç|wym|}}\n"
        + "* Volapük: {{ç|vo|}}\n"
        + "* Võro dili: {{ç|vro|}}\n"
        + "* Votça: {{ç|vot|}}\n"
        + "* Wolof dili: {{ç|wo|}}\n"
        + "* Xârâcùù dili: {{ç|ane|}}\n"
        + "* Xhosa dili: {{ç|xh|}}\n"
        + "* Yağnupça: {{ç|yai|}}\n"
        + "* Yakutça: {{ç|sah|}}\n"
        + "* Yorubaca: {{ç|yo|}}\n"
        + "* Yunanca:\n"
        + "*: Grekçe: {{ç|grc|}}\n"
        + "*: Modern Yunanca: {{ç|el|}}\n"
        + "* Yupik dili: {{ç|esu|}}\n"
        + "* Zarma dili: {{ç|dje|}}\n"
        + "* Zazaca: {{ç|zza|}}\n"
        + "*: Güney Zazaca: {{ç|diq|}}\n"
        + "*: Kuzey Zazaca: {{ç|kiu|}} \n"
        + "* Zhuangca: {{ç|za|}}\n"
        + "* Zuluca: {{ç|zu|}}\n"
        + "{{Alt}}\n"
        + "\n"
        + "{{Üst|anatomi|tip=çeviriler}}\n"
        + "* Aari dili: {{ç|aiw|}}\n"
        + "* Abhazca: {{ç|ab|}}\n"
        + "* Afarca: {{ç|aa|}}\n"
        + "* Afrikanca: {{ç|af|oog|n}}\n"
        + "* Ainu dili: {{ç|ain|}}\n"
        + "* Aiton dili: {{ç|aio|}}\n"
        + "* Akadca: {{ç|akk|}}\n"
        + "* Almanca: {{ç|de|Auge|n}}\n"
        + "*: Bavyeraca: {{ç|bar|}}\n"
        + "*: İsviçre Almancası: {{ç|gsw|}}\n"
        + "*: Yahudi Almancası: {{ç|yi|oyg}}\n"
        + "* Altayca:\n"
        + "*: Güney Altai: {{ç|alt|кöс}}\n"
        + "*: Kuzey Altayca: {{ç|atv|}}\n"
        + "* Amharca: {{ç|am|}}\n"
        + "* Amorice:\n"
        + "*: Ugaritçe: {{ç|uga|}}\n"
        + "* Aragonca: {{ç|an|}}\n"
        + "* Aramice:\n"
        + "*: Arami: {{ç|arc||tr=|sc=Syrc}}\n"
        + "*: İbrani: {{ç|arc||tr=|sc=Hebr}}\n"
        + "* Arapça: {{ç|ar|عَيْن|m}}\n"
        + "*: Cezayir Arapçası: {{ç|arq||tr=aḡrum}}\n"
        + "*: Hicaz Arapçası: {{ç|acw||tr=}}\n"
        + "*: Fas Arapçası: {{ç|ary||tr=}}\n"
        + "*: Irak Arapçası: {{ç|acm||tr=}}\n"
        + "*: Kuzey Levantin Arapçası: {{ç|apc||tr=}}\n"
        + "*: Mısır Arapçası: {{ç|arz||tr=fi|sc=Arab}}\n"
        + "* Argobaca: {{ç|agj||tr=}}\n"
        + "* Arnavutça: {{ç|sq|sy}}\n"
        + "* Asturyasça: {{ç|ast|}}\n"
        + "* Aşağı Almanca: {{ç|nds|}}\n"
        + "*: Alman Aşağı Almancası: {{ç|nds-de|}}\n"
        + "*:: Plautdieç: {{ç|pdt|}}\n"
        + "*: Hollanda Aşağı Almancası: {{ç|nds-nl|}}\n"
        + "* Aşo dili: {{ç|csh|}}\n"
        + "* Avarca: {{ç|av|}}\n"
        + "* Aymaraca: {{ç|ay|nayra}}\n"
        + "* Azerice: {{ç|az|göz}}\n"
        + "* Balinezce: {{ç|ban|}}\n"
        + "* Bambara dili: {{ç|bm|}}\n"
        + "* Baskça: {{ç|eu|}}\n"
        + "* Başkurtça: {{ç|ba|}}\n"
        + "* Batsça: {{ç|bbl|}}\n"
        + "* Beluçça: {{ç|bal|}}\n"
        + "* Bengalce: {{ç|bn|}}\n"
        + "* Beyaz Rusça: {{ç|be|}}\n"
        + "* Bikolca:\n"
        + "*: Albay Bikol: {{ç|bhk|}}\n"
        + "*: Merkezî Bikol: {{ç|bcl|}}\n"
        + "*: Pandan Bikol: {{ç|cts|}}\n"
        + "*: Rinkonada Bikol: {{ç|bto|}}\n"
        + "* Birmanca: {{ç|my|}}\n"
        + "* Bretonca: {{ç|br|lagad}}\n"
        + "* Budukça: {{ç|bdk|}}\n"
        + "* Bulgarca: {{ç|bg|око|n}}\n"
        + "* Calo dili: {{ç|rmq|}}\n"
        + "* Cingpo dili: {{ç|kac|}}\n"
        + "* Çeçence: {{ç|ce|}}\n"
        + "* Çekçe: {{ç|cs|oko|n}}\n"
        + "* Çerkesçe:\n"
        + "*: Batı Çerkesçesi: {{ç|ady|}}\n"
        + "*: Doğu Çerkesçesi: {{ç|kbd|}}\n"
        + "* Çerokice: {{ç|chr|}}\n"
        + "* Çevaca: {{ç|ny|}}\n"
        + "* Çikasav dili: {{ç|cic|}}\n"
        + "* Çince: {{ç|zh|眼睛}}\n"
        + "*: Dungan: {{ç|dng|}}\n"
        + "*: Hakka: {{ç|hak||tr=}}\n"
        + "*: Mandarin dili: {{ç|cmn|眼|tr=yǎn}}, {{ç|cmn|眼睛|tr=yǎnjīng}}\n"
        + "*: Min Bei: {{ç|mnp|}}\n"
        + "*: Min Dong: {{ç|cdo||tr=}}\n"
        + "*: Min Nan: {{ç|nan||tr=}}\n"
        + "*: Wu: {{ç|wuu||tr=}}\n"
        + "*: Yue: {{ç|yue||tr=}}\n"
        + "* Çuvaşça: {{ç|cv|}}\n"
        + "* Dagbanice: {{ç|dag|}}\n"
        + "* Dalmaçyaca: {{ç|dlm|}}\n"
        + "* Danca: {{ç|da|øje}}\n"
        + "* Dolganca: {{ç|dlg|}}\n"
        + "* Endonezce: {{ç|id|mata}}\n"
        + "* Ermenice: {{ç|hy|}}\n"
        + "*: Batı Ermenicesi: {{ç|hyw|}}\n"
        + "*: Doğu Ermenicesi: {{ç|hye|}}\n"
        + "*: Eski Ermenice: {{ç|xcl|}}\n"
        + "*: Orta Ermenice: {{ç|axm|}}\n"
        + "* Ersya dili: {{ç|myv|сельме}}\n"
        + "* Eski Norsça: {{ç|non|}}\n"
        + "* Eski Slavca:\n"
        + "*: Eski Doğu Slavcası: {{ç|orv|}}\n"
        + "*: Eski Kilise Slavcası: {{ç|cu|}}\n"
        + "* Esperanto: {{ç|eo|okulo}}\n"
        + "* Estonca: {{ç|et|silm}}\n"
        + "* Evenkice: {{ç|evn|}}\n"
        + "* Ewece: {{ç|ee|}}\n"
        + "* Farfarca: {{ç|gur|}}\n"
        + "* Faroece: {{ç|fo|}}\n"
        + "* Farsça: {{ç|fa|چشم|tr=čašm}}\n"
        + "*: Orta Farsça: {{ç|pal|}}\n"
        + "* Felemenkçe: {{ç|nl|oog|n}}\n"
        + "* Fijice: {{ç|fj|}}\n"
        + "* Fince: {{ç|fi|silmä}}, {{ç|fi|lähde}}, {{ç|fi|silmu}}\n"
        + "* Frankça: {{ç|frk|}}\n"
        + "* Fransızca: {{ç|fr|œil}}\n"
        + "* Frigce: {{ç|xpg|}}\n"
        + "* Friuli dili: {{ç|fur|}}\n"
        + "* Frizce:\n"
        + "*: Batı Frizce: {{ç|fy|each}}\n"
        + "*: Kuzey Frizce: {{ç|frr|}}\n"
        + "*: Sater Frizcesi: {{ç|stq|}}\n"
        + "* Fulanice:\n"
        + "*: Adlam: {{ç|ff|}}\n"
        + "*: Latin: {{ç|ff|}}\n"
        + "* Furlanca: {{ç|fur|}}\n"
        + "* Gagavuzca: {{ç|gag|}}\n"
        + "* Galce: {{ç|cy|llygad}}\n"
        + "* Galiçyaca: {{ç|gl|ollo}}\n"
        + "* Gilyakça: {{ç|niv|}}\n"
        + "* Gotça: {{ç|got|}}\n"
        + "* Grönlandca: {{ç|kl|}}\n"
        + "* Guanarice: {{ç|gn|}}\n"
        + "* Gucaratça: {{ç|gu|આંખ}}, {{ç|gu|ચક્ષુ}}\n"
        + "* Gürcüce: {{ç|ka|}}\n"
        + "* Haiti Kreolü: {{ç|ht|}}\n"
        + "* Hausaca: {{ç|ha|}}\n"
        + "* Hawaii dili: {{ç|haw|}}\n"
        + "* Hınalık dili: {{ç|kjj|}}\n"
        + "* Hintçe: {{ç|hi|आंख}}, {{ç|hi|नयन}}, {{ç|hi|नेत्र}}\n"
        + "* Hititçe: {{ç|hit|}}\n"
        + "* Hmong dili: {{ç|hmn|}}\n"
        + "* Hunzib dili: {{ç|huz|}}\n"
        + "* İbranice: {{ç|he|עין}}\n"
        + "* İdo dili: {{ç|io|}}\n"
        + "* İngilizce: {{ç|en|eye}}\n"
        + "*: Eski İngilizce: {{ç|ang|}}\n"
        + "*: İskoç İngilizcesi: {{ç|sco|}}\n"
        + "*: Orta İngilizce: {{ç|enm|}}\n"
        + "* İnterlingua: {{ç|ia|}}\n"
        + "* İnuitçe:\n"
        + "*: Doğu Kanada İnuitçesi: {{ç|iu|}}\n"
        + "* İrlandaca: {{ç|ga|súil}}\n"
        + "*: Eski İrlandaca: {{ç|sga|}}\n"
        + "* İskoçça: {{ç|gd|}}\n"
        + "* İspanyolca: {{ç|es|ojo|m}}\n"
        + "*:Yahudi İspanyolcası: {{ç|lad|ojo}}\n"
        + "* İstrioça: {{ç|ist|}}\n"
        + "* İsveççe: {{ç|sv|öga}}\n"
        + "* İtalyanca: {{ç|it|occhio|m}}\n"
        + "* İzlandaca: {{ç|is|}}\n"
        + "* Japonca: {{ç|ja|目}}\n"
        + "* Kabardeyce: {{ç|kbd|}}\n"
        + "* Kalmukça: {{ç|xal|}}\n"
        + "* Kapampangan dili: {{ç|pam|}}\n"
        + "* Kannada dili: {{ç|kn|}}\n"
        + "* Karaçay-Balkarca: {{ç|krc|köz}}\n"
        + "* Karakalpakça: {{ç|kaa|}}\n"
        + "* Karayca: {{ç|kdr|}}\n"
        + "* Karipúna Fransız Kreolü: {{ç|kmv|}}\n"
        + "* Kaşupça: {{ç|csb|}}\n"
        + "{{ç|krc|köz}}\n"
        + "* Katalanca: {{ç|ca|ull}}, {{ç|ca|uyl}}\n"
        + "* Kazakça: {{ç|kk|көз}}\n"
        + "* Keçuva dili: {{ç|qu|}}\n"
        + "* Kernevekçe: {{ç|kw|}}\n"
        + "* Kıptîce: {{ç|cop|}}\n"
        + "* Kırgızca: {{ç|ky|көз}}\n"
        + "* Kikuyu dili: {{ç|ki|}}\n"
        + "* Kmer dili: {{ç|km|}}\n"
        + "* Komi dili: {{ç|kv|}}\n"
        + "* Komorca: {{ç|bnt|}}\n"
        + "* Konkani dili: {{ç|kok|}}\n"
        + "* Korece: {{ç|ko|눈}}\n"
        + "* Korsikaca: {{ç|co|}}\n"
        + "* Koryakça: {{ç|kpy|}}\n"
        + "* Krice: {{ç|cr|}}\n"
        + "*: Ova Kricesi: {{ç|crk|}}\n"
        + "* Kuçean dili: {{ç|txb|}}\n"
        + "* Kumukça: {{ç|kum|}}\n"
        + "* Kürtçe: {{ç|ku|çav}}\n"
        + "*: Kuzey Kürtçe: {{ç|kmr|}}\n"
        + "*: Lekçe: {{ç|lki|}}\n"
        + "*: Orta Kürtçe: {{ç|ckb|}}\n"
        + "* Ladince: {{ç|lld|}}\n"
        + "* Laoca: {{ç|lo|}}\n"
        + "* Laponca:\n"
        + "*: Akkala: {{ç|sia|}}\n"
        + "*: Güney Laponca: {{ç|sma|}}\n"
        + "*: İnari: {{ç|smn|}}\n"
        + "*: Kemi: {{ç|sjk|}}\n"
        + "*: Kildin: {{ç|sjd|}}\n"
        + "*: Kuzey Laponca: {{ç|sme|}}\n"
        + "*: Lule: {{ç|smj|}}\n"
        + "*: Pite: {{ç|sje|}}\n"
        + "*: Skolt: {{ç|sms|}}\n"
        + "*: Ter: {{ç|sjt|}}\n"
        + "*: Ume: {{ç|sju|}}\n"
        + "* Latgalce: {{ç|ltg|}}\n"
        + "* Latince: {{ç|la|oculus|m}}\n"
        + "* Lehçe: {{ç|pl|oko}}\n"
        + "* Letonca: {{ç|lv|}}\n"
        + "* Lezgice: {{ç|lez|}}\n"
        + "* Ligurya dili: {{ç|lij|}}\n"
        + "* Limburgca: {{ç|li|}}\n"
        + "* Lingala: {{ç|ln|}}\n"
        + "* Litvanca: {{ç|lt|akis}}\n"
        + "* Livonca: {{ç|liv|}}\n"
        + "* Lombardça: {{ç|lmo|}}\n"
        + "* Luhyaca: {{ç|luy|}}\n"
        + "* Luvice:\n"
        + "*: Çivi yazısı: {{ç|xlu|}}\n"
        + "*: Hiyeroglif: {{ç|hlu|}}\n"
        + "* Lüksemburgca: {{ç|lb|}}\n"
        + "* Maasai dili: {{ç|mas|}}\n"
        + "* Macarca: {{ç|hu|szem}}; {{ç|hu|fiók}}\n"
        + "* Magindanao dili: {{ç|mdh|mata}}\n"
        + "* Makedonca: {{ç|mk|око}}\n"
        + "* Malayalam dili: {{ç|ml|}}\n"
        + "* Malayca: {{ç|ms|mata}}\n"
        + "*: Ambonez Malayca: {{ç|abs|}}\n"
        + "* Malgaşça: {{ç|mg|}}\n"
        + "* Maltaca: {{ç|mt|ghajn|f}}\n"
        + "* Mançuca: {{ç|mnc|}}\n"
        + "* Manksça {{ç|gv|}}\n"
        + "* Maorice: {{ç|mi|mata}}\n"
        + "* Mapudungun dili: {{ç|arn|}}\n"
        + "* Maranao dili: {{ç|mrw|mata}}\n"
        + "* Marathi dili: {{ç|mr|}}\n"
        + "* Mari dili: {{ç|chm|шинча}}\n"
        + "* Massachusett dili: {{ç|wam|}}\n"
        + "* Mayaca:\n"
        + "*: Yukatek Mayacası: {{ç|yua|}}\n"
        + "* Mbyá Guaraní dili: {{ç|gun|}}\n"
        + "* Merkezî Sierra Miwok dili: {{ç|csm|}}\n"
        + "* Mısırca: {{ç|egy|}}\n"
        + "* Moğolca:\n"
        + "*: Kiril: {{ç|mn|нүд}}\n"
        + "*: Latin: {{ç|mn|}}\n"
        + "* Mòkeno dili: {{ç|mhn|}}\n"
        + "* Mokşa dili: {{ç|mdf|сельме}}\n"
        + "* Moldovaca: {{ç|mo|}}\n"
        + "* Munsee dili: {{ç|umu|}}\n"
        + "* Mwani dili: {{ç|wmw|}}\n"
        + "* Nahuatl dili: {{ç|nah|}}\n"
        + "*: Klâsik Nahuatl dili: {{ç|nci|}}\n"
        + "* Nandi dili: {{ç|kln|}}\n"
        + "* Napolice: {{ç|nap|}}\n"
        + "* Nauruca: {{ç|na|}}\n"
        + "* Navahoca: {{ç|nv|}}\n"
        + "* Nenetsçe: {{ç|yrk|}}\n"
        + "* Nepalce: {{ç|ne|}}\n"
        + "* Nogayca: {{ç|nog|}}\n"
        + "* Normanca: {{ç|nrf|}}\n"
        + "* Norveççe: {{ç|no|øye|n}}\n"
        + "* Novial: {{ç|nov|}}\n"
        + "* Ojibvaca: {{ç|oji|}}\n"
        + "* Oksitanca: {{ç|oc|}}\n"
        + "* Orta Atlas Tamazit dili: {{ç|tzm|}}\n"
        + "* Osetçe: {{ç|os|}}\n"
        + "* Övdalca: {{ç|ovd|}}\n"
        + "* Özbekçe: {{ç|uz|ko'z}}\n"
        + "* Pali dili: {{ç|pi|}}\n"
        + "* Papiamento: {{ç|pap|}}\n"
        + "* Pencapça: {{ç|pa|}}\n"
        + "*: Batı Pencapça: {{ç|pnb|}}\n"
        + "* Permyakça: {{ç|koi|}}\n"
        + "* Peştuca: {{ç|ps|}}\n"
        + "* Piemontça: {{ç|pms|}}\n"
        + "* Polapça: {{ç|pox|}}\n"
        + "* Portekizce: {{ç|pt|olho}}\n"
        + "* Rapa Nui dili: {{ç|rap|}}\n"
        + "* Rohingyaca: {{ç|rhg|}}\n"
        + "* Romanca: {{ç|rom|}}\n"
        + "* Romanşça: {{ç|ro|}}\n"
        + "* Ruanda dili: {{ç|rw|}}\n"
        + "* Rumence: {{ç|ro|ochi}}\n"
        + "* Rusça: {{ç|ru|глаз}}\n"
        + "* Rusince: {{ç|rue|}}\n"
        + "* Sabuanca: {{ç|ceb|}}\n"
        + "* Samoaca: {{ç|sm|}}\n"
        + "* Samogitçe: {{ç|sgs|}}\n"
        + "* Sango dili: {{ç|sg|}}\n"
        + "* Sanskritçe: {{ç|sa|}}\n"
        + "* Santalice: {{ç|sat|}}\n"
        + "* Sardunyaca: {{ç|sc|}}\n"
        + "*: Kampidanez Sardunyacası: {{ç|sro|}}\n"
        + "* Savahili dili: {{ç|sw|}}\n"
        + "* Seylanca: {{ç|si|}}\n"
        + "* Sırp-Hırvatça:\n"
        + "*:Kiril: {{ç|sh|око}}\n"
        + "*:Latin: {{ç|sh|oko}}\n"
        + "* Sicilyaca: {{ç|scn|}}\n"
        + "* Sidama dili: {{ç|sid|}}\n"
        + "* Slovakça: {{ç|sk|oko}}, {{ç|sk|svorník}}\n"
        + "* Slovence: {{ç|sl|oko}}\n"
        + "* Somalice: {{ç|so|il}}, {{ç|so|indho|ç}}\n"
        + "* Sorbca:\n"
        + "*: Aşağı Sorbca: {{ç|dsb|}}\n"
        + "*: Yukarı Sorbca: {{ç|hsb|}}\n"
        + "* Sotho dili: {{ç|st|}}\n"
        + "* Sunda dili: {{ç|su|genep}}\n"
        + "* Supikçe: {{ç|ems|}}\n"
        + "* Sümerce: {{ç|sux|}}\n"
        + "* Süryanice: {{ç|syc|}}\n"
        + "*: Neo-Süryanice: {{ç|aii||tr=}}\n"
        + "* Savahili: {{ç|sw|}}\n"
        + "* Svanca: {{ç|sva|}}\n"
        + "* Şan dili: {{ç|shn|}}\n"
        + "* Şilha dili: {{ç|shi|}}\n"
        + "* Şona dili: {{ç|sn|}}\n"
        + "* Şorca: {{ç|cjs|қарақ}}\n"
        + "* Tabasaranca: {{ç|tab|}}\n"
        + "* Tacikçe: {{ç|tg|}}\n"
        + "* Tagalogca: {{ç|tl|mata}}\n"
        + "* Tai Lü dili: {{ç|khb|}}\n"
        + "* Tamilce: {{ç|ta|}}\n"
        + "* Tarifit: {{ç|rif|}}\n"
        + "* Tatarca: {{ç|tt|küz}}\n"
        + "*:Kırım Tatarcası: {{ç|crh|köz}}\n"
        + "* Tayca: {{ç|th|ตา}}\n"
        + "*: Kuzey Tayca: {{ç|nod|}}\n"
        + "* Telugu dili: {{ç|te|}}\n"
        + "* Tesmence: {{ç|tk|}}\n"
        + "* Tetum: {{ç|tet|}}\n"
        + "* Tibetçe:\n"
        + "*: Klâsik Tibetçe: {{ç|xct|}}\n"
        + "*: Lhasa Tibetçesi: {{ç|bo|}}\n"
        + "* Tigrinya dili: {{ç|ti|}}\n"
        + "* Tok Pisin dili: {{ç|tpi|}}\n"
        + "* Tsonga dili: {{ç|ts|}}\n"
        + "* Tutelo dili: {{ç|tta|}}\n"
        + "* Tupinambá: {{ç|tpn|}}\n"
        + "* Tuvaca: {{ç|tyv|}}\n"
        + "* Türkçe:\n"
        + "*: Eski Türkçe: {{ç|otk|}}\n"
        + "*: Osmanlı Türkçesi: {{ç|ota|گوز}}\n"
        + "* Türkmence: {{ç|tk|göz}}\n"
        + "* Tzotzilce: {{ç|tzo|}}\n"
        + "* Udmurtça: {{ç|udm|син}}\n"
        + "* Ukraynaca: {{ç|uk|око}}\n"
        + "* Ulahça: {{ç|rup|}}\n"
        + "* Umbundu dili: {{ç|umb|}}\n"
        + "* Urduca: {{ç|ur|چشم|tr=t͡ʃeʃm}}\n"
        + "* Uygurca: {{ç|ug|كۆز}}\n"
        + "* Valonca: {{ç|wa|}}\n"
        + "* Venedikçe: {{ç|vec|}}\n"
        + "* Vepsçe: {{ç|vep|}}\n"
        + "* Vietnamca: {{ç|vi|}}\n"
        + "* Vilamov dili: {{ç|wym|}}\n"
        + "* Volapük: {{ç|vo|}}\n"
        + "* Võro dili: {{ç|vro|}}\n"
        + "* Votça: {{ç|vot|}}\n"
        + "* Wolof dili: {{ç|wo|}}\n"
        + "* Xârâcùù dili: {{ç|ane|}}\n"
        + "* Xhosa dili: {{ç|xh|}}\n"
        + "* Yağnupça: {{ç|yai|}}\n"
        + "* Yakutça: {{ç|sah|харах}}\n"
        + "* Yorubaca: {{ç|yo|}}\n"
        + "* Yunanca:\n"
        + "*: Grekçe: {{ç|grc|}}\n"
        + "*:Modern Yunanca: {{ç|el|μάτι}}\n"
        + "* Yupik dili: {{ç|esu|}}\n"
        + "* Zarma dili: {{ç|dje|}}\n"
        + "* Zazaca: {{ç|zza|}}\n"
        + "*: Güney Zazaca: {{ç|diq|}}\n"
        + "*: Kuzey Zazaca: {{ç|kiu|}} \n"
        + "* Zhuangca: {{ç|za|}}\n"
        + "* Zuluca: {{ç|zu|}}\n"
        + "{{Alt}}\n"
        + "\n"
        + "{{Üst|bakış, görüş|tip=çeviriler}}\n"
        + "* Aari dili: {{ç|aiw|}}\n"
        + "* Abhazca: {{ç|ab|}}\n"
        + "* Afarca: {{ç|aa|}}\n"
        + "* Afrikanca: {{ç|af|}}\n"
        + "* Ainu dili: {{ç|ain|}}\n"
        + "* Aiton dili: {{ç|aio|}}\n"
        + "* Akadca: {{ç|akk|}}\n"
        + "* Almanca: {{ç|de|Sicht|f}}, {{ç|de|Sichtweise|f}}\n"
        + "*: Bavyeraca: {{ç|bar|}}\n"
        + "*: İsviçre Almancası: {{ç|gsw|}}\n"
        + "*: Yahudi Almancası: {{ç|yi|}}\n"
        + "* Altayca:\n"
        + "*: Güney Altai: {{ç|alt|}}\n"
        + "*: Kuzey Altayca: {{ç|atv|}}\n"
        + "* Amharca: {{ç|am|}}\n"
        + "* Amorice:\n"
        + "*: Ugaritçe: {{ç|uga|}}\n"
        + "* Aragonca: {{ç|an|}}\n"
        + "* Aramice:\n"
        + "*: Arami: {{ç|arc||tr=|sc=Syrc}}\n"
        + "*: İbrani: {{ç|arc||tr=|sc=Hebr}}\n"
        + "* Arapça: {{ç|ar|}}\n"
        + "*: Cezayir Arapçası: {{ç|arq||tr=aḡrum}}\n"
        + "*: Hicaz Arapçası: {{ç|acw||tr=}}\n"
        + "*: Fas Arapçası: {{ç|ary||tr=}}\n"
        + "*: Irak Arapçası: {{ç|acm||tr=}}\n"
        + "*: Kuzey Levantin Arapçası: {{ç|apc||tr=}}\n"
        + "*: Mısır Arapçası: {{ç|arz||tr=fi|sc=Arab}}\n"
        + "* Argobaca: {{ç|agj||tr=}}\n"
        + "* Arnavutça: {{ç|sq|}}\n"
        + "* Asturyasça: {{ç|ast|}}\n"
        + "* Aşağı Almanca: {{ç|nds|}}\n"
        + "*: Alman Aşağı Almancası: {{ç|nds-de|}}\n"
        + "*:: Plautdieç: {{ç|pdt|}}\n"
        + "*: Hollanda Aşağı Almancası: {{ç|nds-nl|}}\n"
        + "* Aşo dili: {{ç|csh|}}\n"
        + "* Avarca: {{ç|av|}}\n"
        + "* Aymaraca: {{ç|ay|}}\n"
        + "* Azerice: {{ç|az|}}\n"
        + "* Balinezce: {{ç|ban|}}\n"
        + "* Bambara dili: {{ç|bm|}}\n"
        + "* Baskça: {{ç|eu|}}\n"
        + "* Başkurtça: {{ç|ba|}}\n"
        + "* Batsça: {{ç|bbl|}}\n"
        + "* Beluçça: {{ç|bal|}}\n"
        + "* Bengalce: {{ç|bn|}}\n"
        + "* Beyaz Rusça: {{ç|be|}}\n"
        + "* Bikolca:\n"
        + "*: Albay Bikol: {{ç|bhk|}}\n"
        + "*: Merkezî Bikol: {{ç|bcl|}}\n"
        + "*: Pandan Bikol: {{ç|cts|}}\n"
        + "*: Rinkonada Bikol: {{ç|bto|}}\n"
        + "* Birmanca: {{ç|my|}}\n"
        + "* Bretonca: {{ç|br|}}\n"
        + "* Budukça: {{ç|bdk|}}\n"
        + "* Bulgarca: {{ç|bg|}}\n"
        + "* Calo dili: {{ç|rmq|}}\n"
        + "* Cingpo dili: {{ç|kac|}}\n"
        + "* Çeçence: {{ç|ce|}}\n"
        + "* Çekçe: {{ç|cs|}}\n"
        + "* Çerkesçe:\n"
        + "*: Batı Çerkesçesi: {{ç|ady|}}\n"
        + "*: Doğu Çerkesçesi: {{ç|kbd|}}\n"
        + "* Çerokice: {{ç|chr|}}\n"
        + "* Çevaca: {{ç|ny|}}\n"
        + "* Çikasav dili: {{ç|cic|}}\n"
        + "* Çince: {{ç|zh|}}\n"
        + "*: Dungan: {{ç|dng|}}\n"
        + "*: Hakka: {{ç|hak||tr=}}\n"
        + "*: Mandarin: {{ç|cmn||tr=}}\n"
        + "*: Min Bei: {{ç|mnp|}}\n"
        + "*: Min Dong: {{ç|cdo||tr=}}\n"
        + "*: Min Nan: {{ç|nan||tr=}}\n"
        + "*: Wu: {{ç|wuu||tr=}}\n"
        + "*: Yue: {{ç|yue||tr=}}\n"
        + "* Çuvaşça: {{ç|cv|}}\n"
        + "* Dagbanice: {{ç|dag|}}\n"
        + "* Dalmaçyaca: {{ç|dlm|}}\n"
        + "* Danca: {{ç|da|}}\n"
        + "* Dolganca: {{ç|dlg|}}\n"
        + "* Endonezce: {{ç|id|}}\n"
        + "* Ermenice: {{ç|hy|}}\n"
        + "*: Batı Ermenicesi: {{ç|hyw|}}\n"
        + "*: Doğu Ermenicesi: {{ç|hye|}}\n"
        + "*: Eski Ermenice: {{ç|xcl|}}\n"
        + "*: Orta Ermenice: {{ç|axm|}}\n"
        + "* Ersya dili: {{ç|myv|}}\n"
        + "* Eski Norsça: {{ç|non|}}\n"
        + "* Eski Slavca:\n"
        + "*: Eski Doğu Slavcası: {{ç|orv|}}\n"
        + "*: Eski Kilise Slavcası: {{ç|cu|}}\n"
        + "* Esperanto: {{ç|eo|}}\n"
        + "* Estonca: {{ç|et|}}\n"
        + "* Evenkice: {{ç|evn|}}\n"
        + "* Ewece: {{ç|ee|}}\n"
        + "* Farfarca: {{ç|gur|}}\n"
        + "* Faroece: {{ç|fo|}}\n"
        + "* Farsça: {{ç|fa|}}\n"
        + "*: Orta Farsça: {{ç|pal|}}\n"
        + "* Felemenkçe: {{ç|nl|}}\n"
        + "* Fijice: {{ç|fj|}}\n"
        + "* Fince: {{ç|fi|}}\n"
        + "* Frankça: {{ç|frk|}}\n"
        + "* Fransızca: {{ç|fr|}}\n"
        + "* Frigce: {{ç|xpg|}}\n"
        + "* Friuli dili: {{ç|fur|}}\n"
        + "* Frizce:\n"
        + "*: Batı Frizce: {{ç|fy|}}\n"
        + "*: Kuzey Frizce: {{ç|frr|}}\n"
        + "*: Sater Frizcesi: {{ç|stq|}}\n"
        + "* Fulanice:\n"
        + "*: Adlam: {{ç|ff|}}\n"
        + "*: Latin: {{ç|ff|}}\n"
        + "* Furlanca: {{ç|fur|}}\n"
        + "* Gagavuzca: {{ç|gag|}}\n"
        + "* Galce: {{ç|cy|}}\n"
        + "* Galiçyaca: {{ç|gl|}}\n"
        + "* Gilyakça: {{ç|niv|}}\n"
        + "* Gotça: {{ç|got|}}\n"
        + "* Grönlandca: {{ç|kl|}}\n"
        + "* Guanarice: {{ç|gn|}}\n"
        + "* Gucaratça: {{ç|gu|}}\n"
        + "* Gürcüce: {{ç|ka|}}\n"
        + "* Haiti Kreolü: {{ç|ht|}}\n"
        + "* Hausaca: {{ç|ha|}}\n"
        + "* Hawaii dili: {{ç|haw|}}\n"
        + "* Hınalık dili: {{ç|kjj|}}\n"
        + "* Hintçe: {{ç|hi|}}\n"
        + "* Hititçe: {{ç|hit|}}\n"
        + "* Hmong dili: {{ç|hmn|}}\n"
        + "* Hunzib dili: {{ç|huz|}}\n"
        + "* İbranice: {{ç|he|}}\n"
        + "* İdo dili: {{ç|io|}}\n"
        + "* İngilizce: {{ç|en|look}}\n"
        + "*: Eski İngilizce: {{ç|ang|}}\n"
        + "*: İskoç İngilizcesi: {{ç|sco|}}\n"
        + "*: Orta İngilizce: {{ç|enm|}}\n"
        + "* İnterlingua: {{ç|ia|}}\n"
        + "* İnuitçe:\n"
        + "*: Doğu Kanada İnuitçesi: {{ç|iu|}}\n"
        + "* İrlandaca: {{ç|ga|}}\n"
        + "*: Eski İrlandaca: {{ç|sga|}}\n"
        + "* İskoçça: {{ç|gd|}}\n"
        + "* İspanyolca: {{ç|es|}}\n"
        + "*: Yahudi İspanyolcası: {{ç|lad|}}\n"
        + "* İstrioça: {{ç|ist|}}\n"
        + "* İsveççe: {{ç|sv|}}\n"
        + "* İtalyanca: {{ç|it|}}\n"
        + "* İzlandaca: {{ç|is|}}\n"
        + "* Japonca: {{ç|ja|}}\n"
        + "* Kabardeyce: {{ç|kbd|}}\n"
        + "* Kalmukça: {{ç|xal|}}\n"
        + "* Kapampangan dili: {{ç|pam|}}\n"
        + "* Kannada dili: {{ç|kn|}}\n"
        + "* Karaçay-Balkarca: {{ç|krc|}}\n"
        + "* Karakalpakça: {{ç|kaa|}}\n"
        + "* Karayca: {{ç|kdr|}}\n"
        + "* Karipúna Fransız Kreolü: {{ç|kmv|}}\n"
        + "* Kaşupça: {{ç|csb|}}\n"
        + "* Katalanca: {{ç|ca|}}\n"
        + "* Kazakça: {{ç|kk|}}\n"
        + "* Keçuva dili: {{ç|qu|}}\n"
        + "* Kernevekçe: {{ç|kw|}}\n"
        + "* Kıptîce: {{ç|cop|}}\n"
        + "* Kırgızca: {{ç|ky|}}\n"
        + "* Kikuyu dili: {{ç|ki|}}\n"
        + "* Kmer dili: {{ç|km|}}\n"
        + "* Komi dili: {{ç|kv|}}\n"
        + "* Komorca: {{ç|bnt|}}\n"
        + "* Konkani dili: {{ç|kok|}}\n"
        + "* Korece: {{ç|ko|}}\n"
        + "* Korsikaca: {{ç|co|}}\n"
        + "* Koryakça: {{ç|kpy|}}\n"
        + "* Krice: {{ç|cr|}}\n"
        + "*: Ova Kricesi: {{ç|crk|}}\n"
        + "* Kuçean dili: {{ç|txb|}}\n"
        + "* Kumukça: {{ç|kum|}}\n"
        + "* Kürtçe: {{ç|ku|}}\n"
        + "*: Kuzey Kürtçe: {{ç|kmr|}}\n"
        + "*: Lekçe: {{ç|lki|}}\n"
        + "*: Orta Kürtçe: {{ç|ckb|}}\n"
        + "* Ladince: {{ç|lld|}}\n"
        + "* Laoca: {{ç|lo|}}\n"
        + "* Laponca:\n"
        + "*: Akkala: {{ç|sia|}}\n"
        + "*: Güney Laponca: {{ç|sma|}}\n"
        + "*: İnari: {{ç|smn|}}\n"
        + "*: Kemi: {{ç|sjk|}}\n"
        + "*: Kildin: {{ç|sjd|}}\n"
        + "*: Kuzey Laponca: {{ç|sme|}}\n"
        + "*: Lule: {{ç|smj|}}\n"
        + "*: Pite: {{ç|sje|}}\n"
        + "*: Skolt: {{ç|sms|}}\n"
        + "*: Ter: {{ç|sjt|}}\n"
        + "*: Ume: {{ç|sju|}}\n"
        + "* Latgalce: {{ç|ltg|}}\n"
        + "* Latince: {{ç|la|}}\n"
        + "* Lehçe: {{ç|pl|}}\n"
        + "* Letonca: {{ç|lv|}}\n"
        + "* Lezgice: {{ç|lez|}}\n"
        + "* Ligurya dili: {{ç|lij|}}\n"
        + "* Limburgca: {{ç|li|}}\n"
        + "* Lingala: {{ç|ln|}}\n"
        + "* Litvanca: {{ç|lt|}}\n"
        + "* Livonca: {{ç|liv|}}\n"
        + "* Lombardça: {{ç|lmo|}}\n"
        + "* Luhyaca: {{ç|luy|}}\n"
        + "* Luvice:\n"
        + "*: Çivi yazısı: {{ç|xlu|}}\n"
        + "*: Hiyeroglif: {{ç|hlu|}}\n"
        + "* Lüksemburgca: {{ç|lb|}}\n"
        + "* Maasai dili: {{ç|mas|}}\n"
        + "* Macarca: {{ç|hu|}}\n"
        + "* Magindanao dili: {{ç|mdh|}}\n"
        + "* Makedonca: {{ç|mk|}}\n"
        + "* Malayalam dili: {{ç|ml|}}\n"
        + "* Malayca: {{ç|ms|}}\n"
        + "*: Ambonez Malayca: {{ç|abs|}}\n"
        + "* Malgaşça: {{ç|mg|}}\n"
        + "* Maltaca: {{ç|mt|}}\n"
        + "* Mançuca: {{ç|mnc|}}\n"
        + "* Manksça {{ç|gv|}}\n"
        + "* Maorice: {{ç|mi|}}\n"
        + "* Mapudungun dili: {{ç|arn|}}\n"
        + "* Maranao dili: {{ç|mrw|}}\n"
        + "* Marathi dili: {{ç|mr|}}\n"
        + "* Mari dili: {{ç|chm|}}\n"
        + "* Massachusett dili: {{ç|wam|}}\n"
        + "* Mayaca:\n"
        + "*: Yukatek Mayacası: {{ç|yua|}}\n"
        + "* Mbyá Guaraní dili: {{ç|gun|}}\n"
        + "* Merkezî Sierra Miwok dili: {{ç|csm|}}\n"
        + "* Mısırca: {{ç|egy|}}\n"
        + "* Moğolca:\n"
        + "*: Kiril: {{ç|mn|}}\n"
        + "*: Latin: {{ç|mn|}}\n"
        + "* Mòkeno dili: {{ç|mhn|}}\n"
        + "* Mokşa dili: {{ç|mdf|}}\n"
        + "* Moldovaca: {{ç|mo|}}\n"
        + "* Munsee dili: {{ç|umu|}}\n"
        + "* Mwani dili: {{ç|wmw|}}\n"
        + "* Nahuatl dili: {{ç|nah|}}\n"
        + "*: Klâsik Nahuatl dili: {{ç|nci|}}\n"
        + "* Nandi dili: {{ç|kln|}}\n"
        + "* Napolice: {{ç|nap|}}\n"
        + "* Nauruca: {{ç|na|}}\n"
        + "* Navahoca: {{ç|nv|}}\n"
        + "* Nenetsçe: {{ç|yrk|}}\n"
        + "* Nepalce: {{ç|ne|}}\n"
        + "* Nogayca: {{ç|nog|}}\n"
        + "* Normanca: {{ç|nrf|}}\n"
        + "* Norveççe: {{ç|no|}}\n"
        + "*: Bokmål: {{ç|nb|}}\n"
        + "*: Nynorsk: {{ç|nn|}}\n"
        + "* Novial: {{ç|nov|}}\n"
        + "* Ojibvaca: {{ç|oji|}}\n"
        + "* Oksitanca: {{ç|oc|}}\n"
        + "* Orta Atlas Tamazit dili: {{ç|tzm|}}\n"
        + "* Osetçe: {{ç|os|}}\n"
        + "* Övdalca: {{ç|ovd|}}\n"
        + "* Özbekçe: {{ç|uz|}}\n"
        + "* Pali dili: {{ç|pi|}}\n"
        + "* Papiamento: {{ç|pap|}}\n"
        + "* Pencapça: {{ç|pa|}}\n"
        + "*: Batı Pencapça: {{ç|pnb|}}\n"
        + "* Permyakça: {{ç|koi|}}\n"
        + "* Peştuca: {{ç|ps|}}\n"
        + "* Piemontça: {{ç|pms|}}\n"
        + "* Polapça: {{ç|pox|}}\n"
        + "* Portekizce: {{ç|pt|}}\n"
        + "* Rapa Nui dili: {{ç|rap|}}\n"
        + "* Rohingyaca: {{ç|rhg|}}\n"
        + "* Romanca: {{ç|rom|}}\n"
        + "* Romanşça: {{ç|ro|}}\n"
        + "* Ruanda dili: {{ç|rw|}}\n"
        + "* Rumence: {{ç|ro|}}\n"
        + "* Rusça: {{ç|ru|}}\n"
        + "* Rusince: {{ç|rue|}}\n"
        + "* Sabuanca: {{ç|ceb|}}\n"
        + "* Samoaca: {{ç|sm|}}\n"
        + "* Samogitçe: {{ç|sgs|}}\n"
        + "* Sango dili: {{ç|sg|}}\n"
        + "* Sanskritçe: {{ç|sa|}}\n"
        + "* Santalice: {{ç|sat|}}\n"
        + "* Sardunyaca: {{ç|sc|}}\n"
        + "*: Kampidanez Sardunyacası: {{ç|sro|}}\n"
        + "* Savahili dili: {{ç|sw|}}\n"
        + "* Seylanca: {{ç|si|}}\n"
        + "* Sırp-Hırvatça:\n"
        + "*: Kiril: {{ç|sh|}}\n"
        + "*: Latin: {{ç|sh|}}\n"
        + "* Sicilyaca: {{ç|scn|}}\n"
        + "* Sidama dili: {{ç|sid|}}\n"
        + "* Slovakça: {{ç|sk|}}\n"
        + "* Slovence: {{ç|sl|}}\n"
        + "* Somalice: {{ç|so|}}\n"
        + "* Sorbca:\n"
        + "*: Aşağı Sorbca: {{ç|dsb|}}\n"
        + "*: Yukarı Sorbca: {{ç|hsb|}}\n"
        + "* Sotho dili: {{ç|st|}}\n"
        + "* Sunda dili: {{ç|su|genep}}\n"
        + "* Supikçe: {{ç|ems|}}\n"
        + "* Sümerce: {{ç|sux|}}\n"
        + "* Süryanice: {{ç|syc|}}\n"
        + "*: Neo-Süryanice: {{ç|aii||tr=}}\n"
        + "* Savahili: {{ç|sw|}}\n"
        + "* Svanca: {{ç|sva|}}\n"
        + "* Şan dili: {{ç|shn|}}\n"
        + "* Şilha dili: {{ç|shi|}}\n"
        + "* Şona dili: {{ç|sn|}}\n"
        + "* Şorca: {{ç|cjs|}} \n"
        + "* Tabasaranca: {{ç|tab|}}\n"
        + "* Tacikçe: {{ç|tg|}}\n"
        + "* Tagalogca: {{ç|tl|}}\n"
        + "* Tai Lü dili: {{ç|khb|}}\n"
        + "* Tamilce: {{ç|ta|}}\n"
        + "* Tarifit: {{ç|rif|}}\n"
        + "* Tatarca: {{ç|tt|}}\n"
        + "*: Kırım Tatarcası: {{ç|crh|}}\n"
        + "* Tayca: {{ç|th|}}\n"
        + "*: Kuzey Tayca: {{ç|nod|}}\n"
        + "* Telugu dili: {{ç|te|}}\n"
        + "* Tesmence: {{ç|tk|}}\n"
        + "* Tetum: {{ç|tet|}}\n"
        + "* Tibetçe:\n"
        + "*: Klâsik Tibetçe: {{ç|xct|}}\n"
        + "*: Lhasa Tibetçesi: {{ç|bo|}}\n"
        + "* Tigrinya dili: {{ç|ti|}}\n"
        + "* Tok Pisin dili: {{ç|tpi|}}\n"
        + "* Tsonga dili: {{ç|ts|}}\n"
        + "* Tutelo dili: {{ç|tta|}}\n"
        + "* Tupinambá: {{ç|tpn|}}\n"
        + "* Tuvaca: {{ç|tyv|}}\n"
        + "* Türkçe:\n"
        + "*: Eski Türkçe: {{ç|otk|}}\n"
        + "*: Osmanlı Türkçesi: {{ç|ota|گوز}}\n"
        + "* Türkmence: {{ç|tk|}}\n"
        + "* Tzotzilce: {{ç|tzo|}}\n"
        + "* Udmurtça: {{ç|udm|}}\n"
        + "* Ukraynaca: {{ç|uk|}}\n"
        + "* Ulahça: {{ç|rup|}}\n"
        + "* Umbundu dili: {{ç|umb|}}\n"
        + "* Urduca: {{ç|ur|}}\n"
        + "* Uygurca: {{ç|ug|}}\n"
        + "* Valonca: {{ç|wa|}}\n"
        + "* Venedikçe: {{ç|vec|}}\n"
        + "* Vepsçe: {{ç|vep|}}\n"
        + "* Vietnamca: {{ç|vi|}}\n"
        + "* Vilamov dili: {{ç|wym|}}\n"
        + "* Volapük: {{ç|vo|}}\n"
        + "* Võro dili: {{ç|vro|}}\n"
        + "* Votça: {{ç|vot|}}\n"
        + "* Wolof dili: {{ç|wo|}}\n"
        + "* Xârâcùù dili: {{ç|ane|}}\n"
        + "* Xhosa dili: {{ç|xh|}}\n"
        + "* Yağnupça: {{ç|yai|}}\n"
        + "* Yakutça: {{ç|sah|}}\n"
        + "* Yorubaca: {{ç|yo|}}\n"
        + "* Yunanca:\n"
        + "*: Grekçe: {{ç|grc|}}\n"
        + "*: Modern Yunanca: {{ç|el|}}\n"
        + "* Yupik dili: {{ç|esu|}}\n"
        + "* Zarma dili: {{ç|dje|}}\n"
        + "* Zazaca: {{ç|zza|}}\n"
        + "*: Güney Zazaca: {{ç|diq|}}\n"
        + "*: Kuzey Zazaca: {{ç|kiu|}} \n"
        + "* Zhuangca: {{ç|za|}}\n"
        + "* Zuluca: {{ç|zu|}}\n"
        + "{{Alt}}\n"
        + "\n"
        + "{{Üst|boşluk, delik|tip=çeviriler}}\n"
        + "* Aari dili: {{ç|aiw|}}\n"
        + "* Abhazca: {{ç|ab|}}\n"
        + "* Afarca: {{ç|aa|}}\n"
        + "* Afrikanca: {{ç|af|}}\n"
        + "* Ainu dili: {{ç|ain|}}\n"
        + "* Aiton dili: {{ç|aio|}}\n"
        + "* Akadca: {{ç|akk|}}\n"
        + "* Almanca: {{ç|de|Loch|n}}\n"
        + "*: Bavyeraca: {{ç|bar|}}\n"
        + "*: İsviçre Almancası: {{ç|gsw|}}\n"
        + "*: Yahudi Almancası: {{ç|yi|}}\n"
        + "* Altayca:\n"
        + "*: Güney Altai: {{ç|alt|}}\n"
        + "*: Kuzey Altayca: {{ç|atv|}}\n"
        + "* Amharca: {{ç|am|}}\n"
        + "* Amorice:\n"
        + "*: Ugaritçe: {{ç|uga|}}\n"
        + "* Aragonca: {{ç|an|}}\n"
        + "* Aramice:\n"
        + "*: Arami: {{ç|arc||tr=|sc=Syrc}}\n"
        + "*: İbrani: {{ç|arc||tr=|sc=Hebr}}\n"
        + "* Arapça: {{ç|ar|}}\n"
        + "*: Cezayir Arapçası: {{ç|arq||tr=aḡrum}}\n"
        + "*: Hicaz Arapçası: {{ç|acw||tr=}}\n"
        + "*: Fas Arapçası: {{ç|ary||tr=}}\n"
        + "*: Irak Arapçası: {{ç|acm||tr=}}\n"
        + "*: Kuzey Levantin Arapçası: {{ç|apc||tr=}}\n"
        + "*: Mısır Arapçası: {{ç|arz||tr=fi|sc=Arab}}\n"
        + "* Argobaca: {{ç|agj||tr=}}\n"
        + "* Arnavutça: {{ç|sq|}}\n"
        + "* Asturyasça: {{ç|ast|}}\n"
        + "* Aşağı Almanca: {{ç|nds|}}\n"
        + "*: Alman Aşağı Almancası: {{ç|nds-de|}}\n"
        + "*:: Plautdieç: {{ç|pdt|}}\n"
        + "*: Hollanda Aşağı Almancası: {{ç|nds-nl|}}\n"
        + "* Aşo dili: {{ç|csh|}}\n"
        + "* Avarca: {{ç|av|}}\n"
        + "* Aymaraca: {{ç|ay|}}\n"
        + "* Azerice: {{ç|az|}}\n"
        + "* Balinezce: {{ç|ban|}}\n"
        + "* Bambara dili: {{ç|bm|}}\n"
        + "* Baskça: {{ç|eu|}}\n"
        + "* Başkurtça: {{ç|ba|}}\n"
        + "* Batsça: {{ç|bbl|}}\n"
        + "* Beluçça: {{ç|bal|}}\n"
        + "* Bengalce: {{ç|bn|}}\n"
        + "* Beyaz Rusça: {{ç|be|}}\n"
        + "* Bikolca:\n"
        + "*: Albay Bikol: {{ç|bhk|}}\n"
        + "*: Merkezî Bikol: {{ç|bcl|}}\n"
        + "*: Pandan Bikol: {{ç|cts|}}\n"
        + "*: Rinkonada Bikol: {{ç|bto|}}\n"
        + "* Birmanca: {{ç|my|}}\n"
        + "* Bretonca: {{ç|br|}}\n"
        + "* Budukça: {{ç|bdk|}}\n"
        + "* Bulgarca: {{ç|bg|}}\n"
        + "* Calo dili: {{ç|rmq|}}\n"
        + "* Cingpo dili: {{ç|kac|}}\n"
        + "* Çeçence: {{ç|ce|}}\n"
        + "* Çekçe: {{ç|cs|}}\n"
        + "* Çerkesçe:\n"
        + "*: Batı Çerkesçesi: {{ç|ady|}}\n"
        + "*: Doğu Çerkesçesi: {{ç|kbd|}}\n"
        + "* Çerokice: {{ç|chr|}}\n"
        + "* Çevaca: {{ç|ny|}}\n"
        + "* Çikasav dili: {{ç|cic|}}\n"
        + "* Çince: {{ç|zh|}}\n"
        + "*: Dungan: {{ç|dng|}}\n"
        + "*: Hakka: {{ç|hak||tr=}}\n"
        + "*: Mandarin: {{ç|cmn||tr=}}\n"
        + "*: Min Bei: {{ç|mnp|}}\n"
        + "*: Min Dong: {{ç|cdo||tr=}}\n"
        + "*: Min Nan: {{ç|nan||tr=}}\n"
        + "*: Wu: {{ç|wuu||tr=}}\n"
        + "*: Yue: {{ç|yue||tr=}}\n"
        + "* Çuvaşça: {{ç|cv|}}\n"
        + "* Dagbanice: {{ç|dag|}}\n"
        + "* Dalmaçyaca: {{ç|dlm|}}\n"
        + "* Danca: {{ç|da|}}\n"
        + "* Dolganca: {{ç|dlg|}}\n"
        + "* Endonezce: {{ç|id|}}\n"
        + "* Ermenice: {{ç|hy|}}\n"
        + "*: Batı Ermenicesi: {{ç|hyw|}}\n"
        + "*: Doğu Ermenicesi: {{ç|hye|}}\n"
        + "*: Eski Ermenice: {{ç|xcl|}}\n"
        + "*: Orta Ermenice: {{ç|axm|}}\n"
        + "* Ersya dili: {{ç|myv|}}\n"
        + "* Eski Norsça: {{ç|non|}}\n"
        + "* Eski Slavca:\n"
        + "*: Eski Doğu Slavcası: {{ç|orv|}}\n"
        + "*: Eski Kilise Slavcası: {{ç|cu|}}\n"
        + "* Esperanto: {{ç|eo|}}\n"
        + "* Estonca: {{ç|et|}}\n"
        + "* Evenkice: {{ç|evn|}}\n"
        + "* Ewece: {{ç|ee|}}\n"
        + "* Farfarca: {{ç|gur|}}\n"
        + "* Faroece: {{ç|fo|}}\n"
        + "* Farsça: {{ç|fa|}}\n"
        + "*: Orta Farsça: {{ç|pal|}}\n"
        + "* Felemenkçe: {{ç|nl|}}\n"
        + "* Fijice: {{ç|fj|}}\n"
        + "* Fince: {{ç|fi|}}\n"
        + "* Frankça: {{ç|frk|}}\n"
        + "* Fransızca: {{ç|fr|}}\n"
        + "* Frigce: {{ç|xpg|}}\n"
        + "* Friuli dili: {{ç|fur|}}\n"
        + "* Frizce:\n"
        + "*: Batı Frizce: {{ç|fy|}}\n"
        + "*: Kuzey Frizce: {{ç|frr|}}\n"
        + "*: Sater Frizcesi: {{ç|stq|}}\n"
        + "* Fulanice:\n"
        + "*: Adlam: {{ç|ff|}}\n"
        + "*: Latin: {{ç|ff|}}\n"
        + "* Furlanca: {{ç|fur|}}\n"
        + "* Gagavuzca: {{ç|gag|}}\n"
        + "* Galce: {{ç|cy|}}\n"
        + "* Galiçyaca: {{ç|gl|}}\n"
        + "* Gilyakça: {{ç|niv|}}\n"
        + "* Gotça: {{ç|got|}}\n"
        + "* Grönlandca: {{ç|kl|}}\n"
        + "* Guanarice: {{ç|gn|}}\n"
        + "* Gucaratça: {{ç|gu|}}\n"
        + "* Gürcüce: {{ç|ka|}}\n"
        + "* Haiti Kreolü: {{ç|ht|}}\n"
        + "* Hausaca: {{ç|ha|}}\n"
        + "* Hawaii dili: {{ç|haw|}}\n"
        + "* Hınalık dili: {{ç|kjj|}}\n"
        + "* Hintçe: {{ç|hi|}}\n"
        + "* Hititçe: {{ç|hit|}}\n"
        + "* Hmong dili: {{ç|hmn|}}\n"
        + "* Hunzib dili: {{ç|huz|}}\n"
        + "* İbranice: {{ç|he|}}\n"
        + "* İdo dili: {{ç|io|}}\n"
        + "* İngilizce: {{ç|en|hole}}\n"
        + "*: Eski İngilizce: {{ç|ang|}}\n"
        + "*: İskoç İngilizcesi: {{ç|sco|}}\n"
        + "*: Orta İngilizce: {{ç|enm|}}\n"
        + "* İnterlingua: {{ç|ia|}}\n"
        + "* İnuitçe:\n"
        + "*: Doğu Kanada İnuitçesi: {{ç|iu|}}\n"
        + "* İrlandaca: {{ç|ga|}}\n"
        + "*: Eski İrlandaca: {{ç|sga|}}\n"
        + "* İskoçça: {{ç|gd|}}\n"
        + "* İspanyolca: {{ç|es|}}\n"
        + "*: Yahudi İspanyolcası: {{ç|lad|}}\n"
        + "* İstrioça: {{ç|ist|}}\n"
        + "* İsveççe: {{ç|sv|}}\n"
        + "* İtalyanca: {{ç|it|}}\n"
        + "* İzlandaca: {{ç|is|}}\n"
        + "* Japonca: {{ç|ja|}}\n"
        + "* Kabardeyce: {{ç|kbd|}}\n"
        + "* Kalmukça: {{ç|xal|}}\n"
        + "* Kapampangan dili: {{ç|pam|}}\n"
        + "* Kannada dili: {{ç|kn|}}\n"
        + "* Karaçay-Balkarca: {{ç|krc|}}\n"
        + "* Karakalpakça: {{ç|kaa|}}\n"
        + "* Karayca: {{ç|kdr|}}\n"
        + "* Karipúna Fransız Kreolü: {{ç|kmv|}}\n"
        + "* Kaşupça: {{ç|csb|}}\n"
        + "* Katalanca: {{ç|ca|}}\n"
        + "* Kazakça: {{ç|kk|}}\n"
        + "* Keçuva dili: {{ç|qu|}}\n"
        + "* Kernevekçe: {{ç|kw|}}\n"
        + "* Kıptîce: {{ç|cop|}}\n"
        + "* Kırgızca: {{ç|ky|}}\n"
        + "* Kikuyu dili: {{ç|ki|}}\n"
        + "* Kmer dili: {{ç|km|}}\n"
        + "* Komi dili: {{ç|kv|}}\n"
        + "* Komorca: {{ç|bnt|}}\n"
        + "* Konkani dili: {{ç|kok|}}\n"
        + "* Korece: {{ç|ko|}}\n"
        + "* Korsikaca: {{ç|co|}}\n"
        + "* Koryakça: {{ç|kpy|}}\n"
        + "* Krice: {{ç|cr|}}\n"
        + "*: Ova Kricesi: {{ç|crk|}}\n"
        + "* Kuçean dili: {{ç|txb|}}\n"
        + "* Kumukça: {{ç|kum|}}\n"
        + "* Kürtçe: {{ç|ku|}}\n"
        + "*: Kuzey Kürtçe: {{ç|kmr|}}\n"
        + "*: Lekçe: {{ç|lki|}}\n"
        + "*: Orta Kürtçe: {{ç|ckb|}}\n"
        + "* Ladince: {{ç|lld|}}\n"
        + "* Laoca: {{ç|lo|}}\n"
        + "* Laponca:\n"
        + "*: Akkala: {{ç|sia|}}\n"
        + "*: Güney Laponca: {{ç|sma|}}\n"
        + "*: İnari: {{ç|smn|}}\n"
        + "*: Kemi: {{ç|sjk|}}\n"
        + "*: Kildin: {{ç|sjd|}}\n"
        + "*: Kuzey Laponca: {{ç|sme|}}\n"
        + "*: Lule: {{ç|smj|}}\n"
        + "*: Pite: {{ç|sje|}}\n"
        + "*: Skolt: {{ç|sms|}}\n"
        + "*: Ter: {{ç|sjt|}}\n"
        + "*: Ume: {{ç|sju|}}\n"
        + "* Latgalce: {{ç|ltg|}}\n"
        + "* Latince: {{ç|la|}}\n"
        + "* Lehçe: {{ç|pl|}}\n"
        + "* Letonca: {{ç|lv|}}\n"
        + "* Lezgice: {{ç|lez|}}\n"
        + "* Ligurya dili: {{ç|lij|}}\n"
        + "* Limburgca: {{ç|li|}}\n"
        + "* Lingala: {{ç|ln|}}\n"
        + "* Litvanca: {{ç|lt|}}\n"
        + "* Livonca: {{ç|liv|}}\n"
        + "* Lombardça: {{ç|lmo|}}\n"
        + "* Luhyaca: {{ç|luy|}}\n"
        + "* Luvice:\n"
        + "*: Çivi yazısı: {{ç|xlu|}}\n"
        + "*: Hiyeroglif: {{ç|hlu|}}\n"
        + "* Lüksemburgca: {{ç|lb|}}\n"
        + "* Maasai dili: {{ç|mas|}}\n"
        + "* Macarca: {{ç|hu|}}\n"
        + "* Magindanao dili: {{ç|mdh|}}\n"
        + "* Makedonca: {{ç|mk|}}\n"
        + "* Malayalam dili: {{ç|ml|}}\n"
        + "* Malayca: {{ç|ms|}}\n"
        + "*: Ambonez Malayca: {{ç|abs|}}\n"
        + "* Malgaşça: {{ç|mg|}}\n"
        + "* Maltaca: {{ç|mt|}}\n"
        + "* Mançuca: {{ç|mnc|}}\n"
        + "* Manksça {{ç|gv|}}\n"
        + "* Maorice: {{ç|mi|}}\n"
        + "* Mapudungun dili: {{ç|arn|}}\n"
        + "* Maranao dili: {{ç|mrw|}}\n"
        + "* Marathi dili: {{ç|mr|}}\n"
        + "* Mari dili: {{ç|chm|}}\n"
        + "* Massachusett dili: {{ç|wam|}}\n"
        + "* Mayaca:\n"
        + "*: Yukatek Mayacası: {{ç|yua|}}\n"
        + "* Mbyá Guaraní dili: {{ç|gun|}}\n"
        + "* Merkezî Sierra Miwok dili: {{ç|csm|}}\n"
        + "* Mısırca: {{ç|egy|}}\n"
        + "* Moğolca:\n"
        + "*: Kiril: {{ç|mn|}}\n"
        + "*: Latin: {{ç|mn|}}\n"
        + "* Mòkeno dili: {{ç|mhn|}}\n"
        + "* Mokşa dili: {{ç|mdf|}}\n"
        + "* Moldovaca: {{ç|mo|}}\n"
        + "* Munsee dili: {{ç|umu|}}\n"
        + "* Mwani dili: {{ç|wmw|}}\n"
        + "* Nahuatl dili: {{ç|nah|}}\n"
        + "*: Klâsik Nahuatl dili: {{ç|nci|}}\n"
        + "* Nandi dili: {{ç|kln|}}\n"
        + "* Napolice: {{ç|nap|}}\n"
        + "* Nauruca: {{ç|na|}}\n"
        + "* Navahoca: {{ç|nv|}}\n"
        + "* Nenetsçe: {{ç|yrk|}}\n"
        + "* Nepalce: {{ç|ne|}}\n"
        + "* Nogayca: {{ç|nog|}}\n"
        + "* Normanca: {{ç|nrf|}}\n"
        + "* Norveççe: {{ç|no|}}\n"
        + "*: Bokmål: {{ç|nb|}}\n"
        + "*: Nynorsk: {{ç|nn|}}\n"
        + "* Novial: {{ç|nov|}}\n"
        + "* Ojibvaca: {{ç|oji|}}\n"
        + "* Oksitanca: {{ç|oc|}}\n"
        + "* Orta Atlas Tamazit dili: {{ç|tzm|}}\n"
        + "* Osetçe: {{ç|os|}}\n"
        + "* Övdalca: {{ç|ovd|}}\n"
        + "* Özbekçe: {{ç|uz|}}\n"
        + "* Pali dili: {{ç|pi|}}\n"
        + "* Papiamento: {{ç|pap|}}\n"
        + "* Pencapça: {{ç|pa|}}\n"
        + "*: Batı Pencapça: {{ç|pnb|}}\n"
        + "* Permyakça: {{ç|koi|}}\n"
        + "* Peştuca: {{ç|ps|}}\n"
        + "* Piemontça: {{ç|pms|}}\n"
        + "* Polapça: {{ç|pox|}}\n"
        + "* Portekizce: {{ç|pt|}}\n"
        + "* Rapa Nui dili: {{ç|rap|}}\n"
        + "* Rohingyaca: {{ç|rhg|}}\n"
        + "* Romanca: {{ç|rom|}}\n"
        + "* Romanşça: {{ç|ro|}}\n"
        + "* Ruanda dili: {{ç|rw|}}\n"
        + "* Rumence: {{ç|ro|}}\n"
        + "* Rusça: {{ç|ru|}}\n"
        + "* Rusince: {{ç|rue|}}\n"
        + "* Sabuanca: {{ç|ceb|}}\n"
        + "* Samoaca: {{ç|sm|}}\n"
        + "* Samogitçe: {{ç|sgs|}}\n"
        + "* Sango dili: {{ç|sg|}}\n"
        + "* Sanskritçe: {{ç|sa|}}\n"
        + "* Santalice: {{ç|sat|}}\n"
        + "* Sardunyaca: {{ç|sc|}}\n"
        + "*: Kampidanez Sardunyacası: {{ç|sro|}}\n"
        + "* Savahili dili: {{ç|sw|}}\n"
        + "* Seylanca: {{ç|si|}}\n"
        + "* Sırp-Hırvatça:\n"
        + "*: Kiril: {{ç|sh|}}\n"
        + "*: Latin: {{ç|sh|}}\n"
        + "* Sicilyaca: {{ç|scn|}}\n"
        + "* Sidama dili: {{ç|sid|}}\n"
        + "* Slovakça: {{ç|sk|}}\n"
        + "* Slovence: {{ç|sl|}}\n"
        + "* Somalice: {{ç|so|}}\n"
        + "* Sorbca:\n"
        + "*: Aşağı Sorbca: {{ç|dsb|}}\n"
        + "*: Yukarı Sorbca: {{ç|hsb|}}\n"
        + "* Sotho dili: {{ç|st|}}\n"
        + "* Sunda dili: {{ç|su|genep}}\n"
        + "* Supikçe: {{ç|ems|}}\n"
        + "* Sümerce: {{ç|sux|}}\n"
        + "* Süryanice: {{ç|syc|}}\n"
        + "*: Neo-Süryanice: {{ç|aii||tr=}}\n"
        + "* Savahili: {{ç|sw|}}\n"
        + "* Svanca: {{ç|sva|}}\n"
        + "* Şan dili: {{ç|shn|}}\n"
        + "* Şilha dili: {{ç|shi|}}\n"
        + "* Şona dili: {{ç|sn|}}\n"
        + "* Şorca: {{ç|cjs|}} \n"
        + "* Tabasaranca: {{ç|tab|}}\n"
        + "* Tacikçe: {{ç|tg|}}\n"
        + "* Tagalogca: {{ç|tl|}}\n"
        + "* Tai Lü dili: {{ç|khb|}}\n"
        + "* Tamilce: {{ç|ta|}}\n"
        + "* Tarifit: {{ç|rif|}}\n"
        + "* Tatarca: {{ç|tt|}}\n"
        + "*: Kırım Tatarcası: {{ç|crh|}}\n"
        + "* Tayca: {{ç|th|}}\n"
        + "*: Kuzey Tayca: {{ç|nod|}}\n"
        + "* Telugu dili: {{ç|te|}}\n"
        + "* Tesmence: {{ç|tk|}}\n"
        + "* Tetum: {{ç|tet|}}\n"
        + "* Tibetçe:\n"
        + "*: Klâsik Tibetçe: {{ç|xct|}}\n"
        + "*: Lhasa Tibetçesi: {{ç|bo|}}\n"
        + "* Tigrinya dili: {{ç|ti|}}\n"
        + "* Tok Pisin dili: {{ç|tpi|}}\n"
        + "* Tsonga dili: {{ç|ts|}}\n"
        + "* Tutelo dili: {{ç|tta|}}\n"
        + "* Tupinambá: {{ç|tpn|}}\n"
        + "* Tuvaca: {{ç|tyv|}}\n"
        + "* Türkçe:\n"
        + "*: Eski Türkçe: {{ç|otk|}}\n"
        + "*: Osmanlı Türkçesi: {{ç|ota|گوز}}\n"
        + "* Türkmence: {{ç|tk|}}\n"
        + "* Tzotzilce: {{ç|tzo|}}\n"
        + "* Udmurtça: {{ç|udm|}}\n"
        + "* Ukraynaca: {{ç|uk|}}\n"
        + "* Ulahça: {{ç|rup|}}\n"
        + "* Umbundu dili: {{ç|umb|}}\n"
        + "* Urduca: {{ç|ur|}}\n"
        + "* Uygurca: {{ç|ug|}}\n"
        + "* Valonca: {{ç|wa|}}\n"
        + "* Venedikçe: {{ç|vec|}}\n"
        + "* Vepsçe: {{ç|vep|}}\n"
        + "* Vietnamca: {{ç|vi|}}\n"
        + "* Vilamov dili: {{ç|wym|}}\n"
        + "* Volapük: {{ç|vo|}}\n"
        + "* Võro dili: {{ç|vro|}}\n"
        + "* Votça: {{ç|vot|}}\n"
        + "* Wolof dili: {{ç|wo|}}\n"
        + "* Xârâcùù dili: {{ç|ane|}}\n"
        + "* Xhosa dili: {{ç|xh|}}\n"
        + "* Yağnupça: {{ç|yai|}}\n"
        + "* Yakutça: {{ç|sah|}}\n"
        + "* Yorubaca: {{ç|yo|}}\n"
        + "* Yunanca:\n"
        + "*: Grekçe: {{ç|grc|}}\n"
        + "*: Modern Yunanca: {{ç|el|}}\n"
        + "* Yupik dili: {{ç|esu|}}\n"
        + "* Zarma dili: {{ç|dje|}}\n"
        + "* Zazaca: {{ç|zza|}}\n"
        + "*: Güney Zazaca: {{ç|diq|}}\n"
        + "*: Kuzey Zazaca: {{ç|kiu|}} \n"
        + "* Zhuangca: {{ç|za|}}\n"
        + "* Zuluca: {{ç|zu|}}\n"
        + "{{Alt}}\n"
        + "\n"
        + "{{Üst|çekmece|tip=çeviriler}}\n"
        + "* Aari dili: {{ç|aiw|}}\n"
        + "* Abhazca: {{ç|ab|}}\n"
        + "* Afarca: {{ç|aa|}}\n"
        + "* Afrikanca: {{ç|af|}}\n"
        + "* Ainu dili: {{ç|ain|}}\n"
        + "* Aiton dili: {{ç|aio|}}\n"
        + "* Akadca: {{ç|akk|}}\n"
        + "* Almanca: {{ç|de|Lade|f}}  {{şerh|kısası}}, {{ç|de|Schublade|f}}\n"
        + "*: Bavyeraca: {{ç|bar|}}\n"
        + "*: İsviçre Almancası: {{ç|gsw|}}\n"
        + "*: Yahudi Almancası: {{ç|yi|}}\n"
        + "* Altayca:\n"
        + "*: Güney Altai: {{ç|alt|}}\n"
        + "*: Kuzey Altayca: {{ç|atv|}}\n"
        + "* Amharca: {{ç|am|}}\n"
        + "* Amorice:\n"
        + "*: Ugaritçe: {{ç|uga|}}\n"
        + "* Aragonca: {{ç|an|}}\n"
        + "* Aramice:\n"
        + "*: Arami: {{ç|arc||tr=|sc=Syrc}}\n"
        + "*: İbrani: {{ç|arc||tr=|sc=Hebr}}\n"
        + "* Arapça: {{ç|ar|}}\n"
        + "*: Cezayir Arapçası: {{ç|arq||tr=aḡrum}}\n"
        + "*: Hicaz Arapçası: {{ç|acw||tr=}}\n"
        + "*: Fas Arapçası: {{ç|ary||tr=}}\n"
        + "*: Irak Arapçası: {{ç|acm||tr=}}\n"
        + "*: Kuzey Levantin Arapçası: {{ç|apc||tr=}}\n"
        + "*: Mısır Arapçası: {{ç|arz||tr=fi|sc=Arab}}\n"
        + "* Argobaca: {{ç|agj||tr=}}\n"
        + "* Arnavutça: {{ç|sq|}}\n"
        + "* Asturyasça: {{ç|ast|}}\n"
        + "* Aşağı Almanca: {{ç|nds|}}\n"
        + "*: Alman Aşağı Almancası: {{ç|nds-de|}}\n"
        + "*:: Plautdieç: {{ç|pdt|}}\n"
        + "*: Hollanda Aşağı Almancası: {{ç|nds-nl|}}\n"
        + "* Aşo dili: {{ç|csh|}}\n"
        + "* Avarca: {{ç|av|}}\n"
        + "* Aymaraca: {{ç|ay|}}\n"
        + "* Azerice: {{ç|az|}}\n"
        + "* Balinezce: {{ç|ban|}}\n"
        + "* Bambara dili: {{ç|bm|}}\n"
        + "* Baskça: {{ç|eu|}}\n"
        + "* Başkurtça: {{ç|ba|}}\n"
        + "* Batsça: {{ç|bbl|}}\n"
        + "* Beluçça: {{ç|bal|}}\n"
        + "* Bengalce: {{ç|bn|}}\n"
        + "* Beyaz Rusça: {{ç|be|}}\n"
        + "* Bikolca:\n"
        + "*: Albay Bikol: {{ç|bhk|}}\n"
        + "*: Merkezî Bikol: {{ç|bcl|}}\n"
        + "*: Pandan Bikol: {{ç|cts|}}\n"
        + "*: Rinkonada Bikol: {{ç|bto|}}\n"
        + "* Birmanca: {{ç|my|}}\n"
        + "* Bretonca: {{ç|br|}}\n"
        + "* Budukça: {{ç|bdk|}}\n"
        + "* Bulgarca: {{ç|bg|}}\n"
        + "* Calo dili: {{ç|rmq|}}\n"
        + "* Cingpo dili: {{ç|kac|}}\n"
        + "* Çeçence: {{ç|ce|}}\n"
        + "* Çekçe: {{ç|cs|}}\n"
        + "* Çerkesçe:\n"
        + "*: Batı Çerkesçesi: {{ç|ady|}}\n"
        + "*: Doğu Çerkesçesi: {{ç|kbd|}}\n"
        + "* Çerokice: {{ç|chr|}}\n"
        + "* Çevaca: {{ç|ny|}}\n"
        + "* Çikasav dili: {{ç|cic|}}\n"
        + "* Çince: {{ç|zh|}}\n"
        + "*: Dungan: {{ç|dng|}}\n"
        + "*: Hakka: {{ç|hak||tr=}}\n"
        + "*: Mandarin: {{ç|cmn||tr=}}\n"
        + "*: Min Bei: {{ç|mnp|}}\n"
        + "*: Min Dong: {{ç|cdo||tr=}}\n"
        + "*: Min Nan: {{ç|nan||tr=}}\n"
        + "*: Wu: {{ç|wuu||tr=}}\n"
        + "*: Yue: {{ç|yue||tr=}}\n"
        + "* Çuvaşça: {{ç|cv|}}\n"
        + "* Dagbanice: {{ç|dag|}}\n"
        + "* Dalmaçyaca: {{ç|dlm|}}\n"
        + "* Danca: {{ç|da|}}\n"
        + "* Dolganca: {{ç|dlg|}}\n"
        + "* Endonezce: {{ç|id|}}\n"
        + "* Ermenice: {{ç|hy|}}\n"
        + "*: Batı Ermenicesi: {{ç|hyw|}}\n"
        + "*: Doğu Ermenicesi: {{ç|hye|}}\n"
        + "*: Eski Ermenice: {{ç|xcl|}}\n"
        + "*: Orta Ermenice: {{ç|axm|}}\n"
        + "* Ersya dili: {{ç|myv|}}\n"
        + "* Eski Norsça: {{ç|non|}}\n"
        + "* Eski Slavca:\n"
        + "*: Eski Doğu Slavcası: {{ç|orv|}}\n"
        + "*: Eski Kilise Slavcası: {{ç|cu|}}\n"
        + "* Esperanto: {{ç|eo|}}\n"
        + "* Estonca: {{ç|et|}}\n"
        + "* Evenkice: {{ç|evn|}}\n"
        + "* Ewece: {{ç|ee|}}\n"
        + "* Farfarca: {{ç|gur|}}\n"
        + "* Faroece: {{ç|fo|}}\n"
        + "* Farsça: {{ç|fa|}}\n"
        + "*: Orta Farsça: {{ç|pal|}}\n"
        + "* Felemenkçe: {{ç|nl|lade}}\n"
        + "* Fijice: {{ç|fj|}}\n"
        + "* Fince: {{ç|fi|vetolaatikko}}\n"
        + "* Frankça: {{ç|frk|}}\n"
        + "* Fransızca: {{ç|fr|}}\n"
        + "* Frigce: {{ç|xpg|}}\n"
        + "* Friuli dili: {{ç|fur|}}\n"
        + "* Frizce:\n"
        + "*: Batı Frizce: {{ç|fy|}}\n"
        + "*: Kuzey Frizce: {{ç|frr|}}\n"
        + "*: Sater Frizcesi: {{ç|stq|}}\n"
        + "* Fulanice:\n"
        + "*: Adlam: {{ç|ff|}}\n"
        + "*: Latin: {{ç|ff|}}\n"
        + "* Furlanca: {{ç|fur|}}\n"
        + "* Gagavuzca: {{ç|gag|}}\n"
        + "* Galce: {{ç|cy|}}\n"
        + "* Galiçyaca: {{ç|gl|}}\n"
        + "* Gilyakça: {{ç|niv|}}\n"
        + "* Gotça: {{ç|got|}}\n"
        + "* Grönlandca: {{ç|kl|}}\n"
        + "* Guanarice: {{ç|gn|}}\n"
        + "* Gucaratça: {{ç|gu|}}\n"
        + "* Gürcüce: {{ç|ka|}}\n"
        + "* Haiti Kreolü: {{ç|ht|}}\n"
        + "* Hausaca: {{ç|ha|}}\n"
        + "* Hawaii dili: {{ç|haw|}}\n"
        + "* Hınalık dili: {{ç|kjj|}}\n"
        + "* Hintçe: {{ç|hi|}}\n"
        + "* Hititçe: {{ç|hit|}}\n"
        + "* Hmong dili: {{ç|hmn|}}\n"
        + "* Hunzib dili: {{ç|huz|}}\n"
        + "* İbranice: {{ç|he|}}\n"
        + "* İdo dili: {{ç|io|}}\n"
        + "* İngilizce: {{ç|en|drawer}}\n"
        + "*: Eski İngilizce: {{ç|ang|}}\n"
        + "*: İskoç İngilizcesi: {{ç|sco|}}\n"
        + "*: Orta İngilizce: {{ç|enm|}}\n"
        + "* İnterlingua: {{ç|ia|}}\n"
        + "* İnuitçe:\n"
        + "*: Doğu Kanada İnuitçesi: {{ç|iu|}}\n"
        + "* İrlandaca: {{ç|ga|}}\n"
        + "*: Eski İrlandaca: {{ç|sga|}}\n"
        + "* İskoçça: {{ç|gd|}}\n"
        + "* İspanyolca: {{ç|es|cajón}}\n"
        + "*: Yahudi İspanyolcası: {{ç|lad|}}\n"
        + "* İstrioça: {{ç|ist|}}\n"
        + "* İsveççe: {{ç|sv|}}\n"
        + "* İtalyanca: {{ç|it|}}\n"
        + "* İzlandaca: {{ç|is|}}\n"
        + "* Japonca: {{ç|ja|}}\n"
        + "* Kabardeyce: {{ç|kbd|}}\n"
        + "* Kalmukça: {{ç|xal|}}\n"
        + "* Kapampangan dili: {{ç|pam|}}\n"
        + "* Kannada dili: {{ç|kn|}}\n"
        + "* Karaçay-Balkarca: {{ç|krc|}}\n"
        + "* Karakalpakça: {{ç|kaa|}}\n"
        + "* Karayca: {{ç|kdr|}}\n"
        + "* Karipúna Fransız Kreolü: {{ç|kmv|}}\n"
        + "* Kaşupça: {{ç|csb|}}\n"
        + "* Katalanca: {{ç|ca|}}\n"
        + "* Kazakça: {{ç|kk|}}\n"
        + "* Keçuva dili: {{ç|qu|}}\n"
        + "* Kernevekçe: {{ç|kw|}}\n"
        + "* Kıptîce: {{ç|cop|}}\n"
        + "* Kırgızca: {{ç|ky|}}\n"
        + "* Kikuyu dili: {{ç|ki|}}\n"
        + "* Kmer dili: {{ç|km|}}\n"
        + "* Komi dili: {{ç|kv|}}\n"
        + "* Komorca: {{ç|bnt|}}\n"
        + "* Konkani dili: {{ç|kok|}}\n"
        + "* Korece: {{ç|ko|}}\n"
        + "* Korsikaca: {{ç|co|}}\n"
        + "* Koryakça: {{ç|kpy|}}\n"
        + "* Krice: {{ç|cr|}}\n"
        + "*: Ova Kricesi: {{ç|crk|}}\n"
        + "* Kuçean dili: {{ç|txb|}}\n"
        + "* Kumukça: {{ç|kum|}}\n"
        + "* Kürtçe: {{ç|ku|}}\n"
        + "*: Kuzey Kürtçe: {{ç|kmr|}}\n"
        + "*: Lekçe: {{ç|lki|}}\n"
        + "*: Orta Kürtçe: {{ç|ckb|}}\n"
        + "* Ladince: {{ç|lld|}}\n"
        + "* Laoca: {{ç|lo|}}\n"
        + "* Laponca:\n"
        + "*: Akkala: {{ç|sia|}}\n"
        + "*: Güney Laponca: {{ç|sma|}}\n"
        + "*: İnari: {{ç|smn|}}\n"
        + "*: Kemi: {{ç|sjk|}}\n"
        + "*: Kildin: {{ç|sjd|}}\n"
        + "*: Kuzey Laponca: {{ç|sme|}}\n"
        + "*: Lule: {{ç|smj|}}\n"
        + "*: Pite: {{ç|sje|}}\n"
        + "*: Skolt: {{ç|sms|}}\n"
        + "*: Ter: {{ç|sjt|}}\n"
        + "*: Ume: {{ç|sju|}}\n"
        + "* Latgalce: {{ç|ltg|}}\n"
        + "* Latince: {{ç|la|}}\n"
        + "* Lehçe: {{ç|pl|}}\n"
        + "* Letonca: {{ç|lv|}}\n"
        + "* Lezgice: {{ç|lez|}}\n"
        + "* Ligurya dili: {{ç|lij|}}\n"
        + "* Limburgca: {{ç|li|}}\n"
        + "* Lingala: {{ç|ln|}}\n"
        + "* Litvanca: {{ç|lt|}}\n"
        + "* Livonca: {{ç|liv|}}\n"
        + "* Lombardça: {{ç|lmo|}}\n"
        + "* Luhyaca: {{ç|luy|}}\n"
        + "* Luvice:\n"
        + "*: Çivi yazısı: {{ç|xlu|}}\n"
        + "*: Hiyeroglif: {{ç|hlu|}}\n"
        + "* Lüksemburgca: {{ç|lb|}}\n"
        + "* Maasai dili: {{ç|mas|}}\n"
        + "* Macarca: {{ç|hu|}}\n"
        + "* Magindanao dili: {{ç|mdh|}}\n"
        + "* Makedonca: {{ç|mk|}}\n"
        + "* Malayalam dili: {{ç|ml|}}\n"
        + "* Malayca: {{ç|ms|}}\n"
        + "*: Ambonez Malayca: {{ç|abs|}}\n"
        + "* Malgaşça: {{ç|mg|}}\n"
        + "* Maltaca: {{ç|mt|}}\n"
        + "* Mançuca: {{ç|mnc|}}\n"
        + "* Manksça {{ç|gv|}}\n"
        + "* Maorice: {{ç|mi|}}\n"
        + "* Mapudungun dili: {{ç|arn|}}\n"
        + "* Maranao dili: {{ç|mrw|}}\n"
        + "* Marathi dili: {{ç|mr|}}\n"
        + "* Mari dili: {{ç|chm|}}\n"
        + "* Massachusett dili: {{ç|wam|}}\n"
        + "* Mayaca:\n"
        + "*: Yukatek Mayacası: {{ç|yua|}}\n"
        + "* Mbyá Guaraní dili: {{ç|gun|}}\n"
        + "* Merkezî Sierra Miwok dili: {{ç|csm|}}\n"
        + "* Mısırca: {{ç|egy|}}\n"
        + "* Moğolca:\n"
        + "*: Kiril: {{ç|mn|}}\n"
        + "*: Latin: {{ç|mn|}}\n"
        + "* Mòkeno dili: {{ç|mhn|}}\n"
        + "* Mokşa dili: {{ç|mdf|}}\n"
        + "* Moldovaca: {{ç|mo|}}\n"
        + "* Munsee dili: {{ç|umu|}}\n"
        + "* Mwani dili: {{ç|wmw|}}\n"
        + "* Nahuatl dili: {{ç|nah|}}\n"
        + "*: Klâsik Nahuatl dili: {{ç|nci|}}\n"
        + "* Nandi dili: {{ç|kln|}}\n"
        + "* Napolice: {{ç|nap|}}\n"
        + "* Nauruca: {{ç|na|}}\n"
        + "* Navahoca: {{ç|nv|}}\n"
        + "* Nenetsçe: {{ç|yrk|}}\n"
        + "* Nepalce: {{ç|ne|}}\n"
        + "* Nogayca: {{ç|nog|}}\n"
        + "* Normanca: {{ç|nrf|}}\n"
        + "* Norveççe: {{ç|no|}}\n"
        + "*: Bokmål: {{ç|nb|}}\n"
        + "*: Nynorsk: {{ç|nn|}}\n"
        + "* Novial: {{ç|nov|}}\n"
        + "* Ojibvaca: {{ç|oji|}}\n"
        + "* Oksitanca: {{ç|oc|}}\n"
        + "* Orta Atlas Tamazit dili: {{ç|tzm|}}\n"
        + "* Osetçe: {{ç|os|}}\n"
        + "* Övdalca: {{ç|ovd|}}\n"
        + "* Özbekçe: {{ç|uz|}}\n"
        + "* Pali dili: {{ç|pi|}}\n"
        + "* Papiamento: {{ç|pap|}}\n"
        + "* Pencapça: {{ç|pa|}}\n"
        + "*: Batı Pencapça: {{ç|pnb|}}\n"
        + "* Permyakça: {{ç|koi|}}\n"
        + "* Peştuca: {{ç|ps|}}\n"
        + "* Piemontça: {{ç|pms|}}\n"
        + "* Polapça: {{ç|pox|}}\n"
        + "* Portekizce: {{ç|pt|}}\n"
        + "* Rapa Nui dili: {{ç|rap|}}\n"
        + "* Rohingyaca: {{ç|rhg|}}\n"
        + "* Romanca: {{ç|rom|}}\n"
        + "* Romanşça: {{ç|ro|}}\n"
        + "* Ruanda dili: {{ç|rw|}}\n"
        + "* Rumence: {{ç|ro|}}\n"
        + "* Rusça: {{ç|ru|}}\n"
        + "* Rusince: {{ç|rue|}}\n"
        + "* Sabuanca: {{ç|ceb|}}\n"
        + "* Samoaca: {{ç|sm|}}\n"
        + "* Samogitçe: {{ç|sgs|}}\n"
        + "* Sango dili: {{ç|sg|}}\n"
        + "* Sanskritçe: {{ç|sa|}}\n"
        + "* Santalice: {{ç|sat|}}\n"
        + "* Sardunyaca: {{ç|sc|}}\n"
        + "*: Kampidanez Sardunyacası: {{ç|sro|}}\n"
        + "* Savahili dili: {{ç|sw|}}\n"
        + "* Seylanca: {{ç|si|}}\n"
        + "* Sırp-Hırvatça:\n"
        + "*: Kiril: {{ç|sh|}}\n"
        + "*: Latin: {{ç|sh|}}\n"
        + "* Sicilyaca: {{ç|scn|}}\n"
        + "* Sidama dili: {{ç|sid|}}\n"
        + "* Slovakça: {{ç|sk|}}\n"
        + "* Slovence: {{ç|sl|}}\n"
        + "* Somalice: {{ç|so|}}\n"
        + "* Sorbca:\n"
        + "*: Aşağı Sorbca: {{ç|dsb|}}\n"
        + "*: Yukarı Sorbca: {{ç|hsb|}}\n"
        + "* Sotho dili: {{ç|st|}}\n"
        + "* Sunda dili: {{ç|su|genep}}\n"
        + "* Supikçe: {{ç|ems|}}\n"
        + "* Sümerce: {{ç|sux|}}\n"
        + "* Süryanice: {{ç|syc|}}\n"
        + "*: Neo-Süryanice: {{ç|aii||tr=}}\n"
        + "* Savahili: {{ç|sw|}}\n"
        + "* Svanca: {{ç|sva|}}\n"
        + "* Şan dili: {{ç|shn|}}\n"
        + "* Şilha dili: {{ç|shi|}}\n"
        + "* Şona dili: {{ç|sn|}}\n"
        + "* Şorca: {{ç|cjs|}} \n"
        + "* Tabasaranca: {{ç|tab|}}\n"
        + "* Tacikçe: {{ç|tg|}}\n"
        + "* Tagalogca: {{ç|tl|}}\n"
        + "* Tai Lü dili: {{ç|khb|}}\n"
        + "* Tamilce: {{ç|ta|}}\n"
        + "* Tarifit: {{ç|rif|}}\n"
        + "* Tatarca: {{ç|tt|}}\n"
        + "*: Kırım Tatarcası: {{ç|crh|}}\n"
        + "* Tayca: {{ç|th|}}\n"
        + "*: Kuzey Tayca: {{ç|nod|}}\n"
        + "* Telugu dili: {{ç|te|}}\n"
        + "* Tesmence: {{ç|tk|}}\n"
        + "* Tetum: {{ç|tet|}}\n"
        + "* Tibetçe:\n"
        + "*: Klâsik Tibetçe: {{ç|xct|}}\n"
        + "*: Lhasa Tibetçesi: {{ç|bo|}}\n"
        + "* Tigrinya dili: {{ç|ti|}}\n"
        + "* Tok Pisin dili: {{ç|tpi|}}\n"
        + "* Tsonga dili: {{ç|ts|}}\n"
        + "* Tutelo dili: {{ç|tta|}}\n"
        + "* Tupinambá: {{ç|tpn|}}\n"
        + "* Tuvaca: {{ç|tyv|}}\n"
        + "* Türkçe:\n"
        + "*: Eski Türkçe: {{ç|otk|}}\n"
        + "*: Osmanlı Türkçesi: {{ç|ota|گوز}}\n"
        + "* Türkmence: {{ç|tk|}}\n"
        + "* Tzotzilce: {{ç|tzo|}}\n"
        + "* Udmurtça: {{ç|udm|}}\n"
        + "* Ukraynaca: {{ç|uk|}}\n"
        + "* Ulahça: {{ç|rup|}}\n"
        + "* Umbundu dili: {{ç|umb|}}\n"
        + "* Urduca: {{ç|ur|}}\n"
        + "* Uygurca: {{ç|ug|}}\n"
        + "* Valonca: {{ç|wa|}}\n"
        + "* Venedikçe: {{ç|vec|}}\n"
        + "* Vepsçe: {{ç|vep|}}\n"
        + "* Vietnamca: {{ç|vi|}}\n"
        + "* Vilamov dili: {{ç|wym|}}\n"
        + "* Volapük: {{ç|vo|}}\n"
        + "* Võro dili: {{ç|vro|}}\n"
        + "* Votça: {{ç|vot|}}\n"
        + "* Wolof dili: {{ç|wo|}}\n"
        + "* Xârâcùù dili: {{ç|ane|}}\n"
        + "* Xhosa dili: {{ç|xh|}}\n"
        + "* Yağnupça: {{ç|yai|}}\n"
        + "* Yakutça: {{ç|sah|}}\n"
        + "* Yorubaca: {{ç|yo|}}\n"
        + "* Yunanca:\n"
        + "*: Grekçe: {{ç|grc|}}\n"
        + "*: Modern Yunanca: {{ç|el|}}\n"
        + "* Yupik dili: {{ç|esu|}}\n"
        + "* Zarma dili: {{ç|dje|}}\n"
        + "* Zazaca: {{ç|zza|}}\n"
        + "*: Güney Zazaca: {{ç|diq|}}\n"
        + "*: Kuzey Zazaca: {{ç|kiu|}} \n"
        + "* Zhuangca: {{ç|za|}}\n"
        + "* Zuluca: {{ç|zu|}}\n"
        + "{{Alt}}\n"
        + "\n"
        + "{{Üst|kısım|tip=çeviriler}}\n"
        + "* Aari dili: {{ç|aiw|}}\n"
        + "* Abhazca: {{ç|ab|}}\n"
        + "* Afarca: {{ç|aa|}}\n"
        + "* Afrikanca: {{ç|af|}}\n"
        + "* Ainu dili: {{ç|ain|}}\n"
        + "* Aiton dili: {{ç|aio|}}\n"
        + "* Akadca: {{ç|akk|}}\n"
        + "* Almanca: {{ç|de|Teil|n}}\n"
        + "*: Bavyeraca: {{ç|bar|}}\n"
        + "*: İsviçre Almancası: {{ç|gsw|}}\n"
        + "*: Yahudi Almancası: {{ç|yi|}}\n"
        + "* Altayca:\n"
        + "*: Güney Altai: {{ç|alt|}}\n"
        + "*: Kuzey Altayca: {{ç|atv|}}\n"
        + "* Amharca: {{ç|am|}}\n"
        + "* Amorice:\n"
        + "*: Ugaritçe: {{ç|uga|}}\n"
        + "* Aragonca: {{ç|an|}}\n"
        + "* Aramice:\n"
        + "*: Arami: {{ç|arc||tr=|sc=Syrc}}\n"
        + "*: İbrani: {{ç|arc||tr=|sc=Hebr}}\n"
        + "* Arapça: {{ç|ar|}}\n"
        + "*: Cezayir Arapçası: {{ç|arq||tr=aḡrum}}\n"
        + "*: Hicaz Arapçası: {{ç|acw||tr=}}\n"
        + "*: Fas Arapçası: {{ç|ary||tr=}}\n"
        + "*: Irak Arapçası: {{ç|acm||tr=}}\n"
        + "*: Kuzey Levantin Arapçası: {{ç|apc||tr=}}\n"
        + "*: Mısır Arapçası: {{ç|arz||tr=fi|sc=Arab}}\n"
        + "* Argobaca: {{ç|agj||tr=}}\n"
        + "* Arnavutça: {{ç|sq|}}\n"
        + "* Asturyasça: {{ç|ast|}}\n"
        + "* Aşağı Almanca: {{ç|nds|}}\n"
        + "*: Alman Aşağı Almancası: {{ç|nds-de|}}\n"
        + "*:: Plautdieç: {{ç|pdt|}}\n"
        + "*: Hollanda Aşağı Almancası: {{ç|nds-nl|}}\n"
        + "* Aşo dili: {{ç|csh|}}\n"
        + "* Avarca: {{ç|av|}}\n"
        + "* Aymaraca: {{ç|ay|}}\n"
        + "* Azerice: {{ç|az|}}\n"
        + "* Balinezce: {{ç|ban|}}\n"
        + "* Bambara dili: {{ç|bm|}}\n"
        + "* Baskça: {{ç|eu|}}\n"
        + "* Başkurtça: {{ç|ba|}}\n"
        + "* Batsça: {{ç|bbl|}}\n"
        + "* Beluçça: {{ç|bal|}}\n"
        + "* Bengalce: {{ç|bn|}}\n"
        + "* Beyaz Rusça: {{ç|be|}}\n"
        + "* Bikolca:\n"
        + "*: Albay Bikol: {{ç|bhk|}}\n"
        + "*: Merkezî Bikol: {{ç|bcl|}}\n"
        + "*: Pandan Bikol: {{ç|cts|}}\n"
        + "*: Rinkonada Bikol: {{ç|bto|}}\n"
        + "* Birmanca: {{ç|my|}}\n"
        + "* Bretonca: {{ç|br|}}\n"
        + "* Budukça: {{ç|bdk|}}\n"
        + "* Bulgarca: {{ç|bg|}}\n"
        + "* Calo dili: {{ç|rmq|}}\n"
        + "* Cingpo dili: {{ç|kac|}}\n"
        + "* Çeçence: {{ç|ce|}}\n"
        + "* Çekçe: {{ç|cs|}}\n"
        + "* Çerkesçe:\n"
        + "*: Batı Çerkesçesi: {{ç|ady|}}\n"
        + "*: Doğu Çerkesçesi: {{ç|kbd|}}\n"
        + "* Çerokice: {{ç|chr|}}\n"
        + "* Çevaca: {{ç|ny|}}\n"
        + "* Çikasav dili: {{ç|cic|}}\n"
        + "* Çince: {{ç|zh|}}\n"
        + "*: Dungan: {{ç|dng|}}\n"
        + "*: Hakka: {{ç|hak||tr=}}\n"
        + "*: Mandarin: {{ç|cmn||tr=}}\n"
        + "*: Min Bei: {{ç|mnp|}}\n"
        + "*: Min Dong: {{ç|cdo||tr=}}\n"
        + "*: Min Nan: {{ç|nan||tr=}}\n"
        + "*: Wu: {{ç|wuu||tr=}}\n"
        + "*: Yue: {{ç|yue||tr=}}\n"
        + "* Çuvaşça: {{ç|cv|}}\n"
        + "* Dagbanice: {{ç|dag|}}\n"
        + "* Dalmaçyaca: {{ç|dlm|}}\n"
        + "* Danca: {{ç|da|}}\n"
        + "* Dolganca: {{ç|dlg|}}\n"
        + "* Endonezce: {{ç|id|}}\n"
        + "* Ermenice: {{ç|hy|}}\n"
        + "*: Batı Ermenicesi: {{ç|hyw|}}\n"
        + "*: Doğu Ermenicesi: {{ç|hye|}}\n"
        + "*: Eski Ermenice: {{ç|xcl|}}\n"
        + "*: Orta Ermenice: {{ç|axm|}}\n"
        + "* Ersya dili: {{ç|myv|}}\n"
        + "* Eski Norsça: {{ç|non|}}\n"
        + "* Eski Slavca:\n"
        + "*: Eski Doğu Slavcası: {{ç|orv|}}\n"
        + "*: Eski Kilise Slavcası: {{ç|cu|}}\n"
        + "* Esperanto: {{ç|eo|}}\n"
        + "* Estonca: {{ç|et|}}\n"
        + "* Evenkice: {{ç|evn|}}\n"
        + "* Ewece: {{ç|ee|}}\n"
        + "* Farfarca: {{ç|gur|}}\n"
        + "* Faroece: {{ç|fo|}}\n"
        + "* Farsça: {{ç|fa|}}\n"
        + "*: Orta Farsça: {{ç|pal|}}\n"
        + "* Felemenkçe: {{ç|nl|}}\n"
        + "* Fijice: {{ç|fj|}}\n"
        + "* Fince: {{ç|fi|}}\n"
        + "* Frankça: {{ç|frk|}}\n"
        + "* Fransızca: {{ç|fr|}}\n"
        + "* Frigce: {{ç|xpg|}}\n"
        + "* Friuli dili: {{ç|fur|}}\n"
        + "* Frizce:\n"
        + "*: Batı Frizce: {{ç|fy|}}\n"
        + "*: Kuzey Frizce: {{ç|frr|}}\n"
        + "*: Sater Frizcesi: {{ç|stq|}}\n"
        + "* Fulanice:\n"
        + "*: Adlam: {{ç|ff|}}\n"
        + "*: Latin: {{ç|ff|}}\n"
        + "* Furlanca: {{ç|fur|}}\n"
        + "* Gagavuzca: {{ç|gag|}}\n"
        + "* Galce: {{ç|cy|}}\n"
        + "* Galiçyaca: {{ç|gl|}}\n"
        + "* Gilyakça: {{ç|niv|}}\n"
        + "* Gotça: {{ç|got|}}\n"
        + "* Grönlandca: {{ç|kl|}}\n"
        + "* Guanarice: {{ç|gn|}}\n"
        + "* Gucaratça: {{ç|gu|}}\n"
        + "* Gürcüce: {{ç|ka|}}\n"
        + "* Haiti Kreolü: {{ç|ht|}}\n"
        + "* Hausaca: {{ç|ha|}}\n"
        + "* Hawaii dili: {{ç|haw|}}\n"
        + "* Hınalık dili: {{ç|kjj|}}\n"
        + "* Hintçe: {{ç|hi|}}\n"
        + "* Hititçe: {{ç|hit|}}\n"
        + "* Hmong dili: {{ç|hmn|}}\n"
        + "* Hunzib dili: {{ç|huz|}}\n"
        + "* İbranice: {{ç|he|}}\n"
        + "* İdo dili: {{ç|io|}}\n"
        + "* İngilizce: {{ç|en|part}}\n"
        + "*: Eski İngilizce: {{ç|ang|}}\n"
        + "*: İskoç İngilizcesi: {{ç|sco|}}\n"
        + "*: Orta İngilizce: {{ç|enm|}}\n"
        + "* İnterlingua: {{ç|ia|}}\n"
        + "* İnuitçe:\n"
        + "*: Doğu Kanada İnuitçesi: {{ç|iu|}}\n"
        + "* İrlandaca: {{ç|ga|}}\n"
        + "*: Eski İrlandaca: {{ç|sga|}}\n"
        + "* İskoçça: {{ç|gd|}}\n"
        + "* İspanyolca: {{ç|es|}}\n"
        + "*: Yahudi İspanyolcası: {{ç|lad|}}\n"
        + "* İstrioça: {{ç|ist|}}\n"
        + "* İsveççe: {{ç|sv|}}\n"
        + "* İtalyanca: {{ç|it|}}\n"
        + "* İzlandaca: {{ç|is|}}\n"
        + "* Japonca: {{ç|ja|}}\n"
        + "* Kabardeyce: {{ç|kbd|}}\n"
        + "* Kalmukça: {{ç|xal|}}\n"
        + "* Kapampangan dili: {{ç|pam|}}\n"
        + "* Kannada dili: {{ç|kn|}}\n"
        + "* Karaçay-Balkarca: {{ç|krc|}}\n"
        + "* Karakalpakça: {{ç|kaa|}}\n"
        + "* Karayca: {{ç|kdr|}}\n"
        + "* Karipúna Fransız Kreolü: {{ç|kmv|}}\n"
        + "* Kaşupça: {{ç|csb|}}\n"
        + "* Katalanca: {{ç|ca|}}\n"
        + "* Kazakça: {{ç|kk|}}\n"
        + "* Keçuva dili: {{ç|qu|}}\n"
        + "* Kernevekçe: {{ç|kw|}}\n"
        + "* Kıptîce: {{ç|cop|}}\n"
        + "* Kırgızca: {{ç|ky|}}\n"
        + "* Kikuyu dili: {{ç|ki|}}\n"
        + "* Kmer dili: {{ç|km|}}\n"
        + "* Komi dili: {{ç|kv|}}\n"
        + "* Komorca: {{ç|bnt|}}\n"
        + "* Konkani dili: {{ç|kok|}}\n"
        + "* Korece: {{ç|ko|}}\n"
        + "* Korsikaca: {{ç|co|}}\n"
        + "* Koryakça: {{ç|kpy|}}\n"
        + "* Krice: {{ç|cr|}}\n"
        + "*: Ova Kricesi: {{ç|crk|}}\n"
        + "* Kuçean dili: {{ç|txb|}}\n"
        + "* Kumukça: {{ç|kum|}}\n"
        + "* Kürtçe: {{ç|ku|}}\n"
        + "*: Kuzey Kürtçe: {{ç|kmr|}}\n"
        + "*: Lekçe: {{ç|lki|}}\n"
        + "*: Orta Kürtçe: {{ç|ckb|}}\n"
        + "* Ladince: {{ç|lld|}}\n"
        + "* Laoca: {{ç|lo|}}\n"
        + "* Laponca:\n"
        + "*: Akkala: {{ç|sia|}}\n"
        + "*: Güney Laponca: {{ç|sma|}}\n"
        + "*: İnari: {{ç|smn|}}\n"
        + "*: Kemi: {{ç|sjk|}}\n"
        + "*: Kildin: {{ç|sjd|}}\n"
        + "*: Kuzey Laponca: {{ç|sme|}}\n"
        + "*: Lule: {{ç|smj|}}\n"
        + "*: Pite: {{ç|sje|}}\n"
        + "*: Skolt: {{ç|sms|}}\n"
        + "*: Ter: {{ç|sjt|}}\n"
        + "*: Ume: {{ç|sju|}}\n"
        + "* Latgalce: {{ç|ltg|}}\n"
        + "* Latince: {{ç|la|}}\n"
        + "* Lehçe: {{ç|pl|}}\n"
        + "* Letonca: {{ç|lv|}}\n"
        + "* Lezgice: {{ç|lez|}}\n"
        + "* Ligurya dili: {{ç|lij|}}\n"
        + "* Limburgca: {{ç|li|}}\n"
        + "* Lingala: {{ç|ln|}}\n"
        + "* Litvanca: {{ç|lt|}}\n"
        + "* Livonca: {{ç|liv|}}\n"
        + "* Lombardça: {{ç|lmo|}}\n"
        + "* Luhyaca: {{ç|luy|}}\n"
        + "* Luvice:\n"
        + "*: Çivi yazısı: {{ç|xlu|}}\n"
        + "*: Hiyeroglif: {{ç|hlu|}}\n"
        + "* Lüksemburgca: {{ç|lb|}}\n"
        + "* Maasai dili: {{ç|mas|}}\n"
        + "* Macarca: {{ç|hu|}}\n"
        + "* Magindanao dili: {{ç|mdh|}}\n"
        + "* Makedonca: {{ç|mk|}}\n"
        + "* Malayalam dili: {{ç|ml|}}\n"
        + "* Malayca: {{ç|ms|}}\n"
        + "*: Ambonez Malayca: {{ç|abs|}}\n"
        + "* Malgaşça: {{ç|mg|}}\n"
        + "* Maltaca: {{ç|mt|}}\n"
        + "* Mançuca: {{ç|mnc|}}\n"
        + "* Manksça {{ç|gv|}}\n"
        + "* Maorice: {{ç|mi|}}\n"
        + "* Mapudungun dili: {{ç|arn|}}\n"
        + "* Maranao dili: {{ç|mrw|}}\n"
        + "* Marathi dili: {{ç|mr|}}\n"
        + "* Mari dili: {{ç|chm|}}\n"
        + "* Massachusett dili: {{ç|wam|}}\n"
        + "* Mayaca:\n"
        + "*: Yukatek Mayacası: {{ç|yua|}}\n"
        + "* Mbyá Guaraní dili: {{ç|gun|}}\n"
        + "* Merkezî Sierra Miwok dili: {{ç|csm|}}\n"
        + "* Mısırca: {{ç|egy|}}\n"
        + "* Moğolca:\n"
        + "*: Kiril: {{ç|mn|}}\n"
        + "*: Latin: {{ç|mn|}}\n"
        + "* Mòkeno dili: {{ç|mhn|}}\n"
        + "* Mokşa dili: {{ç|mdf|}}\n"
        + "* Moldovaca: {{ç|mo|}}\n"
        + "* Munsee dili: {{ç|umu|}}\n"
        + "* Mwani dili: {{ç|wmw|}}\n"
        + "* Nahuatl dili: {{ç|nah|}}\n"
        + "*: Klâsik Nahuatl dili: {{ç|nci|}}\n"
        + "* Nandi dili: {{ç|kln|}}\n"
        + "* Napolice: {{ç|nap|}}\n"
        + "* Nauruca: {{ç|na|}}\n"
        + "* Navahoca: {{ç|nv|}}\n"
        + "* Nenetsçe: {{ç|yrk|}}\n"
        + "* Nepalce: {{ç|ne|}}\n"
        + "* Nogayca: {{ç|nog|}}\n"
        + "* Normanca: {{ç|nrf|}}\n"
        + "* Norveççe: {{ç|no|}}\n"
        + "*: Bokmål: {{ç|nb|}}\n"
        + "*: Nynorsk: {{ç|nn|}}\n"
        + "* Novial: {{ç|nov|}}\n"
        + "* Ojibvaca: {{ç|oji|}}\n"
        + "* Oksitanca: {{ç|oc|}}\n"
        + "* Orta Atlas Tamazit dili: {{ç|tzm|}}\n"
        + "* Osetçe: {{ç|os|}}\n"
        + "* Övdalca: {{ç|ovd|}}\n"
        + "* Özbekçe: {{ç|uz|}}\n"
        + "* Pali dili: {{ç|pi|}}\n"
        + "* Papiamento: {{ç|pap|}}\n"
        + "* Pencapça: {{ç|pa|}}\n"
        + "*: Batı Pencapça: {{ç|pnb|}}\n"
        + "* Permyakça: {{ç|koi|}}\n"
        + "* Peştuca: {{ç|ps|}}\n"
        + "* Piemontça: {{ç|pms|}}\n"
        + "* Polapça: {{ç|pox|}}\n"
        + "* Portekizce: {{ç|pt|}}\n"
        + "* Rapa Nui dili: {{ç|rap|}}\n"
        + "* Rohingyaca: {{ç|rhg|}}\n"
        + "* Romanca: {{ç|rom|}}\n"
        + "* Romanşça: {{ç|ro|}}\n"
        + "* Ruanda dili: {{ç|rw|}}\n"
        + "* Rumence: {{ç|ro|}}\n"
        + "* Rusça: {{ç|ru|}}\n"
        + "* Rusince: {{ç|rue|}}\n"
        + "* Sabuanca: {{ç|ceb|}}\n"
        + "* Samoaca: {{ç|sm|}}\n"
        + "* Samogitçe: {{ç|sgs|}}\n"
        + "* Sango dili: {{ç|sg|}}\n"
        + "* Sanskritçe: {{ç|sa|}}\n"
        + "* Santalice: {{ç|sat|}}\n"
        + "* Sardunyaca: {{ç|sc|}}\n"
        + "*: Kampidanez Sardunyacası: {{ç|sro|}}\n"
        + "* Savahili dili: {{ç|sw|}}\n"
        + "* Seylanca: {{ç|si|}}\n"
        + "* Sırp-Hırvatça:\n"
        + "*: Kiril: {{ç|sh|}}\n"
        + "*: Latin: {{ç|sh|}}\n"
        + "* Sicilyaca: {{ç|scn|}}\n"
        + "* Sidama dili: {{ç|sid|}}\n"
        + "* Slovakça: {{ç|sk|}}\n"
        + "* Slovence: {{ç|sl|}}\n"
        + "* Somalice: {{ç|so|}}\n"
        + "* Sorbca:\n"
        + "*: Aşağı Sorbca: {{ç|dsb|}}\n"
        + "*: Yukarı Sorbca: {{ç|hsb|}}\n"
        + "* Sotho dili: {{ç|st|}}\n"
        + "* Sunda dili: {{ç|su|genep}}\n"
        + "* Supikçe: {{ç|ems|}}\n"
        + "* Sümerce: {{ç|sux|}}\n"
        + "* Süryanice: {{ç|syc|}}\n"
        + "*: Neo-Süryanice: {{ç|aii||tr=}}\n"
        + "* Savahili: {{ç|sw|}}\n"
        + "* Svanca: {{ç|sva|}}\n"
        + "* Şan dili: {{ç|shn|}}\n"
        + "* Şilha dili: {{ç|shi|}}\n"
        + "* Şona dili: {{ç|sn|}}\n"
        + "* Şorca: {{ç|cjs|}} \n"
        + "* Tabasaranca: {{ç|tab|}}\n"
        + "* Tacikçe: {{ç|tg|}}\n"
        + "* Tagalogca: {{ç|tl|}}\n"
        + "* Tai Lü dili: {{ç|khb|}}\n"
        + "* Tamilce: {{ç|ta|}}\n"
        + "* Tarifit: {{ç|rif|}}\n"
        + "* Tatarca: {{ç|tt|}}\n"
        + "*: Kırım Tatarcası: {{ç|crh|}}\n"
        + "* Tayca: {{ç|th|}}\n"
        + "*: Kuzey Tayca: {{ç|nod|}}\n"
        + "* Telugu dili: {{ç|te|}}\n"
        + "* Tesmence: {{ç|tk|}}\n"
        + "* Tetum: {{ç|tet|}}\n"
        + "* Tibetçe:\n"
        + "*: Klâsik Tibetçe: {{ç|xct|}}\n"
        + "*: Lhasa Tibetçesi: {{ç|bo|}}\n"
        + "* Tigrinya dili: {{ç|ti|}}\n"
        + "* Tok Pisin dili: {{ç|tpi|}}\n"
        + "* Tsonga dili: {{ç|ts|}}\n"
        + "* Tutelo dili: {{ç|tta|}}\n"
        + "* Tupinambá: {{ç|tpn|}}\n"
        + "* Tuvaca: {{ç|tyv|}}\n"
        + "* Türkçe:\n"
        + "*: Eski Türkçe: {{ç|otk|}}\n"
        + "*: Osmanlı Türkçesi: {{ç|ota|گوز}}\n"
        + "* Türkmence: {{ç|tk|}}\n"
        + "* Tzotzilce: {{ç|tzo|}}\n"
        + "* Udmurtça: {{ç|udm|}}\n"
        + "* Ukraynaca: {{ç|uk|}}\n"
        + "* Ulahça: {{ç|rup|}}\n"
        + "* Umbundu dili: {{ç|umb|}}\n"
        + "* Urduca: {{ç|ur|}}\n"
        + "* Uygurca: {{ç|ug|}}\n"
        + "* Valonca: {{ç|wa|}}\n"
        + "* Venedikçe: {{ç|vec|}}\n"
        + "* Vepsçe: {{ç|vep|}}\n"
        + "* Vietnamca: {{ç|vi|}}\n"
        + "* Vilamov dili: {{ç|wym|}}\n"
        + "* Volapük: {{ç|vo|}}\n"
        + "* Võro dili: {{ç|vro|}}\n"
        + "* Votça: {{ç|vot|}}\n"
        + "* Wolof dili: {{ç|wo|}}\n"
        + "* Xârâcùù dili: {{ç|ane|}}\n"
        + "* Xhosa dili: {{ç|xh|}}\n"
        + "* Yağnupça: {{ç|yai|}}\n"
        + "* Yakutça: {{ç|sah|}}\n"
        + "* Yorubaca: {{ç|yo|}}\n"
        + "* Yunanca:\n"
        + "*: Grekçe: {{ç|grc|}}\n"
        + "*: Modern Yunanca: {{ç|el|}}\n"
        + "* Yupik dili: {{ç|esu|}}\n"
        + "* Zarma dili: {{ç|dje|}}\n"
        + "* Zazaca: {{ç|zza|}}\n"
        + "*: Güney Zazaca: {{ç|diq|}}\n"
        + "*: Kuzey Zazaca: {{ç|kiu|}} \n"
        + "* Zhuangca: {{ç|za|}}\n"
        + "* Zuluca: {{ç|zu|}}\n"
        + "{{Alt}}\n"
        + "\n"
        + "===Kaynakça===\n"
        + "* {{Kaynak-TDK}}\n"
        + "\n"
        + "===Ek okumalar===\n"
        + "* {{mânâ|anatomi}} {{proje bağlantısı|Vikipedi}}\n"
        + "\n");
    Iterator<Token> iter = text.headers(3).iterator();
    assertTrue("There is at least one level 3 header", iter.hasNext());
    assertEquals("Köken", iter.next().asHeading().getContent().toString());
    assertEquals("Söyleniş", iter.next().asHeading().getContent().toString());
    assertEquals("Ad", iter.next().asHeading().getContent().toString());
    assertEquals("Kaynakça", iter.next().asHeading().getContent().toString());
    assertEquals("Ek okumalar", iter.next().asHeading().getContent().toString());
  }

}

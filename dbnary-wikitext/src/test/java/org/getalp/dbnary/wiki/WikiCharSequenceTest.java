package org.getalp.dbnary.wiki;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

/**
 * Created by serasset on 16/02/17.
 */
public class WikiCharSequenceTest {

  @Test
  public void testWikiWikiCharSequence() {
    String test = "{{en-noun}} text [[link]]s truc {{template}} text [[toto]]";
    WikiText text = new WikiText(test);

    WikiCharSequence seq = new WikiCharSequence(text);
    assertEquals("Incorrect length (in code points)",22, seq.codePointCount());
    Pattern pattern = Pattern.compile("^\\p{Co}.*$");
    assertTrue(pattern.matcher(seq).matches());
    pattern = Pattern.compile("^\\p{Alpha}.*$");
    assertFalse(pattern.matcher(seq).matches());
    pattern = WikiPattern.compile("^\\p{Template}\\stext\\s.*$");
    assertTrue(pattern.matcher(seq).matches());
    pattern = WikiPattern.compile("^\\p{Template}\\stext\\s");
    assertTrue(pattern.matcher(seq).find());
    pattern = WikiPattern.compile("\\p{Link}\\struc\\s");
    Matcher m = pattern.matcher(seq);
    assertTrue(m.find());
    assertEquals("The first code point of the match should be a private use char.",
        Character.getType(seq.codePointAt(m.start())), Character.PRIVATE_USE);
    assertEquals("The code point immediately after the match should be a private use char.",
        Character.getType(seq.codePointAt(m.start())), Character.PRIVATE_USE);
  }

  @Test
  public void testWikiCharSubSequence() {
    String test = "{{en-noun}} text [[link]]s truc {{template}} text [[toto]]";
    WikiText text = new WikiText(test);

    WikiCharSequence seq = new WikiCharSequence(text);
    // Reminder: subsequence parameters are in char counts (code UNITS), not in code point.
    // Hence each special element accounts for 2 chars
    WikiCharSequence ss1 = seq.subSequence(0, 10);
    assertEquals("The subsequence should contain 10 code units.",10,
        ss1.length());
    assertEquals("The subsequence should contain 8 code points (with 2 supplementary private use chars).",
        8, ss1.codePointCount());
    assertEquals("{{en-noun}} text [[link]]s", ss1.getSourceContent());


    Pattern pattern = Pattern.compile("^\\p{Co}.*$");
    assertTrue("The subsequence should start with a supplementary private use char.",
        pattern.matcher(ss1).matches());

    WikiCharSequence ss2 = seq.subSequence(11, 22);

    assertEquals('t', ss2.charAt(0));
    assertEquals("truc {{template}} tex", ss2.getSourceContent());

    WikiCharSequence ss3 = ss2.subSequence(3, 10);
    assertEquals(7, ss3.length());
    assertEquals(6, ss3.codePointCount());
    assertEquals("c {{template}} te", ss3.getSourceContent());


    CharSequence ss4 = ss2.subSequence(5, 9);
    assertEquals(' ', ss4.charAt(2));

  }

  @Test
  public void testWikiCharSequenceWithFiltering() {
    String test = "{{en-noun}} text [[link]]s text {{template}} text [[toto]]";
    WikiText text = new WikiText(test);

    ClassBasedSequenceFilter filter = new ClassBasedSequenceFilter();
    filter.voidTemplates();
    WikiCharSequence seq = new WikiCharSequence(text, filter);

    assertEquals(22, seq.length());
    assertEquals(20, seq.codePointCount());
    Pattern pattern = Pattern.compile("^\\p{Co}.*$");
    assertFalse("The first template should be discarded.",
        pattern.matcher(seq).matches()); // no template chars as first character in char
    // sequence
    pattern = Pattern.compile("^\\p{Alpha}.*$");
    assertFalse("First char after discarded template should be a space char",
        pattern.matcher(seq).matches()); // the first char is a space, not an alpha
    assertEquals(' ', seq.charAt(0));
    pattern = Pattern.compile("[" + WikiCharSequence.INTERNAL_LINKS_RANGE + "]");
    Matcher m = pattern.matcher(seq);
    assertTrue(m.find());
    assertEquals("The link is immediately followed by a space (especially, the s is part of the link)",
        ' ', seq.charAt(m.end()));

    // Pay attention to the fact that every special token take 2 chars
    assertEquals(Character.SURROGATE, Character.getType(seq.charAt(seq.length() - 1)));
    assertEquals(Character.SURROGATE, Character.getType(seq.charAt(seq.length() - 2)));
    assertEquals(Character.PRIVATE_USE, Character.getType(seq.codePointAt(seq.length() - 2)));

    pattern = WikiPattern.compile("\\p{Template}"); // matches a template char
    m = pattern.matcher(seq);
    assertFalse("No template char should be in the char sequence.", m.find());

  }

  @Test
  public void testWikiCharSequenceWithKeepContent() {
    String test = "{{en-noun}} text [[link]]s text {{template}} text [[toto]]";
    WikiText text = new WikiText(test);

    ClassBasedSequenceFilter filter = new ClassBasedSequenceFilter();
    filter.clearAction().atomizeTemplates().sourceText().keepContentOfInternalLink();
    WikiCharSequence seq = new WikiCharSequence(text, filter);

    assertEquals(31, seq.length());
    assertEquals(29, seq.codePointCount());

    Pattern pattern = Pattern.compile("^\\p{Co}.*$");
    assertTrue(pattern.matcher(seq).matches()); // first character is template char
    pattern = Pattern.compile("\\blinks\\b");
    Matcher m = pattern.matcher(seq);
    assertTrue(m.find());
    pattern = WikiPattern.compile("\\p{InternalLink}");
    m = pattern.matcher(seq);
    assertFalse(m.find());
    assertEquals('o', seq.charAt(seq.length() - 1));

    pattern = WikiPattern.compile("\\p{Template}"); // matches a template char
    m = pattern.matcher(seq);
    assertTrue(m.find());
    assertTrue(m.find());

  }

  @Test
  public void testWikiCharSequenceWithLinkKeepTarget() {
    String test = "text [[link]]s text";
    WikiText text = new WikiText(test);

    ClassBasedSequenceFilter filter = new ClassBasedSequenceFilter();
    filter.clearAction().sourceText().keepContentOfInternalLink();
    CharSequence seq = new WikiCharSequence(text, filter);
    // System.out.println(seq.toString());

    assertEquals("Content of link should contain suffix.", "text links text", seq.toString());

    filter = new ClassBasedSequenceFilter();
    filter.clearAction().sourceText().voidInternalLink();
    seq = new WikiCharSequence(text, filter);
    // System.out.println(seq.toString());
    assertEquals("Voiding link should void suffix.", "text  text", seq.toString());

    filter = new ClassBasedSequenceFilter();
    filter.clearAction().sourceText().keepTargetOfInternalLink();
    seq = new WikiCharSequence(text, filter);
    // System.out.println(seq.toString());
    assertEquals("target of link should not contain suffix.", "text link text", seq.toString());

    test = "text [[target|source]]s text";
    text = new WikiText(test);

    filter = new ClassBasedSequenceFilter();
    filter.clearAction().sourceText().voidInternalLink();
    seq = new WikiCharSequence(text, filter);
    // System.out.println(seq.toString());
    assertEquals("Voiding link should void suffix.", "text  text", seq.toString());

    filter = new ClassBasedSequenceFilter();
    filter.clearAction().sourceText().keepTargetOfInternalLink();
    seq = new WikiCharSequence(text, filter);
    // System.out.println(seq.toString());
    assertEquals("target of link should not contain suffix.", "text target text", seq.toString());

    filter = new ClassBasedSequenceFilter();
    filter.clearAction().sourceText().keepContentOfInternalLink();
    seq = new WikiCharSequence(text, filter);
    // System.out.println(seq.toString());
    assertEquals("content of link should contain suffix.", "text sources text", seq.toString());

    test = "[[Australian]] [[capital|Capital]] [[territory|Territory]]";
    text = new WikiText(test);

    filter = new ClassBasedSequenceFilter();
    filter.clearAction().sourceText().keepContentOfInternalLink();
    seq = new WikiCharSequence(text, filter);
    // System.out.println(seq.toString());
    assertEquals("Australian Capital Territory", seq.toString());
  }

  @Test
  public void testWikiCharSequenceWithKeepAsis() {
    String test = "{{en-noun}} text [[link]]s text {{template}} text [[toto]]";
    WikiText text = new WikiText(test);

    ClassBasedSequenceFilter filter = new ClassBasedSequenceFilter();
    filter.clearAction().sourceAll();
    CharSequence seq = new WikiCharSequence(text, filter);

    assertEquals(test, seq.toString());

  }
}

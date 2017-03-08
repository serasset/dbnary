package org.getalp.dbnary.wiki;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * Created by serasset on 16/02/17.
 */
public class WikiCharSequenceTest {

    @Test
    public void testWikiWikiCharSequence2() throws Exception {
        String test = "{{en-noun}} text [[link]]s text {{template}} text [[toto]]";
        WikiText text = new WikiText(test);

        WikiCharSequence seq = new WikiCharSequence(text);
        assertEquals(22, seq.length());
        Pattern pattern = Pattern.compile("^\\p{Co}.*$");
        assertTrue(pattern.matcher(seq).matches());
        pattern = Pattern.compile("^\\p{Alpha}.*$");
        assertFalse(pattern.matcher(seq).matches());
        pattern = Pattern.compile("^[\uE200-\uE7FF]\\stext\\s.*$");
        assertTrue(pattern.matcher(seq).matches());
        pattern = Pattern.compile("^[\uE200-\uE7FF]\\stext\\s");
        Matcher m = pattern.matcher(seq);
        assertTrue(m.find());
        assertEquals(Character.getType(seq.charAt(m.end())), Character.PRIVATE_USE);
    }

    @Test
    public void testWikiCharSequence() throws Exception {
        String test = "{{en-noun}} text [[link]]s text {{template}} text [[toto]]";
        WikiText text = new WikiText(test);

        CharSequence seq = new WikiCharSequence(text);
        CharSequence ss1 = seq.subSequence(0, 10);
        assertEquals(10, ss1.length());

        Pattern pattern = Pattern.compile("^\\p{Co}.*$");
        assertTrue(pattern.matcher(ss1).matches());

        CharSequence ss2 = seq.subSequence(11, 22);

        assertEquals('x', ss2.charAt(0));

        CharSequence ss3 = ss1.subSequence(5, 9);
        assertEquals(4, ss3.length());

        CharSequence ss4 = ss2.subSequence(5, 9);
        assertEquals('t', ss4.charAt(0));

    }

    @Test
    public void testWikiCharSequenceWithFiltering() throws Exception {
        String test = "{{en-noun}} text [[link]]s text {{template}} text [[toto]]";
        WikiText text = new WikiText(test);

        ClassBasedSequenceFilter filter = new ClassBasedSequenceFilter();
        filter.voidTemplates();
        CharSequence seq = new WikiCharSequence(text, filter);
        System.out.println(seq.toString());

        assertEquals(20, seq.length());
        Pattern pattern = Pattern.compile("^\\p{Co}.*$");
        assertFalse(pattern.matcher(seq).matches());  // no template chars as first character in char sequence
        pattern = Pattern.compile("^\\p{Alpha}.*$");
        assertFalse(pattern.matcher(seq).matches());  // the first char is a space, not an alpha
        assertEquals(' ', seq.charAt(0));
        pattern = Pattern.compile("[" + WikiCharSequence.INTERNAL_LINKS_RANGE.toString() + "]");
        Matcher m = pattern.matcher(seq);
        assertTrue(m.find());
        assertEquals(' ', seq.charAt(m.end())); // the s is art of the link, hence it has been atomised with it
        assertEquals(Character.getType(seq.charAt(seq.length()-1)), Character.PRIVATE_USE);

        pattern = Pattern.compile("[\uE200-\uE7FF]"); // matches a template char
        m = pattern.matcher(seq);
        assertFalse(m.find());

    }

    @Test
    public void testWikiCharSequenceWithKeepContent() throws Exception {
        String test = "{{en-noun}} text [[link]]s text {{template}} text [[toto]]";
        WikiText text = new WikiText(test);

        ClassBasedSequenceFilter filter = new ClassBasedSequenceFilter();
        filter.clearAction().atomizeTemplates().sourceText().keepContentOfInternalLink();
        CharSequence seq = new WikiCharSequence(text, filter);
        System.out.println(seq.toString());

        assertEquals(28, seq.length());
        Pattern pattern = Pattern.compile("^\\p{Co}.*$");
        assertTrue(pattern.matcher(seq).matches());  // first character is template char
        pattern = Pattern.compile("\\blink\\b");
        Matcher m = pattern.matcher(seq);
        assertTrue(m.find());
        pattern = Pattern.compile("[\uE900-\uE9FF]");
        m = pattern.matcher(seq);
        assertFalse(m.find());
        assertEquals('o', seq.charAt(seq.length()-1));

        pattern = Pattern.compile("[\uE200-\uE7FF]"); // matches a template char
        m = pattern.matcher(seq);
        assertTrue(m.find());

    }


    @Test
    public void testWikiCharSequenceWithKeepAsis() throws Exception {
        String test = "{{en-noun}} text [[link]]s text {{template}} text [[toto]]";
        WikiText text = new WikiText(test);

        ClassBasedSequenceFilter filter = new ClassBasedSequenceFilter();
        filter.clearAction().sourceAll();
        CharSequence seq = new WikiCharSequence(text, filter);

        assertEquals(test, seq.toString());

    }
}
package org.getalp.blexisma.wiktionary;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


public class WiktionaryExtractorTest {
    
    @Test
    public void testLeadingChars() {
        WiktionaryExtractor we = new FrenchWiktionaryExtractor(null);
        String result = we.cleanUpMarkup("   XYZ");
        assertEquals("cleanUp failed", "XYZ", result);
    }

    @Test
    public void testTrailingChars() {
        WiktionaryExtractor we = new FrenchWiktionaryExtractor(null);
        String result = we.cleanUpMarkup("XYZ    ");
        assertEquals("cleanUp failed", "XYZ", result);
    }
    
    @Test
    public void testInsiders() {
        WiktionaryExtractor we = new FrenchWiktionaryExtractor(null);
        String result = we.cleanUpMarkup("   X   Y   Z ");
        assertEquals("cleanUp failed", "X Y Z", result);
    }
    
    @Test
    public void testAllWhites() {
        WiktionaryExtractor we = new FrenchWiktionaryExtractor(null);
        String result = we.cleanUpMarkup("          ");
        assertEquals("cleanUp failed", "", result);
    }
    
    @Test
    public void testEmpty() {
        WiktionaryExtractor we = new FrenchWiktionaryExtractor(null);
        String result = we.cleanUpMarkup("");
        assertEquals("cleanUp failed", "", result);
    }
    
    @Test
    public void testMacroIsIgnored() {
        WiktionaryExtractor we = new FrenchWiktionaryExtractor(null);
        String result = we.cleanUpMarkup("{{toto|titi}} XYZ");
        assertEquals("cleanUp failed", "XYZ", result);
    }
    
    @Test
    public void testLinkIsKeptInHumanReadableForm() {
        WiktionaryExtractor we = new FrenchWiktionaryExtractor(null);
        String result = we.cleanUpMarkup("[[lemma|occurence]] XYZ", true);
        assertEquals("cleanUp failed", "occurence XYZ", result);
    }
    
    @Test
    public void testLinkIsKeptInDefaultForm() {
        WiktionaryExtractor we = new FrenchWiktionaryExtractor(null);
        String result = we.cleanUpMarkup("[[lemma|occurence]] XYZ");
        assertEquals("cleanUp failed", "#{lemma}# XYZ", result);
    }
    
    @Test
    public void testLinkWithoutOccurenceIsKeptInDefaultForm() {
        WiktionaryExtractor we = new FrenchWiktionaryExtractor(null);
        String result = we.cleanUpMarkup("[[lemma]] XYZ");
        assertEquals("cleanUp failed", "#{lemma}# XYZ", result);
    }
    
    @Test
    public void testLinkWithoutOccurenceIsHumanReadableForm() {
        WiktionaryExtractor we = new FrenchWiktionaryExtractor(null);
        String result = we.cleanUpMarkup("[[lemma]] XYZ", true);
        assertEquals("cleanUp failed", "lemma XYZ", result);
    }
    
    @Test
    public void testLinkWithStupidlyEncodedMorphology() {
        WiktionaryExtractor we = new FrenchWiktionaryExtractor(null);
        String result = we.cleanUpMarkup("[[avion]]s", false);
        assertEquals("cleanUp failed", "#{avion}#", result);
    }
    
    @Test
    public void testDefWithStupidlyEncodedMorphology() {
        WiktionaryExtractor we = new EnglishWiktionaryExtractor(null);
        String result = we.cleanUpMarkup("A failing grade in a class or course.  The next best grade is a [[D]].  Some institutions issue [[E]]s instead of [[F]]s.", false);
        assertEquals("cleanUp failed", "A failing grade in a class or course. The next best grade is a #{D}#. Some institutions issue #{E}# instead of #{F}#.", result);
    }
    
    @Test
    public void testEmphasized() {
        WiktionaryExtractor we = new EnglishWiktionaryExtractor(null);
        String result = we.cleanUpMarkup("'''l'action''' ''compte''", false);
        assertEquals("cleanUp failed", "l'action compte", result);
    }
}

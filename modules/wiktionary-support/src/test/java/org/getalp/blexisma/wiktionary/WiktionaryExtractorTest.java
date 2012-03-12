package org.getalp.blexisma.wiktionary;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


public class WiktionaryExtractorTest {
    
    @Test
    public void testLeadingChars() {
        String result = WiktionaryExtractor.cleanUpMarkup("   XYZ");
        assertEquals("cleanUp failed", "XYZ", result);
    }

    @Test
    public void testTrailingChars() {
        String result = WiktionaryExtractor.cleanUpMarkup("XYZ    ");
        assertEquals("cleanUp failed", "XYZ", result);
    }
    
    @Test
    public void testInsiders() {
        String result = WiktionaryExtractor.cleanUpMarkup("   X   Y   Z ");
        assertEquals("cleanUp failed", "X Y Z", result);
    }
    
    @Test
    public void testAllWhites() {
        String result = WiktionaryExtractor.cleanUpMarkup("          ");
        assertEquals("cleanUp failed", "", result);
    }
    
    @Test
    public void testEmpty() {
        String result = WiktionaryExtractor.cleanUpMarkup("");
        assertEquals("cleanUp failed", "", result);
    }
    
    @Test
    public void testMacroIsIgnored() {
        String result = WiktionaryExtractor.cleanUpMarkup("{{toto|titi}} XYZ");
        assertEquals("cleanUp failed", "XYZ", result);
    }
    
    @Test
    public void testLinkIsKeptInHumanReadableForm() {
        String result = WiktionaryExtractor.cleanUpMarkup("[[lemma|occurence]] XYZ", true);
        assertEquals("cleanUp failed", "occurence XYZ", result);
    }
    
    @Test
    public void testLinkIsKeptInDefaultForm() {
        String result = WiktionaryExtractor.cleanUpMarkup("[[lemma|occurence]] XYZ");
        assertEquals("cleanUp failed", "#{lemma|occurence}# XYZ", result);
    }
    
    @Test
    public void testLinkWithoutOccurenceIsKeptInDefaultForm() {
        String result = WiktionaryExtractor.cleanUpMarkup("[[lemma]] XYZ");
        assertEquals("cleanUp failed", "#{lemma|lemma}# XYZ", result);
    }
    
    @Test
    public void testLinkWithoutOccurenceIsHumanReadableForm() {
        String result = WiktionaryExtractor.cleanUpMarkup("[[lemma]] XYZ", true);
        assertEquals("cleanUp failed", "lemma XYZ", result);
    }
    
    @Test
    public void testLinkWithStupidlyEncodedMorphology() {
        String result = WiktionaryExtractor.cleanUpMarkup("[[avion]]s", false);
        assertEquals("cleanUp failed", "#{avion|avions}#", result);
    }
    
    @Test
    public void testDefWithStupidlyEncodedMorphology() {
        String result = WiktionaryExtractor.cleanUpMarkup("A failing grade in a class or course.  The next best grade is a [[D]].  Some institutions issue [[E]]s instead of [[F]]s.", false);
        assertEquals("cleanUp failed", "A failing grade in a class or course. The next best grade is a #{D|D}#. Some institutions issue #{E|Es}# instead of #{F|Fs}#.", result);
    }
    
    @Test  
    public void testDocumentationExampleNonHumanReadable() {
        String result = WiktionaryExtractor.cleanUpMarkup("{{a Macro}} will be [[discard]]ed and [[feed|fed]] to the [[void]].", false);
        assertEquals("cleanUp failed", "will be #{discard|discarded}# and #{feed|fed}# to the #{void|void}#.", result);
    }

    @Test  
    public void testDocumentationExampleHumanReadable() {
        String result = WiktionaryExtractor.cleanUpMarkup("{{a Macro}} will be [[discard]]ed and [[feed|fed]] to the [[void]].", true);
        assertEquals("cleanUp failed", "will be discarded and fed to the void.", result);
    }

    @Test
    public void testEmphasized() {
        String result = WiktionaryExtractor.cleanUpMarkup("'''l'action''' ''compte''", false);
        assertEquals("cleanUp failed", "l'action compte", result);
    }
    
    @Test
    public void testXmlComments1() {
        String result = WiktionaryExtractor.cleanUpMarkup("   X<!-- tagada ploum -- -->Y   Z ");
        assertEquals("cleanUp failed", "XY Z", result);
    }

    @Test
    public void testXmlComments2() {
        String result = WiktionaryExtractor.cleanUpMarkup("   X<!-- {{toto}} -->Y   Z ");
        assertEquals("cleanUp failed", "XY Z", result);
    }

    @Test  
    public void testDefinitionToHumanReadable() {
    	String data = "{{a Macro}} will be [[discard]]ed and [[feed|fed]] to the [[void]].";
        String result1 = WiktionaryExtractor.cleanUpMarkup(data, true);
        String def = WiktionaryExtractor.cleanUpMarkup(data, false);
        String result2 = WiktionaryExtractor.convertToHumanReadableForm(def);
        assertEquals("Hman readable form should be the same in both results", result1, result2);
    }
}

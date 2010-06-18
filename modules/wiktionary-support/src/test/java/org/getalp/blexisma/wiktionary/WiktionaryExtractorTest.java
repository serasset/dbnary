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
}

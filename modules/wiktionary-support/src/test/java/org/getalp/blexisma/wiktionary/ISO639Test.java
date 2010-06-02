package org.getalp.blexisma.wiktionary;

import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertEquals;

public class ISO639Test {

    @Test
    public void testOnlyOneLanguage() {
        ISO639_1 isoLanguages = ISO639_1.sharedInstance;
        ISO639_1.Lang fre = isoLanguages.getLang("fre");
        ISO639_1.Lang fra = isoLanguages.getLang("fra");
        
        assertSame("Different Languages", fra, fre);
    }
    
    @Test
    public void testFrenchName() {
        ISO639_1 isoLanguages = ISO639_1.sharedInstance;
        String french = isoLanguages.getLanguageNameInEnglish("fre");
        
        assertEquals("fre is not French", "French", french);
    }
    
    @Test
    public void testFrenchIsFrancais() {
        ISO639_1 isoLanguages = ISO639_1.sharedInstance;
        ISO639_1.Lang french = isoLanguages.getLang("fre");
        
        assertEquals("Français eponyme n'est pas français", french.fr, french.epo);
    }
    
}

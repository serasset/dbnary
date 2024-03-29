package org.getalp.iso639;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

public class ISO639Test {

  @Test
  public void testOnlyOneLanguage() {
    ISO639_1 isoLanguages = ISO639_1.sharedInstance;
    ISO639_1.Lang fre = isoLanguages.getLang("fre");
    ISO639_1.Lang fra = isoLanguages.getLang("fra");

    assertSame("Different Languages", fra, fre);
  }

  @Test
  public void testOnlyOneLanguage3() {
    ISO639_3 isoLanguages = ISO639_3.sharedInstance;
    ISO639_3.Lang fre = isoLanguages.getLang("fre");
    ISO639_3.Lang fra = isoLanguages.getLang("fra");

    assertSame("Different Languages", fra, fre);
  }

  @Test
  public void testFrenchName() {
    ISO639_1 isoLanguages = ISO639_1.sharedInstance;
    String french = isoLanguages.getLanguageNameInEnglish("fre");

    assertEquals("fre is not French", "French", french);
  }

  @Test
  public void testEnglishName() {
    ISO639_1 isoLanguages = ISO639_1.sharedInstance;
    String en = isoLanguages.getLanguageNameInEnglish("eng");

    assertEquals("eng is not English", "English", en);
  }

  @Test
  public void testEnglishName3() {
    ISO639_3 isoLanguages = ISO639_3.sharedInstance;
    String en = isoLanguages.getLanguageNameInEnglish("eng");

    assertEquals("eng is not English", "English", en);
  }

  @Test
  public void testFrenchIsFrancais() {
    ISO639_1 isoLanguages = ISO639_1.sharedInstance;
    ISO639_1.Lang french = isoLanguages.getLang("fre");

    assertEquals("Français eponyme n'est pas français", french.fr, french.epo);
  }

  @Test
  public void testFrenchIsFrancais3() {
    ISO639_3 isoLanguages = ISO639_3.sharedInstance;
    String fepo = isoLanguages.getEponymLanguageName("fre");
    String ffr = isoLanguages.getLanguageNameInFrench("fre");

    assertEquals("Français eponyme n'est pas français", ffr, fepo);
  }

  @Test
  public void testTripleALanguage() {
    // When language definition file is saved with UTF-8 BOM (as it is in SIL web site), the aaa
    // language does not belong to the map.
    ISO639_3 isoLanguages = ISO639_3.sharedInstance;
    ISO639_3.Lang aaa = isoLanguages.getLang("aaa");

    assertNotNull(aaa);
  }

  @Test
  public void testTripleALanguage1() {
    // When language definition file is saved with UTF-8 BOM (as it is in SIL web site), the aaa
    // language does not belong to the map.
    ISO639_1 isoLanguages = ISO639_1.sharedInstance;
    ISO639_1.Lang aar = isoLanguages.getLang("aar");

    assertNotNull(aar);
  }
}

package org.getalp.dbnary.languages.deu;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class GermanLanguageCodesTest {

  @Test
  void getGermanCode() {
    assertEquals("deu", GermanLanguageCodes.getCode("Deutsch"));
  }

  @Test
  void getFrenchCode() {
    assertEquals("fra", GermanLanguageCodes.getCode("Französisch"));
  }

  @Test
  void getCodeShouldMatchUncased() {
    assertEquals("fra", GermanLanguageCodes.getCode("franzöSisch"));
  }

  @Test
  void getCodeShouldIgnoreLeadingAndTrailingWhitespaces() {
    assertEquals("apc", GermanLanguageCodes.getCode("Levantinisches Arabisch "));
    assertEquals("nah", GermanLanguageCodes.getCode("Klassisches Nahuatl\u200E "));
  }


}

package org.getalp.dbnary.wiki;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

class ParameterStandardizerTest {

  private ParameterStandardizer normalizer;

  @org.junit.jupiter.api.BeforeEach
  void setUp() {
    normalizer = new ParameterStandardizer(Map.ofEntries(entry("t", "t"), entry("trans", "t"),
        entry("transl", "t"), entry("other", "o"), entry("o", "o"), entry("alt", "a")));
  }

  @org.junit.jupiter.api.AfterEach
  void tearDown() {}


  @org.junit.jupiter.api.Test
  void getParameterPattern() {
    Pattern pattern = normalizer.getParameterPattern();
    assertNotNull(pattern);
    Predicate<String> matches = pattern.asMatchPredicate();
    assertTrue(matches.test("t"));
    assertTrue(matches.test("transl1"));
    assertTrue(matches.test("transl12"));
    assertTrue(matches.test("alt12"));
    assertTrue(matches.test("a12"));
    assertFalse(matches.test("transla1"));
    assertFalse(matches.test("transl_1"));
  }

  @org.junit.jupiter.api.Test
  void getParameterPatternWithNullMap() {
    ParameterStandardizer parameterStandardizer = new ParameterStandardizer(new HashMap<>());
    Pattern pattern = parameterStandardizer.getParameterPattern();
    assertNotNull(pattern);
    assertEquals("", pattern.toString());
  }


  @org.junit.jupiter.api.Test
  void getCanonicalParameters() {
    ParameterStandardizer parameterStandardizer = new ParameterStandardizer(new HashMap<>());
    Set<String> canonicalParameters = parameterStandardizer.getCanonicalParameters();
    assertNotNull(canonicalParameters);
  }

  @org.junit.jupiter.api.Test
  void normalizeParameters() {
    Map<String, String> parameters =
        Map.ofEntries(entry("transl1", "traduction 1"), entry("transl2", "traduction 2"),
            entry("trans3", "traduction 3"), entry("t4", "traduction 4"), entry("other", "autre"),
            entry("1", "autre"), entry("alt1", "alternative 1"), entry("a2", "alternative 2"));
    Map<String, String> normalizedParameters = normalizer.normalizeParameters(parameters);
    assertEquals(normalizedParameters.get("t1"), "traduction 1");
    assertEquals(normalizedParameters.get("t2"), "traduction 2");
    assertEquals(normalizedParameters.get("t3"), "traduction 3");
    assertEquals(normalizedParameters.get("t4"), "traduction 4");
    assertEquals(normalizedParameters.get("o"), "autre");
    assertEquals(normalizedParameters.get("1"), "autre");
    assertEquals(normalizedParameters.get("a1"), "alternative 1");
    assertEquals(normalizedParameters.get("a2"), "alternative 2");

  }
}

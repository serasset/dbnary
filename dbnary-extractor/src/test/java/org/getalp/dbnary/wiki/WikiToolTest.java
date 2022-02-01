package org.getalp.dbnary.wiki;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Map;
import org.junit.Test;

/**
 * Created by serasset on 02/03/16.
 */
public class WikiToolTest {

  @Test
  public void testParse1() throws Exception {
    Map<String, String> args = WikiTool.parseArgs("grc|sc=polytonic|βοῦς||ox, cow");

    assertEquals("grc", args.get("1"));
    assertEquals("polytonic", args.get("sc"));
    assertEquals("βοῦς", args.get("2"));
    assertEquals("ox, cow", args.get("4"));
    assertEquals("", args.get("3"));
    assertEquals(5, args.size());

  }

  @Test
  public void testParseWithTemplateNames() throws Exception {
    Map<String, String> args = WikiTool.parseArgs("m|grc|sc=polytonic|βοῦς||ox, cow",
        true);

    assertEquals("m", args.get("0"));
    assertEquals("grc", args.get("1"));
    assertEquals("polytonic", args.get("sc"));
    assertEquals("βοῦς", args.get("2"));
    assertEquals("ox, cow", args.get("4"));
    assertEquals("", args.get("3"));
    assertEquals(6, args.size());

  }

  @Test
  public void testParseWithErroneousTemplateNames() throws Exception {
    Map<String, String> args = WikiTool.parseArgs("m=x|grc|sc=polytonic|βοῦς||ox, cow",
        true);

    assertNull(args.get("0"));
    assertEquals("x", args.get("m"));
    assertEquals("grc", args.get("1"));
    assertEquals("polytonic", args.get("sc"));
    assertEquals("βοῦς", args.get("2"));
    assertEquals("ox, cow", args.get("4"));
    assertEquals("", args.get("3"));
    assertEquals(6, args.size());

  }


  @Test
  public void testRemoveReferences1() {
    String def = "tagada <ref name=\"toto\"/>.";
    assertEquals("tagada .", WikiTool.removeReferencesIn(def));
  }

  @Test
  public void testRemoveReferences2() {
    String def = "tagada <ref name=\"toto\">titi.";
    assertEquals("tagada ", WikiTool.removeReferencesIn(def));
  }

  @Test
  public void testRemoveReferences3() {
    String def = "tagada <ref name=\"toto\">titi</ref>.";
    assertEquals("tagada .", WikiTool.removeReferencesIn(def));
  }

  @Test
  public void testRemoveReferences4() {
    String def = "tagada <ref>titi</ref>.";
    assertEquals("tagada .", WikiTool.removeReferencesIn(def));
  }

  @Test
  public void testRemoveReferences5() {
    String def = "tagada <ref >titi</ref>.";
    assertEquals("tagada .", WikiTool.removeReferencesIn(def));
  }

}

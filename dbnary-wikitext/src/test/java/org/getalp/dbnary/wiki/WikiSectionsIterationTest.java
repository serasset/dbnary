package org.getalp.dbnary.wiki;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.Iterator;
import org.junit.Test;

/**
 * Created by serasset on 16/02/17.
 */
public class WikiSectionsIterationTest {

  private String testContent = "" + "==English==\n" + "\n" + "===Verb===\n" + "\n"
      + "====Translations====\n" + "{{template}} text text text\n" + "Content\n" + "===Noun===\n"
      + "\n" + "====Translations====\n" + "nominal translations\n" + "==French==\n" + "\n"
      + "===Noun===\n" + "\n" + "====Translations====\n" + "Translations of French Noun";

  @Test
  public void testWikiWikiSectionIteration() {
    WikiText text = new WikiText(testContent);

    Iterator<WikiText.WikiSection> it1 = text.sections(2).iterator();

    assertTrue(it1.hasNext());
    WikiText.WikiSection section2 = it1.next();
    assertEquals("English", section2.getHeading().getContent().toString());

    Iterator<WikiText.WikiSection> it2 = section2.getContent().sections(3).iterator();
    assertTrue(it2.hasNext());
    assertEquals("Verb", it2.next().getHeading().getContent().toString());
    assertTrue(it2.hasNext());
    assertEquals("Noun", it2.next().getHeading().getContent().toString());
    assertFalse(it2.hasNext());

    assertTrue(it1.hasNext());
    section2 = it1.next();
    assertEquals("French", section2.getHeading().getContent().toString());

    it2 = section2.getContent().sections(3).iterator();
    assertTrue(it2.hasNext());
    assertEquals("Noun", it2.next().getHeading().getContent().toString());
    assertFalse(it2.hasNext());

  }


}

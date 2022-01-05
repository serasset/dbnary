package org.getalp.dbnary.wiki;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.text.WordUtils;
import org.getalp.dbnary.wiki.WikiText.Heading;
import org.getalp.dbnary.wiki.WikiText.Text;
import org.getalp.dbnary.wiki.WikiText.Token;
import org.getalp.dbnary.wiki.WikiText.WikiContent;
import org.getalp.dbnary.wiki.WikiText.WikiDocument;
import org.getalp.dbnary.wiki.WikiText.WikiSection;
import org.junit.Test;

/**
 * Created by serasset on 16/02/17.
 */
public class WikiDocumentTest {

  @Test
  public void testWikiWikiDocument() {
    WikiText text = new WikiText("Preamble... \n" + "==English==\n" + "\n" + "===Verb===\n" + "\n"
        + "====Translations====\n" + "{{template}} text text text\n" + "Content\n" + "===Noun===\n"
        + "\n" + "====Translations====\n" + "nominal translations\n" + "==French==\n" + "\n"
        + "===Noun===\n" + "\n" + "====Translations====\n" + "Translations of French Noun");
    WikiText.WikiDocument doc = text.asStructuredDocument();

    List<Token> topTokens = doc.getContent().tokens();
    assertEquals("Incorrect number of section.", 3, topTokens.size());

    assertTrue("The first token should be a WikiText containing Preamble.",
        topTokens.get(0) instanceof Text);
    assertTrue("The second token should be a WikiSection.",
        topTokens.get(1) instanceof WikiSection);
    Heading h = topTokens.get(1).asWikiSection().getHeading();
    assertEquals("First WikiSection should be a level 2 header.", 2, h.getLevel());
    assertTrue("First WikiSection should have title containing \"English\".",
        h.getContent().toString().contains("English"));
    h = topTokens.get(2).asWikiSection().getHeading();
    assertEquals("First WikiSection should be a level 2 header.", 2, h.getLevel());
    assertTrue("First WikiSection should have title containing \"French\".",
        h.getContent().toString().contains("French"));

    // WikiTextPrinter.printDocumentTree(doc);
  }

}

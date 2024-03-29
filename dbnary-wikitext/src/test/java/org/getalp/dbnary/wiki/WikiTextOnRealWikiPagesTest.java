package org.getalp.dbnary.wiki;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import org.getalp.dbnary.wiki.WikiText.Heading;
import org.getalp.dbnary.wiki.WikiText.Token;
import org.getalp.dbnary.wiki.WikiText.WikiSection;
import org.junit.Test;

public class WikiTextOnRealWikiPagesTest extends CommonWikiTextLoader {

  private void testWith(String pagename) throws IOException {
    WikiText text = getWikiTextFor(pagename);

    // test standard WikiText structure
    assert !text.wikiTokens().isEmpty();

    List<Heading> h2FromWikiText = text.wikiTokens().stream().filter(h -> h instanceof Heading)
        .map(Token::asHeading).filter(h -> h.getLevel() == 2).collect(Collectors.toList());

    List<Heading> topLevelSections = text.asStructuredDocument().getContent().wikiTokens().stream()
        .filter(t -> t instanceof WikiSection).map(Token::asWikiSection)
        .map(WikiSection::getHeading).collect(Collectors.toList());

    assertThat(h2FromWikiText, is(topLevelSections));

    List<Heading> h3FromWikiText = text.wikiTokens().stream().filter(h -> h instanceof Heading)
        .map(Token::asHeading).filter(h -> h.getLevel() == 3).collect(Collectors.toList());

    List<Heading> h3Headings = text.asStructuredDocument().getContent().wikiTokens().stream()
        .filter(t -> t instanceof WikiSection).map(Token::asWikiSection)
        .map(WikiSection::getContent).flatMap(c -> c.wikiTokens().stream())
        .filter(t -> t instanceof WikiSection).map(Token::asWikiSection)
        .map(WikiSection::getHeading).collect(Collectors.toList());

    assertThat(h3FromWikiText, is(h3Headings));

    // WikiTextPrinter.printDocumentTree(text.asStructuredDocument());

    if (!text.sourceContent.contains("<!--"))
      assertEquals("Regenerated text should be equal to source text when there are no comments",
          text.sourceContent, text.content().getText());

  }

  @Test
  public void testCatFrench() throws Exception {
    testWith("cat_fr");
  }

  @Test
  public void testMousseFrench() throws Exception {
    testWith("mousse_fr");
  }

  @Test
  public void tesChatFrench() throws Exception {
    testWith("chat_fr");
  }

  @Test
  public void testBleuFrench() throws Exception {
    testWith("bleu_fr");
  }

  @Test
  public void testDefinitionExtractBleuFrench() throws Exception {
    WikiText text = getWikiTextFor("bleu_extract_definitions_fr");

    // test standard WikiText structure
    assertFalse(text.wikiTokens().isEmpty());

  }

}

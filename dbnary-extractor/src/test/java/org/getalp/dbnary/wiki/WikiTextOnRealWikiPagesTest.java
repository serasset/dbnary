package org.getalp.dbnary.wiki;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

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

public class WikiTextOnRealWikiPagesTest {

  private WikiText getWikiTextFor(String pagename) throws IOException {
    InputStream inputStream = WikiTextOnRealWikiPagesTest.class
        .getResourceAsStream(pagename + ".wiki");
    ByteArrayOutputStream result = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    int length;
    while ((length = inputStream.read(buffer)) != -1) {
      result.write(buffer, 0, length);
    }
    String pageContent = result.toString(StandardCharsets.UTF_8.name());
    return new WikiText(pagename, pageContent);
  }

  private void testWith(String pagename) throws IOException {
    WikiText text = getWikiTextFor(pagename);

    // test standard WikiText structure
    assert !text.wikiTokens().isEmpty();

    List<Heading> h2FromWikiText = text.wikiTokens().stream()
        .filter(h -> h instanceof Heading)
        .map(Token::asHeading)
        .filter(h -> h.getLevel() == 2)
        .collect(Collectors.toList());

    List<Heading> topLevelSections = text.asStructuredDocument().getContent().wikiTokens().stream()
        .filter(t -> t instanceof WikiSection)
        .map(Token::asWikiSection)
        .map(WikiSection::getHeading)
        .collect(Collectors.toList());

    assertThat(h2FromWikiText, is(topLevelSections));

    List<Heading> h3FromWikiText = text.wikiTokens().stream()
        .filter(h -> h instanceof Heading)
        .map(Token::asHeading)
        .filter(h -> h.getLevel() == 3)
        .collect(Collectors.toList());

    List<Heading> h3Headings = text.asStructuredDocument().getContent().wikiTokens().stream()
        .filter(t -> t instanceof WikiSection)
        .map(Token::asWikiSection)
        .map(WikiSection::getContent)
        .flatMap(c -> c.wikiTokens().stream())
        .filter(t -> t instanceof WikiSection)
        .map(Token::asWikiSection)
        .map(WikiSection::getHeading)
        .collect(Collectors.toList());

    assertThat(h3FromWikiText, is(h3Headings));

    WikiDocumentTest.printDocumentTree(text.asStructuredDocument());

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
    assert !text.wikiTokens().isEmpty();

  }

}

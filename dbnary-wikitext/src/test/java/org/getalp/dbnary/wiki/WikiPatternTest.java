package org.getalp.dbnary.wiki;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

/**
 * Created by serasset on 06/03/17.
 */
public class WikiPatternTest {

  @Test
  public void testWikiPattern() {
    String test = "{{en-noun}} text [[link]]s text {{template}} text [[toto]]";
    String p = "(\\p{Template})\\P{Reserved}*\\p{Link}.*";

    WikiCharSequence seq = new WikiCharSequence(new WikiText(test));
    Pattern pat = WikiPattern.compile(p);

    Matcher m = pat.matcher(seq);
    assertTrue(m.matches());

    assertTrue(seq.getToken(m.group(1)) instanceof WikiText.Template);
  }

  @Test
  public void testWikiPatternWithOpenClose() {
    String test =
        "==== Header 4 ====\n" + "{{en-noun}} text [[link]]s text {{template}} text [[toto]]";
    String p = "(_H1_(.*)_H1_)";

    WikiCharSequence seq = new WikiCharSequence(new WikiText(test));
    Pattern pat = WikiPattern.compile(p);

    Matcher m = pat.matcher(seq);
    assertTrue(m.lookingAt());
    WikiText.Token heading = seq.getToken(m.group("H1"));
    assertTrue(heading instanceof WikiText.Heading);
    WikiText.Heading h = (WikiText.Heading) heading;
    assertEquals(4, h.getLevel());

  }

}

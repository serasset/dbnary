package org.getalp.dbnary;

import static org.junit.Assert.assertEquals;
import org.apache.commons.lang3.StringEscapeUtils;
import org.junit.Test;

/**
 * Created by serasset on 11/12/16.
 */
public class ApacheCommonsLangTest {

  // Fails in commons lang v. 3.0
  @Test
  public void testUnescap() {
    String s = "\uD840\uDD0C#Chinese|\uD840\uDD0C";
    String t = StringEscapeUtils.unescapeHtml4(s);
    assertEquals(s, t);
  }
}

package org.getalp.dbnary;

import org.apache.commons.lang3.StringEscapeUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by serasset on 11/12/16.
 */
public class ApacheCommonsLangTest {

    @Test
    public void testUnescap() {
        String s = "\uD840\uDD0C#Chinese|\uD840\uDD0C";
        String t = StringEscapeUtils.unescapeHtml4(s);
        assertEquals(s, t);
    }
}

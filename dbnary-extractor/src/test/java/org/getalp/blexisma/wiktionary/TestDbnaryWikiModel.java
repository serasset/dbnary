package org.getalp.blexisma.wiktionary;

import org.getalp.dbnary.DefaultGlossFilter;
import org.getalp.dbnary.rus.GlossFilter;
import org.getalp.dbnary.rus.RussianTranslationExtractorWikiModel;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Locale;

public class TestDbnaryWikiModel {
    RussianTranslationExtractorWikiModel wikiModel;

    @Before
    public void setUp() throws Exception {
        wikiModel = new RussianTranslationExtractorWikiModel(null, new Locale("fr"), "http://www.mywiki.com/wiki/${image}",
                "http://www.mywiki.com/wiki/${title}", new GlossFilter());
    }

    @Test
    public void test() throws IOException {
        String wikiText = "some wiki [[text]] we would like to {{convert}} to HTML\n" +
                "* list item 1\n" +
                "* list item 2\n" +
                "** nested item";
        String htmlStr = wikiModel.render(wikiText);
        System.err.print(htmlStr);
    }

}

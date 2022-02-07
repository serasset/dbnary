package org.getalp.dbnary.languages.eng;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.getalp.dbnary.wiki.WikiText;
import org.getalp.dbnary.wiki.WikiText.Template;
import org.getalp.dbnary.wiki.WikiText.Token;
import org.junit.Test;

public class EnglishTranslationTemplateStreamTest {

  @Test
  public void testTemplateStream() {
    String source = "{{trans-top|absorbing all light}}\n" + "{{multitrans|data=\n"
        + "* Abkhaz: {{tt|ab|аиқәаҵәа}}\n" + "* Acehnese: {{tt|ace|itam}}\n"
        + "* Afrikaans: {{tt+|af|swart}}\n" + "* Albanian: {{tt+|sq|zi}}\n"
        + "* Amharic: {{tt|am|ጥቁር}}\n"
        + "* Arabic: {{tt+|ar|أَسْوَد|m}}, {{tt|ar|سَوْدَاء|f}}, {{tt|ar|سُود|p}}\n"
        + "*: Moroccan Arabic: {{tt|ary|كحل|tr=kḥal}}\n" + "* Armenian: {{tt|hy|սև}}\n"
        + "* Aromanian: {{tt|rup|negru}}, {{tt+|rup|laiu}}\n"
        + "* Asháninka: {{tt|cni|cheenkari}}, {{tt|cni|kisaari}}\n"
        + "* Assamese: {{tt|as|ক\u200C’লা}}, {{tt|as|কুলা}} {{qualifier|Central}}\n"
        + "* Asturian: {{tt|ast|ñegru}}, {{tt|ast|negru}}, {{tt|ast|prietu}}\n"
        + "* Atikamekw: {{tt|atj|makatewaw}}\n" + "* Avar: {{tt|av|чӏегӏера}}\n"
        + "* Aymara: {{tt|ay|ch’iyara}}\n" + "* Azerbaijani: {{tt+|az|qara}}\n" + "etc.\n"
        + "{{trans-bottom}}\n" + "\n" + "{{trans-top|without light}}\n"
        + "* Bulgarian: {{tt+|bg|тъмен}}\n" + "* Catalan: {{tt+|ca|fosc}}\n"
        + "* Dutch: {{tt+|nl|donker}}\n" + "* Esperanto: {{tt+|eo|malhela}}\n"
        + "* Estonian: {{tt+|et|pime}}\n" + "* Finnish: {{tt+|fi|pimeä}}\n"
        + "* Georgian: {{tt|ka|ბნელი}}, {{tt|ka|მუქი}}, {{tt|ka|ბუნდოვანი}}\n"
        + "* Greek: {{tt+|el|σκοτεινός|m}}, {{tt+|el|ερεβώδης|m}}\n" + "[etc.]\n"
        + "}}<!-- close {{multitrans}} -->\n" + "{{trans-bottom}}";

    WikiText wt = new WikiText(source);
    Stream<Template> s = new EnglishTranslationTemplateStream().visit(wt.content());
    List<Template> l = s.map(Token::asTemplate).collect(Collectors.toList());
    assertEquals("trans-top", l.get(0).getName());
    // multitrans template is voided and its content should be directly available now.
    assertEquals("tt", l.get(1).getName());
    assertEquals("tt", l.get(2).getName());
    assertEquals("tt+", l.get(3).getName());
    assertEquals("tt+", l.get(4).getName());
    assertEquals("qualifier", l.get(17).getName());
    assertEquals("trans-bottom", l.get(25).getName());
    assertEquals("trans-top", l.get(26).getName());
    assertEquals("trans-bottom", l.get(38).getName());
    assertEquals(39, l.size());
  }
}

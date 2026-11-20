package org.getalp.dbnary.languages.bul;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import java.util.stream.Collectors;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.getalp.dbnary.DBnaryOnt;
import org.getalp.dbnary.OntolexOnt;
import org.junit.jupiter.api.Test;

public class WiktionaryExtractorTest {

  private static final String PAGE = """
      {{-bg-}}
      {{Съществително нарицателно име
      |ID=куче
      |ЗНАЧЕНИЕ=
      # Домашно животно.
      #: Пример, който не е значение.
      # [[Пазач]] на стадо.
      |ПРЕВОД=
      * английски: [[dog]]
      |Синоними=
      * [[пес]], [[псе|псето]]
      }}
      {{Словоформи/куче}}
      {{-ru-}}
      {{Глагол
      |ЗНАЧЕНИЕ=
      # Не трябва да се извлича.
      }}
      """;

  @Test
  public void extractsBulgarianEntryFromTemplateArguments() {
    WiktionaryDataHandler wdh = new WiktionaryDataHandler("bul", null);
    WiktionaryExtractor we = new WiktionaryExtractor(wdh);
    we.setWiktionaryIndex(null);
    we.extractData("куче", PAGE);
    Model m = wdh.getEndolexFeatureBox(org.getalp.dbnary.ExtractionFeature.MAIN);

    assertEquals(1, m.listSubjectsWithProperty(RDF.type, OntolexOnt.LexicalEntry).toList().size());
    assertEquals(2, m.listSubjectsWithProperty(RDF.type, OntolexOnt.LexicalSense).toList().size());
    assertEquals(1, m.listSubjectsWithProperty(RDF.type, DBnaryOnt.Translation).toList().size());
    assertEquals(2, m.listStatements(null, DBnaryOnt.synonym, (org.apache.jena.rdf.model.RDFNode) null).toList().size());
  }

  // Translation lines taken from bg.wiktionary entries забележка, плавник, чума, храня and алчен
  private static final String TRANSLATIONS_PAGE = """
      {{-bg-}}
      {{Съществително нарицателно име|
        ID = тест
      | ЗНАЧЕНИЕ =
      #  Тест.
      | ПРЕВОД1 =
      * английски: {{п|en|plague, pestilence, black death}}, (под черта) - {{п|en|foot-note}}
      * белоруски: {{п|be|}}
      | ПРЕВОД2 =
      * немски: die {{п|de|Flosse}}
      * руски: {{п|ru| жа́дный, а́лчный}}
      * френски: {{п|fr|nourrir}}, {{п|fr|alimenter}}
      }}
      """;

  private static Set<String> translations(Model m, String lang) {
    Resource l = m.createResource("http://lexvo.org/id/iso639-3/" + lang);
    return m.listSubjectsWithProperty(DBnaryOnt.targetLanguage, l).toList().stream()
        .map(t -> t.getProperty(DBnaryOnt.writtenForm).getString() + (t.hasProperty(DBnaryOnt.usage) ? " / " + t.getProperty(DBnaryOnt.usage).getString() : ""))
        .collect(Collectors.toSet());
  }

  @Test
  public void extractsTranslationTemplates() {
    WiktionaryDataHandler wdh = new WiktionaryDataHandler("bul", null);
    WiktionaryExtractor we = new WiktionaryExtractor(wdh);
    we.setWiktionaryIndex(null);
    we.extractData("тест", TRANSLATIONS_PAGE);
    Model m = wdh.getEndolexFeatureBox(org.getalp.dbnary.ExtractionFeature.MAIN);

    assertEquals(Set.of("plague", "pestilence", "black death", "foot-note / под черта"), translations(m, "eng"));
    assertEquals(Set.of("Flosse / prefix=die"), translations(m, "deu"));
    assertEquals(Set.of("жа́дный", "а́лчный"), translations(m, "rus"));
    assertEquals(Set.of("nourrir", "alimenter"), translations(m, "fra"));
    assertEquals(Set.of(), translations(m, "bel"));
  }
}

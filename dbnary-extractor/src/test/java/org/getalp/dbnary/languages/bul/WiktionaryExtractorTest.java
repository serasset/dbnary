package org.getalp.dbnary.languages.bul;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.jena.rdf.model.Model;
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
}

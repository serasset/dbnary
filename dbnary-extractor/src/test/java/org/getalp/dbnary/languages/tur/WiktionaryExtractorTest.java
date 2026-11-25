package org.getalp.dbnary.languages.tur;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.getalp.dbnary.DBnaryOnt;
import org.getalp.dbnary.ExtractionFeature;
import org.getalp.dbnary.OntolexOnt;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Pages taken verbatim from the trwiktionary 2026-11-01 dump (src/test/resources/.../tur). Enabled
 * tests pin what the extractor currently gets right; @Disabled ones describe what it misses.
 */
public class WiktionaryExtractorTest {

  private static Model extract(String resource, String title) throws IOException {
    String page;
    try (InputStream in = WiktionaryExtractorTest.class.getResourceAsStream(resource + ".wiki")) {
      page = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
    return extractPage(title, page);
  }

  private static Model extractPage(String title, String page) {
    WiktionaryDataHandler wdh = new WiktionaryDataHandler("tur", null);
    WiktionaryExtractor we = new WiktionaryExtractor(wdh);
    we.setWiktionaryIndex(null);
    we.extractData(title, page);
    return wdh.getEndolexFeatureBox(ExtractionFeature.MAIN);
  }

  private static int count(Model m, Resource type) {
    return m.listSubjectsWithProperty(RDF.type, type).toList().size();
  }

  private static Set<String> strings(Model m, Property p) {
    return m.listObjectsOfProperty(p).toList().stream().map(n -> n.isLiteral() ? n.asLiteral().getString() : n.asResource().getLocalName())
        .collect(Collectors.toSet());
  }

  private static Set<String> examples(Model m) {
    return m.listObjectsOfProperty(SKOS.example).toList().stream().map(n -> n.asResource().getProperty(RDF.value).getString().trim())
        .collect(Collectors.toSet());
  }

  /**
   * Nym targets per source (entry or sense, as "tur:" local part), e.g. "__ws_1_beyaz__Ad__1" ->
   * {"ak"}.
   */
  private static java.util.Map<String, Set<String>> nyms(Model m, Property relation) {
    return m.listStatements(null, relation, (RDFNode) null).toList().stream()
        .collect(Collectors.groupingBy(st -> st.getSubject().getURI().substring(TUR.length()),
            Collectors.mapping(st -> st.getObject().asResource().getURI().substring(TUR.length()), Collectors.toSet())));
  }

  /** Same as nyms, restricted to the given sources (absent sources are left out of the map). */
  private static java.util.Map<String, Set<String>> nyms(Model m, Property relation, String... sources) {
    java.util.Map<String, Set<String>> all = nyms(m, relation);
    all.keySet().retainAll(Set.of(sources));
    return all;
  }

  // --------------------------------------------------------------------------------------------
  // Current behaviour (passing)
  // --------------------------------------------------------------------------------------------

  @Test
  public void extractsEntriesSensesAndTranslations() throws IOException {
    record E(String res, String title, int entries, int senses, int translations) {}
    for (E e : new E[] {new E("aralik", "aralık", 3, 13, 80), new E("uc", "üç", 3, 5, 104), new E("ayak", "ayak", 1, 19, 88),
        new E("gercek", "gerçek", 2, 10, 109), new E("limon_gibi", "limon gibi", 1, 1, 0), new E("yuvarlamak", "yuvarlamak", 1, 8, 1)}) {
      Model m = extract(e.res, e.title);
      assertEquals(e.entries, count(m, OntolexOnt.LexicalEntry), e.title + " entries");
      assertEquals(e.senses, count(m, OntolexOnt.LexicalSense), e.title + " senses");
      assertEquals(e.translations, count(m, DBnaryOnt.Translation), e.title + " translations");
    }
  }

  @Test
  public void extractsTranslationGlossesFromUstTemplate() throws IOException {
    Model m = extract("aralik", "aralık");
    Set<String> glosses =
        m.listSubjectsWithProperty(RDF.type, DBnaryOnt.Gloss).toList().stream().map(g -> g.getProperty(RDF.value).getString()).collect(Collectors.toSet());
    assertEquals(Set.of("aylar", "yarı açık"), glosses);
  }

  @Test
  public void ignoresNonTurkishLanguageSections() throws IOException {
    // üç also has Azerice and Kırım Tatarcası sections, gerçek has Gagavuzca and Türkmence ones.
    assertEquals(3, count(extract("uc", "üç"), OntolexOnt.LexicalEntry));
    assertEquals(2, count(extract("gercek", "gerçek"), OntolexOnt.LexicalEntry));
  }

  // --------------------------------------------------------------------------------------------
  // Missing elements (disabled until the extractor catches up with the current tr.wiktionary)
  // --------------------------------------------------------------------------------------------

  @Disabled("{{tr-IPA}} (6k pages) is ignored: only {{IPA|..}} / {{Çeviri Yazı|..}} with an explicit arg are read")
  @Test
  public void extractsPronunciation() throws IOException {
    for (String[] p : new String[][] {{"aralik", "aralık"}, {"uc", "üç"}, {"ayak", "ayak"}, {"gercek", "gerçek"}, {"yuvarlamak", "yuvarlamak"}}) {
      assertFalse(strings(extract(p[0], p[1]), OntolexOnt.phoneticRep).isEmpty(), p[1] + " has no pronunciation");
    }
  }

  // Nyms given in the definitions belong to their sense. Senses are tur:__ws_<n>_<entry>.

  @Test
  public void extractsNymsGivenAfterTheDefinitionOnTheirSense() throws IOException {
    Model m = extract("aralik", "aralık");
    // Sense 1 has 2 "#*" lines: {{eş anlamlılar|tr|fasıla}} and {{ux|..}}; only the ux is an example.
    assertEquals(1, m.getResource(TUR + "__ws_1_aralık__Ad__1").listProperties(SKOS.example).toList().size(), "nym list stored as example");
    assertEquals(
        java.util.Map.of("__ws_1_aralık__Ad__1", Set.of("fasıla"), "__ws_4_aralık__Ad__1", Set.of("birinci_kânun", "ilk_kânun", "kânunuevvel"),
            "__ws_6_aralık__Ad__1", Set.of("espas")),
        nyms(m, DBnaryOnt.synonym, "aralık__Ad__1", "__ws_1_aralık__Ad__1", "__ws_2_aralık__Ad__1", "__ws_4_aralık__Ad__1", "__ws_6_aralık__Ad__1"));
    assertEquals(java.util.Map.of("__ws_3_ayak__Ad__1", Set.of("bacak"), "__ws_8_ayak__Ad__1", Set.of("kadem", "fit", "fut")),
        nyms(extract("ayak", "ayak"), DBnaryOnt.synonym, "ayak__Ad__1", "__ws_3_ayak__Ad__1", "__ws_8_ayak__Ad__1"));
    assertEquals(java.util.Map.of("__ws_1_gerçek__Ad__1", Set.of("çın", "ciddi", "hakikat"), "__ws_2_gerçek__Ön_ad__1", Set.of("özbeöz", "hakiki", "reel")),
        nyms(extract("gercek", "gerçek"), DBnaryOnt.synonym, "gerçek__Ad__1", "gerçek__Ön_ad__1", "__ws_1_gerçek__Ad__1", "__ws_2_gerçek__Ön_ad__1"));
    assertEquals(java.util.Map.of("__ws_1_yuvarlamak__Eylem__1", Set.of("tekerlemek")), nyms(extract("yuvarlamak", "yuvarlamak"), DBnaryOnt.synonym));
    // beyaz sense 1: "#* {{eş anlamlılar|tr|ak}}" then "#* {{zıt anlamlılar|tr|kara }}".
    m = extract("beyaz", "beyaz");
    assertEquals(0, m.getResource(TUR + "__ws_1_beyaz__Ad__1").listProperties(SKOS.example).toList().size(), "nym lists stored as examples");
    assertEquals(java.util.Map.of("__ws_1_beyaz__Ad__1", Set.of("ak")), nyms(m, DBnaryOnt.synonym, "beyaz__Ad__1", "__ws_1_beyaz__Ad__1"));
    assertEquals(java.util.Map.of("__ws_1_beyaz__Ad__1", Set.of("kara")), nyms(m, DBnaryOnt.antonym));
    // ateist: same "#:" lists after the single sense of both Ad and Ön ad.
    m = extract("ateist", "ateist");
    assertEquals(java.util.Map.of("__ws_1_ateist__Ad__1", Set.of("ate", "tanrıtanımaz"), "__ws_1_ateist__Ön_ad__1", Set.of("ate", "tanrıtanımaz")),
        nyms(m, DBnaryOnt.synonym));
    assertEquals(java.util.Map.of("__ws_1_ateist__Ad__1", Set.of("teist", "tanrıtanır"), "__ws_1_ateist__Ön_ad__1", Set.of("teist", "tanrıtanır")),
        nyms(m, DBnaryOnt.antonym));
  }

  @Test
  public void extractsSynonymOnlyDefinitionsOnTheirSense() throws IOException {
    // The sense (and its definition text) is kept; X becomes a synonym of that sense.
    assertEquals(java.util.Map.of("__ws_3_aralık__Ad__1", Set.of("koridor"), "__ws_5_aralık__Ad__1", Set.of("tuvalet")),
        nyms(extract("aralik", "aralık"), DBnaryOnt.synonym, "aralık__Ad__1", "__ws_3_aralık__Ad__1", "__ws_5_aralık__Ad__1"));
    assertEquals(
        java.util.Map.of("__ws_5_ayak__Ad__1", Set.of("basamak"), "__ws_6_ayak__Ad__1", Set.of("ayakkabı"), "__ws_9_ayak__Ad__1", Set.of("göl_ayağı"),
            "__ws_17_ayak__Ad__1", Set.of("hile"), "__ws_18_ayak__Ad__1", Set.of("rol_yapmak"), "__ws_19_ayak__Ad__1", Set.of("ilişki")),
        nyms(extract("ayak", "ayak"), DBnaryOnt.synonym, "ayak__Ad__1", "__ws_5_ayak__Ad__1", "__ws_6_ayak__Ad__1", "__ws_9_ayak__Ad__1", "__ws_17_ayak__Ad__1",
            "__ws_18_ayak__Ad__1", "__ws_19_ayak__Ad__1"));
    assertEquals(java.util.Map.of("__ws_2_gerçek__Ad__1", Set.of("gerçeklik"), "__ws_3_gerçek__Ad__1", Set.of("doğruluk")),
        nyms(extract("gercek", "gerçek"), DBnaryOnt.synonym, "gerçek__Ad__1", "__ws_2_gerçek__Ad__1", "__ws_3_gerçek__Ad__1"));
    // beyaz sense 4: "# {{eş anlamlı|dil=tr|beyaz zehir }}" (trailing spaces trimmed).
    assertEquals(java.util.Map.of("__ws_4_beyaz__Ad__1", Set.of("beyaz_zehir")),
        nyms(extract("beyaz", "beyaz"), DBnaryOnt.synonym, "beyaz__Ad__1", "__ws_4_beyaz__Ad__1"));
  }

  // Nyms given in their own ====Eş/Karşıt/Zıt anlamlılar==== / ====Üst/Alt kavramlar==== section
  // belong to the entry of the PoS section holding them.

  @Test
  public void extractsNymSectionsOnTheirEntry() throws IOException {
    Model m = extract("empati", "empati");
    assertEquals(java.util.Map.of("empati__Ad__1", Set.of("eşduyum", "anlayış", "duyarlılık", "duygudaşlık")), nyms(m, DBnaryOnt.synonym));
    assertEquals(java.util.Map.of("empati__Ad__1", Set.of("anlayışsızlık", "duyarsızlık", "egoizm")), nyms(m, DBnaryOnt.antonym));
    assertEquals(java.util.Map.of("empati__Ad__1", Set.of("davranış", "duygu", "psikoloji")), nyms(m, DBnaryOnt.hypernym));
    // Guamlı: ====Zıt anlamlılar==== under Ön ad, ====Üst kavramlar==== under Özel ad.
    m = extract("guamli", "Guamlı");
    assertEquals(java.util.Map.of("Guamlı__Ön_ad__1", Set.of("Guamsız")), nyms(m, DBnaryOnt.antonym));
    // Özel ad has a single sense: OntolexBasedRDFDataHandler.promoteNymProperties moves entry nyms to
    // it.
    assertEquals(java.util.Map.of("__ws_1_Guamlı__Özel_ad__1", Set.of("Mikronezyalı")), nyms(m, DBnaryOnt.hypernym));
    // kırmızı: "* {{mânâ|mecâzî de olur}} [[ahmer]] {{şerh|eskimiş}}" and a comma separated link list.
    m = extract("kirmizi", "kırmızı");
    // kırmızı Ad has a single sense: nyms promoted to it (see Guamlı Özel ad).
    assertEquals(java.util.Map.of("__ws_1_kırmızı__Ad__1", Set.of("ahmer")), nyms(m, DBnaryOnt.synonym));
    assertEquals(java.util.Map.of("__ws_1_kırmızı__Ad__1", Set.of("ışık", "renk", "tayf")), nyms(m, DBnaryOnt.hypernym));
  }

  // --------------------------------------------------------------------------------------------
  // Multi word expressions
  // --------------------------------------------------------------------------------------------

  @Test
  public void typesProverbsAsMultiWordExpressions() throws IOException {
    Model m = extract("misafir_uc_gun_misafirdir", "misafir üç gün misafirdir");
    assertEquals(1, count(m, OntolexOnt.MultiWordExpression));
  }

  @Test
  public void typesIdiomsAsMultiWordExpressions() throws IOException {
    Model m = extract("limon_gibi", "limon gibi");
    assertEquals(1, count(m, OntolexOnt.MultiWordExpression));
    // Every entry is also typed ontolex:LexicalEntry
    // (OntolexBasedRDFDataHandler.initializeLexicalEntry).
    assertEquals(1, count(m, OntolexOnt.LexicalEntry));
  }

  @Test
  public void extractsDerivedTermsIdiomsAndProverbs() throws IOException {
    Model m = extract("uc", "üç");
    // 31 derived terms + 6 idioms + 5 proverbs
    assertEquals(42, m.listStatements(null, DBnaryOnt.derivedFrom, (RDFNode) null).toList().size());
    m = extract("aralik", "aralık");
    Set<String> derived = m.listSubjectsWithProperty(DBnaryOnt.derivedFrom).toList().stream().map(Resource::getLocalName).collect(Collectors.toSet());
    assertTrue(derived.containsAll(Set.of("aralık_korozyonu", "hava_aralığı", "aralık_vermek")));
  }

  @Test
  public void extractsSayiAdiAsNumeral() {
    Model m = extractPage("dört", "==Türkçe==\n===Sayı adı===\n# [[üç]]ten sonra gelen sayı\n" + "====Türetilmiş kavramlar====\n[[dörtlü]]\n");
    Resource e = m.getResource(TUR + "dört__Sayı_adı__1");
    assertTrue(m.contains(e, org.getalp.dbnary.LexinfoOnt.partOfSpeech, org.getalp.dbnary.LexinfoOnt.numeral));
    assertEquals(1, senseCount(m, "dört__Sayı_adı__1"));
    assertTrue(m.contains(m.getResource(TUR + "dörtlü"), DBnaryOnt.derivedFrom, e));
  }

  @Test
  public void extractsOtherPosHeadings() {
    Model m = extractPage("x", "==Türkçe==\n===Sıral sayı===\n# a\n===Yardımcı eylem===\n# b\n===Ek eylem===\n# c\n"
        + "===Ön Ad===\n# d\n===Önad===\n# e\n===Ad 2===\n# f\n===Birleşik ad===\n# g\n");
    assertEquals(Set.of("x__Sıral_sayı__1", "x__Yardımcı_eylem__1", "x__Ek_eylem__1", "x__Ön_Ad__1", "x__Önad__1", "x__Ad__1", "x__Birleşik_ad__1"),
        m.listSubjectsWithProperty(OntolexOnt.sense).toList().stream().map(r -> r.getURI().substring(TUR.length())).collect(Collectors.toSet()));
    assertTrue(m.contains(m.getResource(TUR + "x__Sıral_sayı__1"), org.getalp.dbnary.LexinfoOnt.partOfSpeech, org.getalp.dbnary.LexinfoOnt.ordinalAdjective));
    assertTrue(m.contains(m.getResource(TUR + "x__Ek_eylem__1"), org.getalp.dbnary.LexinfoOnt.partOfSpeech, org.getalp.dbnary.LexinfoOnt.copula));
    assertTrue(m.contains(m.getResource(TUR + "x__Önad__1"), org.getalp.dbnary.LexinfoOnt.partOfSpeech, org.getalp.dbnary.LexinfoOnt.adjective));
  }

  @Test
  public void skipsMisspellingSections() {
    Model m = extractPage("Moldovya", "==Türkçe==\n===Yanlış===\n# {{mânâ|doğrusu}} [[Moldova]]\n");
    assertEquals(0, count(m, OntolexOnt.LexicalEntry));
  }

  @Test
  public void extractsTranslationGenderAndTransliteration() throws IOException {
    for (String[] p : new String[][] {{"aralik", "aralık"}, {"uc", "üç"}, {"ayak", "ayak"}, {"gercek", "gerçek"}}) {
      assertTrue(strings(extract(p[0], p[1]), DBnaryOnt.usage).stream().noneMatch(u -> u.startsWith("{")), p[1] + " has raw map usages");
    }
    Model m = extract("gercek", "gerçek");
    Resource rus = m.listSubjectsWithProperty(DBnaryOnt.writtenForm, m.createLiteral("истина", "ru")).next();
    assertTrue(rus.hasProperty(DBnaryOnt.usage) && rus.getProperty(DBnaryOnt.usage).getString().contains("ístina"));
    // {{ç|ru|истина|f}} [ístina]: gender then transliteration; {{ç|ru|правда}} [pravda].
    assertEquals(Set.of("f|tr=ístina"), usagesOf(m, "истина", "ru"));
    assertEquals(Set.of("tr=pravda"), usagesOf(m, "правда", "ru"));
    // {{ç|ko|셋|tr=sed}}
    assertEquals(Set.of("tr=sed"), usagesOf(extract("uc", "üç"), "셋", "ko"));
    // {{ç|el|φασόλι|n}} {{belirtici|ürün}}, {{ç|el|φασολιά|d}} {{belirtici|bitki}}
    Model f = extract("fasulye", "fasulye");
    assertEquals(Set.of("n|ürün"), usagesOf(f, "φασόλι", "el"));
    assertEquals(Set.of("d|bitki"), usagesOf(f, "φασολιά", "el"));
  }

  @Test
  public void extractsCeviriTemplatesLikeC() {
    Model m = extractPage("dogmatizm", "==Türkçe==\n===Ad===\n# a\n====Çeviriler====\n" + "*Almanca: {{ç|de|Dogmatismus|m}}, {{çeviri|de|Rechthaberei|f}}\n");
    assertEquals(Set.of("m"), usagesOf(m, "Dogmatismus", "de"));
    assertEquals(Set.of("f"), usagesOf(m, "Rechthaberei", "de"));
  }

  private static Set<String> usagesOf(Model m, String word, String lang) {
    return m.listSubjectsWithProperty(DBnaryOnt.writtenForm, m.createLiteral(word, lang)).toList().stream()
        .map(r -> r.hasProperty(DBnaryOnt.usage) ? r.getProperty(DBnaryOnt.usage).getString() : "").collect(Collectors.toSet());
  }

  // --------------------------------------------------------------------------------------------
  // Translation attachment. tr.wiktionary uses two layouts:
  // - per entry: several PoS each have their own ====Çeviriler==== -> attach to the entry whose
  // section holds the block (kırmızı, sekiz, üç);
  // - at end: only the last PoS has ====Çeviriler====, holding blocks for all entries -> attach a
  // block to an entry only if its {{Üst|gloss}} names the PoS of exactly one entry, else to the
  // page (dbnary:Page), leaving sense/entry disambiguation to the enhancer (aralık, yarasa, gerçek,
  // ağır, aç, abanoz).
  // A single-PoS page attaches to its only entry either way (ayak, yuvarlamak).
  // --------------------------------------------------------------------------------------------

  private static final String TUR = "http://kaiko.getalp.org/dbnary/tur/";

  private static String target(Resource translation) {
    return translation.getPropertyResourceValue(DBnaryOnt.isTranslationOf).getURI().substring(TUR.length());
  }

  /** Translation count per attachment, e.g. "kırmızı__Ad__1" = 98. */
  private static java.util.Map<String, Long> attachments(Model m) {
    return m.listSubjectsWithProperty(RDF.type, DBnaryOnt.Translation).toList().stream()
        .collect(Collectors.groupingBy(WiktionaryExtractorTest::target, Collectors.counting()));
  }

  /** Translation count per "gloss -> attachment", e.g. "ön ad -> gerçek__Ön_ad__1" = 38. */
  private static java.util.Map<String, Long> glossAttachments(Model m) {
    return m.listSubjectsWithProperty(RDF.type, DBnaryOnt.Translation).toList().stream().collect(Collectors.groupingBy(t -> {
      Resource gloss = t.getPropertyResourceValue(DBnaryOnt.gloss);
      return (gloss == null ? "" : gloss.getProperty(RDF.value).getString()) + " -> " + target(t);
    }, Collectors.counting()));
  }

  @Test
  public void attachesTranslationsToTheOnlyEntryOfSinglePosPages() throws IOException {
    assertEquals(java.util.Map.of("ayak__Ad__1", 88L), attachments(extract("ayak", "ayak")));
    assertEquals(java.util.Map.of("yuvarlamak__Eylem__1", 1L), attachments(extract("yuvarlamak", "yuvarlamak")));
  }

  @Test
  public void attachesPerEntryTranslationSectionsToTheirEntry() throws IOException {
    assertEquals(java.util.Map.of("kırmızı__Ad__1", 98L, "kırmızı__Ön_ad__1", 20L), attachments(extract("kirmizi", "kırmızı")));
    // The section position wins over the gloss: {{üst|sayı adı}} is under ===Ad===.
    assertEquals(java.util.Map.of("sekiz__Ön_ad__1", 98L, "sekiz__Ad__1", 1L), attachments(extract("sekiz", "sekiz")));
    // Ad has no translations; each Ön ad has its own section.
    assertEquals(java.util.Map.of("üç__Ön_ad__1", 6L, "üç__Ön_ad__2", 98L), attachments(extract("uc", "üç")));
  }

  @Test
  public void attachesAtEndTranslationsToThePage() throws IOException {
    // aralık: Ad, Ön ad, Belirteç; glosses "aylar" (Ad) and "yarı açık" (Ön ad) name no PoS.
    assertEquals(java.util.Map.of("aralık", 80L), attachments(extract("aralik", "aralık")));
    // yarasa: Ad, Eylem; {{üst|memeli hayvan}} (the bat, a noun) sits under ===Eylem===.
    assertEquals(java.util.Map.of("yarasa", 114L), attachments(extract("yarasa", "yarasa")));
  }

  @Test
  public void attachesAtEndTranslationsToTheEntryNamedByTheGloss() throws IOException {
    assertEquals(java.util.Map.of("ad -> ağır__Ad__1", 9L, "ön ad -> ağır__Ön_ad__1", 20L, "belirteç -> ağır__Belirteç__1", 9L),
        glossAttachments(extract("agir", "ağır")));
    // Gloss not naming a PoS -> page.
    assertEquals(java.util.Map.of("doğru olan şey -> gerçek", 71L, "ön ad -> gerçek__Ön_ad__1", 38L), glossAttachments(extract("gercek", "gerçek")));
    // Two Çeviriler sections, both under the last PoS (Eylem): still the at-end layout.
    assertEquals(java.util.Map.of("belirteç -> aç__Belirteç__1", 7L, "aç -> aç", 27L), glossAttachments(extract("ac", "aç")));
    // abanoz has 2 Ad entries (Köken 1 / Köken 2): "ad" is ambiguous -> page.
    assertEquals(java.util.Map.of("ad -> abanoz", 6L, "ön ad -> abanoz__Ön_ad__1", 4L), glossAttachments(extract("abanoz", "abanoz")));
  }

  @Test
  public void attachesAtEndTranslationsWithLowercaseUstToTheEntryNamedByTheGloss() throws IOException {
    assertEquals(java.util.Map.of("ad -> tek__Ad__1", 2L, "belirteç -> tek__Belirteç__1", 2L, "ön ad -> tek__Ön_ad__1", 14L),
        glossAttachments(extract("tek", "tek")));
    // PoS synonyms as known by the data handler: isim = Ad, sıfat = Ön ad.
    assertEquals(java.util.Map.of("isim -> yedek__Ad__1", 1L, "sıfat -> yedek__Ön_ad__1", 4L), glossAttachments(extract("yedek", "yedek")));
    // boğmak has Ad and Eylem: "ön ad" names a PoS absent from the page -> page.
    assertEquals(java.util.Map.of("ad -> boğmak__Ad__1", 1L, "ön ad -> boğmak", 8L), glossAttachments(extract("bogmak", "boğmak")));
  }

  @Test
  public void keepsLowercaseUstGlossesOnPerEntryTranslations() throws IOException {
    assertEquals(java.util.Map.of("kırmızı -> kırmızı__Ad__1", 98L, "kırmızı -> kırmızı__Ön_ad__1", 20L), glossAttachments(extract("kirmizi", "kırmızı")));
    assertEquals(java.util.Map.of("Ön ad -> sekiz__Ön_ad__1", 98L, "sayı adı -> sekiz__Ad__1", 1L), glossAttachments(extract("sekiz", "sekiz")));
  }

  // A level 3 ===Çeviriler=== is a sibling of the PoS sections, hence page level wherever it is:
  // same rule as the at-end layout (single PoS -> entry, gloss naming one PoS -> entry, else page).

  @Test
  public void extractsLevel3TranslationSections() throws IOException {
    // ocak: single Ad, lowercase {{üst}} glosses.
    assertEquals(
        java.util.Map.of("mutfak eşyası -> ocak__Ad__1", 52L, "yılın ayı -> ocak__Ad__1", 77L, "çalışılan yer -> ocak__Ad__1", 3L, "yangın -> ocak__Ad__1", 9L),
        glossAttachments(extract("ocak", "ocak")));
    // kadın: Ad, Ön ad, Ad then two level 3 sections ({{Üst|tip=çeviriler}} has no gloss) -> page.
    assertEquals(java.util.Map.of(" -> kadın", 134L, "analık -> kadın", 11L), glossAttachments(extract("kadin", "kadın")));
    // kâfi: the level 3 section comes BEFORE Ön ad and Ünlem -> page.
    assertEquals(java.util.Map.of("kâfi -> kâfi", 2L), glossAttachments(extract("kafi", "kâfi")));
  }

  // A level 4 ====Çeviriler==== after a non-PoS level 3 section (Kaynakça, Ek okumalar, Köken..)
  // belongs to that section, not to a PoS: 3,497 pages in the 2026-11-01 dump, many created when
  // CavlakBOT promoted ====Kaynakça==== to level 3 (Oct. 2026). It is page level, like a level 3
  // ===Çeviriler===: single PoS -> entry, gloss naming the PoS of exactly one entry -> entry, else
  // page.

  @Test
  public void extractsTranslationSectionsUnderNonPosSections() throws IOException {
    // şemsiye: ===Ad=== then ===Kaynakça=== / ====Çeviriler==== (105 translations in 2026-09-01).
    assertEquals(java.util.Map.of(" -> şemsiye__Ad__1", 104L), glossAttachments(extract("semsiye", "şemsiye")));
    // korgeneral: ===Ad=== then ===Köken=== / ====Çeviriler====.
    assertEquals(java.util.Map.of(" -> korgeneral__Ad__1", 3L), glossAttachments(extract("korgeneral", "korgeneral")));
    // bir: Ad, Ön ad, Belirteç then ===Ek okumalar=== / 2 x ====Çeviriler==== (206 lost translations).
    // "sayı" names the Sayı PoS, absent from the page -> page.
    assertEquals(java.util.Map.of("sembol -> bir", 10L, "insan, şey -> bir", 18L, "belirteç -> bir__Belirteç__1", 6L, "sayı -> bir", 172L),
        glossAttachments(extract("bir", "bir")));
  }

  // --------------------------------------------------------------------------------------------
  // {{Alt}} closes a gloss block: extractTranslations resets the gloss text but not the gloss
  // resource, so a translation after {{Alt}} keeps the previous block's gloss.
  // --------------------------------------------------------------------------------------------

  /** Translation count per gloss ("" when none), e.g. "ülke" = 15. */
  private static java.util.Map<String, Long> glosses(Model m) {
    return m.listSubjectsWithProperty(RDF.type, DBnaryOnt.Translation).toList().stream().collect(Collectors.groupingBy(t -> {
      Resource gloss = t.getPropertyResourceValue(DBnaryOnt.gloss);
      return gloss == null ? "" : gloss.getProperty(RDF.value).getString();
    }, Collectors.counting()));
  }

  @Test
  public void altClosesTheGlossBlock() {
    // Never seen alone in the 2026-11-01 dump (27,404 Türkçe Çeviriler sections), but see below.
    Model m = extractPage("deneme", """
        ==Türkçe==
        ===Ad===
        # Deneme.
        ====Çeviriler====
        {{Üst|birinci|tip=çeviriler}}
        * İngilizce: {{ç|en|first}}
        {{Alt}}
        * Fransızca: {{ç|fr|second}}
        """);
    assertEquals(java.util.Map.of("birinci", 1L, "", 1L), glosses(m));
  }

  @Test
  public void altFollowedByLowercaseUstDoesNotKeepThePreviousGloss() throws IOException {
    // Cezayir: today all 23 are glossed "ülke", including the 8 "şehir" ones.
    assertEquals(java.util.Map.of("ülke", 15L, "şehir", 8L), glosses(extract("cezayir", "Cezayir")));
    // yakın: the 7 "uzak olmadan" ones are glossed "ad", which would also wrongly attach them to Ad.
    assertEquals(java.util.Map.of("ad", 9L, "uzak olmadan", 7L, "yakın", 26L), glosses(extract("yakin", "yakın")));
  }

  // --------------------------------------------------------------------------------------------
  // PoS headings at level 4 under ===Köken N=== (192 Türkçe pages): today the extractor only
  // looks for PoS at level 3, so these pages yield no entry at all.
  // --------------------------------------------------------------------------------------------

  /** The entry (tur: local part) having a sense whose definition contains the given text. */
  private static String entryWithDefinition(Model m, String text) {
    return m.listStatements(null, OntolexOnt.sense, (RDFNode) null).toList().stream()
        .filter(st -> st.getObject().asResource().listProperties(SKOS.definition).toList().stream()
            .anyMatch(d -> d.getObject().asResource().getProperty(RDF.value).getString().contains(text)))
        .map(st -> st.getSubject().getURI().substring(TUR.length())).findFirst().orElse(null);
  }

  private static int senseCount(Model m, String entry) {
    return m.getResource(TUR + entry).listProperties(OntolexOnt.sense).toList().size();
  }

  @Test
  public void extractsPosUnderNumberedEtymologies() throws IOException {
    // gül: Köken 1 / ====Ad==== (2 senses, =====Çeviriler===== at level 5).
    Model m = extract("gul", "gül");
    String ad = entryWithDefinition(m, "Gülgillerin örnek bitkisi");
    assertTrue(ad != null && ad.startsWith("gül__Ad__"), "no gül Ad entry");
    assertEquals(2, senseCount(m, ad));
    assertEquals(java.util.Map.of("çalı -> " + ad, 4L, "çiçek -> " + ad, 61L), glossAttachments(m));
    // koyun: Köken 3 / ====Ad==== (the sheep) and Köken 4 / ====Ad==== (the bosom).
    m = extract("koyun", "koyun");
    String sheep = entryWithDefinition(m, "geviş getirenler");
    String bosom = entryWithDefinition(m, "kollar arası");
    assertTrue(sheep != null && bosom != null && !sheep.equals(bosom), "koyun entries missing");
    assertEquals(2, senseCount(m, sheep));
    assertEquals(3, senseCount(m, bosom));
    assertEquals(java.util.Map.of("evcil hayvan -> " + sheep, 135L), glossAttachments(m));
    // Köken 4 sense 1 "#: {{eş anlamlılar|tr|göğüs|kucak|sine}}".
    assertEquals(java.util.Map.of("__ws_1_" + bosom, Set.of("göğüs", "kucak", "sine")), nyms(m, DBnaryOnt.synonym));
  }

  @Test
  public void skipsInflectedOnlyEntriesUnderNumberedEtymologies() throws IOException {
    // Definitions made only of {{tr-çekim}} / {{fiil}} (-> {{eylem-hâl}} -> {{tr-çekim}}) are
    // inflected forms, ignored like the "çekim" headers already are:
    // gül Köken 2 Eylem ({{fiil|gül||ü}}); koyun Köken 1 and 2 Ad ({{tr-çekim|..|söz=ad}}),
    // Köken 5 Eylem ({{fiil|koy|un}}).
    assertEquals(1, count(extract("gul", "gül"), OntolexOnt.LexicalEntry));
    assertEquals(2, count(extract("koyun", "koyun"), OntolexOnt.LexicalEntry));
  }

  // --------------------------------------------------------------------------------------------
  // {{anlam|..}} (aliases {{mânâ}} 777x, {{mana}} 10x in nym sections) starts a nym line and gives
  // the meaning the following items belong to -> dbnary:gloss of each nym of that line (several
  // args joined with ", "). {{şerh|..}} / {{belirtici|..}} after an item -> dbnary:usage. A
  // {{Üst|gloss}}..{{Alt}} block in a nym section glosses all its items the same way.
  // --------------------------------------------------------------------------------------------

  /** "source -> target" = "gloss | usage" for every nym carrying a gloss or a usage (reified). */
  private static java.util.Map<String, String> nymDetails(Model m, Property relation) {
    java.util.Map<String, String> res = new java.util.HashMap<>();
    for (Resource st : m.listSubjectsWithProperty(RDF.predicate, relation).toList()) {
      Resource gloss = st.getPropertyResourceValue(DBnaryOnt.gloss);
      res.put(
          st.getPropertyResourceValue(RDF.subject).getURI().substring(TUR.length()) + " -> "
              + st.getPropertyResourceValue(RDF.object).getURI().substring(TUR.length()),
          (gloss == null ? "" : gloss.getProperty(RDF.value).getString()) + " | "
              + (st.hasProperty(DBnaryOnt.usage) ? st.getProperty(DBnaryOnt.usage).getString() : ""));
    }
    return res;
  }

  private static void assertContains(java.util.Map<String, String> expected, java.util.Map<String, String> actual) {
    for (var e : expected.entrySet()) {
      assertEquals(e.getValue(), actual.get(e.getKey()), e.getKey());
    }
  }

  @Test
  public void glossesNymsWithManaTemplates() throws IOException {
    // "* {{mânâ|mecâzî de olur}} [[ahmer]] {{şerh|eskimiş}}"
    // Single sense entry: the nym and its gloss/usage are promoted to the sense.
    assertContains(java.util.Map.of("__ws_1_kırmızı__Ad__1 -> ahmer", "mecâzî de olur | eskimiş"),
        nymDetails(extract("kirmizi", "kırmızı"), DBnaryOnt.synonym));
    // "* {{mânâ|Guamla alakalı}} [[Guamsız]]"
    assertContains(java.util.Map.of("Guamlı__Ön_ad__1 -> Guamsız", "Guamla alakalı | "), nymDetails(extract("guamli", "Guamlı"), DBnaryOnt.antonym));
    // {{anlam}} alias, 2 meanings, {{belirtici}}: "* {{anlam|bitki|yiyecek}} [[lobya]] {{belirtici|halk
    // ağzı}}"
    assertContains(java.util.Map.of("fasulye__Ad__1 -> lobya", "bitki, yiyecek | halk ağzı"), nymDetails(extract("fasulye", "fasulye"), DBnaryOnt.synonym));
    // Links inside the gloss are rendered as text; one gloss per line.
    assertContains(java.util.Map.of("bayram__Ad__1 -> yortu", "Hristiyanların/Mûsevîlerin bayramı | ", "bayram__Ad__1 -> ulusal_bayram", "millî bayram | "),
        nymDetails(extract("bayram", "bayram"), DBnaryOnt.synonym));
    // {{mana}} spelling, the gloss applies to every item of its line.
    assertContains(
        java.util.Map.of("kayıt__Ad__1 -> kesin_kayıt", "kopya bulunması | ", "kayıt__Ad__1 -> sabıka_kaydı", "kopya bulunması | ",
            "kayıt__Ad__1 -> düşey_kayıt", "şart | ", "kayıt__Ad__1 -> yatay_kayıt", "şart | ", "kayıt__Ad__1 -> kış_kaydı", "sınırlama | "),
        nymDetails(extract("kayit", "kayıt"), DBnaryOnt.hyponym));
    // güç: "* {{mânâ|ahlâk}} [[efor]], [[kuvvet]]" and Alt kavramlar in {{Üst|..|tip=alt kavramlar}}
    // blocks.
    Model m = extract("guc", "güç");
    assertContains(java.util.Map.of("güç__Ad__1 -> efor", "ahlâk | ", "güç__Ad__1 -> kuvvet", "ahlâk | "), nymDetails(m, DBnaryOnt.synonym));
    assertContains(java.util.Map.of("güç__Ad__1 -> aktif_güç", "elektrik, fizik | ", "güç__Ad__1 -> avlanma_gücü", "kuvvet | "),
        nymDetails(m, DBnaryOnt.hyponym));
  }
}

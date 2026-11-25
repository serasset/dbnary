/**
 *
 */
package org.getalp.dbnary.languages.tur;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.jena.rdf.model.Resource;
import org.getalp.LangTools;
import org.getalp.dbnary.bliki.DbnaryWikiModel;
import org.getalp.dbnary.languages.AbstractWiktionaryExtractor;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.bliki.ExpandAllWikiModel;
import org.getalp.dbnary.wiki.ClassBasedFilter;
import org.getalp.dbnary.wiki.WikiEventsSequence;
import org.getalp.dbnary.wiki.WikiText;
import org.getalp.dbnary.wiki.WikiText.Indentation;
import org.getalp.dbnary.wiki.WikiText.IndentedItem;
import org.getalp.dbnary.wiki.WikiText.InternalLink;
import org.getalp.dbnary.wiki.WikiText.NumberedListItem;
import org.getalp.dbnary.wiki.WikiText.Template;
import org.getalp.dbnary.wiki.WikiText.Text;
import org.getalp.dbnary.wiki.WikiText.Token;
import org.getalp.dbnary.wiki.WikiText.WikiContent;
import org.getalp.dbnary.wiki.WikiText.WikiSection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Barry
 */
public class WiktionaryExtractor extends AbstractWiktionaryExtractor {

  private final Logger log = LoggerFactory.getLogger(WiktionaryExtractor.class);

  protected final static String languageSectionPatternString = "^={2}\\s*([^=]+)\\s*={2}";

  public WiktionaryExtractor(IWiktionaryDataHandler wdh) {
    super(wdh);
  }

  protected final static Pattern languageSectionPattern;
  protected final static HashSet<String> partOfSpeechMarkers;

  protected final static HashMap<String, String> nymMarkerToNymName;

  private static final Set<String> ignoreHeadings = new HashSet<>();
  private ExpandAllWikiModel expander;
  private TurkishPronunciationWikiModel pronunciationModel;

  static {

    languageSectionPattern = Pattern.compile(languageSectionPatternString, Pattern.MULTILINE);

    partOfSpeechMarkers = new HashSet<String>(40);

    partOfSpeechMarkers.add("Ad");
    partOfSpeechMarkers.add("Adıl");
    partOfSpeechMarkers.add("Atasözü");
    partOfSpeechMarkers.add("Bağlaç");
    partOfSpeechMarkers.add("Belirteç");
    partOfSpeechMarkers.add("Deyim");
    partOfSpeechMarkers.add("Emir");
    partOfSpeechMarkers.add("Erkek adı");
    partOfSpeechMarkers.add("Erkek ismi");
    partOfSpeechMarkers.add("Eylem");
    partOfSpeechMarkers.add("İlgeç");
    partOfSpeechMarkers.add("Kısaltma");
    partOfSpeechMarkers.add("Kız adı");
    partOfSpeechMarkers.add("Ön ad");
    partOfSpeechMarkers.add("Ön ek");
    partOfSpeechMarkers.add("Özel Ad");
    partOfSpeechMarkers.add("Özel ad");
    partOfSpeechMarkers.add("Sayı");
    partOfSpeechMarkers.add("Sayı adı");
    partOfSpeechMarkers.add("Sıral sayı");
    partOfSpeechMarkers.add("Yardımcı eylem");
    partOfSpeechMarkers.add("Ek eylem");
    partOfSpeechMarkers.add("Birleşik eylem");
    partOfSpeechMarkers.add("Birleşik ad");
    partOfSpeechMarkers.add("Ön Ad");
    partOfSpeechMarkers.add("Önad");
    partOfSpeechMarkers.add("ön ad");
    partOfSpeechMarkers.add("Son ek");
    partOfSpeechMarkers.add("Sözce");
    partOfSpeechMarkers.add("Ünlem");

    partOfSpeechMarkers.add("Soyadı");

    partOfSpeechMarkers.add("Aile ismi");
    partOfSpeechMarkers.add("Edat");
    partOfSpeechMarkers.add("Erkek ismi");
    partOfSpeechMarkers.add("Fiil");
    partOfSpeechMarkers.add("İbare");
    partOfSpeechMarkers.add("İsim");
    partOfSpeechMarkers.add("Isim");
    partOfSpeechMarkers.add("isim");
    partOfSpeechMarkers.add("Kız ismi");
    partOfSpeechMarkers.add("Özel isim");
    partOfSpeechMarkers.add("Özel İsim");
    partOfSpeechMarkers.add("Ozel ad");
    partOfSpeechMarkers.add("Sıfat");
    partOfSpeechMarkers.add("Soy ismi");
    partOfSpeechMarkers.add("Zamir");
    partOfSpeechMarkers.add("Zarf");

    partOfSpeechMarkers.add("Ek");
    partOfSpeechMarkers.add("Eylem (basit)");
    partOfSpeechMarkers.add("Harf");
    partOfSpeechMarkers.add("İfade");
    partOfSpeechMarkers.add("Önek");


    nymMarkerToNymName = new HashMap<String, String>(20);
    nymMarkerToNymName.put("sinonim", "syn");
    nymMarkerToNymName.put("Eş Anlamlılar", "syn");
    nymMarkerToNymName.put("Eş anlamlılar", "syn");
    nymMarkerToNymName.put("Karşıt Anlamlılar", "ant");
    nymMarkerToNymName.put("Karşıt anlamlılar", "ant");
    nymMarkerToNymName.put("Zıt anlamlılar", "ant");
    nymMarkerToNymName.put("Zıt kavramlar", "ant");
    nymMarkerToNymName.put("Alt Kavramlar", "hypo");
    nymMarkerToNymName.put("Alt kavramlar", "hypo");
    nymMarkerToNymName.put("Üst Kavramlar", "hyper");
    nymMarkerToNymName.put("Üst kavramlar", "hyper");
    nymMarkerToNymName.put("Meronyms", "mero");

    ignoreHeadings.add("Kaynakça"); // bibliography
    ignoreHeadings.add("Ayrıca bakınız"); // See also
    ignoreHeadings.add("Açıklamalar"); // Comments
    ignoreHeadings.add("Ek okumalar"); // Additional readings
    ignoreHeadings.add("Benzer sözcükler"); // Similar concepts or similar word...
    ignoreHeadings.add("Bilimsel adı"); // scientific name...
    ignoreHeadings.add("Heceleme"); // spelling ...
    ignoreHeadings.add("Kısaltmalar"); // abbreviations ...
    ignoreHeadings.add("Resmî adı"); // official name ...
    ignoreHeadings.add("İlgili sözcükler"); // related name ...
    ignoreHeadings.add("Sözcük birliktelikleri"); // word association ...
    ignoreHeadings.add("Çekimleme"); // morphology ...
    ignoreHeadings.add("Örnekler"); // examples ...


  }

  @Override
  public void setWiktionaryIndex(WiktionaryPageSource wi) {
    super.setWiktionaryIndex(wi);
    DbnaryWikiModel turkishModel = new TurkishModulesPatcherWikiModel(wi, Locale.forLanguageTag("tr"), "/images", "/link");
    expander = new ExpandAllWikiModel(turkishModel, wi, Locale.forLanguageTag("tr"), "/images", "/link");
    // Created once: initializing the Lua engine is costly; only its page name changes per page.
    pronunciationModel = new TurkishPronunciationWikiModel(wi, Locale.forLanguageTag("tr"), "/images", "/link");
  }

  public void extractData() {
    wdh.initializePageExtraction(getWiktionaryPageName());
    expander.setPageName(getWiktionaryPageName());
    pronunciationModel.setPageName(getWiktionaryPageName());
    // System.out.println(pageContent);
    Matcher languageFilter = languageSectionPattern.matcher(pageContent);
    while (languageFilter.find() && !languageFilter.group(1).equals("Türkçe")) {
    }
    // Either the filter is at end of sequence or on German language header.
    if (languageFilter.hitEnd()) {
      // There is no Turkish data in this page.
      return;
    }
    int turkishSectionStartOffset = languageFilter.end();
    // Advance till end of sequence or new language section
    // WHY filter on section level ?: while (languageFilter.find() && (languageFilter.start(1) -
    // languageFilter.start()) != 2) {
    languageFilter.find();
    // languageFilter.find();
    int turkishSectionEndOffset = languageFilter.hitEnd() ? pageContent.length() : languageFilter.start();

    extractTurkishData(turkishSectionStartOffset, turkishSectionEndOffset);
    wdh.finalizePageExtraction();
  }


  // TODO: section {{Kısaltmalar}} gives abbreviations
  // TODO: section Yan Kavramlar gives related concepts (apparently not synonyms).
  private void extractTurkishData(int startOffset, int endOffset) {
    WikiText txt = new WikiText(getWiktionaryPageName(), pageContent.substring(startOffset, endOffset));
    wdh.initializeLanguageSection("tr");
    pendingTranslations.clear();
    for (WikiText.Token evt : txt.headers(3)) {
      WikiSection section = evt.asHeading().getSection();
      // Numbered PoS (===Son ek 1===, ===Ad 2===) are their PoS.
      String header = section.getHeading().getContent().toString().trim().replaceFirst("\\s+\\d+$", "");
      // log.debug("Header = {}", header);
      if ("Çeviriler".equals(header)) {
        // Level 3 translations: page level
        extractTranslations(section.getContent(), null);
      } else if (header.equals("Köken")) {
        // Etymology
        extractNestedPosSections(section);
        extractChildTranslationSections(section);
      } else if ("Yanlış".equals(header) || "Yanlış kullanım".equalsIgnoreCase(header)) {
        // Misspelling pages ({{mânâ|doğrusu}} [[correct form]]): not entries
      } else if ("Söyleniş".equals(header)) {
        extractPron(section.getContent());
        extractChildTranslationSections(section);
      } else if (partOfSpeechMarkers.contains(header)) {
        extractPosSection(section, header);
      } else {
        log.debug("Unexpected header {} in {}", header, getWiktionaryPageName());
        // Kaynakça, Ek okumalar, Çekimleme..: a ====Çeviriler==== there is page level
        extractChildTranslationSections(section);
      }
    }
    attachPendingTranslations();
    wdh.finalizeLanguageSection();
  }

  /** Inflection templates: a PoS whose definitions are only these is an inflected form (skipped). */
  private static final Set<String> inflectionTemplates = Set.of("tr-çekim", "fiil");

  /** A PoS section, at level 3 or at level 4 under ===Köken N===. */
  private void extractPosSection(WikiSection section, String header) {
    if (isInflectedOnly(section.getContent())) {
      log.debug("Inflected form {} ignored in {}", header, getWiktionaryPageName());
      extractChildTranslationSections(section); // at-end translations of the page may sit there
      return;
    }
    wdh.initializeLexicalEntry(header);
    // Extract definitions
    extractDefinitions(section.getContent());
    for (Token wikiToken : section.getContent().wikiTokens()) {
      if (wikiToken instanceof WikiSection) {
        WikiSection ws = (WikiSection) wikiToken;
        String h4 = ws.getHeading().getContent().toString().trim();
        if ("Çeviriler".equals(h4)) {
          extractTranslations(ws.getContent(), turkishHandler().currentEntry());
        } else if ("Söyleniş".equals(h4)) {
          extractPron(ws.getContent());
        } else if (nymMarkerToNymName.containsKey(h4)) {
          String nymType = nymMarkerToNymName.get(h4);
          extractLinkList(ws.getContent(), (t, g, u) -> registerSectionNym(t, nymType, g, u), null);
        } else if ("Türetilmiş kavramlar".equalsIgnoreCase(h4) || "Deyimler".equals(h4) || "Atasözleri".equals(h4)) {
          // Derived terms, idioms and proverbs: the block gloss becomes the derivation note
          extractLinkList(ws.getContent(), (t, g, u) -> wdh.registerDerivation(t, g), null);
        } else if ("Köken".equals(h4)) {
          // Etymology
        } else if (ignoreHeadings.contains(h4)) {
          // Just ignore
        } else {
          log.debug("Unhandled sub section {} in {}", h4, getWiktionaryPageName());
        }
      }
    }
  }

  /** True when every definition is a sole {{tr-çekim|..}} / {{fiil|..}} (punctuation allowed). */
  private static boolean isInflectedOnly(WikiContent content) {
    boolean any = false;
    for (Token t : content.wikiTokens()) {
      if (t instanceof NumberedListItem item && item.getListPrefix().length() == item.getLevel()) {
        List<Token> significant =
            item.getContent().tokens().stream().filter(x -> !(x instanceof Text && x.getText().replaceAll("[\\s.,;]", "").isEmpty())).toList();
        if (significant.size() != 1 || !(significant.get(0) instanceof Template tpl) || !inflectionTemplates.contains(tpl.getName().trim())) {
          return false;
        }
        any = true;
      }
    }
    return any;
  }

  /** PoS headings at level 4 under an etymology (===Köken N=== / ====Ad====). */
  private void extractNestedPosSections(WikiSection section) {
    for (Token t : section.getContent().wikiTokens()) {
      if (t instanceof WikiSection ws) {
        String h = ws.getHeading().getContent().toString().trim().replaceFirst("\\s+\\d+$", "");
        if (partOfSpeechMarkers.contains(h)) {
          extractPosSection(ws, h);
        }
      }
    }
  }

  private WiktionaryDataHandler turkishHandler() {
    return (WiktionaryDataHandler) wdh;
  }

  /**
   * A Çeviriler section under a non-PoS section (Kaynakça, Köken N / ====Türetilmiş kavramlar====,
   * ..) belongs to the page. PoS subsections are left to extractNestedPosSections.
   */
  private void extractChildTranslationSections(WikiSection section) {
    for (Token t : section.getContent().wikiTokens()) {
      if (t instanceof WikiSection ws) {
        String h = ws.getHeading().getContent().toString().trim();
        if ("Çeviriler".equals(h)) {
          extractTranslations(ws.getContent(), null);
        } else if (!partOfSpeechMarkers.contains(h.replaceFirst("\\s+\\d+$", ""))) {
          extractChildTranslationSections(ws);
        }
      }
    }
  }

  /** A parsed translation; owner is the entry of its PoS section, null for a page level section. */
  private record PendingTranslation(Resource owner, String glossText, Resource gloss, String lang, String usage, String word) {}

  private final List<PendingTranslation> pendingTranslations = new ArrayList<>();

  /**
   * Per-entry layout (Çeviriler under several PoS, or not under the last one): attach to the owner
   * entry. At-end layout (all under the last PoS) and page level sections: the only entry, else the
   * entry whose PoS the gloss names, else the page.
   */
  private void attachPendingTranslations() {
    WiktionaryDataHandler tdh = turkishHandler();
    Resource last = tdh.lastEntry();
    boolean atEnd = pendingTranslations.stream().map(PendingTranslation::owner).filter(o -> null != o).allMatch(o -> o.equals(last));
    for (PendingTranslation t : pendingTranslations) {
      Resource target = null != t.owner() && !atEnd ? t.owner() : tdh.pageLevelTarget(t.glossText());
      if (null != target) {
        tdh.registerTranslationToEntity(target, t.lang(), t.gloss(), t.usage(), t.word());
      }
    }
    pendingTranslations.clear();
  }

  /** A transliteration written after a translation: {{ç|ru|правда}} [pravda] (LRM marks allowed). */
  private static final Pattern bracketTransliteration = Pattern.compile("^\\s*\u200e?\\[([^\\[\\]]+?)\u200e?\\]");

  /** {{ç}} is an alias (redirect) of {{çeviri}}. */
  private static final Set<String> translationTemplates = Set.of("ç", "çeviri");

  /** The block gloss of a translation section: {{Üst|gloss}} .. {{Alt}}. */
  private Resource translationGloss = null;
  private int translationGlossRank = 1;

  private Resource translationOwner = null;
  private String translationGlossText = null;

  public void extractTranslations(WikiContent wk, Resource owner) {
    translationOwner = owner;
    translationGloss = null;
    translationGlossText = null;
    translationGlossRank = 1;
    for (Token tok : wk.wikiTokens()) {
      if (tok instanceof IndentedItem item) {
        extractTranslationLine(item.getContent().tokens());
      } else if (tok instanceof Template template && !handleTranslationBlockTemplate(template)) {
        extractTranslationLine(List.of(tok)); // a {{ç}} outside of a list item
      }
    }
  }

  private boolean handleTranslationBlockTemplate(Template template) {
    String name = template.getName().trim();
    if ("Üst".equalsIgnoreCase(name)) {
      String g = template.getParsedArgs().get("1");
      translationGloss = null == g ? null : wdh.createGlossResource(g, translationGlossRank++);
      translationGlossText = null == g || g.isBlank() ? null : g.trim();
      return true;
    } else if ("Alt".equalsIgnoreCase(name)) {
      translationGloss = null;
      translationGlossText = null;
      return true;
    }
    return false;
  }

  /**
   * A translation line: {{ç|lang|word|gender..|tr=..}} items, each optionally followed by a
   * [transliteration] and/or a {{şerh|..}} qualifier, which go to its usage.
   */
  private void extractTranslationLine(List<Token> tokens) {
    Template pending = null;
    String translit = null;
    List<String> qualifiers = new ArrayList<>();
    for (Token t : tokens) {
      if (t instanceof Template tpl && translationTemplates.contains(tpl.getName().trim())) {
        registerTranslationTemplate(pending, translit, qualifiers);
        pending = tpl;
        translit = null;
        qualifiers = new ArrayList<>();
      } else if (t instanceof Template tpl && handleTranslationBlockTemplate(tpl)) {
        registerTranslationTemplate(pending, translit, qualifiers);
        pending = null;
      } else if (null != pending && t instanceof Template tpl && qualifierTemplates.contains(tpl.getName().trim())) {
        String q = tpl.getParsedArgs().get("1");
        if (null != q && !q.isBlank()) {
          qualifiers.add(cleanUpMarkup(q, true).trim());
        }
      } else if (null != pending && null == translit && qualifiers.isEmpty() && t instanceof Text text) {
        Matcher m = bracketTransliteration.matcher(text.getText());
        if (m.find()) {
          translit = m.group(1).trim();
        }
      }
    }
    registerTranslationTemplate(pending, translit, qualifiers);
  }

  private void registerTranslationTemplate(Template tpl, String translit, List<String> qualifiers) {
    if (null == tpl) {
      return;
    }
    Map<String, String> args = tpl.getParsedArgs();
    String lang = LangTools.getCode(args.get("1"));
    String word = args.get("2");
    if (null == lang || null == word || word.isBlank()) {
      return;
    }
    List<String> usage = new ArrayList<>();
    String genders = args.entrySet().stream().filter(a -> a.getKey().matches("\\d+") && Integer.parseInt(a.getKey()) >= 3).map(a -> a.getValue().trim())
        .filter(g -> !g.isEmpty() && !"?".equals(g)).collect(Collectors.joining(",")); // "?": unknown gender
    if (!genders.isEmpty()) {
      usage.add(genders);
    }
    String tr = firstNonBlank(args.get("tr"), args.get("ç"), translit);
    if (null != tr) {
      usage.add("tr=" + tr);
    }
    String alt = firstNonBlank(args.get("alt"));
    if (null != alt) {
      usage.add("alt=" + alt);
    }
    String lit = firstNonBlank(args.get("lit"), args.get("tam"));
    if (null != lit) {
      usage.add("lit=" + lit);
    }
    usage.addAll(qualifiers);
    pendingTranslations
        .add(new PendingTranslation(translationOwner, translationGlossText, translationGloss, lang, usage.isEmpty() ? null : String.join("|", usage), word));
  }

  private static String firstNonBlank(String... values) {
    for (String v : values) {
      if (null != v && !v.isBlank()) {
        return v.trim();
      }
    }
    return null;
  }


  private void extractPron(WikiContent wk) {
    // WARN: this may be called from level 3 (no entry defined yet or as level 4 from inside an
    // entry)
    log.debug("Pronunciation Section : {}", wk.toString());
    for (Token tok : wk.templates()) {
      Template template = tok.asTemplate();
      String name = template.getName();
      if ("Çeviri Yazı".equals(name) || "IPA".equals(name)) {
        String pron = template.getParsedArgs().get("1");
        if (null != pron && pron.trim().length() > 0) {
          wdh.registerPronunciation(pron, "tur-fonipa");
        }
      } else if ("tr-IPA".equals(name.trim())) {
        // Computed by Modül:tr-IPA (Lua) from the page name or the given respellings
        for (String pron : pronunciationModel.pronunciations(template.getText())) {
          wdh.registerPronunciation(pron, "tur-fonipa");
        }
      }
    }
  }

  private static Pattern senseNumPattern = Pattern.compile("\\[(\\d+)\\]");
  private final static Pattern CONTROL_CHAR = Pattern.compile("\\p{Cntrl}");

  protected void extractDefinitions(WikiContent wk) {
    WikiEventsSequence indentationsOrTemplates = wk.filteredTokens(new ClassBasedFilter().allowIndentedItem().allowTemplates());
    label: for (Token indent : indentationsOrTemplates) {
      switch (indent) {
        case NumberedListItem numberedListItem -> {
          // Do not extract numbered list items that begin with ":" as they are indeed examples.
          String additionalPrefix = numberedListItem.getListPrefix().substring(numberedListItem.getLevel());
          if (additionalPrefix.startsWith(":") || additionalPrefix.startsWith("*")) {
            Template nymList = soleNymTemplate(numberedListItem.getContent());
            if (null != nymList) {
              registerNymTemplate(nymList);
              continue;
            }
            String expandedExample = expander.expandAll(numberedListItem.getContent().getText().trim(), null);
            expandedExample = CONTROL_CHAR.matcher(expandedExample).replaceAll("");
            wdh.registerExample(expandedExample, null);
          } else {
            String expandedDefinition = expander.expandAll(numberedListItem.getContent().getText().trim(), null);
            expandedDefinition = CONTROL_CHAR.matcher(expandedDefinition).replaceAll("");
            wdh.registerNewDefinition(expandedDefinition.replace("\n", ""));
            // "# {{eş anlamlı|dil=tr|X}}": the definition is a synonym of the sense.
            for (Token t : numberedListItem.getContent().templates()) {
              if (nymTemplates.containsKey(t.asTemplate().getName().trim())) {
                registerNymTemplate(t.asTemplate());
              }
            }
          }
        }
        case Indentation indentation -> {
          String def = indentation.getContent().toString();
          Matcher m = senseNumPattern.matcher(def);
          if (m.lookingAt()) {
            wdh.registerNewDefinition(def.substring(m.end()), m.group(1));
          } else {
            // TODO: it's usually an example given after a definition.
            String expandedExample = expander.expandAll(indentation.getContent().getText().trim(), null);
            expandedExample = CONTROL_CHAR.matcher(expandedExample).replaceAll("");
            wdh.registerExample(expandedExample, null);
          }
        }
        case Template template -> {
          String tname = template.getName();
          if ("Resmi Adı".equals(tname) || "Resmî Adı".equals(tname)) {
            break label;
          } else {
            log.debug("In Def[{}] - got template {}", getWiktionaryPageName(), tname);
          }
        }
        default ->
          // TODO: test and handle these !
          log.debug("Unhandled indented item in def[{}]: {}", getWiktionaryPageName(), indent);
      }
    }
  }

  // {{eş anlamlılar|tr|a|b}} / {{zıt anlamlılar|tr|a}} list items from arg 2, {{eş anlamlı|dil=tr|a}}
  // from arg 1.
  private static final Map<String, String> nymTemplates = Map.of("eş anlamlılar", "syn", "zıt anlamlılar", "ant", "eş anlamlı", "syn", "zıt anlamlı", "ant");
  // {{anlam|meaning..}} (aliases mânâ, mana) gives the meaning of the nyms of its line.
  private static final Set<String> meaningTemplates = Set.of("anlam", "mânâ", "mana");
  private static final Set<String> qualifierTemplates = Set.of("şerh", "belirtici");

  /** The nym template if it is the only (non blank) content of the item, else null. */
  private static Template soleNymTemplate(WikiContent content) {
    Template found = null;
    for (Token t : content.tokens()) {
      if (t instanceof Text && t.getText().isBlank()) {
        continue;
      }
      if (found != null || !(t instanceof Template tpl) || !nymTemplates.containsKey(tpl.getName().trim())) {
        return null;
      }
      found = tpl;
    }
    return found;
  }

  /** Registers the items of a nym template on the current sense (or entry if there is none). */
  private void registerNymTemplate(Template template) {
    String name = template.getName().trim();
    Map<String, String> args = template.getParsedArgs();
    String usage = args.getOrDefault("not", args.get("şerh"));
    int first = name.endsWith("lar") ? 2 : 1;
    for (Map.Entry<String, WikiContent> arg : template.getArgs().entrySet()) {
      if (arg.getKey().matches("\\d+") && Integer.parseInt(arg.getKey()) >= first) {
        String target = nymTarget(arg.getValue());
        if (!target.isEmpty()) {
          wdh.registerNymRelationOnCurrentSense(target, nymTemplates.get(name), null, usage);
        }
      }
    }
  }

  /** The link target if the value is a single link, else its text without markup. */
  private static String nymTarget(WikiContent value) {
    List<Token> nonBlank = value.tokens().stream().filter(t -> !(t instanceof Text && t.getText().isBlank())).toList();
    if (nonBlank.size() == 1 && nonBlank.get(0) instanceof InternalLink l) {
      return l.getTargetText().trim();
    }
    return cleanUpMarkup(value.getText(), true).trim();
  }

  /** Receives each link of a link list section, with its gloss and usage (both may be null). */
  private interface LinkSink {
    void accept(String target, String gloss, String usage);
  }

  /**
   * Link list section of a PoS (nyms, derived terms, idioms, proverbs). A {{Üst|gloss}}..{{Alt}}
   * block glosses its items, a leading {{anlam|..}} glosses the items of its line, {{şerh|..}}
   * qualifies the preceding item. Items are [[links]] or {{l|tr|..}}; {{sütun|N|..}} lists are
   * entered.
   */
  private void extractLinkList(WikiContent content, LinkSink sink, String blockGloss) {
    for (Token t : content.wikiTokens()) {
      if (t instanceof Template tpl) {
        String name = tpl.getName().trim();
        if ("Üst".equalsIgnoreCase(name)) {
          String g = tpl.getParsedArgs().get("1");
          blockGloss = null == g || g.isBlank() ? null : g.trim();
        } else if ("Alt".equalsIgnoreCase(name)) {
          blockGloss = null;
        } else if ("sütun".equals(name)) {
          for (WikiContent arg : tpl.getArgs().values()) {
            extractLinkList(arg, sink, blockGloss);
          }
        } else if (null != turkishLinkTarget(tpl)) {
          extractLinkLine(List.of(t), sink, blockGloss);
        }
      } else if (t instanceof IndentedItem item) {
        extractLinkLine(item.getContent().tokens(), sink, blockGloss);
      } else if (t instanceof InternalLink) {
        extractLinkLine(List.of(t), sink, blockGloss);
      }
    }
  }

  /** The target of a {{l|tr|X}} template, null for any other template or language. */
  private static String turkishLinkTarget(Template tpl) {
    if (!"l".equals(tpl.getName().trim()) || !"tr".equals(tpl.getParsedArgs().get("1"))) {
      return null;
    }
    String target = tpl.getParsedArgs().get("2");
    return null == target || target.isBlank() ? null : target.trim();
  }

  private void extractLinkLine(List<Token> tokens, LinkSink sink, String gloss) {
    String target = null;
    String usage = null;
    for (Token t : tokens) {
      if (t instanceof Template tpl && meaningTemplates.contains(tpl.getName().trim())) {
        gloss = tpl.getArgs().entrySet().stream().filter(a -> a.getKey().matches("\\d+")).map(a -> cleanUpMarkup(a.getValue().getText(), true).trim())
            .filter(a -> !a.isEmpty()).collect(Collectors.joining(", "));
      } else if (t instanceof Template tpl && qualifierTemplates.contains(tpl.getName().trim())) {
        String u = tpl.getParsedArgs().get("1");
        usage = null == u ? null : cleanUpMarkup(u, true).trim();
      } else if (t instanceof InternalLink || t instanceof Template tpl && null != turkishLinkTarget(tpl)) {
        acceptLink(sink, target, gloss, usage);
        target = t instanceof InternalLink l ? l.getTargetText().trim() : turkishLinkTarget((Template) t);
        usage = null;
      }
    }
    acceptLink(sink, target, gloss, usage);
  }

  private static void acceptLink(LinkSink sink, String target, String gloss, String usage) {
    if (null != target && !target.isEmpty()) {
      sink.accept(target, null == gloss || gloss.isEmpty() ? null : gloss, usage);
    }
  }

  private void registerSectionNym(String target, String nymType, String gloss, String usage) {
    wdh.registerNymRelation(target, nymType, null == gloss ? null : wdh.createGlossResource(gloss), usage);
  }
}

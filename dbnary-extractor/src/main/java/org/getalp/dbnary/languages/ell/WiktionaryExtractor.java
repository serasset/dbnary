/**
 *
 */
package org.getalp.dbnary.languages.ell;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Resource;
import org.getalp.LangTools;
import org.getalp.dbnary.ExtractionFeature;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.languages.AbstractWiktionaryExtractor;
import org.getalp.dbnary.wiki.WikiPatterns;
import org.getalp.dbnary.wiki.WikiText;
import org.getalp.dbnary.wiki.WikiText.Heading;
import org.getalp.dbnary.wiki.WikiText.IndentedItem;
import org.getalp.dbnary.wiki.WikiText.ListItem;
import org.getalp.dbnary.wiki.WikiText.NumberedListItem;
import org.getalp.dbnary.wiki.WikiText.Template;
import org.getalp.dbnary.wiki.WikiText.Text;
import org.getalp.dbnary.wiki.WikiText.Token;
import org.getalp.dbnary.wiki.WikiText.WikiContent;
import org.getalp.dbnary.wiki.WikiText.WikiDocument;
import org.getalp.dbnary.wiki.WikiText.WikiSection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Barry
 */
public class WiktionaryExtractor extends AbstractWiktionaryExtractor {

  private final Logger log = LoggerFactory.getLogger(WiktionaryExtractor.class);

  protected final static String definitionPatternString =
      // "(?:^#{1,2}([^\\*#:].*))|(?:^\\*([^\\*#:].*))$";
      "^(?:#{1,2}([^\\*#:].*)|\\*([^\\*#:].*))$";

  protected final static String pronPatternString = "\\{\\{ΔΦΑ\\|([^\\|\\}]*)(.*)\\}\\}";

  // protected final static Pattern languageSectionPattern;

  private static HashSet<String> posMacros;
  private static HashSet<String> ignoredSection;
  private final static HashMap<String, String> nymMarkerToNymName;

  private static void addPos(String pos) {
    posMacros.add(pos);
    if (pos.contains(" ")) {
      posMacros.add(pos.replaceAll(" ", "_"));
    }
  }

  static {

    posMacros = new HashSet<>(20);
    // defMarkers.add("ουσιαστικό"); // Noun
    // defMarkers.add("επίθετο"); // Adjective
    // // defMarkers.add("μορφή επιθέτου"); // Adjective
    // defMarkers.add("επίρρημα"); //Adverb
    // // defMarkers.add("μορφή ρήματος"); // Verb form
    // defMarkers.add("ρήμα"); // Verb
    // defMarkers.add("κύριο όνομα"); //Proper noun
    // defMarkers.add("παροιμία"); // Proverb
    // defMarkers.add("πολυλεκτικός όρος");// Multi word term
    // defMarkers.add("ρηματική έκφραση"); // Verbal Expressions
    // defMarkers.add("επιφώνημα"); // interjection
    // defMarkers.add("επιρρηματική έκφραση"); // adverbial expression
    // defMarkers.add("μετοχή"); // both adjective and verbs

    addPos("αντωνυμία");
    addPos("απαρέμφατο");
    addPos("άρθρο");
    addPos("αριθμητικό");
    addPos("γερουνδιακό");
    addPos("γερούνδιο");
    addPos("έκφραση");
    addPos("επιθετική έκφραση");
    addPos("επίθετο");
    addPos("επίθημα");
    addPos("επίρρημα");
    addPos("επιρρηματική έκφραση");
    addPos("επιφώνημα");
    addPos("κατάληξη");
    addPos("κατάληξη αρσενικών επιθέτων");
    addPos("κατάληξη αρσενικών και θηλυκών ουσιαστικών");
    addPos("κατάληξη αρσενικών ουσιαστικών");
    addPos("κατάληξη επιρρημάτων");
    addPos("κατάληξη θηλυκών ουσιαστικών");
    addPos("κατάληξη ουδέτερων ουσιαστικών");
    addPos("κατάληξη ρημάτων");
    addPos("κύριο όνομα");
    addPos("μετοχή");
    addPos("μόριο");
    addPos("μορφή αντωνυμίας");
    addPos("μορφή αριθμητικού");
    addPos("μορφή γερουνδιακού");
    addPos("μορφή επιθέτου");
    addPos("μορφή επιρρήματος");
    addPos("μορφή κυρίου ονόματος");
    addPos("μορφή μετοχής");
    addPos("μορφή ουσιαστικού");
    addPos("μορφή πολυλεκτικού όρου");
    addPos("μορφή ρήματος");
    addPos("ουσιαστικό");
    addPos("παροιμία");
    addPos("πολυλεκτικός όρος");
    addPos("πρόθεση");
    addPos("προθετική έκφραση");
    addPos("πρόθημα");
    addPos("πρόσφυμα");
    addPos("ρήμα");
    addPos("ρηματική έκφραση");
    addPos("ρίζα");
    addPos("σουπίνο");
    addPos("συγχώνευση");
    addPos("σύμβολο");
    addPos("συνδεσμική έκφραση");
    addPos("σύνδεσμος");
    addPos("συντομομορφή");
    addPos("φράση");
    addPos("χαρακτήρας");
    addPos("ένθημα");
    addPos("μεταγραφή"); // A transcription from another language...
    addPos("μορφή άρθρου"); // Clitic article type...
    addPos("μορφή επιθήματοςς"); // Clitic suffix...
    addPos("μορφή επιθήματος"); // Clitic suffix...

    nymMarkerToNymName = new HashMap<>(20);
    nymMarkerToNymName.put("συνώνυμα", "syn");
    nymMarkerToNymName.put("συνώνυμο", "syn");
    nymMarkerToNymName.put("συνων", "syn");
    nymMarkerToNymName.put("ταυτόσημα", "syn");
    nymMarkerToNymName.put("αντώνυμα", "ant");
    nymMarkerToNymName.put("αντώνυμο", "ant");
    nymMarkerToNymName.put("αντών", "ant");
    nymMarkerToNymName.put("hyponyms", "hypo");
    nymMarkerToNymName.put("υπώνυμα", "hypo");
    nymMarkerToNymName.put("hypernyms", "hyper");
    nymMarkerToNymName.put("υπερώνυμα", "hyper");
    nymMarkerToNymName.put("meronyms", "mero");
    nymMarkerToNymName.put("μερώνυμα", "mero");

    ignoredSection = new HashSet<>(20);
    ignoredSection.add("άλλες γραφές"); // TODO: Other forms
    ignoredSection.add("μορφές"); // TODO: Other forms
    ignoredSection.add("άλλες μορφές"); // TODO: Other forms (is there a difference with the
    // previous one ?)
    ignoredSection.add("άλλη γραφή"); // TODO: Other forms (???)
    ignoredSection.add("αλλόγλωσσα"); // Foreign language derivatives
    ignoredSection.add("αναγραμματισμοί"); // Anagrams
    ignoredSection.add("βλέπε"); // See also
    ignoredSection.add("βλ"); // See also
    ignoredSection.add("κοιτ"); // See also
    ignoredSection.add("εκφράσεις"); // Expressions
    ignoredSection.add("κλίση"); // TODO: Conjugations
    ignoredSection.add("υποκοριστικά"); // diminutive (?)
    ignoredSection.add("μεγεθυντικά"); // Augmentative (?)
    ignoredSection.add("μεταγραφές"); // Transcriptions
    ignoredSection.add("ομώνυμα"); // Homonym / Similar
    ignoredSection.add("παράγωγα"); // Derived words
    ignoredSection.add("πηγές"); // Sources
    ignoredSection.add("πηγή"); // Sources
    ignoredSection.add("πολυλεκτικοί όροι"); // Multilingual Terms ?
    ignoredSection.add("σημείωση"); // Notes
    ignoredSection.add("σημειώσεις"); // Notes
    ignoredSection.add("συγγενικά"); // Related words
    ignoredSection.add("σύνθετα"); // Compound words
    ignoredSection.add("αναφορές"); // References
    ignoredSection.add("παροιμίες"); // Proverbs

    ignoredSection.add("ρηματική φωνή"); // Forms verbales

  }

  // Non standard language codes used in Greek edition
  static {
    NON_STANDARD_LANGUAGE_MAPPINGS.put("conv", "mul-conv");
  }


  protected final static Pattern pronPattern;

  private static final Pattern definitionPattern;

  static {
    pronPattern = Pattern.compile(pronPatternString);
    definitionPattern = Pattern.compile(definitionPatternString, Pattern.MULTILINE);
  }

  protected GreekDefinitionExtractorWikiModel definitionExpander;

  public WiktionaryExtractor(IWiktionaryDataHandler wdh) {
    super(wdh);
  }

  @Override
  public void setWiktionaryIndex(WiktionaryPageSource wi) {
    super.setWiktionaryIndex(wi);
    definitionExpander = new GreekDefinitionExtractorWikiModel(this.wdh, this.wi, new Locale("el"),
        "/${image}", "/${title}");

  }

  @Override
  protected void setWiktionaryPageName(String wiktionaryPageName) {
    super.setWiktionaryPageName(wiktionaryPageName);
    definitionExpander.setPageName(this.getWiktionaryPageName());
  }

  public void extractData() {
    wdh.initializePageExtraction(getWiktionaryPageName());
    WikiText page = new WikiText(getWiktionaryPageName(), pageContent);
    WikiDocument doc = page.asStructuredDocument();
    doc.getContent().wikiTokens().stream().filter(t -> t instanceof WikiSection)
        .map(Token::asWikiSection).forEach(this::extractSection);
    wdh.finalizePageExtraction();
  }


  private void extractSection(WikiSection section) {
    Optional<String> language = sectionLanguage(section);
    language.ifPresent(l -> extractLanguageSection(section, l));
  }

  private final static Pattern languageTemplate = Pattern.compile("-(.+)-");

  public static Optional<String> sectionLanguage(WikiSection section) {
    if (section.getHeading().getLevel() == 2) {
      return section.getHeading().getContent().templatesOnUpperLevel().stream()
          .map(Token::asTemplate).map(Template::getName).map(name -> {
            Matcher m = languageTemplate.matcher(name);
            return m.matches() ? m.group(1) : null;
          }).filter(Objects::nonNull).findFirst();
    }
    return Optional.empty();
  }


  private void extractLanguageSection(WikiSection languageSection, String language) {
    if (null == language) {
      return;
    }
    if (null == wdh.getExolexFeatureBox(ExtractionFeature.MAIN)
        && !wdh.getExtractedLanguage().equals(language)) {
      return;
    }

    // The language is always defined when arriving here, but we should check if we extract it
    String normalizedLanguage = validateAndStandardizeLanguageCode(language);
    if (normalizedLanguage == null) {
      log.trace("Ignoring language section {} for {}", language, getWiktionaryPageName());
      return;
    }
    wdh.initializeLanguageSection(normalizedLanguage);

    for (Token t : languageSection.getContent().headers()) {
      Heading heading = t.asHeading();
      Pair<Template, String> templateAndTitle = sectionType(heading);

      Template title = templateAndTitle.getLeft();
      String sectionName = templateAndTitle.getRight();
      String pos;
      if ("ετυμολογία".equals(sectionName)) {
        // NOTHING YET
      } else if ("μεταφράσεις".equals(sectionName)) {
        // Translations
        extractTranslations(heading.getSection().getPrologue().getText());
      } else if ("προφορά".equals(sectionName)) {
        // pronunciation
        extractPron(heading.getSection().getPrologue());
      } else if ((posMacros.contains(sectionName))) {
        wdh.initializeLexicalEntry(sectionName);
        extractDefinitions(heading.getSection().getPrologue());
      } else if (nymMarkerToNymName.containsKey(sectionName)) {
        // Nyms
        WikiContent prologue = heading.getSection().getPrologue();
        extractNyms(nymMarkerToNymName.get(sectionName), prologue.getBeginIndex(),
            prologue.getEndIndex());
      } else if (!ignoredSection.contains(sectionName)) {
        log.debug("Unexpected title {} in {}", title == null ? sectionName : title.getText(),
            getWiktionaryPageName());
      }
    }
    wdh.finalizeLanguageSection();
  }

  private void extractDefinitions(WikiContent prologue) {
    prologue.wikiTokens().forEach(t -> {
      if (t instanceof Text) {
        String txt;
        if (!"".equals(txt = t.asText().getText().trim()))
          log.trace("Dangling text inside definition {} in {}", txt, wdh.currentPagename());
      } else if (t instanceof ListItem || t instanceof NumberedListItem) {
        IndentedItem item = t.asIndentedItem();
        if (item.getContent().toString().startsWith(":")) {
          // It's an example
          wdh.registerExample(item.getContent().getText().substring(1), null);
        } else {
          extractDefinition(item.getContent().getText(), item.getLevel());
        }
      }
    });
  }

  private Pair<Template, String> sectionType(Heading heading) {
    List<Token> titleTemplate = heading.getContent().tokens().stream()
        .filter(t -> !(t instanceof Text
            && t.asText().getText().replaceAll("\u00A0", "").trim().equals("")))
        .collect(Collectors.toList());
    if (titleTemplate.size() == 0) {
      log.trace("Unexpected empty title in {}", getWiktionaryPageName());
      return new ImmutablePair<>(null, "");
    }
    if (titleTemplate.size() > 1) {
      log.trace("Unexpected multi title {} in {}", heading.getText(), getWiktionaryPageName());
    }
    if (!(titleTemplate.get(0) instanceof Template)) {
      log.trace("Unexpected non template title {} in {}", heading.getText(),
          getWiktionaryPageName());
      return new ImmutablePair<>(null, heading.getContent().getText().toLowerCase().trim());
    }
    return new ImmutablePair<>(titleTemplate.get(0).asTemplate(),
        titleTemplate.get(0).asTemplate().getName().toLowerCase().trim());
  }

  private void extractTranslations(String source) {
    Matcher macroMatcher = WikiPatterns.macroPattern.matcher(source);
    Resource currentGlose = null;

    while (macroMatcher.find()) {
      String g1 = macroMatcher.group(1);

      switch (g1) {
        case "τ": {
          String g2 = macroMatcher.group(2);
          int i1, i2;
          String lang, word;
          if (g2 != null && (i1 = g2.indexOf('|')) != -1) {
            lang = LangTools.normalize(g2.substring(0, i1));
            String usage = null;
            if ((i2 = g2.indexOf('|', i1 + 1)) == -1) {
              word = g2.substring(i1 + 1);
            } else {
              word = g2.substring(i1 + 1, i2);
              usage = g2.substring(i2 + 1);
            }
            lang = GreekLangtoCode.threeLettersCode(lang);
            if (lang != null) {
              wdh.registerTranslation(lang, currentGlose, usage, word);
            }
          }
          break;
        }
        case "μτφ-αρχή":
        case "(": {
          // Get the glose that should help disambiguate the source acception
          String g2 = macroMatcher.group(2);
          // Ignore glose if it is a macro
          if (g2 != null && !g2.startsWith("{{")) {
            currentGlose = wdh.createGlossResource(glossFilter.extractGlossStructure(g2));
          }
          break;
        }
        case "μτφ-μέση":
          // just ignore it
          break;
        case "μτφ-τέλος":
        case ")":
          // Forget the current glose
          currentGlose = null;
          break;
      }
    }
  }

  private void extractPron(WikiContent pronContent) {
    pronContent.wikiTokens().stream().filter(t -> t instanceof Template).map(Token::asTemplate)
        .filter(t -> "ΔΦΑ".equals(t.getName())).forEach(t -> {
          String pronLg = t.getParsedArg("1");
          if (!pronLg.startsWith(wdh.getCurrentEntryLanguage()))
            log.trace("Pronunciation language incorrect in section template {} ≠ {} in {}",
                wdh.getCurrentEntryLanguage(), pronLg, wdh.currentPagename());
          wdh.registerPronunciation(t.getParsedArgs().get("2"),
              wdh.getCurrentEntryLanguage() + "-fonipa");
        });
  }

  @Override
  public void extractDefinition(String definition, int defLevel) {
    definitionExpander.parseDefinition(definition, defLevel);
  }

}

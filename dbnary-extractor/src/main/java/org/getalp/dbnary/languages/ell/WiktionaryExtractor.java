/**
 *
 */
package org.getalp.dbnary.languages.ell;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import org.getalp.dbnary.languages.AbstractWiktionaryExtractor;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.wiki.WikiPatterns;
import org.getalp.dbnary.wiki.WikiText;
import org.getalp.dbnary.wiki.WikiText.Heading;
import org.getalp.dbnary.wiki.WikiText.Template;
import org.getalp.dbnary.wiki.WikiText.Text;
import org.getalp.dbnary.wiki.WikiText.Token;
import org.getalp.dbnary.wiki.WikiText.WikiContent;
import org.getalp.dbnary.wiki.WikiText.WikiDocument;
import org.getalp.dbnary.wiki.WikiText.WikiSection;
import org.getalp.iso639.ISO639_3;
import org.getalp.iso639.ISO639_3.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Barry
 */
public class WiktionaryExtractor extends AbstractWiktionaryExtractor {

  private final Logger log = LoggerFactory.getLogger(WiktionaryExtractor.class);

  protected final static String SectionPatternString; // "={2,5}\\s*\\{\\{([^=]*)([^\\}\\|\n\r]*)\\s*(?:\\|([^\\}\n\r]*))?)\\s*\\}\\}={2,5}"

  protected final static String languageSectionPatternString = "==\\s*\\{\\{-([^-]*)-\\}\\}\\s*==";
  protected final static String definitionPatternString =
      // "(?:^#{1,2}([^\\*#:].*))|(?:^\\*([^\\*#:].*))$";
      "^(?:#{1,2}([^\\*#:].*)|\\*([^\\*#:].*))$";

  protected final static String pronPatternString = "\\{\\{ΔΦΑ\\|([^\\|\\}]*)(.*)\\}\\}";

  // protected final static Pattern languageSectionPattern;

  private static HashSet<String> posMacros;
  private static HashSet<String> ignoredSection;
  protected final static Pattern languageSectionPattern;
  private final static HashMap<String, String> nymMarkerToNymName;


  static {

    SectionPatternString = new StringBuilder().append("={2,5}").append("\\{\\{\\s*")
        .append("([^\\}\\|\n\r]*)(?:([^\\}\n\r]*))?").append("\\s*\\}\\}").append("={2,5}")
        .toString();

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

    posMacros.add("αντωνυμία");
    posMacros.add("απαρέμφατο");
    posMacros.add("άρθρο");
    posMacros.add("αριθμητικό");
    posMacros.add("γερουνδιακό");
    posMacros.add("γερούνδιο");
    posMacros.add("έκφραση");
    posMacros.add("επιθετική έκφραση");
    posMacros.add("επίθετο");
    posMacros.add("επίθημα");
    posMacros.add("επίρρημα");
    posMacros.add("επιρρηματική έκφραση");
    posMacros.add("επιφώνημα");
    posMacros.add("κατάληξη");
    posMacros.add("κατάληξη αρσενικών επιθέτων");
    posMacros.add("κατάληξη αρσενικών και θηλυκών ουσιαστικών");
    posMacros.add("κατάληξη αρσενικών ουσιαστικών");
    posMacros.add("κατάληξη επιρρημάτων");
    posMacros.add("κατάληξη θηλυκών ουσιαστικών");
    posMacros.add("κατάληξη ουδέτερων ουσιαστικών");
    posMacros.add("κατάληξη ρημάτων");
    posMacros.add("κύριο όνομα");
    posMacros.add("μετοχή");
    posMacros.add("μόριο");
    posMacros.add("μορφή αντωνυμίας");
    posMacros.add("μορφή αριθμητικού");
    posMacros.add("μορφή γερουνδιακού");
    posMacros.add("μορφή επιθέτου");
    posMacros.add("μορφή επιρρήματος");
    posMacros.add("μορφή κυρίου ονόματος");
    posMacros.add("μορφή μετοχής");
    posMacros.add("μορφή ουσιαστικού");
    posMacros.add("μορφή πολυλεκτικού όρου");
    posMacros.add("μορφή ρήματος");
    posMacros.add("ουσιαστικό");
    posMacros.add("παροιμία");
    posMacros.add("πολυλεκτικός όρος");
    posMacros.add("πρόθεση");
    posMacros.add("προθετική έκφραση");
    posMacros.add("πρόθημα");
    posMacros.add("πρόσφυμα");
    posMacros.add("ρήμα");
    posMacros.add("ρηματική έκφραση");
    posMacros.add("ρίζα");
    posMacros.add("σουπίνο");
    posMacros.add("συγχώνευση");
    posMacros.add("σύμβολο");
    posMacros.add("συνδεσμική έκφραση");
    posMacros.add("σύνδεσμος");
    posMacros.add("συντομομορφή");
    posMacros.add("φράση");
    posMacros.add("χαρακτήρας");
    posMacros.add("ένθημα");
    posMacros.add("μεταγραφή"); // A transcription from another language...
    posMacros.add("μορφή άρθρου"); // Clitic article type...
    posMacros.add("μορφή επιθήματοςς"); // Clitic suffix...

    nymMarkerToNymName = new HashMap<>(20);
    nymMarkerToNymName.put("συνώνυμα", "syn");
    nymMarkerToNymName.put("αντώνυμα", "ant");
    nymMarkerToNymName.put("hyponyms", "hypo");
    nymMarkerToNymName.put("υπώνυμα", "hypo");
    nymMarkerToNymName.put("hypernyms", "hyper");
    nymMarkerToNymName.put("υπερώνυμα", "hyper");
    nymMarkerToNymName.put("meronyms", "mero");

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

    ignoredSection.add("ρηματική φωνή"); // Forms verbales


  }


  public WiktionaryExtractor(IWiktionaryDataHandler wdh) {
    super(wdh);
  }

  protected final static Pattern SectionPattern;
  protected final static Pattern pronPattern;

  private static final Pattern definitionPattern;

  static {
    SectionPattern = Pattern.compile(SectionPatternString);
    pronPattern = Pattern.compile(pronPatternString);
    languageSectionPattern = Pattern.compile(languageSectionPatternString);
    definitionPattern = Pattern.compile(definitionPatternString, Pattern.MULTILINE);
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
    if (null == wdh.getExolexFeatureBox(ExtractionFeature.MAIN) && !"el".equals(language)) {
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
        extractPron(heading.getSection().getPrologue().getText());
      } else if ((posMacros.contains(sectionName)) && title != null) {
        // Only initialize entries when title is a template ?
        wdh.initializeLexicalEntry(sectionName);
        extractDefinitions(heading.getSection().getPrologue());
      } else if (nymMarkerToNymName.containsKey(sectionName)) {
        // Nyms
        WikiContent prologue = heading.getSection().getPrologue();
        extractNyms(nymMarkerToNymName.get(sectionName), prologue.getBeginIndex(),
            prologue.getEndIndex());
      } else {
        log.debug("Unexpected title {} in {}", title == null ? sectionName : title.getText(),
            getWiktionaryPageName());
      }
    }
    wdh.finalizeLanguageSection();
  }

  private void extractDefinitions(WikiContent prologue) {
    Matcher definitionMatcher = definitionPattern.matcher(prologue.getText());
    while (definitionMatcher.find()) {
      extractDefinition(definitionMatcher);
    }
  }

  private static final Map<String, String> additionalLanguages = new HashMap<>();

  static {
    additionalLanguages.put("conv", "mul-conv");
  }

  private static String validateAndStandardizeLanguageCode(String language) {
    Lang languageObject = ISO639_3.sharedInstance.getLang(language);
    if (languageObject != null) {
      return languageObject.getId();
    }
    return additionalLanguages.get(language);
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

  private void extractPron(String source) {
    Matcher pronMatcher = pronPattern.matcher(source);
    while (pronMatcher.find()) {
      String pron = pronMatcher.group(1);

      if (null == pron || pron.equals("")) {
        return;
      }
      wdh.registerPronunciation(pron, wdh.getCurrentEntryLanguage() + "-fonipa");
    }
  }

  @Override
  public void extractDefinition(Matcher definitionMatcher) {
    // TODO: properly handle macros in definitions.
    String definition = definitionMatcher.group(1);
    int defLevel = 1;
    if (null == definition) {
      definition = definitionMatcher.group(2);
    } else {
      if (definitionMatcher.group().charAt(1) == '#') {
        defLevel = 2;
      }
    }
    extractDefinition(definition, defLevel);
  }

}

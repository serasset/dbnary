/**
 *
 */
package org.getalp.dbnary.languages.tur;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.jena.rdf.model.Resource;
import org.getalp.LangTools;
import org.getalp.dbnary.languages.AbstractWiktionaryExtractor;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.bliki.ExpandAllWikiModel;
import org.getalp.dbnary.wiki.ClassBasedFilter;
import org.getalp.dbnary.wiki.WikiEventsSequence;
import org.getalp.dbnary.wiki.WikiText;
import org.getalp.dbnary.wiki.WikiText.Indentation;
import org.getalp.dbnary.wiki.WikiText.NumberedListItem;
import org.getalp.dbnary.wiki.WikiText.Template;
import org.getalp.dbnary.wiki.WikiText.Token;
import org.getalp.dbnary.wiki.WikiText.WikiContent;
import org.getalp.dbnary.wiki.WikiText.WikiSection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Barry
 */
public class WiktionaryExtractor extends AbstractWiktionaryExtractor {

  private Logger log = LoggerFactory.getLogger(WiktionaryExtractor.class);

  protected final static String languageSectionPatternString = "^={2}\\s*([^=]+)\\s*={2}";

  public WiktionaryExtractor(IWiktionaryDataHandler wdh) {
    super(wdh);
  }

  protected final static Pattern languageSectionPattern;
  protected final static HashSet<String> partOfSpeechMarkers;

  protected final static HashMap<String, String> nymMarkerToNymName;

  private static final Set<String> ignoreHeadings = new HashSet<>();
  private ExpandAllWikiModel definitionExpander;

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
    ignoreHeadings.add("Türetilmiş kavramlar"); // derivations ...
    ignoreHeadings.add("Çekimleme"); // morphology ...
    ignoreHeadings.add("Örnekler"); // examples ...


  }

  @Override
  public void setWiktionaryIndex(WiktionaryPageSource wi) {
    super.setWiktionaryIndex(wi);
    definitionExpander =
        new ExpandAllWikiModel(wi, Locale.forLanguageTag("tr"), "/images", "/link");
  }

  public void extractData() {
    wdh.initializePageExtraction(getWiktionaryPageName());
    definitionExpander.setPageName(getWiktionaryPageName());
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
    int turkishSectionEndOffset =
        languageFilter.hitEnd() ? pageContent.length() : languageFilter.start();

    extractTurkishData(turkishSectionStartOffset, turkishSectionEndOffset);
    wdh.finalizePageExtraction();
  }


  // TODO: section {{Kısaltmalar}} gives abbreviations
  // TODO: section Yan Kavramlar gives related concepts (apparently not synonyms).
  private void extractTurkishData(int startOffset, int endOffset) {
    WikiText txt =
        new WikiText(getWiktionaryPageName(), pageContent.substring(startOffset, endOffset));
    wdh.initializeLanguageSection("tr");
    for (WikiText.Token evt : txt.headers(3)) {
      WikiSection section = evt.asHeading().getSection();
      String header = section.getHeading().getContent().toString().trim();
      // log.debug("Header = {}", header);
      if (header.equals("Köken")) {
        // Etymology
        log.debug("Etymology section ignored in {}", getWiktionaryPageName());
      } else if ("Söyleniş".equals(header)) {
        extractPron(section.getContent());
      } else if (partOfSpeechMarkers.contains(header)) {
        // Part of speech
        wdh.initializeLexicalEntry(header);
        // Extract definitions
        extractDefinitions(section.getContent());
        for (Token wikiToken : section.getContent().wikiTokens()) {
          if (wikiToken instanceof WikiSection) {
            WikiSection ws = (WikiSection) wikiToken;
            String h4 = ws.getHeading().getContent().toString().trim();
            if ("Çeviriler".equals(h4)) {
              extractTranslations(ws.getContent());
            } else if ("Söyleniş".equals(h4)) {
              extractPron(ws.getContent());
            } else if (nymMarkerToNymName.containsKey(h4)) {
              // Nym block
              log.debug("Nym section {} ignored in {}", h4, getWiktionaryPageName());
            } else if ("Deyimler".equals(h4) || "Atasözleri".equals(h4)) {
              // Idioms or proverbs
            } else if ("Köken".equals(h4)) {
              // Etymology
            } else if (ignoreHeadings.contains(h4)) {
              // Just ignore
            } else {
              log.debug("Unhandled sub section {} in {}", h4, getWiktionaryPageName());
            }
          }
        }
      } else {
        log.debug("Unexpected header {} in {}", header, getWiktionaryPageName());
      }
    }
    wdh.finalizeLanguageSection();
  }

  public void extractTranslations(WikiContent wk) {
    // if (log.isDebugEnabled()) {
    // ClassBasedFilter lis = new ClassBasedFilter().allowListItem();
    // wk.filteredTokens(lis).stream().forEach(t -> log.debug("trad: {}", t.toString()));
    // }

    Resource globalGlossResource = null;
    String globalGloss = "";
    int rank = 1;
    for (Token tok : wk.templates()) {
      Template template = (Template) tok;
      Map<String, String> args = template.cloneParsedArgs();
      if ("Üst".equals(template.getName())) {
        if (template.getParsedArgs().get("1") != null) {
          globalGloss = args.get("1");
          globalGlossResource = wdh.createGlossResource(globalGloss, rank++);
        } else {
          globalGloss = "";
          globalGlossResource = null;
        }
      } else if ("Alt".equals(template.getName())) {
        globalGloss = "";
      } else if ("Orta".equals(template.getName())) {
        // ignore
      } else if ("ç".equals(template.getName())) {
        String lang = LangTools.getCode(args.get("1"));
        String word = args.get("2");
        args.remove("1");
        args.remove("2");
        String usage = null;
        if (!args.isEmpty()) {
          usage = args.toString();
        }
        if (null != lang && null != word && word.trim().length() > 0) {
          wdh.registerTranslation(lang, globalGlossResource, usage, word);
        }
      }

    }
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
      }
    }
  }

  private static Pattern senseNumPattern = Pattern.compile("\\[(\\d+)\\]");

  protected void extractDefinitions(WikiContent wk) {
    WikiEventsSequence indentationsOrTemplates =
        wk.filteredTokens(new ClassBasedFilter().allowIndentedItem().allowTemplates());
    for (Token indent : indentationsOrTemplates) {
      if (indent instanceof NumberedListItem) {
        // Do not extract numbered list items that begin with ":" as they are indeed examples.
        if (indent.asNumberedListItem().getContent().getText().startsWith(":"))
          continue;
        String expandedDefinition =
            definitionExpander.expandAll(indent.asNumberedListItem().getContent().toString(), null);
        wdh.registerNewDefinition(expandedDefinition.replace("\n", ""));
      } else if (indent instanceof Indentation) {
        String def = indent.asIndentation().getContent().toString();
        Matcher m = senseNumPattern.matcher(def);
        if (m.lookingAt()) {
          wdh.registerNewDefinition(def.substring(m.end()), m.group(1));
        } else {
          // TODO: it's usually an example given after a definition.
        }
      } else if (indent instanceof Template) {
        String tname = indent.asTemplate().getName();
        if ("Resmi Adı".equals(tname) || "Resmî Adı".equals(tname)) {
          break;
        } else {
          log.debug("In Def[{}] - got template {}", getWiktionaryPageName(), tname);
        }
      } else {
        // TODO: test and handle these !
        log.debug("Unhandled indented item in def[{}]: {}", getWiktionaryPageName(),
            indent.toString());
      }
    }
  }

}

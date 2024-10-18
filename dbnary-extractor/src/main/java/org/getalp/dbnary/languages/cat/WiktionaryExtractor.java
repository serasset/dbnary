package org.getalp.dbnary.languages.cat;

import info.bliki.wiki.filter.PlainTextConverter;
import org.apache.jena.rdf.model.RDFNode;
import org.getalp.LangTools;
import org.getalp.dbnary.ExtractionFeature;
import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.StructuredGloss;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.languages.AbstractWiktionaryExtractor;
import org.getalp.dbnary.morphology.MorphoSyntacticFeature;
import org.getalp.dbnary.morphology.StrictInflexionScheme;
import org.getalp.dbnary.tools.PageIterator;
import org.getalp.dbnary.wiki.WikiText;
import org.getalp.iso639.ISO639_3;
import org.getalp.model.lexinfo.Gender;
import org.getalp.model.lexinfo.Mood;
import org.getalp.model.lexinfo.Number;
import org.getalp.model.ontolex.LexicalForm;
import org.getalp.model.ontolex.WrittenRepresentation;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Arnaud Alet 13/07/2023
 */
public class WiktionaryExtractor extends AbstractWiktionaryExtractor {

  private static final Logger log = LoggerFactory.getLogger(WiktionaryExtractor.class);

  private static final String URL = "https://ca.wiktionary.org/wiki/";

  public static final Set<String> ignoredTemplate = new HashSet<>();
  public static final Set<String> ignoredSection = new HashSet<>();
  public static final Set<String> ignoredText = new HashSet<>();


  private static final Pattern extractLanguage = Pattern.compile("(-)(.{2,3})(-)");


  static {
    addIgnoredT("vegeu");
    addIgnoredT("t-vegeu");
    addIgnoredT("àudio");
    addIgnoredT("colauto");
    addIgnoredT("mig");
    addIgnoredT("q");
    addIgnoredT("sigles");
    addIgnoredT("homòfons");
    addIgnoredT("map shape");
    addIgnoredT("verb-forma");
    addIgnoredT("parònims");
    addIgnoredT("numeral");
    addIgnoredT("ca-num");
    addIgnoredT("catllengua");
    addIgnoredT("categoritza");
    addIgnoredT("-pronafi-");
    addIgnoredT("m");
    addIgnoredT("trad-vegeu");
    addIgnoredT("rel-top");
    addIgnoredT("rel-mid");
    addIgnoredT("rel-bottom");
    addIgnoredT("parts del dia");
    addIgnoredT("ca-rima");
    addIgnoredT("Wikimedia");
    addIgnoredT("Viquipèdia");
    addIgnoredT("figures de la baralla/ca");
    addIgnoredT("esborrany");
    addIgnoredT("alfabet àrab/ca");
    addIgnoredT("vegeu-der-afix");
    addIgnoredT("àudio simple");
    addIgnoredT("àudio");
    addIgnoredT("homòfons");
    addIgnoredT("ca-rima");
    addIgnoredT("homòfons");
    addIgnoredT("map draw");
    addIgnoredT("rimes");
    addIgnoredT("Col-begin");
    addIgnoredT("Col-3");
    addIgnoredT("Col-end");
    addIgnoredT("ca-interj");
    addIgnoredT("referències");
    addIgnoredT("etim-fpref");
    addIgnoredT("escala de Douglas");
    addIgnoredT("ampliar");
    addIgnoredT("zodíac/ca");
    addIgnoredT("terme");
    addIgnoredT("map_draw");
    addIgnoredT("etim-fsuf");
    addIgnoredT("entrada");
    addIgnoredT("en-adv");
    addIgnoredT("eo-entrada");
    addIgnoredT("pronunciació");
    addIgnoredT("ja-lema");
    addIgnoredT("zh-lema");
    addIgnoredT("el-nN-ο-α-1");
    addIgnoredT("zh-forma");
    addIgnoredT("es-num");
    addIgnoredT("wikimedia");
    addIgnoredT("audio");
    addIgnoredT("peces d'escacs/en");
    addIgnoredT("ja-forma");
    addIgnoredT("rom-entrada");
    addIgnoredT("es-interj");
    addIgnoredT("fr-interj");
    addIgnoredT("columnes");
    addIgnoredT("columna nova");
    addIgnoredT("colls de la baralla/fr");
    addIgnoredT("eo-lema");
    addIgnoredT("eo-part");
    addIgnoredT("colls de la baralla/es");
    addIgnoredT("signes");
    addIgnoredT("e");
    addIgnoredT("zodíac/fr");
    addIgnoredT("hi-pronoms");
    addIgnoredT("final columnes");
    addIgnoredT("ja-kanji");
    addIgnoredT("en-noun");
    addIgnoredT("la-decl-3a-cons-n");
    addIgnoredT("it-part");
    addIgnoredT("fr-haspirada");
    addIgnoredT("ISBN");
    addIgnoredT("top2");
    addIgnoredT("mid2");
    addIgnoredT("zodíac/it");
    addIgnoredT("arn-entrada");
    addIgnoredT("Ã udio simple");
    addIgnoredT("it-sil");
    addIgnoredT("peces d'escacs/fr");
    addIgnoredT("xib-lema");
    addIgnoredT("marca");
    addIgnoredT("arn-entrada");
    addIgnoredT("mesos/ca");
    addIgnoredT("de-adj-decl");
    addIgnoredT("Icelandic declension kvk sb 03 æ");
    addIgnoredT("ga-noun-m3");
    addIgnoredT("de-nota llengua");
    addIgnoredT("pl-adj-decl");
    addIgnoredT("de-nota llengua");
    addIgnoredT("e-propi");
    addIgnoredT("el-nN-ι-ια-2b");
    addIgnoredT("hr-decl-noun");
    addIgnoredT("la-adj-decl");
    addIgnoredT("mesos/es");
    addIgnoredT("sq-noun-f-të");
    addIgnoredT("ésser-estar");
    addIgnoredT("w");
    addIgnoredT("fi-decl-ovi");
    addIgnoredT("rel-top4");
    addIgnoredT("mid4");
    addIgnoredT("rel-mid4");
    addIgnoredT("rel-bottom4");
    addIgnoredT("ca-pronoms personals");
    addIgnoredT("taula periòdica/de");
    addIgnoredT("es.v.conj.ar");
    addIgnoredT("DEFAULTSORT:ejar");
    addIgnoredT("lleng");
    addIgnoredT("mesos/ty");
    addIgnoredT("ru-adjectiu2");
    addIgnoredT("ref-web");
    addIgnoredT("fi-decl-risti");
    addIgnoredT("hi-adj-1");
    addIgnoredT("IPAchar");
    addIgnoredT("ca-nota-l'a");
    addIgnoredT("ca-octes");
    addIgnoredT("zgh-pronoms parentiu");
    addIgnoredT("zgh-pronoms parentiu");
    addIgnoredT("mesos/ast");
    addIgnoredT("2");
    addIgnoredT("forma-");
    addIgnoredT("peces d'escacs/ca");
    addIgnoredT("taula periòdica/ca");

    addIgnoredText(",");
    addIgnoredText(".");
    addIgnoredText("__NOTOC__");

    // CHANGE THE SECTION NAME COMPARAISON.
    addIgnoredSec("Miscel·lània");
    addIgnoredSec("Vegeu també");
    addIgnoredSec("vegeu també");
    addIgnoredSec("Vegeu tembé");
    addIgnoredSec("Veugeu també");
    addIgnoredSec("Vegeu tammbé");
    addIgnoredSec("Vegueu també");
    addIgnoredSec("Vegeu tambe");
    addIgnoredSec("Vegeu");
    addIgnoredSec("Vegue també");
    addIgnoredSec("Vegeu també =");
    addIgnoredSec("= Vegeu també");
    addIgnoredSec("Vegeu tmbé");
    addIgnoredSec("Referències");
    addIgnoredSec("Relacionats");
    addIgnoredSec("Nota");
    addIgnoredSec("Notes");
    addIgnoredSec("Gentilicis");
    addIgnoredSec("Gentilici");
    addIgnoredSec("Nota d'ús");
    addIgnoredSec("Notes d'ús");
    addIgnoredSec("Derivats");
    addIgnoredSec("Variants");
    addIgnoredSec("Notes gramaticals");
    addIgnoredSec("Compostos");
    addIgnoredSec("Expressions");
    addIgnoredSec("Etimologia");
    addIgnoredSec("Arcaismes");
    addIgnoredSec("Citacions");
    addIgnoredSec("Altres formes");
    addIgnoredSec("Cites");
    addIgnoredSec("Notes d'us");
    addIgnoredSec("Grafia alternativa");
    addIgnoredSec("Onomàstica");
    addIgnoredSec("Notes d’ús");
    addIgnoredSec("Compostos i expressions");
    addIgnoredSec("Veure també");
    addIgnoredSec("Miscel·lania");
    addIgnoredSec("Termes derivats");
    addIgnoredSec("Termes relacionats");
    addIgnoredSec("Contraccions");
    addIgnoredSec("Declinació");
    addIgnoredSec("Locucions");
    addIgnoredSec("Enunciatiu");
    addIgnoredSec("Locució");
    addIgnoredSec("Nota d'us");
    addIgnoredSec("Variacions");
    addIgnoredSec("Glossa");
    addIgnoredSec("Forma alternativa");
    addIgnoredSec("Àmbit");
    addIgnoredSec("Flexió");
    addIgnoredSec("Refèrencies");
    addIgnoredSec("Kanji");
    addIgnoredSec("Cognats");
    addIgnoredSec("Formes alternatives");
    addIgnoredSec("Descendents");
    addIgnoredSec("Verb suru");
    addIgnoredSec("forma-");
    addIgnoredSec("Col·locacions");
    addIgnoredSec("Vegeu  també");
    addIgnoredSec("Fonts i referències");
    addIgnoredSec("Partícula");
    addIgnoredSec("Síl·laba");
  }

  protected final WiktionaryDataHandler catwdh;
  protected ExpandAllWikiModelCat templateRender;

  public WiktionaryExtractor(IWiktionaryDataHandler wdh) {
    super(wdh);
    this.catwdh = (WiktionaryDataHandler) wdh;
  }

  @Override
  public void setWiktionaryIndex(WiktionaryPageSource wi) {
    super.setWiktionaryIndex(wi);
    this.templateRender =
        new ExpandAllWikiModelCat(this.wi, new Locale("ca"), "/${image}", "/${title}", this.catwdh);
  }

  @Override
  protected void setWiktionaryPageName(String wiktionaryPageName) {
    super.setWiktionaryPageName(wiktionaryPageName);
    this.templateRender.setPageName(getWiktionaryPageName());
    this.templateRender.resetContext();
  }

  @Override
  public void extractData() {
    this.wdh.initializePageExtraction(getWiktionaryPageName());

    pageAnalyser(PageIterator.of(pageContent, ignoredTemplate));

    this.wdh.finalizePageExtraction();
  }

  public void pageAnalyser(final PageIterator page) {

    while (page.hasNext()) {

      if (page.next() instanceof WikiText.WikiSection
          && page.get().asWikiSection().getLevel() == 2) {
        extractLanguageSection(parseLanguage(page.get().asWikiSection().getHeading()),
            PageIterator.of(page.get().asWikiSection(), ignoredTemplate));
      } else if (page.get() instanceof WikiText.WikiSection) {
        log.debug("{} => Wrong level section found \"{}\"! ---> {}", getWiktionaryPageName(),
            page.get().asWikiSection().getHeading().getContent().getText().trim(), url());
      } else if (!ignoredText.contains(page.get().getText().trim())
          && !page.get().getText().contains("#REDIRECCIÓ")
          && !page.get().getText().contains("#REDIRECT")
          && !page.get().getText().contains("[[Fitxer:")) {
        log.debug("{} => Low level token found \"{}\" ---> {}", getWiktionaryPageName(), page.get(),
            url());
      }
    }

  }

  public void extractLanguageSection(final String currentLanguage, final PageIterator sec) {

    if (currentLanguage == null || null == this.wdh.getExolexFeatureBox(ExtractionFeature.MAIN)
        && !currentLanguage.equals("cat")) {
      return;
    }

    log.trace("{} => Extracting language : {} ---> {}", getWiktionaryPageName(), currentLanguage,
        url());

    this.wdh.initializeLanguageSection(currentLanguage);

    while (sec.hasNext()) {
      sec.next();

      if (sec.isTemplate()) {
        templateDispatcher(sec);
      } else if (sec.isSection()) {
        sectionDispatcher(sec); // if the section wasn't dispatched.
      } else if (!ignoredText.contains(sec.get().getText().trim())
          && !sec.get().getText().startsWith("[[Fitxer")) {
        log.trace("{} => Unhandled component \"{}\" in language section ---> {}",
            getWiktionaryPageName(), sec.get().getText(), url());
      }
    }

    this.wdh.finalizeLanguageSection();
  }

  public void extractLexicalEntry(final WikiText.WikiSection section) {
    this.wdh.initializeLexicalEntry(getSectionTitle(section));
    log.trace("{} => Extracting section \"{}\" ---> {}", getWiktionaryPageName(),
        getSectionTitle(section), url());

    PageIterator sectionIt = PageIterator.of(section.getContent().tokens(), ignoredTemplate);

    while (sectionIt.hasNext()) {
      sectionIt.next();

      if (sectionIt.isTemplate()) {
        templateDispatcher(sectionIt);
      } else if (sectionIt.get().getText().startsWith("#")) {
        extractDefinitionField(sectionIt);
      } else if (sectionIt.isSection()) {
        sectionDispatcher(sectionIt);
      } else if (!sectionIt.get().getText().trim().equals("!")
          && !sectionIt.get().getText().contains("<gallery>")
          && !sectionIt.get().getText().contains("[[Categoria:")
          && !sectionIt.get().getText().startsWith("[[Fitxer:")) {
        log.trace("{} => Text unhandled in \"{}\" -> {} ---> {}", getWiktionaryPageName(),
            getSectionTitle(section), sectionIt.get().getText().trim(), url());
      }

    }
  }

  private void sectionDispatcher(PageIterator sec) {
    final String name = sec.get().asWikiSection().getHeading().getContent().getText().trim();
    PageIterator childs = PageIterator.of(sec.get().asWikiSection(), ignoredTemplate);

    switch (name) {
      case "Conjugation":
      case "Conjucació":
      case "Conjugació":
        if (this.wdh.getCurrentEntryLanguage().equals("ca")
            && !this.wdh.isDisabled(ExtractionFeature.MORPHOLOGY)) {
          extractConjugation(childs);
        }

        while (childs.hasNext() && (!childs.isNextATemplate()
            || !childs.shadowNext().asTemplate().getName().startsWith("-"))) {
          childs.next();
        }

        // TODO extract conj
        break;
      case "Expressions i frases fetes":
        extractExpressions(childs);
        break;
      case "Antònims":
      case "Sinònims":
      case "Hipònims":
      case "Hiperònims":
      case "Parònims":
        extractNym(name, childs);
        break;
      default:
        if (sec.get().asWikiSection().getLevel() == 3) {
          if (isIgnoredSection(sec.get().asWikiSection())) {
            return;
          }
          extractLexicalEntry(sec.get().asWikiSection());
        }
        break;
    }
    while (childs.hasNextTemplate()) { // if the current sections contains unused childs dispatch
      childs.nextTemplate();
      templateDispatcher(childs);
    }

  }

  private void extractConjugation(PageIterator childs) {
    if (!childs.hasNext()) {
      return;
    }
    String html = this.templateRender.renderHtml(childs.next().getText());
    log.trace("{} => Conjugaison found -> {} ---> {}", getWiktionaryPageName(), childs.get(),
        url());

    if (html == null) {
      return; // failing silently: error message already given.
    }

    Document doc = Jsoup.parse(html);
    Elements navFrame = doc.getElementsByClass("NavFrame");

    CatalanConjugationTableExtractor declinationExtractor =
        new CatalanConjugationTableExtractor(this.getWiktionaryPageName(), "fr", new ArrayList<>());

    navFrame.stream().filter(element -> {
      final Elements navHead = element.getElementsByClass("NavHead");
      if (navHead.size() != 0) {
        final Elements spans = element.select("span");
        if (spans.size() != 0) {
          Element title = spans.get(0);
          switch (title.text().trim().toLowerCase(Locale.ROOT)) {
            case "formes no normatives o col·loquials":
            case "formes personals compostes i perifràstiques":
            case "formes compostes i perifràstiques":
              return false;
            case "primera conjugació regular":
            case "conjugació antiga":
            case "primera conjugació regular, amb hiat entre el radical i la desinència":
            case "primera conjugació regular, amb alternança consonàntica en el radical":
            case "primera conjugació regular, amb radical modificat sense desinència":
            case "tercera conjugació incoativa":
            case "tercera conjugació pura":
            case "segona conjugació irregular":
            case "segona conjugació regular":
            case "segona conjugació amb formes velaritzades, participi irregular":
            case "segona conjugació amb alternança consonàntica en el radical":
              return true;
            default:
              log.trace("{} => Non registred conj title (not a problem) -> {} ---> {}",
                  getWiktionaryPageName(), title.text(), url());
              return true;
          }
        }
      }
      return true;
    }).forEach(element -> {
      element.select("table").forEach(table -> {
        Set<LexicalForm> forms = declinationExtractor.parseTable(table);
        forms.stream().filter(lexicalForm -> lexicalForm.getFeature().size() != 0)
            .forEach(this.catwdh::addLexicalForm);
      });
    });
  }

  private void templateDispatcher(PageIterator sec) {
    final WikiText.Template template = sec.get().asTemplate();
    final String name = template.getName().trim();

    if (name.matches("(.*)-pron")) {
      extractPrononciation(template);
      return;
    } else if (name.matches("(.*)-adj-forma") || name.matches("(.*)-nom(.*)")
        || name.matches("(.*)-num-forma")) {
      extractGenderAndNumber(template);
      return;
    } else if (name.matches("(.*)-adj")) {
      extractAdj(template);
      return;
    } else if (name.matches("(.*)-verb")) {
      if (name.equals("ca-verb")) {

        final String verb_status = toText(template.getArg("1"));
        this.catwdh.registerVerbForm(verb_status);

        if (!this.catwdh.isDisabled(ExtractionFeature.MORPHOLOGY))
        // pronominal F
        {
          FormBuilder.of(toText(template.getArg("p")), this.catwdh).save();
        }

      } else {
        int argFound = 1;
        if (template.getArgs().size() > argFound) {
          log.trace("{} => Verb args unhandled found {} ---> {}", getWiktionaryPageName(),
              template.getText(), url());
        }
      }
      return;
    } else if (name.matches("(.*)-verb-forma") || name.equals("verb-forma")) {
      if (!this.catwdh.isDisabled(ExtractionFeature.MORPHOLOGY)) {
        FormBuilder.of(toText(template.getArg("1")), this.catwdh).addMorpho(Mood.INFINITIVE).save();
      }
      return;
    }

    switch (template.getName().trim()) {
      case "-pron-":
      case "pron":
      case "pronafi":
        if (template.getArg("2") != null) {
          final String lang = template.getArg("1").getText();
          if (ISO639_3.sharedInstance.getLang(lang) != null) {
            this.wdh.registerPronunciation(template.getArg("2").getText(), lang);
          } else {
            log.debug("Unknown language code in pron for '{}' : {}", getWiktionaryPageName(), lang);
          }
        }
        break;
      case "lema":
        final String gen = toText(template.getArg("g"));
        if (gen == null) {
          break;
        }
        extractNumber(gen);
        extractGender(gen);
        break;
      case "-trad-":
        extractTranslation(sec);
        break;
      case "inici": // Try to handle translation without an opened field.
      case "t-inici": // Try to handle translation without an opened field.
        translationFieldWithOutHeadExtractor(sec);
        break;
      case "-comp-":
        extractExpressions(sec);
        break;
      case "-hipo-":
      case "-mero-":
      case "-ant-":
      case "-sin-":
      case "-hiper-":
      case "-holo-":
      case "-paro-":
        extractNymWithPage(sec);
        break;
      case "-der-":
        extractDerived(sec);
        break;
      case "etim-comp":
      case "-etimologia-":
      case "etimologia":
      case "etim-s":
      case "-etim-":
      case "etim-lang":
        skipUntilNextSection(sec);
        break;
      case "-rel-":
        skipSimpleText(sec);
        if (sec.hasNext() && sec.isNextATemplate()
            && !sec.shadowNextTemplate().getName().startsWith("-")) {
          sec.next();
        }
      case "-cog-":
      case "-desc-":
      case "-var-":
      case "-notes-":
      case "-nota-":
      case "-fals-":
      case "-exe-":
        skipSimpleText(sec);
        break;
      default:
        if (!ignoredTemplate.contains(template.getName().trim())) {
          log.debug("{} => Unhandled template \"{}\" in language section ---> {}",
              getWiktionaryPageName(), sec.get().getText(), url());
        }
        break;
    }
  }

  private void extractDerived(PageIterator sec) {
    log.trace("{} => Derived field found. ---> {}", getWiktionaryPageName(), url());
    while (sec.hasNext() && ((!sec.isNextASection() && !sec.isNextATemplate())
        || (sec.isNextATemplate() && sec.shadowNext().asTemplate().getName().contains("Col-")))) {
      sec.next();
      String content = render(sec.get().getText());
      if (!content.isEmpty() && content.charAt(content.length() - 1) == '.') {
        content = content.substring(0, content.length() - 1);
      }
      this.catwdh.registerDerivedForms(content);
    }
  }

  private void extractAdj(WikiText.Template template) {
    extractGender(toText(template.getArg("1")));
    if (this.catwdh.isDisabled(ExtractionFeature.ETYMOLOGY)) {
      return;
    }
    FormBuilder.of(toText(template.getArg("f")), this.catwdh).addMorpho(Gender.FEMININE).save();
    FormBuilder.of(toText(template.getArg("f2")), this.catwdh).addMorpho(Gender.FEMININE).save();
    FormBuilder.of(toText(template.getArg("p")), this.catwdh).addGender("m")
        .addMorpho(Number.PLURAL).save();
    FormBuilder.of(toText(template.getArg("p2")), this.catwdh).addGender("m")
        .addMorpho(Number.PLURAL).save();
    FormBuilder
        .of(template.getArg("pf") == null ? toText(template.getArg("fp"))
            : toText(template.getArg("pf")), this.catwdh)
        .addGender("f").addMorpho(Number.PLURAL).save();
  }

  private void extractGenderAndNumber(WikiText.Template template) {
    final String arg = toText(template.getArg("1"));
    if (arg == null) {
      return;
    }
    extractNumber(arg);
    extractGender(arg);
  }

  private void translationFieldWithOutHeadExtractor(PageIterator sec) {
    PageIterator clone = sec.cloneIt();
    clone.skip(sec.getCursor() - 2);
    extractTranslation(clone);
    sec.skip(clone.getCursor() - sec.getCursor());
  }

  public void skipSimpleText(final PageIterator section) {
    while (section.hasNext()
        && ((!section.isNextASection() && !section.isNextATemplate()) || (section.isNextATemplate()
            && section.shadowNext().asTemplate().getName().contains("Col-")))) // TODO do something
    {
      section.next();
    }
  }

  public void extractTranslation(final PageIterator section) {
    log.trace("{} => Translation field found ---> {}", getWiktionaryPageName(), url());

    String gloss = null;
    String shadowNextName;
    if (section.hasNext() && section.isNextATemplate()
        && ((shadowNextName = section.shadowNext().asTemplate().getName()).equals("inici")
            || shadowNextName.equals("t-inici"))) {
      if (section.next().asTemplate().getArg("1") != null) {
        log.trace("{} => Glossary found -> {} ---> {}", getWiktionaryPageName(),
            section.get().asTemplate().getArg("1").getText(), url());
        gloss = section.get().asTemplate().getArg("1").getText();
      }
    } else {
      log.trace("{} => Translation field open fail. ---> {}", getWiktionaryPageName(), url());
    }

    while (section.hasNext() && !section.isNextATemplate()) {
      PageIterator line =
          PageIterator.of(section.next().getText().substring(1).trim(), ignoredTemplate);

      if (line.goToNextTemplate("trad") == null) {
        log.trace("{} => Translation line with out template ---> {}", getWiktionaryPageName(),
            url());
        continue;
      }

      final String lang = LangTools.normalize(line.get().asTemplate().getArg("1").getText());
      final String trans = line.get().asTemplate().getArg("2").getText();
      this.wdh.registerTranslation(lang, this.wdh.createGlossResource(gloss), "", trans);

      log.trace("{} => Translation found --{}--> {} ---> {}", getWiktionaryPageName(), lang, trans,
          url());
    }

    if (!section.hasNext() || !section.isNextATemplate()
        || !((shadowNextName = section.shadowNext().asTemplate().getName()).equals("final")
            || shadowNextName.equals("t-final"))) {
      log.trace("{} => Translation field end fail. ---> {}", getWiktionaryPageName(), url());
    } else {
      section.next();
    }

    if (section.hasNext() && section.isNextATemplate()
        && ((shadowNextName = section.shadowNext().asTemplate().getName()).equals("inici")
            || shadowNextName.equals("t-inici"))) {
      extractTranslation(section);
    }
  }

  public void extractExpressions(final PageIterator section) {
    while (section.hasNext() && !section.isNextATemplate() && !section.isNextASection()) {
      section.next();
    }
  }

  private void extractNumber(final String number) {
    if (number.contains("p")) {
      this.wdh.registerPropertyOnCanonicalForm(LexinfoOnt.number, LexinfoOnt.plural);
    }
  }

  private void extractGender(final String gender) {
    if (gender == null) {
      return;
    }

    RDFNode gen;

    if (gender.contains("mf")) {
      this.wdh.registerPropertyOnCanonicalForm(LexinfoOnt.gender, LexinfoOnt.neuter);
      log.trace("{} => Gender found -> {} ---> {}", getWiktionaryPageName(), gender, url());
    } else {

      for (int i = 0; i < gender.length(); i++) {
        gen = null;

        if (gender.charAt(i) == 'm') {
          gen = LexinfoOnt.masculine;
        } else if (gender.charAt(i) == 'f') {
          gen = LexinfoOnt.feminine;
        }

        if (gen != null) {
          this.wdh.registerPropertyOnCanonicalForm(LexinfoOnt.gender, gen);
          log.trace("{} => Gender found -> {} ---> {}", getWiktionaryPageName(), gender, url());
        }
      }

    }
  }

  public void extractNymWithPage(final PageIterator field) {
    final String relation = field.get().asTemplate().getName().substring(1,
        field.get().asTemplate().getName().length() - 1);
    extractNym(relation, field);
  }

  public void extractNym(final String relation, final PageIterator field) {

    while (field.hasNext() && !field.isNextATemplate() && !field.isNextASection()) {

      String text = field.next().getText();
      if (text.startsWith("*") || text.startsWith("#")) {
        text = text.substring(1);
      }

      PageIterator line = PageIterator.of(text.trim(), ignoredTemplate);

      while (line.hasNext() && !line.shadowNext().getText().contains("(")) {

        if (line.next() instanceof WikiText.Link) {
          this.catwdh.registerNymRelation(line.get().asLink().getLinkText(), relation);
          log.trace("{} => {} detected -> {} ---> {}", getWiktionaryPageName(), relation,
              line.get().asLink().getLinkText(), url());
        } else if (!line.get().getText().trim().equals(",")
            && !line.get().getText().trim().equals(".")) {
          log.trace("{} => Unhandled text in {} field -> {} ---> {}", getWiktionaryPageName(),
              relation, line.get().getText(), url());
        }

      }

      final StringBuilder skipped = new StringBuilder();
      line.forEachRemaining(token -> skipped.append(token.getText()));

      if (!skipped.toString().isBlank()) {
        log.trace("{} => Text in {} skipped -> {} ---> {}", getWiktionaryPageName(), relation,
            skipped, url());
      }
    }
  }


  public void extractDefinitionField(final PageIterator field) {
    extractDefinitionLine(field.get());
    while (field.hasNext() && field.shadowNext().getText().startsWith("#")) {
      extractDefinitionLine(field.next());
    }
  }

  public void extractDefinitionLine(final WikiText.Token line) {

    if (line.getText().startsWith("#:") || line.getText().startsWith("##:")) {
      final String ex = render(line.getText().substring(2).trim());
      log.trace("{} => Example found -> {} with context -> {} ---> {}", getWiktionaryPageName(), ex,
          this.templateRender.getContext(), url());
      if (!ex.isBlank()) {
        this.wdh.registerExample(ex, this.templateRender.getContext());
        this.templateRender.resetContext();
      }
    } else if (line.getText().startsWith("##")) {
      log.trace("{} => Definition found -> {} ---> {}", getWiktionaryPageName(), line.getText(),
          url());
      this.wdh.registerNewDefinition(render(line.getText().substring(1).trim()), 2);
    } else if (line.getText().startsWith("#")) {
      log.trace("{} => Definition found -> {} ---> {}", getWiktionaryPageName(), line.getText(),
          url());
      this.wdh.registerNewDefinition(render(line.getText().substring(1).trim()), 1);
    } else if (!line.getText().substring(1).isBlank()) {
      log.debug("{} => Definition component non handled -> {} ---> {}", getWiktionaryPageName(),
          line.getText(), url());
    }

  }


  public static class FormBuilder {

    protected final WiktionaryDataHandler catwdh;
    protected LexicalForm form;
    protected StrictInflexionScheme sch;

    protected final String value;

    /**
     * if the value is null, this object do nothing.
     */
    private FormBuilder(@Nullable final String value, final WiktionaryDataHandler catwdh) {
      this.catwdh = catwdh;

      if (value != null) {
        this.form = new LexicalForm();
        this.sch = new StrictInflexionScheme();
      }

      this.value = value;
    }

    public FormBuilder addGender(@Nullable final String gender) {
      if (this.value == null) {
        return this;
      }

      if (gender != null) {
        if (gender.equals("m")) {
          this.sch.add(Gender.MASCULINE);
        } else if (gender.equals("f")) {
          this.sch.add(Gender.FEMININE);
        } else {
          this.sch.add(Gender.NEUTER);
        }
      }
      return this;
    }

    public FormBuilder addMorpho(final MorphoSyntacticFeature morpho) {
      if (this.value == null) {
        return this;
      }

      this.sch.add(morpho);
      return this;
    }

    public boolean save() {
      if (this.value == null) {
        return false;
      }

      this.form.setFeature(this.sch);
      this.form
          .addValue(new WrittenRepresentation(this.value, this.catwdh.getCurrentEntryLanguage()));

      this.catwdh.addLexicalForm(this.form);

      return true;
    }


    public static FormBuilder of(final String value, final WiktionaryDataHandler catwdh) {
      return new FormBuilder(value, catwdh);
    }
  }

  public void extractPrononciation(final WikiText.Template template) {
    String prononciationText = render(template.getText());

    if (prononciationText.startsWith("Pronúncia(i): ")) {
      prononciationText = prononciationText.substring("Pronúncia(i): ".length());
      prononciationText = prononciationText.split("Rimes:")[0];
    }

    prononciationText =
        prononciationText.replaceAll("\n", " ").replaceAll("/, /", "/,/").replaceAll(", ", " ");

    String[] args = prononciationText.split(" ");
    ArrayList<PronBuilder> pronBuilders = new ArrayList<>();
    PronBuilder builder = new PronBuilder("root");

    for (String s : args) {
      if (s.startsWith("/")) {
        for (String pron : s.split(",")) {
          pronBuilders.add(builder.of(pron));
        }
      } else if (this.catwdh.getCurrentEntryLanguage().equals("ca") && !s.startsWith("(")) {
        builder = new PronBuilder(s);
      }
    }

    this.catwdh.registerPron(pronBuilders);

  }

  public static class PronBuilder {

    public String loc;
    public String pron;

    public PronBuilder(final String loc) {
      this.loc = loc.trim();
      if (loc.endsWith(":")) {
        this.loc = this.loc.substring(0, this.loc.length() - 1);
      }
    }

    public PronBuilder(final String loc, final String pron) {
      this.loc = loc;
      this.pron = pron;
    }

    public PronBuilder of(final String pron) {
      return new PronBuilder(loc, pron);
    }

    @Override
    public String toString() {
      return "{" + "loc:'" + loc + '\'' + ", pron:'" + pron + '\'' + '}';
    }
  }

  public void skipUntilNextSection(final PageIterator sec) {
    while (sec.hasNext() && !sec.isNextASection()) {
      sec.next();
    }
  }

  public String parseLanguage(final WikiText.Heading heading) {
    Matcher match = extractLanguage.matcher(heading.getText());
    return match.find() ? LangTools.normalize(match.group(2)) : null;
  }

  protected String render(final String content) {
    String rended = "";
    try {
      rended = this.templateRender.render(new PlainTextConverter(), content).trim();
    } catch (IOException e) {
      log.debug("{} => Render error -> {} ---> {}", getWiktionaryPageName(), e.getMessage(), url());
    }
    return rended;
  }

  protected String url() {
    return URL + getWiktionaryPageName();
  }

  protected static String toText(final WikiText.WikiContent content) {
    return content == null ? null : content.getText();
  }

  public static void addIgnoredT(final String ignored) {
    ignoredTemplate.add(ignored);
  }

  public static void addIgnoredText(final String text) {
    ignoredText.add(text);
  }

  public static void addIgnoredSec(final String ignored) {
    ignoredSection.add(ignored.trim());
  }

  public boolean isIgnoredSection(final WikiText.WikiSection section) {
    return ignoredSection.contains(section.getHeading().getContent().getText().trim());
  }

  public static String getSectionTitle(final WikiText.WikiSection section) {
    return section.getHeading().getContent().getText().trim();
  }

  public static boolean isNameEquals(final String name, final WikiText.WikiSection section) {
    return section.getHeading().getContent().getText().trim().equals(name);
  }

}

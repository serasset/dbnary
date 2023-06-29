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
import org.getalp.dbnary.tools.PageIterator;
import org.getalp.dbnary.wiki.WikiText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TODO :
 *  - complete all TOD0 resuming to :
 *      - Extract Conjugaison
 *      - Extract Datas from some templates
 *      - Extract Datas of the templates to the DataHandler
 *  - Make a big check for external languages
 *  - Make a big refactor and clean the entire code, which is ugly :
 *      - Section name comparator
 *      - The way that the templates, and sections are currently dispatched.
 *      - Other things.
 */

/*--endolex=ontolex,morphology,lime --exolex=ontolex*/
public class WiktionaryExtractor extends AbstractWiktionaryExtractor {

    private static final Logger log = LoggerFactory.getLogger(WiktionaryExtractor.class);

    private static final String URL = "https://ca.wiktionary.org/wiki/";

    public static final Set<String> ignoredTemplate = new HashSet<>();
    public static final Set<String> ignoredSection = new HashSet<>();
    public static final Set<String> ignoredText = new HashSet<>();

    public static int count = 0;

    private static final Pattern extractLanguage = Pattern.compile("(-)(.{2,3})(-)");


    static {
        addIgnoredT("vegeu");
        addIgnoredT("àudio");
        addIgnoredT("colauto");
        addIgnoredT("mig");
        addIgnoredT("q");
        addIgnoredT("homòfons");
        addIgnoredT("map shape");
        addIgnoredT("verb-forma");
        addIgnoredT("parònims");
        addIgnoredT("numeral");
        addIgnoredT("ca-num");
        addIgnoredT("etimologia");
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
        addIgnoredT("etimologia");
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


        addIgnoredText(",");
        addIgnoredText(".");
        addIgnoredText("__NOTOC__");

        // CHANGE THE SECTION NAME COMPARAISON.
        addIgnoredSec("Miscel·lània", 3);
        addIgnoredSec("Vegeu també", 3);
        addIgnoredSec("Relacionats", 4);
        addIgnoredSec("Nota", 4);
        addIgnoredSec("Notes", 4);
        addIgnoredSec("Gentilicis", 4);
        addIgnoredSec("Gentilicis", 3);
        addIgnoredSec("Nota d'ús", 4);
        addIgnoredSec("Notes d'ús", 4);
        addIgnoredSec("Derivats", 4);
        addIgnoredSec("Variants", 4);
        addIgnoredSec("Referències", 3);
        addIgnoredSec("Notes gramaticals", 4);
        addIgnoredSec("Compostos", 5);
        addIgnoredSec("Expressions", 5);
        addIgnoredSec("Etimologia", 4);
        addIgnoredSec("Arcaismes", 5);
        addIgnoredSec("Arcaismes", 4);
        addIgnoredSec("Citacions", 3);
        addIgnoredSec("Citacions", 4);
        addIgnoredSec("Altres formes", 4);
        addIgnoredSec("Notes", 3);
        addIgnoredSec("Cites", 4);
        addIgnoredSec("Cites", 3);
        addIgnoredSec("Notes d'us", 4);
        addIgnoredSec("Grafia alternativa", 4);
        addIgnoredSec("Grafia alternativa", 3);
        addIgnoredSec("Nota", 3);
        addIgnoredSec("Onomàstica", 4);
        addIgnoredSec("Notes d’ús", 4);
        addIgnoredSec("Compostos i expressions", 4);
        addIgnoredSec("Vegeu tambe", 3);
        addIgnoredSec("Veure també", 3);
        addIgnoredSec("Miscel·lania", 3);
        addIgnoredSec("Termes derivats", 4);
        addIgnoredSec("Referències", 4);
        addIgnoredSec("Termes relacionats", 3);
        addIgnoredSec("Vegeu", 3);
        addIgnoredSec("Variants", 4);

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
        this.templateRender = new ExpandAllWikiModelCat(this.wi, new Locale("ca"), "/${image}", "/${title}");
    }

    @Override
    protected void setWiktionaryPageName(String wiktionaryPageName) {
        super.setWiktionaryPageName(wiktionaryPageName);
        this.templateRender.setPageName(getWiktionaryPageName());
    }

    @Override
    public void extractData() {
        count++;

        this.wdh.initializePageExtraction(getWiktionaryPageName());

        pageAnalyser(PageIterator.of(pageContent, ignoredTemplate));

        showCount();

        this.wdh.finalizePageExtraction();
    }

    public void pageAnalyser(final PageIterator page) {

        while (page.hasNext()) {
            if (page.next() instanceof WikiText.WikiSection && page.get().asWikiSection().getLevel() == 2)
                extractLanguageSection(parseLanguage(page.get().asWikiSection().getHeading()), PageIterator.of(page.get().asWikiSection()));
            else if (page.get() instanceof WikiText.WikiSection)
                log.warn("{} => Wrong level section found \"{}\"! ---> {}", getWiktionaryPageName(), page.get().asWikiSection().getHeading().getContent().getText().trim(), url());
            else if (!ignoredText.contains(page.get().getText().trim()) && !page.get().getText().contains("#REDIRECCIÓ") && !page.get().getText().contains("#REDIRECT") && !page.get().getText().contains("[[Fitxer:"))
                log.warn("{} => Low level thing found \"{}\" ---> {}", getWiktionaryPageName(), page.get(), url());
        }

    }

    public void extractLanguageSection(final String currentLanguage, final PageIterator sec) {

        if (currentLanguage == null || null == this.wdh.getExolexFeatureBox(ExtractionFeature.MAIN) && !currentLanguage.equals("cat"))
            return;

        log.trace("{} => Extracting language : {} ---> {}", getWiktionaryPageName(), currentLanguage, url());

        this.wdh.initializeLanguageSection(currentLanguage);

        while (sec.hasNext()) {
            sec.next();
            if (sec.isTemplate()) {
                WikiText.Template template = sec.get().asTemplate();
                switch (template.getName().trim()) {
                    case "ca-pron":
                        // extractPrononciation(template); // proncom
                        break;
                    case "pron":
                    case "pronafi":
                        this.wdh.registerPronunciation(template.getArg("2").getText(), template.getArg("1").getText());
                        break;
                    case "etim-comp":
                    case "-etimologia-":
                    case "etim-s":
                    case "-etim-":
                    case "etim-lang":
                        skipUntilNextSection(sec);
                        break;
                    default:
                        if (!ignoredTemplate.contains(template.getName().trim()))
                            log.warn("{} => Unhandled template \"{}\" in language section ---> {}", getWiktionaryPageName(), sec.get().getText(), url());
                        break;
                }
            } else if (sec.isSection()) {
                if (ignoredSection.contains(sec.get().asWikiSection().getHeading().getText()))
                    continue;
                if (sec.get().asWikiSection().getHeading().getContent().getText().trim().equals("Conjugació")) {
                    // TODO extract conj
                } else
                    extractLexicalEntry(sec.get().asWikiSection());
            } else if (!ignoredText.contains(sec.get().getText().trim()) && !sec.get().getText().startsWith("[[Fitxer"))
                log.trace("{} => Unhandled text \"{}\" in language section ---> {}", getWiktionaryPageName(), sec.get().getText(), url());
        }

        this.wdh.finalizeLanguageSection();
    }

    public void extractLexicalEntry(final WikiText.WikiSection section) {
        if (section.getLevel() != 3) {
            log.warn("{} => STRANGE SECTION LEVEL -> expected 3. ---> {}", getWiktionaryPageName(), url());
            return;
        }

        log.trace("{} => Extracting section \"{}\" ---> {}", getWiktionaryPageName(), section.getHeading().getText(), url());
        this.wdh.initializeLexicalEntry(section.getHeading().getText());

        PageIterator sectionIt = PageIterator.of(section.getContent().tokens(), ignoredTemplate);

        while (sectionIt.hasNext()) {
            sectionIt.next();
            if (sectionIt.isTemplate()) {
                WikiText.Template template = sectionIt.get().asTemplate();
                switch (template.getName()) {
                    case "lema":
                        final String gen = toText(template.getArg("g"));
                        if (gen == null)
                            break;
                        extractNumber(gen);
                        extractGender(gen);
                        break;
                    case "ca-adj-forma":
                    case "es-adj-forma":
                    case "ca-nom-forma":
                    case "ca-nom":
                    case "ca-num-forma":
                        final String arg = toText(template.getArg("1"));
                        if (arg == null)
                            break;
                        extractNumber(arg);
                        extractGender(arg);
                        break;
                    case "verb-forma":
                        //TODO extract
                        break;
                    case "ca-verb":
                        // TODO extract
                        break;
                    case "-hipo-":
                    case "-mero-":
                    case "-ant-":
                    case "-sin-":
                    case "-hiper-":
                    case "-holo-":
                        extractNymWithPage(sectionIt);
                        break;
                    case "-comp-":
                        extractExpressions(sectionIt);
                        break;
                    case "-trad-":
                        extractTranslation(sectionIt);
                        break;
                    case "inici": // Try to handle translation without an opened field.
                        PageIterator clone = sectionIt.cloneIt();
                        clone.skip(sectionIt.getCursor() - 2);
                        extractTranslation(clone);
                        sectionIt.skip(clone.getCursor() - sectionIt.getCursor());
                        break;
                    case "ca-adj":
                        final String gender = toText(template.getArg("1"));
                        if (gender != null && gender.equals("m"))
                            this.wdh.registerPropertyOnCanonicalForm(LexinfoOnt.gender, LexinfoOnt.masculine);
                        final String feminine = toText(template.getArg("f"));
                        final String plural1 = toText(template.getArg("p"));
                        final String plural2 = toText(template.getArg("p2"));
                        String pluralFeminine = toText(template.getArg("pf"));
                        if (pluralFeminine == null) pluralFeminine = toText(template.getArg("fp"));
                        int argFound = 1;
                        if (feminine != null) argFound++;
                        if (plural1 != null) argFound++;
                        if (plural2 != null) argFound++;
                        if (pluralFeminine != null) argFound++;
                        if (toText(template.getArg("cat")) != null) argFound++;
                        // TODO do something with this.
                        if (template.getArgs().size() > argFound)
                            log.warn("{} => Args detected on ajd template ---> {}", getWiktionaryPageName(), url());
                        break;
                    case "sigles": // TODO extract may be.
                        break;
                    case "-rel-":
                        skipSimpleText(sectionIt);
                        if (sectionIt.hasNext() && sectionIt.isNextATemplate() && !sectionIt.shadowNextTemplate().getName().startsWith("-"))
                            sectionIt.next();
                    case "-var-":
                    case "-der-":
                    case "-cog-":
                    case "-desc-":
                    case "-notes-":
                    case "-nota-":
                    case "-fals-":
                        skipSimpleText(sectionIt);
                        break;
                    case "ca-pron":
                    case "entrada":
                        break;
                    case "ca-verb-forma":
                        // TODO check if some have args
                        break;
                    default:
                        log.warn("{} => Template unhandled template in \"{}\" -> {} ---> {}", getWiktionaryPageName(), this.catwdh.parseSectionName(section.getHeading().getText()), sectionIt.get().getText(), url());
                        break;
                }
            } else if (sectionIt.isSection()) {
                final String headingTitle = sectionIt.get().asWikiSection().getHeading().getContent().getText().trim();
                if (headingTitle.equals("Conjugació"))
                    // TODO EXTRACT
                    continue;
                else if (headingTitle.equals("Expressions i frases fetes"))
                    extractExpressions(sectionIt);
                else if (headingTitle.equals("Antònims") || headingTitle.equals("Sinònims") || headingTitle.equals("Hipònims")
                         || headingTitle.equals("Hiperònims") || headingTitle.equals("Parònims"))
                    extractNymWithSection(sectionIt.get().asWikiSection());
                else if (!ignoredSection.contains(sectionIt.get().asWikiSection().getHeading().getText()))
                    log.warn("{} => Section unhandled in \"{}\" -> {} ---> {}", getWiktionaryPageName(), this.catwdh.parseSectionName(section.getHeading().getText()), this.catwdh.parseSectionName(sectionIt.get().asWikiSection().getHeading().getText()), url());
            } else {
                if (sectionIt.get().getText().startsWith("#"))
                    extractDefinitionField(sectionIt);
                else if (!sectionIt.get().getText().contains("<gallery>") && !sectionIt.get().getText().contains("[[Categoria:") && !sectionIt.get().getText().startsWith("[[Fitxer:"))
                    log.warn("{} => Text unhandled in \"{}\" -> {} ---> {}", getWiktionaryPageName(), this.catwdh.parseSectionName(section.getHeading().getText()), sectionIt.get().getText().trim(), url());
            }
        }
    }

    public void skipSimpleText(final PageIterator section) {
        log.trace("{} => Derived field found. ---> {}", getWiktionaryPageName(), url());
        while (section.hasNext() && ((!section.isNextASection() && !section.isNextATemplate()) || (section.isNextATemplate() && section.shadowNext().asTemplate().getName().contains("Col-")))) // TODO do something with this.
            section.next();
    }

    public void extractTranslation(final PageIterator section) {
        final StructuredGloss gloss = new StructuredGloss();
        if (section.hasNext() && section.isNextATemplate() && section.shadowNext().asTemplate().getName().equals("inici")) {
            if (section.next().asTemplate().getArg("1") != null) {
                log.trace("{} => Glossary found -> {} ---> {}", getWiktionaryPageName(), section.get().asTemplate().getArg("1").getText(), url());
                gloss.setGloss(section.get().asTemplate().getArg("1").getText());
            }
        } else
            log.trace("{} => Translation field open fail. ---> {}", getWiktionaryPageName(), url());

        while (section.hasNext() && !section.isNextATemplate()) {
            PageIterator line = PageIterator.of(section.next().getText().substring(1).trim(), ignoredTemplate);

            if (line.goToNextTemplate("trad") == null) {
                log.trace("{} => Translation line with out template ---> {}", getWiktionaryPageName(), url());
                continue;
            }

            final String lang = LangTools.normalize(line.get().asTemplate().getArg("1").getText());
            final String trans = line.get().asTemplate().getArg("2").getText();
            this.wdh.registerTranslation(lang, this.wdh.createGlossResource(gloss), "", trans);

            log.trace("{} => Translation found --{}--> {} ---> {}", getWiktionaryPageName(), lang, trans, url());
        }

        if (!section.hasNext() || !section.isNextATemplate() || !section.shadowNext().asTemplate().getName().equals("final"))
            log.trace("{} => Translation field end fail. ---> {}", getWiktionaryPageName(), url());
        else
            section.next();

        if (section.hasNext() && section.isNextATemplate() && section.shadowNext().asTemplate().getName().equals("inici"))
            extractTranslation(section);
    }

    public void extractExpressions(final PageIterator section) {
        while (section.hasNext() && !section.isNextATemplate() && !section.isNextASection()) { // TODO do somethings with this datas.
            if (section.next().getText().startsWith("* ")) {
                log.trace("{} => -comp- Changing part -> {} ---> {}", getWiktionaryPageName(), section.get().getText().substring(2), url());
                continue;
            }
            log.trace("{} => -comp- Text found -> {} ---> {}", getWiktionaryPageName(), section.get().getText(), url());
        }
    }

    private void extractNumber(final String number) {
        if (number.contains("p"))
            this.wdh.registerPropertyOnCanonicalForm(LexinfoOnt.number, LexinfoOnt.plural);
    }

    private void extractGender(final String gender) {
        RDFNode gen;

        if (gender.contains("mf")) {
            this.wdh.registerPropertyOnCanonicalForm(LexinfoOnt.gender, LexinfoOnt.neuter);
            log.trace("{} => Gender found -> {} ---> {}", getWiktionaryPageName(), gender, url());
        } else {

            for (int i = 0; i < gender.length(); i++) {
                gen = null;

                if (gender.charAt(i) == 'm') gen = LexinfoOnt.masculine;
                else if (gender.charAt(i) == 'f') gen = LexinfoOnt.feminine;

                if (gen != null) {
                    this.wdh.registerPropertyOnCanonicalForm(LexinfoOnt.gender, gen);
                    log.trace("{} => Gender found -> {} ---> {}", getWiktionaryPageName(), gender, url());
                }
            }

        }
    }

    public void extractNymWithPage(final PageIterator field) {
        final String relation = field.get().asTemplate().getName().substring(1, field.get().asTemplate().getName().length() - 1);
        extractNym(relation, field);
    }

    public void extractNymWithSection(final WikiText.WikiSection section) {
        final String relation = section.getHeading().getContent().getText().trim();
        extractNym(relation, PageIterator.of(section.getContent().tokens(), ignoredTemplate));
    }

    public void extractNym(final String relation, final PageIterator field) {

        while (field.hasNext() && !field.isNextASection() && !field.isNextASection()) {

            String text = field.next().getText();
            if (text.startsWith("*"))
                text = text.substring(1);

            PageIterator line = PageIterator.of(text.trim(), ignoredTemplate);

            while (line.hasNext() && !line.shadowNext().getText().contains("(")) {

                if (line.next() instanceof WikiText.Link) {
                    this.catwdh.registerNymRelation(line.get().asLink().getLinkText(), relation);
                    log.trace("{} => {} detected -> {} ---> {}", getWiktionaryPageName(), relation, line.get().asLink().getLinkText(), url());
                } else if (!line.get().getText().trim().equals(",") && !line.get().getText().trim().equals(".")) // TODO check for use gloss
                    log.trace("{} => Unhandled text in {} field -> {} ---> {}", getWiktionaryPageName(), relation, line.get().getText(), url());

            }

            final StringBuilder skipped = new StringBuilder();
            line.forEachRemaining(token -> skipped.append(token.getText()));

            if (!skipped.toString().isBlank())
                log.trace("{} => Text in {} skipped -> {} ---> {}", getWiktionaryPageName(), relation, skipped, url());
        }
    }


    public void extractDefinitionField(final PageIterator field) {
        extractDefinitionLine(field.get());
        while (field.hasNext() && field.shadowNext().getText().startsWith("#"))
            extractDefinitionLine(field.next());
    }

    public void extractDefinitionLine(final WikiText.Token line) {
        if (line.getText().startsWith("# ")) { // TODO use render
            log.trace("{} => Definition found -> {} ---> {}", getWiktionaryPageName(), line.getText(), url());
            this.wdh.registerNewDefinition(line.getText().substring(2));
        } else if (line.getText().startsWith("#: ")) {
            log.trace("{} => Example found -> {} ---> {}", getWiktionaryPageName(), line.getText().substring(3), url());
            this.wdh.registerExample(line.getText().substring(3), null);
        } else if (line.getText().startsWith("##")) // TODO handle complex definitions
            log.trace("{} => Complex definition found, not currently handled. ---> {}", getWiktionaryPageName(), url());
        else if (!line.getText().substring(1).isBlank())// TODO accept : "#direct def text snap to the #"
            log.warn("{} => Definition component non handled -> {} ---> {}", getWiktionaryPageName(), line.getText(), url());
    }

    public void extractPrononciation(final WikiText.Template template) {
        String prononciationText = render(template.getText());
        int start = -1;
        for (int i = 0; i < prononciationText.length(); i++) {
            if (prononciationText.charAt(i) == '/') {
                if (start == -1)
                    start = i;
                else {
                    log.trace("{} => Prononciation found {} ---> {}", getWiktionaryPageName(), prononciationText.substring(start, i + 1), url());
                    this.wdh.registerPronunciation(prononciationText.substring(start, i + 1), this.wdh.getCurrentEntryLanguage());
                    start = -1;
                }
            }
        }
    }

    public void skipUntilNextSection(final PageIterator sec) {
        while (sec.hasNext() && !sec.isNextASection())
            sec.next();
    }

    public String parseLanguage(final WikiText.Heading heading) {
        Matcher match = extractLanguage.matcher(heading.getText());
        return match.find() ? LangTools.normalize(match.group(2)) : null;
    }


    private void showCount() {
        if (getWiktionaryPageName().equals("lepiej"))
            log.warn("Count => {}", count);
    }

    protected String render(final String content) {
        String rended;
        try {
            rended = this.templateRender.render(new PlainTextConverter(), content).trim();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return rended;
    }

    protected String url() {
        return URL + getWiktionaryPageName() + " -> page_number " + count;
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

    public static void addIgnoredSec(final String ignored, final int level) {
        String equals = "";
        for (int i = 0; i < level; i++)
            equals += "=";
        ignoredSection.add(equals + " " + ignored + " " + equals);
    }

}

package org.getalp.dbnary.languages.gle;

import info.bliki.wiki.filter.PlainTextConverter;
import org.getalp.LangTools;
import org.getalp.dbnary.ExtractionFeature;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.bliki.ExpandAllWikiModel;
import org.getalp.dbnary.languages.AbstractWiktionaryExtractor;
import org.getalp.dbnary.wiki.WikiText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * TODO export data from the Builder to the DataHandler. May be remove the LexicalBuilder.
 */
public class WiktionaryExtractor extends AbstractWiktionaryExtractor {

    private final Logger log = LoggerFactory.getLogger(WiktionaryExtractor.class);
    private static final String URL = "https://ga.wiktionary.org/wiki/";

    private static int count = 0;
    private static final int TOTAL = 3076;

    /* STATIC DATA, OWN THE WIKI STRUCTURE */
    private static final Set<String> ignoredTemplate = new HashSet<>();
    private static final Set<String> ignoredStringContent = new HashSet<>();
    private static final Set<String> fieldsName = new HashSet<>();
    private static final Set<String> fieldsNameExtracted = new HashSet<>();
    private static final Set<String> ignoredLanguage = new HashSet<>();

    static {
        // MAKE THEM INVISIBLE
        addIgnoredT("pn");
        addIgnoredT("ucf");
        addIgnoredT("vicipéid");
        addIgnoredT("Vicipéid");
        addIgnoredT("-");
        addIgnoredT("--");
        addIgnoredT("Tíortha na hEorpa-ga");
        addIgnoredT("-fréamh-");
        addIgnoredT("mal");
        addIgnoredT("audio");
        addIgnoredT("tábla peiriadach-gle");
        addIgnoredT("rós an chompáis-nld");
        addIgnoredT("rós an chompáis-kur");
        addIgnoredT("CFh");
        addIgnoredT("Mumh");
        addIgnoredT("#if:");
        addIgnoredT("CFh");
        addIgnoredT("DnG");
        addIgnoredT("míonna");
        addIgnoredT("laethanta");
        addIgnoredT("plainéid");
        addIgnoredT("sampla datha");
        addIgnoredT("Acaill");
        addIgnoredT("Síol");
        addIgnoredT("féach");
        addIgnoredT("aigéan");
        addIgnoredT("loing");
        addIgnoredT("tír");
        addIgnoredT("Gaillimh");
        addIgnoredT("Conamara");
        addIgnoredT("uimhir");
        addIgnoredT("frith");
        addIgnoredT("rfh");
        addIgnoredT(".mal");

        // USELESS, JUST FOR REMOVE SOME USELESS TRACE
        addIgnoredT("Tíortha na hEorpa-es");
        addIgnoredT("Tíortha na hEorpa-en");
        addIgnoredT("Tíortha na hEorpa-ca");
        addIgnoredT("Tíortha na hEorpa-fr");
        addIgnoredT("Tíortha na hEorpa-de");
        addIgnoredT("uimhir (es)");
        addIgnoredT("uimhir (el)");
        addIgnoredT("-nlnoun-");
        addIgnoredT("de-Substantiv-Tabelle\n");
        addIgnoredT("míonna-gd");
        addIgnoredT("míonna-gv");
        addIgnoredT("tábla peiriadach-ell");
        addIgnoredT("uimhir (de)");
        addIgnoredT("uimhir (fr)");
        addIgnoredT("rós an chompáis-spa");
        addIgnoredT("tábla peiriadach-ell");
        addIgnoredT("Stáit na Gearmáine-de");
        addIgnoredT("uimhir (br)");
        addIgnoredT("Uimhir (ru)");
        addIgnoredT("Uimhir (hi)");
        addIgnoredT("Uimhir (it)");
        addIgnoredT("uimhir (pt)");
        addIgnoredT("Uimhir (is)");
        addIgnoredT("uimhir (tr)");
        addIgnoredT("Uimhir (oc)");
        addIgnoredT("uimhir (ca)");
        addIgnoredT("uimhir (oc)");
        addIgnoredT("uimhir (it)");
        addIgnoredT("uimhir (nl)");
        addIgnoredT("sampla datha\n");
        addIgnoredT("uimhir (is)");
        addIgnoredT("uimhir (ru)");
        addIgnoredT("Uimhir (fi)");
        addIgnoredT("uimhir (hi)");
        addIgnoredT("en-noun");
        addIgnoredT("hira");
        addIgnoredT("roma");
        addIgnoredT("míonna-fr");
        addIgnoredT("míonna-es");
        addIgnoredT("míonna-it");
        addIgnoredT("míonna-en");
        addIgnoredT("laethanta-de");
        addIgnoredT("míonna-ca");
        addIgnoredT("míonna-oc");
        addIgnoredT("míonna-cy");
        addIgnoredT("míonna-br");
        addIgnoredT("míonna-de");
        addIgnoredT("míonna-la");
        addIgnoredT("míonna-nl");
        addIgnoredT("Teimpléad:laethanta-de");
        addIgnoredT("p");


        // USELESS JUST TO DON'T MAKE USELESS TRACE
        addIgnoredS("*IPA");
        addIgnoredS(".");
        addIgnoredS("");
        addIgnoredS("Ón");

        // DON'T FLAG AS LANGUAGE.
        addIgnoredL("num");
        addIgnoredL("int");
        addIgnoredL("aid");
        addIgnoredL("sym");
        addIgnoredL("rfh");
        addIgnoredL("for");
        addIgnoredL("adj");
        addIgnoredL("art");
        addIgnoredL("mgm");
        addIgnoredL("mal");


        // SECTIONS NAMES WHICH ARE DELIMITING FIELDS
        addFieldT("-etym-");
        addFieldT("-phon-");
        addFieldT("-pron-");
        addFieldT("-tag-");
        addFieldT("-féach-");
        addFieldT("-mír-");
        addFieldT("-nótaí-");
        addFieldT("-sanas-");
        addFieldT("-cón-");

        // SECTIONS NAMES WHICH ARE DELIMITING FIELDS THAT WE EXTRACT
        addFieldExtractedT("-int-");
        addFieldExtractedT("-adj-");
        addFieldExtractedT("-aid-");
        addFieldExtractedT("-nounf-");
        addFieldExtractedT("-fainm-");
        addFieldExtractedT("-propn-");
        addFieldExtractedT("-ainmd-");
        addFieldExtractedT("-pronoun-");
        addFieldExtractedT("-for-");
        addFieldExtractedT("-rfh-");
        addFieldExtractedT("-prep-");
        addFieldExtractedT("-noun-");
        addFieldExtractedT("-ainm-");
        addFieldExtractedT("-verb-");
        addFieldExtractedT("-briath-");
        addFieldExtractedT("-fbriath-");
        addFieldExtractedT("-verbf-");
        addFieldExtractedT("-dobh-");
        addFieldExtractedT("-adv-");
        addFieldExtractedT("-réim-");
        addFieldExtractedT("-pfx-");
        addFieldExtractedT("-forc-");
        addFieldExtractedT("-intpr-");
        addFieldExtractedT("-giorr-");
        addFieldExtractedT("-abbr-");
        addFieldExtractedT("-contr-");
        addFieldExtractedT("-uimh-");
        addFieldExtractedT("-num-");
        addFieldExtractedT("-inis-");
        addFieldExtractedT("-sym-");
        addFieldExtractedT("-alt-");
        addFieldExtractedT("-art-");
        addFieldExtractedT("-aidsheal-");
        addFieldExtractedT("-possadj-");
        addFieldExtractedT("-seanfh-");
        addFieldExtractedT("-conj-");
        addFieldExtractedT("-cón-");
        addFieldExtractedT("-mgm-");
        addFieldExtractedT("-faid-");
        addFieldExtractedT("-adjf-");
        addFieldExtractedT("-mír-");
    }

    protected String currentLanguage = null;
    protected ExpandAllWikiModel templateRender;

    public WiktionaryExtractor(IWiktionaryDataHandler wdh) {
        super(wdh);
    }

    @Override
    public void setWiktionaryIndex(WiktionaryPageSource wi) {
        super.setWiktionaryIndex(wi);
        this.templateRender = new ExpandAllWikiModel(this.wi, new Locale("ga"), "/${image}", "/${title}");
    }

    @Override
    protected void setWiktionaryPageName(String wiktionaryPageName) {
        super.setWiktionaryPageName(wiktionaryPageName);
        this.templateRender.setPageName(getWiktionaryPageName());
    }

    @Override
    public void extractData() {
        if (getWiktionaryPageName().equals("Príomhleathanach")) // Don't extract the home page.
            return;

        if (true && !getWiktionaryPageName().equals("beo")) // Test only on a word
            return;

        this.wdh.initializePageExtraction(getWiktionaryPageName());

        PageIterator page = PageIterator.of(pageContent, ignoredTemplate);
        pageAnalyzer(page, true);

        showCounter();
        this.wdh.finalizePageExtraction();
    }

    /* EXTRACTION CORE */

    public void pageAnalyzer(final PageIterator page) {
        this.pageAnalyzer(page, false);
    }

    public void pageAnalyzer(final PageIterator page, final boolean finalizer) {

        String nextLanguage;

        while (page.hasNext()) {
            if (page.next() instanceof WikiText.Template && (nextLanguage = parseLanguageTemplate(page.get().asTemplate())) != null) // If a language template is found.
                switchCurrentLanguage(nextLanguage);
            else if (this.currentLanguage != null) // If current token isn't a language template and a language is initialized.
                templateDispatcher(page.get(), page);
        }
        if (finalizer)
            switchCurrentLanguage(null); // close the current languages.

    }

    private void switchCurrentLanguage(String nextLanguage) {
        if (this.currentLanguage != null) { // Finalize the currentLanguage before switching it.
            log.trace("{} => Finalizing {} language.", getWiktionaryPageName(), this.currentLanguage);
            this.wdh.finalizeLanguageSection();
        }
        this.currentLanguage = nextLanguage; // Switch current language.

        if (this.currentLanguage == null)
            return;

        if (null == wdh.getExolexFeatureBox(ExtractionFeature.MAIN) && !this.currentLanguage.equals("gle"))
            this.currentLanguage = null;
        else {
            log.trace("{} => Language swap {} was found on the page ---> {}.", getWiktionaryPageName(), this.currentLanguage, url());
            this.wdh.initializeLanguageSection(this.currentLanguage); // Initialize the new currentLanguage.
        }
    }


    public void templateDispatcher(final WikiText.Token token, final PageIterator page) { // TODO analyse all things.
        if (!(token instanceof WikiText.Template)) {

            if (!ignoredStringContent.contains(token.getText().trim())
                && !token.getText().startsWith("[[Íomhá:")
                && !token.getText().contains("*{{t:")
                && !token.getText().startsWith("[[Image:")) {

                log.warn("{} => Unhandled text found -> \"{}\" ---> {}", getWiktionaryPageName(), token.getText().trim(), url());
            }

        } else {
            WikiText.Template template = token.asTemplate();

            if (template.getName().contains("t:"))
                return;

            if (fieldsNameExtracted.contains(template.getName())) {
                log.trace("{} => {} found ! ---> {}", getWiktionaryPageName(), template.getName(), url());
                extractLexicalField(page);
                return;
            }

            if (template.getName().startsWith("ainm") && page.hasNext()) { // Try to detect noun with out section like a default section when no one is found.

                log.trace("{} => Noun detected with out field ! ---> {}", getWiktionaryPageName(), url());

                List<WikiText.Token> newTokens = new ArrayList<>(page.remaining());
                newTokens.add(0, page.get());

                PageIterator cloneIt = PageIterator.of(newTokens);
                cloneIt.skip(1);

                extractLexicalField(cloneIt);
                page.skip(cloneIt.getCursor() - 1);

                return;
            }

            switch (template.getName()) {
                case "-sanas-":
                case "-etym-":
                    extractEtymology(page);
                    break;
                case "-aistr-": // translation outside a lexical field. Extract any way...
                    extractTranslation(page);
                    break;
                case "-nótaí-": // Shitty data.
                case "-féach-":
                    extractNothing(page);
                    break;
                case "-fuaim-": // Prononciation are extracted too in the lexical part. Depend on how is builded the page.
                case "-phon-":
                case "-pron-":
                    extractPrononciation(page);
                case "aistr": // Translation without head :/ Ignore.
                case ")":
                    // Ignore
                    break;
                default:
                    log.warn("{} => {} template was found in {} language. ---> {}.", getWiktionaryPageName(), page.get().asTemplate().getName(), this.currentLanguage, url());
                    break;
            }
        }
    }

    /* EXTRACTORS */

    private void extractLexicalField(final PageIterator page) { // FIELD EXTRACTOR MAIN. // TODO may be remove Builder and direct extract to the dataHandler.
        LexicalBuilder builder = new LexicalBuilder(page.get() != page.shadowNext() ? page.get().asTemplate().getName() : "-ainm-");

        this.wdh.initializeLexicalEntry(builder.field);

        while (insideField(page)) {
            if (page.next() instanceof WikiText.Template) {
                WikiText.Template template = page.get().asTemplate();
                switch (template.getName()) {
                    case "m":
                    case "fir":
                        if (builder.gender == null)
                            builder.gender = "f";
                        break;
                    case "bain":
                        if (builder.gender == null)
                            builder.gender = "b";
                        break;
                    case "n":
                        if (builder.gender == null)
                            builder.gender = "n";
                        break;
                    case "ainm nr":
                        if (builder.gender == null)
                            builder.gender = toText(template.getArg("1"));
                        break;
                    case "ainm 1":
                        builder.genitiveSingular = toText(template.getArg("1"));
                        builder.plural = toText(template.getArg("2"));
                        builder.singular = toText(template.getArg("au"));
                        break;
                    case "ainm 2":
                        builder.genitiveSingular = toText(template.getArg("1"));
                        builder.plural = toText(template.getArg("2"));
                        builder.gender = toText(template.getArg("i"));
                        if (builder.gender == null) builder.gender = "b";
                        builder.dativeSingular = toText(template.getArg("t"));
                        builder.singular = toText(template.getArg("au"));
                        break;
                    case "ainm 3":
                        builder.gender = toText(template.getArg("1"));
                        builder.genitiveSingular = toText(template.getArg("2"));
                        builder.plural = toText(template.getArg("3"));
                        builder.genitivePlural = toText(template.getArg("4"));
                        builder.singular = toText(template.getArg("au"));
                        break;
                    case "ainm 4":
                        builder.gender = toText(template.getArg("1"));
                        builder.plural = toText(template.getArg("2"));
                        builder.singular = toText(template.getArg("au"));
                        builder.genitiveSingular = toText(template.getArg("gu"));
                        break;
                    case "ainm 5":
                        builder.gender = toText(template.getArg("1"));
                        builder.genitiveSingular = toText(template.getArg("2"));
                        builder.plural = toText(template.getArg("3"));
                        builder.genitivePlural = toText(template.getArg("4")); // TODO check for little change on this
                        builder.singular = toText(template.getArg("au"));
                        break;
                    case "briath":
                    case "verb":
                        builder.verbType = toText(template.getArg("1"));
                        builder.conjugaison = toText(template.getArg("2"));
                        builder.nounVerbal = toText(template.getArg("3"));
                        builder.ajdVerbal = toText(template.getArg("4"));
                        break;
                    case "comh":
                        extractSynonymes(template, builder);
                        break;
                    case "aid.": // TODO extract potential external data -> https://ga.wiktionary.org/wiki/Teimpl%C3%A9ad:aid.
                        break;
                    case "-trans-":
                    case "-aistr-":
                        extractTranslation(page);
                        break;
                    case "-fuaim-":
                    case "-phon-":
                    case "-pron-":
                        extractPrononciation(page);
                        break;
                    case "(":
                    case ")":
                    case "iol.":
                    case "gu.":
                    case "guai.":
                    case "gi.":
                    case "ai.":
                    case "gugi.":
                    case "aistr":
                    case "Ciarraí":
                        break;
                    default:
                        log.trace("{} => Lexical content \"{}\" template non handled -> {} ---> {}", getWiktionaryPageName(), builder.field, page.get().getText(), url());
                        break;
                }

            } else {

                if (page.get().getText().startsWith("#*")) {

                    if (builder.definitions.size() != 0)
                        builder.definitions.get(builder.definitions.size() - 1).append(" ").append(page.get().getText().substring(2));
                    else
                        builder.addDefinition(page.get().getText().substring(2));

                } else if (page.get().getText().startsWith("#")) {
                    builder.addDefinition(page.get().getText().substring(1));
                    log.trace("{} => Definition found -> \"{}\" ---> {}", getWiktionaryPageName(), page.get().getText().substring(1), url());
                } else
                    log.trace("{} => Noun content text non handled     -> {} ---> {}", getWiktionaryPageName(), page.get().getText().trim(), url());

            }
        }

        builder.definitions.replaceAll(stringBuilder -> new StringBuilder(render(stringBuilder.toString())));

        log.trace("{} => {} \n                                ---> {}", getWiktionaryPageName(), builder.toString(), url());

    }

    private static class LexicalBuilder {
        public String field;
        public String gender = null;
        public String genitiveSingular = null;
        public String genitivePlural = null;
        public String plural = null;
        public String dativeSingular = null;
        public String singular = null;
        public String verbType = null;
        public String conjugaison = null;
        public String nounVerbal = null;
        public String ajdVerbal = null;
        public List<String> synonymes;
        public List<StringBuilder> definitions;

        public LexicalBuilder(final String field) {
            this.field = field;
            this.definitions = new ArrayList<>();
            this.synonymes = new ArrayList<>();
        }

        public void addDefinition(final String def) {
            this.definitions.add(new StringBuilder(def));
        }

        @Override
        public String toString() {
            return "LexicalBuilder{" +
                   "field='" + field + '\'' +
                   ", gender='" + gender + '\'' +
                   ", genitiveSingular='" + genitiveSingular + '\'' +
                   ", genitivePlural='" + genitivePlural + '\'' +
                   ", plural='" + plural + '\'' +
                   ", dativeSingular='" + dativeSingular + '\'' +
                   ", singular='" + singular + '\'' +
                   ", verbType='" + verbType + '\'' +
                   ", conjugaison='" + conjugaison + '\'' +
                   ", nounVerbal='" + nounVerbal + '\'' +
                   ", ajdVerbal='" + ajdVerbal + '\'' +
                   ", synonymes=" + synonymes +
                   ", definitions=" + definitions +
                   '}';
        }
    }

    private void extractSynonymes(final WikiText.Template temp, final LexicalBuilder builder) {
        log.trace("{} => Synonymes extractor -> {}  ---> {}", getWiktionaryPageName(), temp.getArgs(), url());
        for (int i = 1; i < temp.getArgs().size() + 1; i++)
            if (temp.getArgs().get(String.valueOf(i)) == null)
                break;
            else {
                builder.synonymes.add(temp.getArg(String.valueOf(i)).getText());
                log.trace("{} => {} <=> {} ---> {}", getWiktionaryPageName(), getWiktionaryPageName(), temp.getArg(i + "").getText(), url());
            }
    }

    private static class TransBuilder {
        public String lang = null;
        public String word = null;
        public String usage = "";
    }

    private void extractTranslation(final PageIterator page) {
        // Collect the list in the translation field.
        List<WikiText.Token> internToken = new ArrayList<>();
        internToken.add(page.get());
        while (insideField(page) && page.hasNext() && (!(page.next() instanceof WikiText.Template) || !page.get().asTemplate().getName().equals(")")))
            internToken.add(page.get());

        if (!(page.get() instanceof WikiText.Template) || !page.get().asTemplate().getName().equals(")")) {
            log.trace("{} => translation field didn't closed !  ---> {}", getWiktionaryPageName(), url());
        }

        if (page.hasNextTemplate() && page.shadowNextTemplate().getName().equals("(")) // Check if there is another translation table after.
            extractTranslation(page);

        PageIterator transIt = PageIterator.of(internToken, ignoredTemplate);

        if (!transIt.hasNextTemplate() || transIt.goToNextTemplate("(") == null) { // If the translation field don't open
            log.trace("{} => translation field didn't open. ---> {}", getWiktionaryPageName(), url());
            return;
        }

        String gloss;
        if (transIt.get().asTemplate().getArgs().size() > 0) { // glossary found
            gloss = transIt.get().asTemplate().getArg("1").getText();
            log.trace("{} => Glossary found \"{}\" ---> {}", getWiktionaryPageName(), gloss, url());
        }

        TransBuilder builder = new TransBuilder();
        while (transIt.hasNext()) {

            if (transIt.next() instanceof WikiText.Template && (transIt.get().asTemplate().getName().equals("aistr") || transIt.get().asTemplate().getName().equals("aistr2") || transIt.get().asTemplate().getName().equals("trad"))) {

                if (builder.word != null && builder.lang != null) {
                    this.wdh.registerTranslation(builder.lang, null /* TODO gloss*/, builder.usage, builder.word);
                    log.trace("{} => translation -> {}  --{}-> {}, {}.  ---> {}."
                            , getWiktionaryPageName(), getWiktionaryPageName(), builder.lang, builder.word, builder.usage, url());
                }

                builder = new TransBuilder();
                builder.lang = LangTools.normalize(transIt.get().asTemplate().getArg("1").getText());
                builder.word = transIt.get().asTemplate().getArg("2").getText();

            } else if (transIt.get() instanceof WikiText.Template) // adding template name to usage. Like {{fir}}, {{bain}}, {{n}}.
            {
                builder.usage += transIt.get().asTemplate().getName() + ".";
                if (!transIt.get().asTemplate().getName().equals("fir") && !transIt.get().asTemplate().getName().equals("bain") && !transIt.get().asTemplate().getName().equals("n")) {
                    log.debug("{} => Strange traduction field ID -> {} ---> {} ", getWiktionaryPageName(), transIt.get().asTemplate().getName(), url());
                }
            } else // Adding classic text between 2 traduction as usage.
                builder.usage += " " + transIt.get().getText().trim();

        }

        if (builder.word != null && builder.lang != null) { // Save the last trans.
            this.wdh.registerTranslation(builder.lang, null /* TODO gloss*/, builder.usage, builder.word);
            log.trace("{} => translation -> {}  --{}-> {}.  ---> {}."
                    , getWiktionaryPageName(), getWiktionaryPageName(), builder.lang, builder.word, url());
        }
    }

    private void extractEtymology(final PageIterator page) {
        StringBuilder etym = new StringBuilder();
        while (insideField(page))
            etym.append(" ").append(page.next()).append(" ");
        String etymText = render(etym.toString().replaceAll("\\s+", " ").replaceAll(" \\.", ".").replaceAll(" ,", "."));
        log.trace("{} => Etymology -> \"{}\" ---> {}", getWiktionaryPageName(), etymText, url());
    }

    private void extractPrononciation(final PageIterator page) {
        while (insideField(page) && page.shadowNext().getText().contains("IPA")) {

            PageIterator newIt = PageIterator.of(page.next().getText().substring(1));

            if (newIt.hasNextTemplate() && newIt.nextTemplate().getName().equals("IPA")) {

                WikiText.Template template = newIt.get().asTemplate();
                if (template.getArg("1").getText().equals("//"))
                    continue;
                if (template.getArg("1").getText().isBlank())
                    continue;

                this.wdh.registerPronunciation(template.getArg("1").getText(), "ga-pron");
                log.trace("{} => Prononciation \"{}\" found. --> {}.", getWiktionaryPageName(), template.getArg("1").getText(), url());
            }

        }
    }

    public void extractNothing(final PageIterator page) {
        while (insideField(page))
            page.next();
    }

    /* UTILS */

    private boolean insideField(final PageIterator page) {
        return page.hasNext() && (!(page.shadowNext() instanceof WikiText.Template) || (!fieldsName.contains(page.shadowNext().asTemplate().getName()) && parseLanguageTemplate(page.shadowNext().asTemplate()) == null));
    }

    public static String toText(final WikiText.WikiContent content) {
        return content == null ? null : content.getText();
    }

    public String parseLanguageTemplate(final WikiText.Template template) {
        String lang = null;
        if (template.getName().equals("t"))
            lang = template.getArgs().get("1").getText();
        if (template.getName().matches("-[a-z][a-z]-") || template.getName().matches("-[a-z][a-z][a-z]-"))
            lang = template.getName().substring(1, template.getName().length() - 1);

        if (ignoredLanguage.contains(lang)) return null;

        if (lang != null) lang = validateAndStandardizeLanguageCode(lang);

        return lang;
    }

    public String render(final String content) {
        String rended;
        try {
            rended = this.templateRender.render(new PlainTextConverter(), content).trim();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return rended;
    }

    public void showCounter() {
        if (getWiktionaryPageName().equals("chance"))
            log.warn("Count : {}", count);
    }

    public String url() {
        return URL + getWiktionaryPageName();
    }

    /* PageIterator Object used to iterate the Page, using WikiText.*/

    public static class PageIterator implements Iterator<WikiText.Token> {

        protected final List<WikiText.Token> page;
        protected final Set<String> skippedTemplate;
        protected int cursor;
        protected int size;

        public PageIterator(final List<WikiText.Token> page) {
            this(page, Set.of());
        }

        public PageIterator(final List<WikiText.Token> page, final Set<String> skippedTemplate) {
            this.page = page;
            this.skippedTemplate = skippedTemplate;
            this.cursor = 0;
            this.size = this.page.size();
        }

        public int getCursor() {
            return this.cursor;
        }

        /**
         * @return the last value of the next() or nextTemplate()
         */
        public WikiText.Token get() {
            return this.page.get(this.cursor - 1);
        }

        protected WikiText.Token getAndIncrement() {
            return this.page.get(this.cursor++);
        }

        protected WikiText.Token pageOffsetGet(final int cursor) {
            return this.page.get(this.cursor + cursor);
        }

        protected WikiText.Token pageGetNext() {
            return this.page.get(this.cursor);
        }

        @Override
        public WikiText.Token next() {
            while (pageGetNext().getText().trim().isBlank() || (pageGetNext() instanceof WikiText.Template && this.skippedTemplate.contains(pageGetNext().asTemplate().getName())))
                this.cursor++;
            return getAndIncrement();
        }


        public WikiText.Template nextTemplate() {
            while (!(next() instanceof WikiText.Template)) ;
            return get().asTemplate();
        }

        public WikiText.Token shadowNext() {
            int shadowCursor = 0;
            while (pageOffsetGet(shadowCursor).getText().trim().isBlank() || (pageOffsetGet(shadowCursor) instanceof WikiText.Template && this.skippedTemplate.contains(pageOffsetGet(shadowCursor).asTemplate().getName())))
                shadowCursor++;
            return pageOffsetGet(shadowCursor);
        }

        public WikiText.Template shadowNextTemplate() {
            int shadowCursor = 0;
            while (!(pageOffsetGet(shadowCursor) instanceof WikiText.Template) || this.skippedTemplate.contains(pageOffsetGet(shadowCursor).asTemplate().getName()))
                shadowCursor++;
            return pageOffsetGet(shadowCursor).asTemplate();
        }

        public void skip(final int length) {
            this.cursor += length;
        }

        @Override
        public boolean hasNext() {
            int shadowCursor = 0;
            while (this.cursor + shadowCursor < this.size && (pageOffsetGet(shadowCursor).getText().trim().isBlank() || (pageOffsetGet(shadowCursor) instanceof WikiText.Template && this.skippedTemplate.contains(pageOffsetGet(shadowCursor).asTemplate().getName()))))
                shadowCursor++;
            return this.cursor + shadowCursor < this.size;
        }

        public boolean hasNextTemplate() {
            int shadowCursor = 0;
            while (this.cursor + shadowCursor < this.size && (!(pageOffsetGet(shadowCursor) instanceof WikiText.Template) || this.skippedTemplate.contains(pageOffsetGet(shadowCursor).asTemplate().getName())))
                shadowCursor++;
            return this.cursor + shadowCursor < this.size;
        }

        public int findNextTemplate(final String name) {
            int shadowCursor = 0;
            while (this.cursor + shadowCursor < this.page.size() && pageOffsetGet(shadowCursor) instanceof WikiText.Template && !pageOffsetGet(shadowCursor).asTemplate().getName().equals(name))
                shadowCursor++;
            return this.cursor + shadowCursor < this.size ? shadowCursor : -1;
        }

        public WikiText.Template goToNextTemplate(final String name) {
            WikiText.Template template = null;
            while (this.hasNextTemplate() && !(template = this.nextTemplate()).getName().equals(name)) ;
            return template.getName().equals(name) ? template : null;
        }

        public List<WikiText.Token> remaining() {
            return this.page.subList(this.cursor, this.size);
        }

        public static PageIterator of(final String content) {
            return of(content, Set.of());
        }

        public static PageIterator of(final String content, final Set<String> skippedTemplate) {
            WikiText page = new WikiText("PageIterator parser.", content);
            WikiText.WikiDocument doc = page.asStructuredDocument();

            return new PageIterator(doc.getContent().tokens(), skippedTemplate);
        }

        public static PageIterator of(final List<WikiText.Token> tokens) {
            return of(tokens, Set.of());
        }

        public static PageIterator of(final List<WikiText.Token> tokens, final Set<String> skippedTemplate) {
            return new PageIterator(tokens, skippedTemplate);
        }

        @Override
        public String toString() {
            PageIterator newIt = this.cloneIt();
            StringBuilder builder = new StringBuilder("{\n");
            while (newIt.hasNext())
                builder.append(newIt.getCursor() == cursor ? " -> " : "    ").append(newIt.cursor).append(": ").append(newIt.next()).append("\n");
            builder.append("}");
            return builder.toString();
        }

        protected PageIterator cloneIt() {
            return new PageIterator(page, skippedTemplate);
        }
    }

    /* STATIC METHOD TO FILL SETs*/

    public static void addIgnoredT(final String ignored) {
        ignoredTemplate.add(ignored);
    }

    public static void addIgnoredS(final String ignored) {
        ignoredStringContent.add(ignored);
    }

    public static void addFieldT(final String temp) {
        fieldsName.add(temp);
    }

    public static void addFieldExtractedT(final String name) {
        addFieldT(name);
        fieldsNameExtracted.add(name);
    }

    public static void addIgnoredL(final String lang) {
        ignoredLanguage.add(lang);
    }

}
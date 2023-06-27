package org.getalp.dbnary.languages.gle;

import info.bliki.wiki.filter.PlainTextConverter;
import org.apache.jena.rdf.model.Resource;
import org.getalp.LangTools;
import org.getalp.dbnary.ExtractionFeature;
import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.StructuredGloss;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.bliki.ExpandAllWikiModel;
import org.getalp.dbnary.languages.AbstractWiktionaryExtractor;
import org.getalp.dbnary.morphology.MorphoSyntacticFeature;
import org.getalp.dbnary.morphology.StrictInflexionScheme;
import org.getalp.dbnary.wiki.WikiText;
import org.getalp.model.lexinfo.Case;
import org.getalp.model.lexinfo.Gender;
import org.getalp.model.lexinfo.Number;
import org.getalp.model.ontolex.LexicalForm;
import org.getalp.model.ontolex.WrittenRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;

/**
 * @author Arnaud Alet
 */
public class WiktionaryExtractor extends AbstractWiktionaryExtractor {

    private static final String URL = "https://ga.wiktionary.org/wiki/";

    private static final Logger log = LoggerFactory.getLogger(WiktionaryExtractor.class);

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
        addFieldT("-fuaim-");

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

    private final WiktionaryDataHandler glewdh;

    protected String currentLanguage = null;
    protected ExpandAllWikiModel templateRender;

    public WiktionaryExtractor(IWiktionaryDataHandler wdh) {
        super(wdh);
        glewdh = (WiktionaryDataHandler) wdh;
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

        this.wdh.initializePageExtraction(getWiktionaryPageName());

        PageIterator page = PageIterator.of(pageContent, ignoredTemplate);
        pageAnalyzer(page);

        this.wdh.finalizePageExtraction();
    }

    /* EXTRACTION CORE */

    protected void pageAnalyzer(final PageIterator page) {

        String nextLanguage;

        while (page.hasNext()) {
            if (page.next() instanceof WikiText.Template && (nextLanguage = parseLanguageTemplate(page.get().asTemplate())) != null) // If a language template is found.
                switchCurrentLanguage(nextLanguage);
            else if (this.currentLanguage != null) // If current token isn't a language template and a language is initialized.
                templateDispatcher(page.get(), page);
        }

        switchCurrentLanguage(null); // close the current languages.

    }

    protected void switchCurrentLanguage(String nextLanguage) {
        if (this.currentLanguage != null) { // Finalize the currentLanguage before switching it.
            log.trace("{} => Finalizing {} language.", getWiktionaryPageName(), this.currentLanguage);
            this.wdh.finalizeLanguageSection();
        }
        this.currentLanguage = nextLanguage; // Switch current language.

        if (this.currentLanguage == null) return;

        if (null == wdh.getExolexFeatureBox(ExtractionFeature.MAIN) && !this.currentLanguage.equals("gle"))
            this.currentLanguage = null;
        else {
            log.trace("{} => Language swap {} was found on the page ---> {}.", getWiktionaryPageName(), this.currentLanguage, url());
            this.wdh.initializeLanguageSection(this.currentLanguage); // Initialize the new currentLanguage.
        }
    }


    protected void templateDispatcher(final WikiText.Token token, final PageIterator page) {
        if (!(token instanceof WikiText.Template)) {

            if (!ignoredStringContent.contains(token.getText().trim()) && !token.getText().startsWith("[[Íomhá:")
                && !token.getText().contains("*{{t:") && !token.getText().startsWith("[[Image:")) {

                log.trace("{} => Unhandled text found -> \"{}\" ---> {}", getWiktionaryPageName(), token.getText().trim(), url());
            }

        } else {
            WikiText.Template template = token.asTemplate();

            if (template.getName().contains("t:")) // remove useless trace
                return;

            if (fieldsNameExtracted.contains(template.getName())) { // Lexical field found, so extract it.
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
                case "-aistr-": // translation field outside a lexical field. Extract any way...
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
                case "aistr": // Translation without field :/. Ignore.
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

    protected void extractLexicalField(final PageIterator page) { // FIELD EXTRACTOR MAIN.
        LexicalBuilder builder = new LexicalBuilder(page.get() != page.shadowNext() ? page.get().asTemplate().getName() : "-ainm-" /* USED TO SET A DEFAULT FIELD NAME*/);

        this.wdh.initializeLexicalEntry(builder.field);

        while (insideField(page) || (page.hasNext() && page.shadowNext() instanceof WikiText.Template && page.shadowNext().asTemplate().getName().equals("-fuaim-"))) {
            if (page.next() instanceof WikiText.Template) {

                WikiText.Template template = page.get().asTemplate();
                switch (template.getName()) {
                    case "m":
                    case "fir":
                        if (builder.gender == null) builder.gender = "f";
                        break;
                    case "bain":
                        if (builder.gender == null) builder.gender = "b";
                        break;
                    case "n":
                        if (builder.gender == null) builder.gender = "n";
                        break;
                    case "ainm nr":
                        if (builder.gender == null) builder.gender = toText(template.getArg("1"));
                        builder.singular = toText(template.getArg("au"));
                        break;
                    case "ainm 1":
                        builder.genitiveSingular = toText(template.getArg("1"));
                        builder.plural = toText(template.getArg("2"));
                        if (builder.genitivePlural != null && builder.genitivePlural.equals("a"))
                            builder.genitivePlural = builder.genitiveSingular + "a";
                        else if (builder.genitivePlural != null && builder.genitivePlural.equals("gan"))
                            builder.genitivePlural = null;
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
                        if (builder.plural != null && builder.plural.equals("gan")) builder.plural = null;
                        builder.singular = toText(template.getArg("au"));
                        break;
                    case "ainm 4":
                        builder.gender = toText(template.getArg("1"));
                        builder.plural = toText(template.getArg("2"));
                        if (builder.plural != null && builder.plural.equals("gan")) builder.plural = null;
                        builder.singular = toText(template.getArg("au"));
                        builder.genitiveSingular = toText(template.getArg("gu"));
                        break;
                    case "ainm 5":
                        builder.gender = toText(template.getArg("1"));
                        builder.genitiveSingular = toText(template.getArg("2"));
                        builder.plural = toText(template.getArg("3"));
                        builder.genitivePlural = toText(template.getArg("4"));
                        if (builder.genitivePlural != null && builder.genitivePlural.equals("a"))
                            builder.genitivePlural = builder.genitiveSingular + "a";
                        else if (builder.genitivePlural != null && builder.genitivePlural.equals("gan"))
                            builder.genitivePlural = null;
                        builder.singular = toText(template.getArg("au"));
                        break;
                    case "ga-ainmfhocal":
                        builder.plural = toText(template.getArg("1"));
                        break;
                    case "briath":
                    case "verb":
                        builder.verbType = toText(template.getArg("1"));
                        builder.conjugaison = toText(template.getArg("2"));
                        builder.nounVerbal = toText(template.getArg("3"));
                        builder.adjVerbal = toText(template.getArg("4"));
                        break;
                    case "comh":
                        extractSynonymes(template, builder);
                        break;
                    case "aid.":
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
                    case "iol":
                        break;
                    default:
                        log.warn("{} => Lexical content \"{}\" template non handled -> {} ---> {}", getWiktionaryPageName(), builder.field, page.get().getText(), url());
                        break;
                }

            } else {

                if (page.get().getText().startsWith("#*")) {

                    if (builder.definitions.size() != 0)
                        builder.definitions.get(builder.definitions.size() - 1).append(" ").append(page.get().getText().substring(2));
                    else builder.addDefinition(page.get().getText().substring(2));

                } else if (page.get().getText().startsWith("#")) {

                    if (page.get().getText().contains("{{comh|"))
                        extractSynonymes(PageIterator.of(page.get().getText().substring(2)).goToNextTemplate("comh"), builder);

                    builder.addDefinition(page.get().getText().substring(1).split("\\{\\{comh")[0]);
                    log.trace("{} => Definition found -> \"{}\" ---> {}", getWiktionaryPageName(), page.get().getText().substring(1), url());
                } else
                    log.trace("{} => Noun content text non handled     -> {} ---> {}", getWiktionaryPageName(), page.get().getText().trim(), url());

            }
        }

        builder.definitions.replaceAll(stringBuilder -> new StringBuilder(render(stringBuilder.toString())));

        saveToDataHandler(builder);

        log.trace("{} => Data exported to DataHandler. ---> {}", getWiktionaryPageName(), url());

    }

    protected static class LexicalBuilder {
        public String field, gender = null,
                genitiveSingular = null,
                genitivePlural = null,
                plural = null,
                dativeSingular = null,
                singular = null,
                verbType = null,
                conjugaison = null,
                nounVerbal = null,
                adjVerbal = null;

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

    }

    protected void saveToDataHandler(final LexicalBuilder builder) {
        if (wdh.isDisabled(ExtractionFeature.MORPHOLOGY))
            return;

        saveCanonicalForm(builder);

        // TODO EXTRACT NOUN VERBAL AND ADJECTIV VERBAL FROM VERB TEMPLATE

        /*

        FormBuilder.of(builder.nounVerbal, this.glewdh)
                .addMorpho(LexinfoOnt.derivedForm)
                .save();

        FormBuilder.of(builder.adjVerbal, this.glewdh)
                .addMorpho(LexinfoOnt.derivedForm)
                .save();
         */

        FormBuilder.of(builder.singular, this.glewdh)
                .addGender(builder.gender)
                .addMorpho(Number.SINGULAR)
                .save();

        FormBuilder.of(builder.plural, this.glewdh)
                .addGender(builder.gender)
                .addMorpho(Number.PLURAL)
                .save();

        FormBuilder.of(builder.genitiveSingular, this.glewdh)
                .addGender(builder.gender)
                .addMorpho(Case.GENITIVE)
                .addMorpho(Number.SINGULAR)
                .save();

        FormBuilder.of(builder.genitivePlural, this.glewdh)
                .addGender(builder.gender)
                .addMorpho(Case.GENITIVE)
                .addMorpho(Number.PLURAL)
                .save();

        FormBuilder.of(builder.dativeSingular, this.glewdh)
                .addGender(builder.gender)
                .addMorpho(Case.DATIVE)
                .addMorpho(Number.SINGULAR)
                .save();

    }

    protected void saveCanonicalForm(final LexicalBuilder builder) {
        builder.definitions.forEach(def -> this.wdh.registerNewDefinition(def.toString()));

        if (builder.gender != null) {
            Resource gender;

            if (builder.gender.equals("f")) gender = LexinfoOnt.masculine;
            else if (builder.gender.equals("b")) gender = LexinfoOnt.feminine;
            else gender = LexinfoOnt.neuter;

            this.glewdh.registerPropertyOnCanonicalForm(LexinfoOnt.gender, gender);
        }

        if (builder.verbType != null) {
            Resource type;

            if (builder.verbType.equals("a") || builder.verbType.equals("t"))
                type = LexinfoOnt.TransitiveFrame;
            else if (builder.verbType.equals("n") || builder.verbType.equals("i"))
                type = LexinfoOnt.IntransitiveFrame;
            else { // "an" || "ti" -> transitive AND intransitive.
                type = LexinfoOnt.TransitiveFrame;
                this.glewdh.registerPropertyOnCanonicalForm(LexinfoOnt.verbFormMood, LexinfoOnt.IntransitiveFrame);
            }

            this.glewdh.registerPropertyOnCanonicalForm(LexinfoOnt.verbFormMood, type);
        }

        if (builder.synonymes != null)
            builder.synonymes.forEach(s -> this.glewdh.registerNymRelation(s, "syn"));

    }

    public static class FormBuilder {
        protected final WiktionaryDataHandler glewdh;
        protected LexicalForm form;
        protected StrictInflexionScheme sch;

        protected final String value;

        /**
         * if the value is null, this object do nothing.
         */
        private FormBuilder(@Nullable final String value, final WiktionaryDataHandler glewdh) {
            this.glewdh = glewdh;

            if (value != null) {
                this.form = new LexicalForm();
                this.sch = new StrictInflexionScheme();
            }

            this.value = value;
        }

        public FormBuilder addGender(@Nullable final String gender) {
            if (this.value == null) return this;

            if (gender != null) if (gender.equals("f")) this.sch.add(Gender.MASCULINE);
            else if (gender.equals("b")) this.sch.add(Gender.FEMININE);
            else this.sch.add(Gender.NEUTER);
            return this;
        }

        public FormBuilder addMorpho(final MorphoSyntacticFeature morpho) {
            if (this.value == null) return this;

            this.sch.add(morpho);
            return this;
        }

        public boolean save() {
            if (this.value == null) return false;

            this.form.setFeature(this.sch);
            this.form.addValue(new WrittenRepresentation(this.value, this.glewdh.getCurrentEntryLanguage()));

            this.glewdh.addLexicalForm(this.form);

            return true;
        }


        public static FormBuilder of(final String value, final WiktionaryDataHandler glewdh) {
            return new FormBuilder(value, glewdh);
        }
    }

    protected void extractSynonymes(final WikiText.Template temp, final LexicalBuilder builder) {
        log.trace("{} => Synonymes extractor -> {}  ---> {}", getWiktionaryPageName(), temp.getArgs(), url());
        for (int i = 1; i < temp.getArgs().size() + 1; i++)
            if (temp.getArgs().get(String.valueOf(i)) == null) break;
            else {
                builder.synonymes.add(temp.getArg(String.valueOf(i)).getText());
                log.trace("{} => {} <=> {} ---> {}", getWiktionaryPageName(), getWiktionaryPageName(), temp.getArg(String.valueOf(i)).getText(), url());
            }
    }

    private static class TransBuilder {
        public String lang = null;
        public String word = null;
        public String usage = "";
    }

    protected void extractTranslation(final PageIterator page) {
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

        StructuredGloss gloss = new StructuredGloss();
        if (transIt.get().asTemplate().getArgs().size() > 0) { // glossary found
            gloss.setGloss(transIt.get().asTemplate().getArg("1").getText());
            log.trace("{} => Glossary found \"{}\" ---> {}", getWiktionaryPageName(), gloss, url());
        }

        TransBuilder builder = new TransBuilder();
        while (transIt.hasNext()) {

            if (transIt.next() instanceof WikiText.Template && (transIt.get().asTemplate().getName().equals("aistr") || transIt.get().asTemplate().getName().equals("aistr2") || transIt.get().asTemplate().getName().equals("trad"))) {

                if (builder.word != null && builder.lang != null) {
                    this.wdh.registerTranslation(builder.lang, this.wdh.createGlossResource(gloss), builder.usage, builder.word);
                    log.trace("{} => translation -> {}  --{}-> {}, {}.  ---> {}.", getWiktionaryPageName(), getWiktionaryPageName(), builder.lang, builder.word, builder.usage, url());
                }

                builder = new TransBuilder();
                builder.lang = LangTools.normalize(transIt.get().asTemplate().getArg("1").getText());
                builder.word = transIt.get().asTemplate().getArg("2").getText();

            } else if (transIt.get() instanceof WikiText.Template) { // adding template name to usage. Like {{fir}}, {{bain}}, {{n}}.
                builder.usage += transIt.get().asTemplate().getName();

                if (!transIt.get().asTemplate().getName().equals("fir") && !transIt.get().asTemplate().getName().equals("bain") && !transIt.get().asTemplate().getName().equals("n"))
                    log.debug("{} => Strange translation usage field ID -> {} ---> {} ", getWiktionaryPageName(), transIt.get().asTemplate().getName(), url());

            } else if (!transIt.get().getText().trim().equals(",")) // Adding classic text between 2 traduction as usage.
                builder.usage += " " + transIt.get().getText().trim();

        }

        if (builder.word != null && builder.lang != null) { // Save the last trans.
            this.wdh.registerTranslation(builder.lang, this.wdh.createGlossResource(gloss), builder.usage, builder.word);
            log.trace("{} => translation -> {}  --{}-> {}, {}.  ---> {}.", getWiktionaryPageName(), getWiktionaryPageName(), builder.lang, builder.word, builder.usage, url());
        }

    }

    protected void extractEtymology(final PageIterator page) { // TODO ASK

        StringBuilder etym = new StringBuilder();
        while (insideField(page)) etym.append(" ").append(page.next()).append(" ");
        String etymText = render(etym.toString().replaceAll("\\s+", " ").replaceAll(" \\.", ".").replaceAll(" ,", "."));
        log.trace("{} => Etymology -> \"{}\" ---> {}", getWiktionaryPageName(), etymText, url());

        if (this.wdh.isDisabled(ExtractionFeature.ETYMOLOGY))
            return;

        // DO NOTHING WITH THIS.

    }

    protected void extractPrononciation(final PageIterator page) {
        while (insideField(page) && page.shadowNext().getText().contains("IPA")) {

            PageIterator newIt = PageIterator.of(page.next().getText().substring(1));

            if (newIt.hasNextTemplate() && newIt.nextTemplate().getName().equals("IPA")) {

                WikiText.Template template = newIt.get().asTemplate();
                if (template.getArg("1").getText().equals("//")) continue;
                if (template.getArg("1").getText().isBlank()) continue;

                this.wdh.registerPronunciation(template.getArg("1").getText(), this.wdh.getCurrentEntryLanguage() + "-fonipa");
                log.trace("{} => Prononciation \"{}\" found. --> {}.", getWiktionaryPageName(), template.getArg("1").getText(), url());
            }

        }
    }

    protected void extractNothing(final PageIterator page) {
        while (insideField(page)) page.next();
    }

    /* UTILS */

    protected boolean insideField(final PageIterator page) {
        return page.hasNext() && (!(page.shadowNext() instanceof WikiText.Template) || (!fieldsName.contains(page.shadowNext().asTemplate().getName()) && parseLanguageTemplate(page.shadowNext().asTemplate()) == null));
    }

    protected static String toText(final WikiText.WikiContent content) {
        return content == null ? null : content.getText();
    }

    protected String parseLanguageTemplate(final WikiText.Template template) {
        String lang = null;
        if (template.getName().equals("t")) lang = template.getArgs().get("1").getText();
        if (template.getName().matches("-[a-z][a-z]-") || template.getName().matches("-[a-z][a-z][a-z]-"))
            lang = template.getName().substring(1, template.getName().length() - 1);

        if (ignoredLanguage.contains(lang)) return null;

        if (lang != null) lang = validateAndStandardizeLanguageCode(lang);

        return lang;
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
            if (template == null) return null;
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
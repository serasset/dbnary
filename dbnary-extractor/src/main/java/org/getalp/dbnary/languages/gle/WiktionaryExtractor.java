package org.getalp.dbnary.languages.gle;

import org.getalp.LangTools;
import org.getalp.dbnary.ExtractionFeature;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.bliki.ExpandAllWikiModel;
import org.getalp.dbnary.languages.AbstractWiktionaryExtractor;
import org.getalp.dbnary.wiki.WikiText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class WiktionaryExtractor extends AbstractWiktionaryExtractor {

    private final Logger log = LoggerFactory.getLogger(WiktionaryExtractor.class);
    private static final String URL = "https://ga.wiktionary.org/wiki/";

    private static int count = 0;
    private static final int TOTAL = 3076;
    private static final int FINISH = 2015;

    private static final Set<String> ignoredTemplate = new HashSet<>();

    static {
        addIgnored("pn");
        addIgnored("ucf");
        addIgnored("vicipéid");
        addIgnored("-fuaim-");
        addIgnored("-");
        addIgnored("--");
    }

    protected String currentLanguage;
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
        if (false && !getWiktionaryPageName().equals("zeventien")) // Test only on a word
            return;

        this.wdh.initializePageExtraction(getWiktionaryPageName());

        PageIterator page = PageIterator.of(pageContent, ignoredTemplate);
        pageAnalyzer(page);

        showCounter();
        this.wdh.finalizePageExtraction();

    }

    public void pageAnalyzer(final PageIterator page) {

        String nextLanguage;

        while (page.hasNext()) {
            if (page.next() instanceof WikiText.Template && (nextLanguage = parseLanguageTemplate(page.get().asTemplate())) != null) // If a language template is found.
                switchCurrentLanguage(nextLanguage);
            else if (this.currentLanguage != null) // If current token isn't a language template and a language is initialized.
                templateDispatcher(page.get(), page);
        }

        if (this.currentLanguage != null) { // Close the current language at the end of the page.
            log.trace("{} => Finalizing {} language.", getWiktionaryPageName(), this.currentLanguage);
            this.wdh.finalizeLanguageSection();
        }

    }

    private void switchCurrentLanguage(String nextLanguage) {
        if (this.currentLanguage != null) { // Finalize the currentLanguage before switching it.
            log.trace("{} => Finalizing {} language.", getWiktionaryPageName(), this.currentLanguage);
            this.wdh.finalizeLanguageSection();
        }
        currentLanguage = nextLanguage; // Switch current language.

        if (null == wdh.getExolexFeatureBox(ExtractionFeature.MAIN) && !this.currentLanguage.equals("gle"))
            this.currentLanguage = null;
        else {
            log.trace("{} => Language swap {} was found on the page ---> {}.", getWiktionaryPageName(), this.currentLanguage, url());
            this.wdh.initializeLanguageSection(this.currentLanguage); // Initialize the new currentLanguage.
        }
    }


    public void templateDispatcher(final WikiText.Token token, final PageIterator page) { // TODO analyse all things.
        if (!(token instanceof WikiText.Template)) {
            if (token.getText().startsWith("*IPA"))
                pageAnalyzer(PageIterator.of(token.getText().substring(1)));
        } else {
            WikiText.Template template = token.asTemplate();
            switch (template.getName()) {
                case "IPA":
                    extractPrononciation(template);
                    break;
                case "-aistr-":
                    extractTranslation(template, page);
                    break;
                default:
                    log.trace("{} => {} template was found in {} language. ---> {}.", getWiktionaryPageName(), page.get().asTemplate().getName(), this.currentLanguage, url());
                    break;
            }
        }
    }

    private void extractTranslation(final WikiText.Template template, final PageIterator page) {
        // Collect the list in the translation field.
        List<WikiText.Token> internToken = new ArrayList<>();
        while (page.hasNext() && (!(page.next() instanceof WikiText.Template) || !page.get().asTemplate().getName().equals(")")))
            internToken.add(page.get());

        PageIterator transIt = PageIterator.of(internToken, ignoredTemplate);

        if (transIt.goToNextTemplate("(") == null) { // If the translation field don't close
            log.warn("{} => translation field don't close !", getWiktionaryPageName());
            return;
        }

        String gloss = "";
        if (transIt.get().asTemplate().getArgs().size() > 0) { // glossary found
            gloss = transIt.get().asTemplate().getArg("1").getText();
            log.debug("{} => Glossary found \"{}\" ---> {}", getWiktionaryPageName(), gloss, url());
        }

        TransBuilder builder = new TransBuilder();
        while (transIt.hasNext()) {

            if (transIt.next() instanceof WikiText.Template && (transIt.get().asTemplate().getName().equals("aistr") || transIt.get().asTemplate().getName().equals("aistr2"))) {

                if (builder.word != null && builder.lang != null) {
                    this.wdh.registerTranslation(builder.lang, null /* TODO gloss*/, builder.usage, builder.word);
                    log.trace("{} => translation -> {}  --{}-> {}, {}.  ---> {} "
                            , getWiktionaryPageName(), getWiktionaryPageName(), builder.lang, builder.word, builder.usage, url());
                }

                builder = new TransBuilder();
                builder.lang = LangTools.normalize(transIt.get().asTemplate().getArg("1").getText());
                builder.word = transIt.get().asTemplate().getArg("2").getText();

            } else if (transIt.get() instanceof WikiText.Template) // adding template name to usage. Like {{fir}}, {{bain}}, {{n}}.
                builder.usage += transIt.get().asTemplate().getName() + ".";
            else // Adding classic text between 2 traduction as usage.
                builder.usage += " " + transIt.get().getText().trim();

        }

        if (builder.word != null && builder.lang != null) { // Save the last trans.
            this.wdh.registerTranslation(builder.lang, null /* TODO gloss*/, builder.usage, builder.word);
            log.trace("{} => translation -> {}  --{}-> {}.  ---> {} "
                    , getWiktionaryPageName(), getWiktionaryPageName(), builder.lang, builder.word, url());
        }
    }

    private static class TransBuilder {
        public String lang = null;
        public String word = null;
        public String usage = "";
    }

    private void extractPrononciation(final WikiText.Template template) {
        if (template.getArg("1").getText().equals("//"))
            return;
        if (template.getArg("1").getText().isBlank())
            return;

        this.wdh.registerPronunciation(template.getArg("1").getText(), "ga-pron");
        log.trace("{} => Prononciation \"{}\" found. --> {}.", getWiktionaryPageName(), template.getArg("1").getText(), url());
    }

    public String parseLanguageTemplate(final WikiText.Template template) {
        String lang = null;
        if (template.getName().equals("t"))
            lang = template.getArgs().get("1").getText();
        if (template.getName().matches("-[a-z][a-z]-") || template.getName().matches("\"-[a-z][a-z][a-z]-\""))
            lang = template.getName().substring(1, template.getName().length() - 1);

        if (lang != null)
            lang = validateAndStandardizeLanguageCode(lang);

        return lang;
    }

    public void showCounter() {
        if (getWiktionaryPageName().equals("chance"))
            log.trace("Count : {}", count);
    }

    public String url() {
        return URL + getWiktionaryPageName();
    }

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
            this.cursor = -1;
            this.size = this.page.size();
        }

        public int getCursor() {
            return this.cursor;
        }

        public WikiText.Token get() {
            return this.page.get(this.cursor);
        }

        @Override
        public WikiText.Token next() {
            do {
                cursor++;
            } while (get().getText().trim().isBlank() || (get() instanceof WikiText.Template && this.skippedTemplate.contains(get().asTemplate().getName())));

            return get();
        }


        public WikiText.Template nextTemplate() {
            do {
                cursor++;
            } while (!(get() instanceof WikiText.Template) || this.skippedTemplate.contains(get().asTemplate().getName()));

            return get().asTemplate();
        }

        public WikiText.Token shadowNext() {
            return this.shadowNext(1);
        }

        public WikiText.Token shadowNext(int next) {
            return this.page.get(cursor + next);
        }

        public void skip(final int length) {
            this.cursor += length;
        }

        @Override
        public boolean hasNext() {
            int shadowCursor = 0;
            do {
                shadowCursor++;
            } while (this.cursor + shadowCursor < size
                     && (shadowNext(shadowCursor).getText().trim().isBlank()
                         || (shadowNext(shadowCursor) instanceof WikiText.Template && this.skippedTemplate.contains(shadowNext(shadowCursor).asTemplate().getName()))));

            return this.cursor + shadowCursor < size;
        }

        public boolean hasNextTemplate() {
            int shadowCursor = 0;
            do {
                shadowCursor++;
            } while (this.cursor + shadowCursor < size && (!(shadowNext(shadowCursor) instanceof WikiText.Template)
                                                           || this.skippedTemplate.contains(shadowNext(shadowCursor).asTemplate().getName())));

            return this.cursor + shadowCursor < size;
        }

        public int findNextTemplate(final String name) {
            int shadowCursor = 0;
            do {
                shadowCursor++;
            } while (this.cursor + shadowCursor < this.size &&
                     (!(this.shadowNext(shadowCursor) instanceof WikiText.Template) || !this.shadowNext(shadowCursor).asTemplate().getName().equals(name)));
            return this.cursor + shadowCursor < this.size ? shadowCursor : -1;
        }

        public WikiText.Template goToNextTemplate(final String name) {
            WikiText.Template template = null;
            while (this.hasNextTemplate() && !(template = this.nextTemplate()).getName().equals(name)) ;
            return template;
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

    public static void addIgnored(final String ignored) {
        ignoredTemplate.add(ignored);
    }

}











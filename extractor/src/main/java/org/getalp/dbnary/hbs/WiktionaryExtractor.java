/**
 *
 */
package org.getalp.dbnary.hbs;

import com.hp.hpl.jena.rdf.model.Property;
import org.getalp.dbnary.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author roques
 *
 */
public class WiktionaryExtractor extends AbstractWiktionaryExtractor {

    private Logger log = LoggerFactory.getLogger(WiktionaryExtractor.class);

    protected final static String languageSectionPatternString;

    protected final static String languageSectionPatternString1 = "={2}\\s*([^=]+)\\s*={2}\n";

    protected final static String blockPatternString;
    protected final static String blockPatternStringLevel = "={3,5}\\s*([^=]+)\\s*={3,5}";

//   protected final static String tradPatternString = "(\\S+):\\s\\[{2}(\\S+)\\]{2}(\\s\\{{2}(\\S+)\\}{2})*|(\\S+):\\s\\{{2}.*\\[{2}(.+)\\]{2}\\}{2}";
   protected final static String tradPatternString = "\\*\\s*([^:]*):[^\\[,^\\{]*[\\[,\\{]*([^\\],^\\}]*)";

    protected final static String localdefinitionPatternString = "#\\s*([^:][^#]*)";
    protected final static String examplePatternString = "#:\\s*(.+)";

    protected final static String posPatternString = "(\\{{2}([^\\{]+)\\}{2})";

    protected final static String pronPatternString = "\\{{2}([^\\{]+)\\}{2}";

    protected final static String declinationPatternString = "([^\\{^\\}^\\|]*)";

    static {
        languageSectionPatternString = "("
                + languageSectionPatternString1
                + ")";

        blockPatternString = "("
                + blockPatternStringLevel
                + ")";

    }


    public WiktionaryExtractor(IWiktionaryDataHandler wdh) {
        super(wdh);
    }

    protected final static Pattern blockPattern;
    protected final static Pattern tradPattern;
    protected final static Pattern localdefinitionPattern;
    protected final static Pattern examplePattern;
    protected final static Pattern posPattern;
    protected final static Pattern pronPattern;
    protected final static Pattern declinationPattern;


    static {
        blockPattern = Pattern.compile(blockPatternString);
        tradPattern = Pattern.compile(tradPatternString);
        localdefinitionPattern = Pattern.compile(localdefinitionPatternString);
        examplePattern = Pattern.compile(examplePatternString);
        posPattern = Pattern.compile(posPatternString);
        pronPattern = Pattern.compile(pronPatternString);
        declinationPattern = Pattern.compile(declinationPatternString);
    }

    protected final static Pattern languageSectionPattern;

    static {
        languageSectionPattern = Pattern.compile(languageSectionPatternString);
    }

    @Override
    public void extractData() {
        wdh.initializePageExtraction(wiktionaryPageName);
        Matcher languageFilter = languageSectionPattern.matcher(pageContent);
        int startSection = -1;

        String nextLang , lang = null;

        while (languageFilter.find()) {
            nextLang = languageFilter.group(2);
            extractDataLang(startSection, languageFilter.start(), lang);
            lang = nextLang;
            startSection = languageFilter.end();
        }

        // Either the filter is at end of sequence or on hbs language header.
        if (languageFilter.hitEnd()) {
            extractDataLang(startSection, pageContent.length(), lang);
        }
        wdh.finalizePageExtraction();
    }

    private enum Block {NOBLOCK, IGNOREPOS, TRADBLOCK, DEFBLOCK, PRONBLOCK, DEKLINBLOCK}

    private Block getBlock(String blockString){
        Block res = Block.IGNOREPOS;
        switch(blockString){
            case "Izgovor": // prononciation
                res = Block.PRONBLOCK;
                break;
            case "Imenica": // noun
            case "znači-imenica" : // means-noun ?
            case "Glagol" : // verb
            case "Pridjev" : // adj
            case "Prilog" : // adv
                res = Block.DEFBLOCK;
                break;
            case "Prevod":
                res = Block.TRADBLOCK;
                break;
            case "Deklinacija":
                res = Block.DEKLINBLOCK;
                break;
        }
        return res;
    }

    protected void extractDataLang(int startOffset, int endOffset, String lang){
        if (lang == null) {
            return;
        }
        lang = lang.trim();

        if (lang.toLowerCase().equals("srpskohrvatski")){
            wdh.initializeEntryExtraction(wiktionaryPageName);
        }
        else {
             return;
//            wdh.initializeEntryExtraction(wiktionaryPageName, lang);
        }

        Matcher m = blockPattern.matcher(pageContent);
        m.region(startOffset, endOffset);
        String blockString = null;
        Block block = Block.IGNOREPOS;
        int start = startOffset;

        if(m.find()){
            start = m.start();
            if(m.group(2) != null)
                blockString = m.group(2).trim();

            block = getBlock(blockString);
        }
        while (m.find()) {
            extractDataBlock(start, m.start(), block, blockString);
            start = m.end();
            if(m.group(2) != null)
                blockString = m.group(2).trim();
            block = getBlock(blockString);
        }

        extractDataBlock(start, endOffset, block, blockString);
        wdh.finalizeEntryExtraction();
    }

    protected void extractPron(int start, int end){
        Matcher pron = pronPattern.matcher(this.pageContent);
        pron.region(start, end);

        while(pron.find()){
            String tab[] = pron.group(1).split("\\|");
            if(tab.length == 3 && tab[0].equals("IPA")){
                String t2[] = tab[2].split("=");
                if(t2.length == 2) {
                    wdh.registerPronunciation(tab[1], t2[1] + "-fonipa");
                }
                else{
                    wdh.registerPronunciation(tab[1], "sh-fonipa");
                }
            }
        }
    }

    protected void extractExample(int start, int end) {
        Matcher exampleMatcher = examplePattern.matcher(this.pageContent);
        exampleMatcher.region(start, end);

        if(exampleMatcher.find()){
            String example[] = exampleMatcher.group().substring(2).split("\\|");
            if(example.length > 1) {
                String ex = example[1];
                if (ex != null && !ex.equals("")) {
                    wdh.registerExample(ex, new HashMap<Property, String>());
                }
            }
        }
    }

    protected void extractDefinitions(int start, int end, String blockString) {

        // Found and extract definition
        Matcher definitionMatcher = localdefinitionPattern.matcher(this.pageContent);
        definitionMatcher.region(start, end);
        int startSample = -1;
        int senseNum = 1;

        while (definitionMatcher.find()) {
            if(startSample == -1){
                extractPosInfo(start, definitionMatcher.start(), blockString);
            }
            else{
                extractExample(startSample, definitionMatcher.start());
            }
            String def = cleanUpMarkup(definitionMatcher.group(1)).trim();
            if(!def.equals("")) {
                wdh.registerNewDefinition(definitionMatcher.group(1), senseNum);
                senseNum++;
            }
            startSample = definitionMatcher.end();
        }

        if(definitionMatcher.hitEnd()){
            if(startSample == -1){
                startSample = start;
            }
            if(startSample < end) {
                extractExample(startSample, end);
            }
        }
    }

    private void extractPosInfo(int start, int endPos, String blockString){

        // add allInformation in currentLexicalEntry
        WiktionaryDataHandler dwdh = (WiktionaryDataHandler) wdh;

        dwdh.addPartOfSpeech(blockString);
        // found extraInformation
        Matcher pos = posPattern.matcher(this.pageContent);
        if(endPos != -1) {
            pos.region(start, endPos);
        }

        while(pos.find()){
            dwdh.extractPOSandExtraInfos(pos.group(2));
        }
    }

    private void extractTranslations(int startOffset, int endOffset) {
        Matcher trad = tradPattern.matcher(pageContent);
        trad.region(startOffset, endOffset);

        while (trad.find()) {
            String lang = null;
            String usage = null;
            String currentGloss = this.wiktionaryPageName;
            String word = "";

            if(trad.group(1) != null) {
                lang = trad.group(1);
                String[] t = trad.group(2).split("\\|");
                if(t.length == 2){
                    word = t[1];
                }
                else {
                    word = trad.group(2);
                }
            }

            String lang3 = SerboCroatianLangToCode.threeLettersCode(lang);
            if(lang3!=null){
                wdh.registerTranslation(lang3, currentGloss, usage, word);
            }
            else log.debug("Unknown lang {} --in-- {}", lang, this.wiktionaryPageName);

        }

    }

    private void extractDeklinacija(int start, int end){
        Matcher dekl = declinationPattern.matcher(pageContent);
        dekl.region(start, end);

        int template = 0;
        String templateName = "";
        ArrayList<String> parameter = new ArrayList<>();
        while (dekl.find()) {
            String curr = dekl.group().trim();
            if(!curr.equals("")) {
                if(template == 0){
                    templateName = curr;
                    template++;
                }
                else{
                    parameter.add(curr);
                }
            }
        }

        SerboCroatianMorphoExtractorWikiModel morpho = new SerboCroatianMorphoExtractorWikiModel(wdh);
        morpho.extractTemplate(templateName, parameter);

    }

    protected void extractDataBlock(int startOffset, int endOffset, Block currentBlock, String blockString){
        switch (currentBlock) {
            case NOBLOCK:
            case IGNOREPOS:
                break;
            case DEFBLOCK:
                extractDefinitions(startOffset, endOffset, blockString);
                break;
            case PRONBLOCK:
                extractPron(startOffset,endOffset);
                break;
            case TRADBLOCK:
                extractTranslations(startOffset, endOffset);
                break;
            case DEKLINBLOCK:
              //TODO
              //  extractDeklinacija(startOffset, endOffset);
                break;
            default:
                assert false : "Unexpected block while ending extraction of entry: " + wiktionaryPageName;
        }
    }

}

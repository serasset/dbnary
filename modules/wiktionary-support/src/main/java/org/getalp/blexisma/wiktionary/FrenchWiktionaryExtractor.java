/**
 * 
 */
package org.getalp.blexisma.wiktionary;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.getalp.blexisma.api.SemanticNetwork;

/**
 * @author serasset
 *
 */
public class FrenchWiktionaryExtractor extends WiktionaryExtractor {

    protected final static String languageSectionPatternString = "==\\s*\\{\\{=([^=]*)=\\}\\}\\s*==";
    protected final static String definitionPatternString = "^#{1,2}([^\\*#].*)$";
    protected final static String macroPatternString;
    protected final static String linkPatternString;
    protected final static String macroOrLinkPatternString;

    private final int NOTRAD = 0;
    private final int TRADBLOCK = 1;
    
    static {
        linkPatternString = 
            new StringBuilder()
            .append("\\[\\[")
            .append("([^\\]\\|]*)(?:\\|([^\\]]*))?")
            .append("\\]\\]")
            .toString();
        macroPatternString = 
            new StringBuilder().append("\\{\\{")
            .append("([^\\}\\|]*)(?:\\|([^\\}]*))?")
            .append("\\}\\}")
            .toString();
        macroOrLinkPatternString = new StringBuilder()
        .append("(?:")
        .append(macroPatternString)
        .append(")|(?:")
        .append(linkPatternString)
        .append(")").toString();
    }
    
    public FrenchWiktionaryExtractor(WiktionaryIndex wi) {
        super(wi);
        // TODO Auto-generated constructor stub
    }

    private final static Pattern languageSectionPattern;
    private final static Pattern definitionPattern;
    private final static Pattern macroPattern;
    private final static Pattern macroOrLinkPattern;

    static {
        languageSectionPattern = Pattern.compile(languageSectionPatternString);
        definitionPattern = Pattern.compile(definitionPatternString, Pattern.MULTILINE);
        macroPattern = Pattern.compile(macroPatternString);
        macroOrLinkPattern = Pattern.compile(macroOrLinkPatternString);
    }
    
    /* (non-Javadoc)
     * @see org.getalp.blexisma.wiktionary.WiktionaryExtractor#extractData(java.lang.String, org.getalp.blexisma.semnet.SemanticNetwork)
     */
    @Override
    public void extractData(String wiktionaryPageName, SemanticNetwork<String, String> semnet) {
        String pageContent = wiktionaryIndex.getTextOfPage(wiktionaryPageName);
        
        // System.out.println(pageContent);
        Matcher languageFilter = languageSectionPattern.matcher(pageContent);
        while (languageFilter.find() && ! languageFilter.group(1).equals("fr")) {
            ;
        }
        // Either the filter is at end of sequence or on French language header.
        if (languageFilter.hitEnd()) {
            // There is no french data in this page.
            return ;
        }
        int frenchSectionStartOffset = languageFilter.end();
        // Advance till end of sequence or new language section
        languageFilter.find();
        int frenchSectionEndOffset = languageFilter.hitEnd() ? pageContent.length() : languageFilter.start();
        
        extractDefinitions(wiktionaryPageName, pageContent, frenchSectionStartOffset, frenchSectionEndOffset, semnet);
        extractTranslations(wiktionaryPageName, pageContent, frenchSectionStartOffset, frenchSectionEndOffset, semnet);
     }

    private void extractTranslations(String wiktionaryPageName, String pageContent, int startOffset, int endOffset, SemanticNetwork<String, String> semnet) {        
        Matcher m = macroPattern.matcher(pageContent);
        m.region(startOffset, endOffset);
        // TODO: should I use a macroOrLink pattern to detect translations that are not macro based ?
        int state = NOTRAD;
        int nbtrad = 0;
        String currentGlose = null;
        while (m.find()) {
            switch (state) {
            case NOTRAD:
                if (m.group(1).equals("-trad-")) state = TRADBLOCK;
                break;
            case TRADBLOCK:
                String g1 = m.group(1);
                if (g1.equals("trad+") || g1.equals("trad-")) {
                    // TODO: what is the difference between trad+ and trad-
                    // TODO: keep the glose in the semantic network
                    String g2 = m.group(2);
                    int i1, i2, i3;
                    String lang, word, remaining = "";
                    if ((i1 = g2.indexOf('|')) != -1) {
                        lang = g2.substring(0, i1);
                        if ((i2 = g2.indexOf('|', i1+1)) == -1) {
                            word = g2.substring(i1+1);
                        } else {
                            word = g2.substring(i1+1, i2);
                        }
                        if (ISO639_1.sharedInstance.getBib3Code(lang) == null) System.out.println("Unknown language: " + lang);
                        semnet.addRelation(wiktionaryPageName, new String(lang + "|" + word), 1, "trad"); nbtrad++;
                    }
                } else if (g1.equals("boîte début")) {
                    // Get the glose that should help disambiguate the source acception
                    String g2 = m.group(2);
                    // Ignore glose if it is a macro
                    if (g2 != null && ! g2.startsWith("{{")) {
                        currentGlose = g2;
                    }
                } else if (g1.equals("-")) {
                    // just ignore it
                } else if (g1.equals(")")) {
                    // Forget the current glose
                    currentGlose = null;
                } else if (g1.equals("T")) {
                    // this a a language identifier, 
                    
                } else if (g1.startsWith("-") && g1.endsWith("-")) {
                    // It cannot be "-" as it is tested earlier
                    state = NOTRAD;
                }
                break;
            default:
                assert false : "Unexpected state while extracting translations from dictionary.";
            } 
        }
        // System.out.println(""+ nbtrad + " Translations extracted");
    }

    private void extractDefinitions(String wiktionaryPageName, String pageContent, int startOffset, int endOffset, SemanticNetwork<String, String> semnet) {
        Matcher definitionMatcher = definitionPattern.matcher(pageContent);
        definitionMatcher.region(startOffset, endOffset);
        while (definitionMatcher.find()) {
            semnet.addRelation(wiktionaryPageName, cleanUpMarkup(definitionMatcher.group(1)), 1, "def");
        }      
    }

    public String cleanUpMarkup(String str) {
        Matcher m = macroOrLinkPattern.matcher(str);
        StringBuffer sb = new StringBuffer(str.length());
        String leftGroup, rightGroup;
        while (m.find()) {
            if ((leftGroup = m.group(1)) != null) {
                // It's a macro, ignore it for now
                m.appendReplacement(sb, "");
            } else if ((leftGroup = m.group(3)) != null) {
                // It's a link, only keep the alternate string if present.
                rightGroup = m.group(4);
                String replacement = (rightGroup == null) ? leftGroup : rightGroup;
                replacement = replacement.replaceAll("\\\\", "\\\\\\\\");
                m.appendReplacement(sb, replacement);
            } else {
                // This really should not happen
                assert false : "The detected wiktionary markup is neither a link nor a macro.";
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }
    
    public static void main(String args[]) throws Exception {
        long startTime = System.currentTimeMillis();
        WiktionaryIndex wi = new WiktionaryIndex(args[0]);
        long endloadTime = System.currentTimeMillis();
        System.out.println("Loaded index in " + (endloadTime - startTime) +"ms.");
         
        FrenchWiktionaryExtractor fwe = new FrenchWiktionaryExtractor(wi);
        SimpleSemanticNetwork<String, String> s = new SimpleSemanticNetwork<String, String>();
        startTime = System.currentTimeMillis();
        int nbpages = 0;
        for (String page : wi.keySet()) {
            // System.out.println("Extracting: " + page);
            fwe.extractData(page, s); 
            // System.out.println("Extracted: " + page + " in: " + (System.currentTimeMillis() - startTime));
            nbpages++;
            //if (nbpages == 100000) break;
        }
        System.out.println(nbpages + " entries extracted in : " + (System.currentTimeMillis() - startTime));
        System.out.println("Semnet contains: " + s.getNbNodes() + " nodes and " + s.getNbEdges() + " edges.");
    }
}

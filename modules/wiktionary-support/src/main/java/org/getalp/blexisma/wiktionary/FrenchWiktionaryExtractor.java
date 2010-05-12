/**
 * 
 */
package org.getalp.blexisma.wiktionary;

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
    private static final String macroOrLinkPatternString;

    static {
        linkPatternString = 
            new StringBuilder()
            .append("\\[\\[")
            .append("([^\\]\\|]*)(?:\\|([^\\]]*))?")
            .append("\\]\\]")
            .toString();
        macroPatternString = 
            new StringBuilder().append("\\{\\{")
            .append("([^\\]\\|]*)(?:\\|([^\\]]*))?")
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
    private final static Pattern macroOrLinkPattern;

    static {
        languageSectionPattern = Pattern.compile(languageSectionPatternString);
        definitionPattern = Pattern.compile(definitionPatternString, Pattern.MULTILINE);
        macroOrLinkPattern = Pattern.compile(macroOrLinkPatternString);
    }
    
    /* (non-Javadoc)
     * @see org.getalp.blexisma.wiktionary.WiktionaryExtractor#extractData(java.lang.String, org.getalp.blexisma.semnet.SemanticNetwork)
     */
    @Override
    public void extractData(String wiktionaryPageName, SemanticNetwork semnet) {
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
        
        Matcher definitionMatcher = definitionPattern.matcher(pageContent);
        definitionMatcher.region(frenchSectionStartOffset, frenchSectionEndOffset);
        while (definitionMatcher.find()) {
            // TODO: do not allocate new Strings, but use region in original String
            System.out.println(definitionMatcher.group(1));
            System.out.println(cleanUpMarkup(definitionMatcher.group(1)));
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
        FrenchWiktionaryExtractor fwi = new FrenchWiktionaryExtractor(new WiktionaryIndex(args[0]));
        long endloadTime = System.currentTimeMillis();
        System.out.println("Loaded index in " + (endloadTime - startTime));
        fwi.extractData("dictionnaire", null); 
        long endextractTime = System.currentTimeMillis();
        System.out.println("Extrated dictionnaire in " + (endextractTime - endloadTime));
        endloadTime = endextractTime;
        fwi.extractData("table", null);
        endextractTime = System.currentTimeMillis();
        System.out.println("Extrated table in " + (endextractTime - endloadTime));
        endloadTime = endextractTime;
        fwi.extractData("Wiktionnaire:Historique des effacements", null);
        endextractTime = System.currentTimeMillis();
        System.out.println("Extrated non-french in " + (endextractTime - endloadTime));        
    }
}

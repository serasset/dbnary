package org.getalp.blexisma.wiktionary;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.getalp.blexisma.api.SemanticNetwork;

public abstract class WiktionaryExtractor {
    
    // These should be independant of the language
    protected final static String macroPatternString;
    protected final static String linkPatternString;
    protected final static String macroOrLinkPatternString;

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
    
    protected final static Pattern macroPattern;
    protected final static Pattern macroOrLinkPattern;

    static {
        macroPattern = Pattern.compile(macroPatternString);
        macroOrLinkPattern = Pattern.compile(macroOrLinkPatternString);
    }
    
    protected WiktionaryIndex wiktionaryIndex;

    public WiktionaryExtractor(WiktionaryIndex wi) {
        super();
        this.wiktionaryIndex = wi;
    }

    /**
     * @return the wiktionaryIndex
     */
    public WiktionaryIndex getWiktionaryIndex() {
        return wiktionaryIndex;
    }
    
    public abstract void extractData(String wiktionaryPageName, SemanticNetwork<String, String> semnet);
    
    // Some utility methods that should be common to all languages
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
                replacement = replacement.replaceAll("\\$", "\\\\\\$");   
                m.appendReplacement(sb, replacement);
            } else {
                // This really should not happen
                assert false : "The detected wiktionary markup is neither a link nor a macro.";
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

}

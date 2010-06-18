package org.getalp.blexisma.wiktionary;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.getalp.blexisma.api.SemanticNetwork;

public abstract class WiktionaryExtractor {
    
    // These should be independant of the language
    protected final static String macroPatternString;
    protected final static String linkPatternString;
    protected final static String macroOrLinkPatternString;
    protected final static String definitionPatternString = "^#{1,2}([^\\*#:].*)$";

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
    protected final static Pattern definitionPattern = Pattern.compile(definitionPatternString, Pattern.MULTILINE);

    static {
        macroPattern = Pattern.compile(macroPatternString);
        macroOrLinkPattern = Pattern.compile(macroOrLinkPatternString);
    }

    protected WiktionaryIndex wiktionaryIndex;
    protected SemanticNetwork<String, String> semnet;
    protected String wiktionaryPageName;
    protected String pageContent;

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
    
    public void extractData(String wiktionaryPageName, SemanticNetwork<String, String> semnet) {
        this.wiktionaryPageName = wiktionaryPageName;
        this.semnet = semnet;
        
        pageContent = wiktionaryIndex.getTextOfPage(wiktionaryPageName);
        
        if (pageContent == null) return;
        
        extractData();
     }

    public abstract void extractData();
    
    
    protected void extractDefinitions(int startOffset, int endOffset) {
        
        Matcher definitionMatcher = definitionPattern.matcher(this.pageContent);
        definitionMatcher.region(startOffset, endOffset);
        while (definitionMatcher.find()) {
            String def = definitionMatcher.group(1);
            if (def != null && ! def.equals("")) {
                this.semnet.addRelation(this.wiktionaryPageName, cleanUpMarkup(definitionMatcher.group(1)), 1, "def");
            }
        }      
    }
    
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
        // normalize whitespaces
        int l = 0;
        int i = 0; boolean previousCharIsASpace = true;
        while (i != sb.length()) {
            if (Character.isSpaceChar(sb.charAt(i))) {
                if (! previousCharIsASpace) {
                    previousCharIsASpace = true;
                    sb.setCharAt(l, ' ');
                    l++;
                } 
            } else {
                previousCharIsASpace = false;
                sb.setCharAt(l, sb.charAt(i));
                l++;
            }
            i++;
        }
        if (l > 0 && sb.charAt(l-1) == ' ') l--;
        sb.setLength(l);
        return sb.toString();
    }

}

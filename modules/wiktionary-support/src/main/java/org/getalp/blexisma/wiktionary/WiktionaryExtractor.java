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
    protected final static String bulletListPatternString = "\\*\\s*(.*)";

    protected static  String langPrefix = "";
    static {
        linkPatternString = 
            new StringBuilder()
            .append("\\[\\[")
            .append("([^\\]\\|\n\r]*)(?:\\|([^\\]\n\r]*))?")
            .append("\\]\\]")
            .toString();
        macroPatternString = 
            new StringBuilder().append("\\{\\{")
            .append("([^\\}\\|\n\r]*)(?:\\|([^\\}\n\r]*))?")
            .append("\\}\\}")
            .toString();
        macroOrLinkPatternString = new StringBuilder()
        .append("(?:")
        .append(macroPatternString)
        .append(")|(?:")
        .append(linkPatternString)
        .append(")|(?:")
        .append("'{2,3}")
        .append(")").toString();
    }
    
    protected final static Pattern macroPattern;
    protected final static Pattern linkPattern;
    protected final static Pattern macroOrLinkPattern;
    protected final static Pattern definitionPattern;
    protected final static Pattern bulletListPattern;

    static {
        macroPattern = Pattern.compile(macroPatternString);
        linkPattern = Pattern.compile(linkPatternString);
        macroOrLinkPattern = Pattern.compile(macroOrLinkPatternString);
        definitionPattern = Pattern.compile(definitionPatternString, Pattern.MULTILINE);
        bulletListPattern = Pattern.compile(bulletListPatternString);
    }

    protected final static String POS_RELATION = "pos";
    protected final static String DEF_RELATION = "def";
    protected final static String ALT_RELATION = "alt";
    protected final static String SYN_RELATION = "syn";
    protected final static String ANT_RELATION = "ant";
    protected final static String TRANSLATION_RELATION = "trad";
    protected final static String POS_PREFIX = "#" + POS_RELATION + "|";
    protected final static String DEF_PREFIX = "#" + DEF_RELATION + "|";
    
    protected WiktionaryIndex wiktionaryIndex;
    protected SemanticNetwork<String, String> semnet;
    protected String wiktionaryPageNameWithLangPrefix;
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
    
    // TODO: filter out pages that are in specific Namespaces (Wiktionary:, Categories:, ...)
    // TODO: take Redirect page into account as alternate spelling.
    // TODO: take homography into account (ex: mousse) and separate different definitions for the same pos.
    // TODO: some xml comments may be in the string values. Remove them.
    public void extractData(String wiktionaryPageName, SemanticNetwork<String, String> semnet) {
        this.wiktionaryPageName = wiktionaryPageName;
        this.wiktionaryPageNameWithLangPrefix = langPrefix + wiktionaryPageName;
        this.semnet = semnet;
        
        pageContent = wiktionaryIndex.getTextOfPage(wiktionaryPageName);
        
        if (pageContent == null) return;
        
        extractData();
     }

    public abstract void extractData();
    
    protected String currentPos = "";
    
    protected void extractDefinitions(int startOffset, int endOffset) { 
        Matcher definitionMatcher = definitionPattern.matcher(this.pageContent);
        definitionMatcher.region(startOffset, endOffset);
        while (definitionMatcher.find()) {
            String def = cleanUpMarkup(definitionMatcher.group(1));
            if (def != null && ! def.equals("")) {
                def = DEF_PREFIX + def;
                this.semnet.addRelation(this.wiktionaryPageNameWithLangPrefix, def, 1, DEF_RELATION);
                if (currentPos != null && ! currentPos.equals("")) {
                    this.semnet.addRelation(def, POS_PREFIX + currentPos, 1, POS_RELATION);
                }
            }
        }      
    }
    
    public String cleanUpMarkup(String group) {
        return cleanUpMarkup(group, false);
    }

    // public static Set<String> affixesToDiscardFromLinks = null;
    
    // Some utility methods that should be common to all languages
    // DONE: (priority: top) keep annotated lemma (#{lemma}#) in definitions.
    // DONE: handle ''...'' and '''...'''.
    // DONE: suppress affixes that follow links, like: e in [[français]]e.
    public String cleanUpMarkup(String str, boolean humanReadable) {
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
                String replacement ;
                if (rightGroup == null && humanReadable) {
                    replacement = leftGroup;
                } else if (humanReadable) {
                    replacement = rightGroup;
                } else {
                    replacement = "#{" + leftGroup + "}#";
                }
                replacement = Matcher.quoteReplacement(replacement);
                m.appendReplacement(sb, replacement);
                // Discard stupidly encoded morphological affixes.
                if (!humanReadable && str.length() > m.end() && Character.isLetter(str.charAt(m.end()))) {
                    int i = m.end();
                    StringBuffer affix = new StringBuffer();
                    while(i < str.length() && Character.isLetter(str.charAt(i))) {
                        affix.append(str.charAt(i));
                        i++;
                    }
                    str = str.substring(i);
                    m.reset(str); 
                }
            } else {
                m.appendReplacement(sb, "");
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

    protected void extractOrthoAlt(int startOffset, int endOffset) {
        Matcher bulletListMatcher = WiktionaryExtractor.bulletListPattern.matcher(this.pageContent);
        bulletListMatcher.region(startOffset, endOffset);
        while (bulletListMatcher.find()) {
            String alt = cleanUpMarkup(bulletListMatcher.group(1), true);
            if (alt != null && ! alt.equals("")) {
                alt = langPrefix + alt;
                this.semnet.addRelation(this.wiktionaryPageNameWithLangPrefix, alt, 1, ALT_RELATION);
            }
        }      
     }
 
    // TODO: Some nyms can be placed in sublists and lists (hence with ** or ***). In this case, we currently extract the additional stars.
    protected void extractNyms(String synRelation, int startOffset, int endOffset) {
        // System.out.println(wiktionaryPageName + " contains: " + pageContent.substring(startOffset, endOffset));
        // Extract all links
//        String nym = pageContent.substring(startOffset, endOffset);
//        if (nym.contains(",")) {
//            System.out.println(this.wiktionaryPageNameWithLangPrefix + " ---> ");
//            System.out.println(nym);
//        }
        Matcher linkMatcher = WiktionaryExtractor.linkPattern.matcher(this.pageContent);
        linkMatcher.region(startOffset, endOffset);
        while (linkMatcher.find()) {
            // It's a link, only keep the alternate string if present.
            String leftGroup = linkMatcher.group(1) ;
            if (leftGroup != null && ! leftGroup.equals("") && ! leftGroup.startsWith("Wikisaurus:")) {
                leftGroup = langPrefix + leftGroup;
                this.semnet.addRelation(this.wiktionaryPageNameWithLangPrefix, leftGroup, 1, synRelation);
            }
        }      
    }
    

}

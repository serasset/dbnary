package org.getalp.dbnary;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.getalp.blexisma.api.ISO639_3;

public abstract class WiktionaryExtractor {
    
	// TODO: Alter the extraction process by allowing multiple lines in a macro and evaluate the final result
	// TODO: Determine how many nested macro are used in the different wiktionary languages.
    // These should be independent of the language
    protected final static String macroPatternString;
    protected final static String linkPatternString;
    protected final static String macroOrLinkPatternString;
    protected final static String definitionPatternString = "^#{1,2}([^\\*#:].*)$";
    protected final static String bulletListPatternString = "\\*\\s*(.*)";

    protected final static String catOrInterwikiLink = "^\\s*\\[\\[([^\\:\\]]*)\\:([^\\]]*)\\]\\]\\s*$";
    protected final static Pattern categoryOrInterwikiLinkPattern;

    static {
    	// DONE: Validate the fact that links and macro should be on one line or may be on several...
    	// DONE: for this, evaluate the difference in extraction !
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
        // TODO: We should suppress multiline xml comments even if macros or line are to be on a single line.
        macroOrLinkPatternString = new StringBuilder()
        .append("(?:")
        .append(macroPatternString)
        .append(")|(?:")
        .append(linkPatternString)
        .append(")|(?:")
        .append("'{2,3}")
        .append(")|(?:")
        .append("<!--.*-->")
        .append(")").toString();
        
        categoryOrInterwikiLinkPattern = Pattern.compile(catOrInterwikiLink, Pattern.MULTILINE);

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
    
    /**
	 * @uml.property  name="pageContent"
	 */
    protected String pageContent;
    /**
	 * @uml.property  name="wdh"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
    protected WiktionaryDataHandler wdh;
	/**
	 * @uml.property  name="wiktionaryPageName"
	 */
	protected String wiktionaryPageName;
    
    public WiktionaryExtractor(WiktionaryDataHandler wdh) {
        super();
        this.wdh = wdh;
    //    this.wiktionaryIndex = wi;
    }

    /**
     * @return the wiktionaryIndex
     */
    // public WiktionaryIndex getWiktionaryIndex() {
    //    return wiktionaryIndex;
    //}
    
    // DONE: filter out pages that are in specific Namespaces (Wiktionary:, Categories:, ...)
    // TODO: take Redirect page into account as alternate spelling.
    // TODO: take homography into account (ex: mousse) and separate different definitions for the same pos.
    // TODO: some xml comments may be in the string values. Remove them.
    public void extractData(String wiktionaryPageName, String pageContent) {
    	// Entries containing the special char ":" are pages belonging to specific namespaces.(Wiktionary:, Categories:, ...).
    	// Such pages are simply ingnored.
    	if (wiktionaryPageName.contains(":")) {
    		return;
    	}
        this.wiktionaryPageName = wiktionaryPageName;
        
        this.pageContent = pageContent;
        
        if (pageContent == null) return;
        try {
        	extractData();
        } catch (RuntimeException e) {
        	System.err.println("Caught RuntimeException while parsing entry [" + this.wiktionaryPageName + "]");
        	throw e;
        }
     }

    public abstract void extractData();
    
    protected void extractDefinitions(int startOffset, int endOffset) { 
        Matcher definitionMatcher = definitionPattern.matcher(this.pageContent);
        definitionMatcher.region(startOffset, endOffset);
        while (definitionMatcher.find()) {
            String def = cleanUpMarkup(definitionMatcher.group(1));
            if (def != null && ! def.equals("")) {
            	wdh.registerNewDefinition(definitionMatcher.group(1));
            }
        }      
    }
    
	public static String cleanUpMarkup(String group) {
        return cleanUpMarkup(group, false);
    }

    
    // Some utility methods that should be common to all languages
    // DONE: (priority: top) keep annotated lemma (#{lemma}#) in definitions.
    // DONE: handle ''...'' and '''...'''.
    // DONE: suppress affixes that follow links, like: e in [[français]]e.
    // DONE: Extract lemma AND OCCURENCE of links in non human readable form

    /**
     * cleans up the wiktionary markup from a string in the following maner: <br/>
     * str is the string to be cleaned up.
     * the result depends on the value of humanReadable.
     * Wiktionary macros are always discarded.
     * xml/xhtml comments are always discarded.
     * Wiktionary links are modified depending on the value of humanReadable.
     * e.g. str = "{{a Macro}} will be [[discard]]ed and [[feed|fed]] to the [[void]]."
     * if humanReadable is true, it will produce:
     * "will be discarded and fed to the void."
     * if humanReadable is false, it will produce:
     * "will be #{discard|discarded}# and #{feed|fed}# to the #{void|void}#."
     * @param str
     * @param humanReadable
     * @return
     */
    public static String cleanUpMarkup(String str, boolean humanReadable) {
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
                    replacement = "#{" + leftGroup + "|" + ((rightGroup == null) ? leftGroup : rightGroup);
                }
                // Discard stupidly encoded morphological affixes.
                if (!humanReadable ) { // && str.length() > m.end() && Character.isLetter(str.charAt(m.end()))
                    int i = m.end();
                    StringBuffer affix = new StringBuffer();
                    while(i < str.length() && Character.isLetter(str.charAt(i))) {
                        affix.append(str.charAt(i));
                        i++;
                    }
                    replacement = replacement + affix.toString();
                	replacement = replacement + "}#";
                	replacement = Matcher.quoteReplacement(replacement);
                    m.appendReplacement(sb, replacement);
                    // Start over the match after discarded affix
                    str = str.substring(i);
                    m.reset(str); 
                } else {
                	 replacement = Matcher.quoteReplacement(replacement);
                     m.appendReplacement(sb, replacement);
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

    private static String  definitionMarkupString = "#\\{([^\\|]*)\\|([^\\}]*)\\}\\#";
    private static Pattern definitionMarkup = Pattern.compile(definitionMarkupString);
    public static String convertToHumanReadableForm(String def) {
    	Matcher m = definitionMarkup.matcher(def);
        StringBuffer sb = new StringBuffer(def.length());
        while (m.find()) {
        	m.appendReplacement(sb, m.group(2));
        }
        m.appendTail(sb);
        return sb.toString();
    }
    
    public static String getHumanReadableForm(String id) {
    	String def = id.substring(id.indexOf("|")+1);
    	return convertToHumanReadableForm(def);
    }
    
    // TODO: dissociates entry parsing and structure building in 2 classes.
    // So that we will factorize the matching code.
   protected void extractOrthoAlt(int startOffset, int endOffset) {
        Matcher bulletListMatcher = WiktionaryExtractor.bulletListPattern.matcher(this.pageContent);
        bulletListMatcher.region(startOffset, endOffset);
        while (bulletListMatcher.find()) {
            String alt = cleanUpMarkup(bulletListMatcher.group(1), true);
            if (alt != null && ! alt.equals("")) {
            	wdh.registerAlternateSpelling(alt);
            }
        }      
     }
 
    // TODO: There are entries where Files, Fichier or Image Links are inside the entry and not at the end of it...
    // links.group(1).equalsIgnoreCase("Image") || 
    // links.group(1).equalsIgnoreCase("File") ||
    // links.group(1).equalsIgnoreCase("Fichier")
    int computeRegionEnd(int blockStart, Matcher m) {
        if (m.hitEnd()) {
            // Take out categories, files and interwiki links.
            Matcher links = categoryOrInterwikiLinkPattern.matcher(pageContent);
            links.region(blockStart, m.regionEnd());
            while (links.find()) {
                if 	(	links.group(2).equals(this.wiktionaryPageName) ||
                		links.group(1).equalsIgnoreCase("Catégorie") ||
                		links.group(1).equalsIgnoreCase("Category") ||
                		links.group(1).equalsIgnoreCase("Kategorie") ||
                		links.group(1).equalsIgnoreCase("Annexe") ||
                		ISO639_3.sharedInstance.getLang(links.group(1)) != null
                		)
                    return links.start();
                else if (links.group(1) != null) {
                	// System.out.println("--- In: " + this.wiktionaryPageName + " --->");
                	// System.out.println(links.group());
                }
            } 
            return m.regionEnd();
        } else {
            return m.start();
        }
    }

   
    // TODO: Some nyms can be placed in sublists and lists (hence with ** or ***). In this case, we currently extract the additional stars.
    protected void extractNyms(String synRelation, int startOffset, int endOffset) {
        // System.out.println(wiktionaryPageName + " contains: " + pageContent.substring(startOffset, endOffset));
        // Extract all links
        Matcher linkMatcher = WiktionaryExtractor.linkPattern.matcher(this.pageContent);
        linkMatcher.region(startOffset, endOffset);
//        int lastNymEndOffset = startOffset;
//        int lastNymStartOffset = startOffset;
//        System.err.println("---- In: " + wiktionaryPageName + " ----");
//        System.err.println(this.pageContent.substring(startOffset, endOffset));
        while (linkMatcher.find()) {
        	// TODO: remove debug specific treatment for nym extraction and take a better heuristic
//        	if (lastNymEndOffset != startOffset) {
//        		String inbetween = this.pageContent.substring(lastNymEndOffset, linkMatcher.start());
//        		// if (! inbetween.matches(".*[,\\r\\n].*")) {	
//        		if (inbetween.equals(" ")) {
//        			System.out.println("---- In: " + wiktionaryPageName + " ----");
//        			System.out.println(this.pageContent.substring(lastNymStartOffset,linkMatcher.end()));
//        		}
//        	}
//        	lastNymStartOffset = linkMatcher.start();
//        	lastNymEndOffset = linkMatcher.end();
//        	// End of debug specific treatment for nym extraction...
//            System.err.println("Matched: " + linkMatcher.group(0));

            // It's a link, only keep the alternate string if present.
            String leftGroup = linkMatcher.group(1) ;
            if (leftGroup != null && ! leftGroup.equals("") && 
            		! leftGroup.startsWith("Wikisaurus:") &&
            		! leftGroup.startsWith("Catégorie:")) {
            	wdh.registerNymRelation(leftGroup, synRelation);  
            }
        }      
    }



}

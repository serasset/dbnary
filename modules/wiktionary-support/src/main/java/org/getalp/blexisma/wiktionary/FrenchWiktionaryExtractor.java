/**
 * 
 */
package org.getalp.blexisma.wiktionary;

import java.io.PrintStream;
import java.util.Date;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.getalp.blexisma.api.SemanticNetwork;

/**
 * @author serasset
 *
 */
public class FrenchWiktionaryExtractor extends WiktionaryExtractor {

    protected final static String languageSectionPatternString = "==\\s*\\{\\{=([^=]*)=\\}\\}\\s*==";
    protected final static String definitionPatternString = "^#{1,2}([^\\*#:].*)$";
    protected final static String macroPatternString;
    protected final static String linkPatternString;
    protected final static String macroOrLinkPatternString;

    private final int NODATA = 0;
    private final int TRADBLOCK = 1;
    private final int DEFBLOCK = 2;
    private final int ORTHOALTBLOCK = 3;
    
    private static HashSet<String> posMarkers;
    private static HashSet<String> ignorablePosMarkers;
    private static HashSet<String> sectionMarkers;
    private static HashSet<String> ontologyMarkers;
    
    private static HashSet<String> unsupportedMarkers = new HashSet<String>();
    
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
        
        posMarkers = new HashSet<String>(130);
        ignorablePosMarkers = new HashSet<String>(130);

        posMarkers.add("-déf-");
        posMarkers.add("-déf-/2");
        posMarkers.add("-déf2-");
        posMarkers.add("--");
        posMarkers.add("-adj-");
        posMarkers.add("-adj-/2");
        ignorablePosMarkers.add("-flex-adj-indéf-");
        posMarkers.add("-adj-dém-");
        posMarkers.add("-adj-excl-");
        posMarkers.add("-adj-indéf-");
        posMarkers.add("-adj-int-");
        posMarkers.add("-adj-num-");
        posMarkers.add("-adj-pos-");
        posMarkers.add("-adv-");
        posMarkers.add("-adv-int-");
        posMarkers.add("-adv-pron-");
        posMarkers.add("-adv-rel-");
        posMarkers.add("-aff-");
        posMarkers.add("-art-");
        ignorablePosMarkers.add("-flex-art-déf-");
        ignorablePosMarkers.add("-flex-art-indéf-");
        ignorablePosMarkers.add("-flex-art-part-");
        posMarkers.add("-art-déf-");
        posMarkers.add("-art-indéf-");
        posMarkers.add("-art-part-");
        posMarkers.add("-aux-");
        posMarkers.add("-circonf-");
        posMarkers.add("-class-");
        posMarkers.add("-cpt-");
        posMarkers.add("-conj-");
        posMarkers.add("-conj-coord-");
        posMarkers.add("-cont-");
        posMarkers.add("-copule-");
        posMarkers.add("-corrélatif-");
        posMarkers.add("-erreur-");
        posMarkers.add("-faux-prov-");
        ignorablePosMarkers.add("-flex-adj-");
        ignorablePosMarkers.add("-flex-adj-num-");
        ignorablePosMarkers.add("-flex-adj-pos-");
        ignorablePosMarkers.add("-flex-adv-");
        ignorablePosMarkers.add("-flex-art-");
        ignorablePosMarkers.add("-flex-aux-");
        ignorablePosMarkers.add("-flex-conj-");
        ignorablePosMarkers.add("-flex-interj-");
        ignorablePosMarkers.add("-flex-lettre-");
        ignorablePosMarkers.add("-flex-loc-adj-");
        ignorablePosMarkers.add("-flex-loc-conj-");
        ignorablePosMarkers.add("-flex-loc-nom-");
        ignorablePosMarkers.add("-flex-loc-verb-");
        ignorablePosMarkers.add("-flex-nom-");
        ignorablePosMarkers.add("-flex-nom-fam-");
        ignorablePosMarkers.add("-flex-nom-pr-");
        ignorablePosMarkers.add("-flex-mots-diff-");
        ignorablePosMarkers.add("-flex-prénom-");
        ignorablePosMarkers.add("-flex-prép-");
        ignorablePosMarkers.add("-flex-pronom-");
        ignorablePosMarkers.add("-flex-pronom-indéf-");
        ignorablePosMarkers.add("-flex-pronom-int-");
        ignorablePosMarkers.add("-flex-pronom-pers-");
        ignorablePosMarkers.add("-flex-pronom-rel-");
        ignorablePosMarkers.add("-flex-verb-");
        ignorablePosMarkers.add("-inf-");
        posMarkers.add("-interf-");
        posMarkers.add("-interj-");
        posMarkers.add("-lettre-");
        posMarkers.add("-loc-");
        posMarkers.add("-loc-adj-");
        posMarkers.add("-loc-adv-");
        posMarkers.add("-loc-conj-");
        posMarkers.add("-loc-dét-");
        posMarkers.add("-loc-interj-");
        posMarkers.add("-loc-nom-");
        posMarkers.add("-loc-phr-");
        posMarkers.add("-loc-post-");
        posMarkers.add("-loc-prép-");
        posMarkers.add("-loc-pronom-");
        posMarkers.add("-loc-verb-");
        posMarkers.add("-nom-");
        posMarkers.add("-nom-fam-");
        posMarkers.add("-nom-ni-");
        posMarkers.add("-nom-nu-");
        posMarkers.add("-nom-nn-");
        posMarkers.add("-nom-npl-");
        posMarkers.add("-nom-pr-");
        posMarkers.add("-nom-sciences-");
        posMarkers.add("-numér-");
        posMarkers.add("-onoma-");
        posMarkers.add("-part-");
        posMarkers.add("-post-");
        posMarkers.add("-préf-");
        posMarkers.add("-prénom-");
        posMarkers.add("-prép-");
        posMarkers.add("-pronom-");
        posMarkers.add("-pronom-adj-");
        posMarkers.add("-pronom-dém-");
        posMarkers.add("-pronom-indéf-");
        posMarkers.add("-pronom-int-");
        posMarkers.add("-pronom-pers-");
        posMarkers.add("-pronom-pos-");
        posMarkers.add("-pronom-rel-");
        posMarkers.add("-prov-");
        posMarkers.add("-racine-");
        posMarkers.add("-radical-");
        posMarkers.add("-rimes-");
        posMarkers.add("-signe-");
        posMarkers.add("-sin-");
        posMarkers.add("-subst-pron-pers-");
        posMarkers.add("-suf-");
        ignorablePosMarkers.add("-flex-suf-");
        posMarkers.add("type");
        posMarkers.add("-var-typo-");
        posMarkers.add("-verb-");
        posMarkers.add("-verb-pr-");
        
        ontologyMarkers = new HashSet<String>();
        ontologyMarkers.add("-mero-"); // ??
        ontologyMarkers.add("-hyper-");
        ontologyMarkers.add("-hypo-");
        ontologyMarkers.add("-méton-");
        
        sectionMarkers = new HashSet<String>(200);
        sectionMarkers.addAll(posMarkers);
        sectionMarkers.addAll(ontologyMarkers);
        sectionMarkers.add("-étym-");
        sectionMarkers.add("-syn-");
        sectionMarkers.add("-ant-");
        sectionMarkers.add("-voc-");
        sectionMarkers.add("-trad-");
        // TODO trouver tous les modèles de section...

    }
    
    public FrenchWiktionaryExtractor(WiktionaryIndex wi) {
        super(wi);
    }

    protected final static Pattern languageSectionPattern;
    protected final static Pattern definitionPattern;
    protected final static Pattern macroPattern;
    protected final static Pattern macroOrLinkPattern;

    static {
        languageSectionPattern = Pattern.compile(languageSectionPatternString);
        definitionPattern = Pattern.compile(definitionPatternString, Pattern.MULTILINE);
        macroPattern = Pattern.compile(macroPatternString);
        macroOrLinkPattern = Pattern.compile(macroOrLinkPatternString);
    }

    int state = NODATA;
    int definitionBlockStart = -1;
    int orthBlockStart = -1;
    SemanticNetwork<String, String> semnet;
    String wiktionaryPageName;
    String pageContent;
    
    /* (non-Javadoc)
     * @see org.getalp.blexisma.wiktionary.WiktionaryExtractor#extractData(java.lang.String, org.getalp.blexisma.semnet.SemanticNetwork)
     */
    @Override
    public void extractData(String wiktionaryPageName, SemanticNetwork<String, String> semnet) {
        this.wiktionaryPageName = wiktionaryPageName;
        this.semnet = semnet;
        
        pageContent = wiktionaryIndex.getTextOfPage(wiktionaryPageName);
        
        if (pageContent == null) return;
        
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
        
        extractFrenchData(frenchSectionStartOffset, frenchSectionEndOffset);
     }

    void gotoNoData(Matcher m) {
        state = NODATA;
    }

    
    void gotoTradBlock(Matcher m) {
        state = TRADBLOCK;
    }

    void gotoDefBlock(Matcher m){
        state = DEFBLOCK;
        definitionBlockStart = m.end();
        semnet.addRelation(wiktionaryPageName, m.group(1), 1, "pos"); // TODO: mark the semnet node with the pos
    }
    
    void gotoOrthoAltBlock(Matcher m) {
        state = ORTHOALTBLOCK;    
        orthBlockStart = m.end();
    }
    
    void leaveDefBlock(Matcher m) {
        extractDefinitions(definitionBlockStart, (m.hitEnd()) ? m.regionEnd() : m.start());
        definitionBlockStart = -1;
    }
    
    private void extractFrenchData(int startOffset, int endOffset) {        
        Matcher m = macroPattern.matcher(pageContent);
        m.region(startOffset, endOffset);
        gotoNoData(m);
        // TODO: should I use a macroOrLink pattern to detect translations that are not macro based ?
        int nbtrad = 0;
        String currentGlose = null;
        while (m.find()) {
            String currentToken = m.group();
            if (! sectionMarkers.contains(m.group(1))) unsupportedMarkers.add(m.group(1));
            switch (state) {
            case NODATA:
                if (m.group(1).equals("-trad-")) {
                    gotoTradBlock(m);
                } else if (posMarkers.contains(m.group(1))) {
                    gotoDefBlock(m);
                } else if (ignorablePosMarkers.contains(m.group(1))) {
                    // TODO: maybe also ignore trads and co...
                } else if (m.group(1).equals("-ortho-alt-")) {
                    gotoOrthoAltBlock(m);
                } 
                break;
            case DEFBLOCK:
                // Iterate until we find a new section
                if (m.group(1).equals("-trad-")) {
                    leaveDefBlock(m);
                    gotoTradBlock(m);
                } else if (posMarkers.contains(m.group(1))) {
                    leaveDefBlock(m);
                    gotoDefBlock(m);
                } else if (ignorablePosMarkers.contains(m.group(1))) {
                    leaveDefBlock(m);
                    gotoNoData(m);
                } else if (m.group(1).equals("-ortho-alt-")) {
                    leaveDefBlock(m);
                    gotoOrthoAltBlock(m);
                } else if (sectionMarkers.contains(m.group(1))) {
                    leaveDefBlock(m);
                    gotoNoData(m);
                }
                break;
            case TRADBLOCK:
                String g1 = m.group(1);
                if (g1.equals("trad+") || g1.equals("trad-") || g1.equals("trad")) {
                    // TODO: Sometimes translation links have a remaining info after the word
                    String g2 = m.group(2);
                    int i1, i2;
                    String lang, word;
                    if ((i1 = g2.indexOf('|')) != -1) {
                        lang = g2.substring(0, i1);
                        if ((i2 = g2.indexOf('|', i1+1)) == -1) {
                            word = g2.substring(i1+1);
                        } else {
                            word = g2.substring(i1+1, i2);
                        }
                        String rel = "trad|" + lang + ((currentGlose == null) ? "" : "|" + currentGlose);
                        semnet.addRelation(wiktionaryPageName, new String(lang + "|" + word), 1, rel ); nbtrad++;
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
                    
                } else if (m.group(1).equals("-trad-")) {
                    gotoTradBlock(m);
                } else if (posMarkers.contains(m.group(1))) {
                    gotoDefBlock(m);
                } else if (ignorablePosMarkers.contains(m.group(1))) {
                    gotoNoData(m);
                } else if (m.group(1).equals("-ortho-alt-")) {
                    gotoOrthoAltBlock(m);
                } else if (sectionMarkers.contains(m.group(1))) {
                    gotoNoData(m);
                }
                break;
            case ORTHOALTBLOCK:
                // TODO: Handle spelling variants
                if (m.group(1).equals("-trad-")) {
                    gotoTradBlock(m);
                } else if (posMarkers.contains(m.group(1))) {
                    gotoDefBlock(m);
                } else if (ignorablePosMarkers.contains(m.group(1))) {
                    gotoNoData(m);
                } else if (sectionMarkers.contains(m.group(1))) {
                    gotoNoData(m);
                }
                break;
            default:
                assert false : "Unexpected state while extracting translations from dictionary.";
            } 
        }
        // Finalize the entry parsing
        switch (state) {
        case NODATA:
            break;
        case DEFBLOCK:
            leaveDefBlock(m);
            break;
        case TRADBLOCK:
            break;
        case ORTHOALTBLOCK:
            break;
        default:
            assert false : "Unexpected state while extracting translations from dictionary.";
        } 
        // System.out.println(""+ nbtrad + " Translations extracted");
    }

    private void extractDefinitions(int startOffset, int endOffset) {
        
        Matcher definitionMatcher = definitionPattern.matcher(pageContent);
        definitionMatcher.region(startOffset, endOffset);
        while (definitionMatcher.find()) {
            // String def = definitionMatcher.group(0);
            // if (def.startsWith("##")) {
                // System.out.println(wiktionaryPageName + " --> " + def);
            // }
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
        SimpleSemanticNetwork<String, String> s = new SimpleSemanticNetwork<String, String>(100000, 1000000);
        startTime = System.currentTimeMillis();
        long totalRelevantTime = 0, relevantstartTime = 0, relevantTimeOfLastThousands;
        int nbpages = 0, nbrelevantPages = 0;
        relevantTimeOfLastThousands = System.currentTimeMillis();
        for (String page : wi.keySet()) {
            // System.out.println("Extracting: " + page);
            int nbnodes = s.getNbNodes();
            relevantstartTime = System.currentTimeMillis();
            fwe.extractData(page, s); 
            nbpages ++;
            if (nbnodes != s.getNbNodes()) {
                totalRelevantTime += (System.currentTimeMillis() - relevantstartTime);
                nbrelevantPages++;
                if (nbrelevantPages % 1000 == 0) {
                    System.out.println("Extracted: " + nbrelevantPages + " pages in: " + totalRelevantTime + " / Average = " 
                            + (totalRelevantTime/nbrelevantPages) + " ms/extracted page (" + (System.currentTimeMillis() - relevantTimeOfLastThousands) / 1000 + " ms) (" + nbpages 
                            + " processed Pages in " + (System.currentTimeMillis() - startTime) + " ms / Average = " + (System.currentTimeMillis() - startTime) / nbpages + ")" );
                    System.out.println("      NbNodes = " + s.getNbNodes());
                    relevantTimeOfLastThousands = System.currentTimeMillis();
                }
                if (nbrelevantPages == 3000) break;
            }
        }
//        fwe.extractData("dictionnaire", s);
//        fwe.extractData("amour", s);
//        fwe.extractData("bateau", s);
        
        System.out.println(unsupportedMarkers);
        
        s.dumpToWriter(new PrintStream(args[1] + new Date()));
        System.out.println(nbpages + " entries extracted in : " + (System.currentTimeMillis() - startTime));
        System.out.println("Semnet contains: " + s.getNbNodes() + " nodes and " + s.getNbEdges() + " edges.");
        //for (SemanticNetwork<String,String>.Edge e : s.getEdges("dictionnaire")) {
        //    System.out.println(e.getRelation() + " --> " + e.getDestination());
        //}
    }
    
}

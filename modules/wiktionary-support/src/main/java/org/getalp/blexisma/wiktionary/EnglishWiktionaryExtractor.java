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
public class EnglishWiktionaryExtractor extends WiktionaryExtractor {

    // protected final static String languageSectionPatternString = "==\\s*([^=]*)\\s*==";
    protected final static String sectionPatternString = "={2,4}\\s*([^=]*)\\s*={2,4}";
    //protected final static String subSectionPatternString = "===\\s*([^=]*)\\s*===";
    //protected final static String subsubSectionPatternString = "====\\s*([^=]*)\\s*====";

    private final int NODATA = 0;
    private final int TRADBLOCK = 1;
    private final int DEFBLOCK = 2;
    private final int ORTHOALTBLOCK = 3;
           
    public EnglishWiktionaryExtractor(WiktionaryIndex wi) {
        super(wi);
    }

    // protected final static Pattern languageSectionPattern;
    protected final static Pattern sectionPattern;
    
    protected final static HashSet<String> posMarkers;

    static {
        // languageSectionPattern = Pattern.compile(languageSectionPatternString);
       
        sectionPattern = Pattern.compile(sectionPatternString);
        
        posMarkers = new HashSet<String>(130);
        posMarkers.add("Noun");
        posMarkers.add("Adjective");
        posMarkers.add("Adverb");
        posMarkers.add("Verb");
    }

    int state = NODATA;
    int definitionBlockStart = -1;
    int orthBlockStart = -1;
    int translationBlockStart = -1;
    
    /* (non-Javadoc)
     * @see org.getalp.blexisma.wiktionary.WiktionaryExtractor#extractData(java.lang.String, org.getalp.blexisma.semnet.SemanticNetwork)
     */
    @Override
    public void extractData() {
        
        // System.out.println(pageContent);
        Matcher languageFilter = sectionPattern.matcher(pageContent);
        while (languageFilter.find() && ! languageFilter.group(1).equals("English")) {
            ;
        }
        // Either the filter is at end of sequence or on English language header.
        if (languageFilter.hitEnd()) {
            // There is no english data in this page.
            return ;
        }
        int englishSectionStartOffset = languageFilter.end();
        // Advance till end of sequence or new language section
        while (languageFilter.find() && (languageFilter.start(1) - languageFilter.start()) != 2) {
            ;
        }
        // languageFilter.find();
        int englishSectionEndOffset = languageFilter.hitEnd() ? pageContent.length() : languageFilter.start();
        
        extractEnglishData(englishSectionStartOffset, englishSectionEndOffset);
     }

    void gotoNoData(Matcher m) {
        state = NODATA;
    }

    
    void gotoTradBlock(Matcher m) {
        translationBlockStart = m.end();
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
    
    void leaveTradBlock(Matcher m) {
        extractTranslations(translationBlockStart, (m.hitEnd()) ? m.regionEnd() : m.start());
        translationBlockStart = -1;
    }

    void leaveOrthoAltBlock(Matcher m) {
        extractOrthoAlt(orthBlockStart, (m.hitEnd()) ? m.regionEnd() : m.start());
        orthBlockStart = -1;
    }

    private void extractEnglishData(int startOffset, int endOffset) {        
        Matcher m = sectionPattern.matcher(pageContent);
        m.region(startOffset, endOffset);
        gotoNoData(m);
        // TODO: should I use a macroOrLink pattern to detect translations that are not macro based ?
        while (m.find()) {
            switch (state) {
            case NODATA:
                if (m.group(1).equals("Translations")) {
                    gotoTradBlock(m);
                } else if (posMarkers.contains(m.group(1))) {
                    gotoDefBlock(m);
                } else if (m.group(1).equals("Alternative spellings")) {
                    gotoOrthoAltBlock(m);
                } 
                break;
            case DEFBLOCK:
                // Iterate until we find a new section
                if (m.group(1).equals("Translations")) {
                    leaveDefBlock(m);
                    gotoTradBlock(m);
                } else if (posMarkers.contains(m.group(1))) {
                    leaveDefBlock(m);
                    gotoDefBlock(m);
                } else if (m.group(1).equals("Alternative spellings")) {
                    leaveDefBlock(m);
                    gotoOrthoAltBlock(m);
                } else {
                    leaveDefBlock(m);
                    gotoNoData(m);
                }
                break;
            case TRADBLOCK:
                if (m.group(1).equals("Translations")) {
                    leaveTradBlock(m);
                    gotoTradBlock(m);
                } else if (posMarkers.contains(m.group(1))) {
                    leaveTradBlock(m);
                    gotoDefBlock(m);
                } else if (m.group(1).equals("Alternative spellings")) {
                    leaveTradBlock(m);
                    gotoOrthoAltBlock(m);
                } else {
                    leaveTradBlock(m);
                    gotoNoData(m);
                }
                break;
            case ORTHOALTBLOCK:
                // TODO: Handle spelling variants
                if (m.group(1).equals("Translations")) {
                    leaveOrthoAltBlock(m);
                    gotoTradBlock(m);
                } else if (posMarkers.contains(m.group(1))) {
                    leaveOrthoAltBlock(m);
                    gotoDefBlock(m);
                } else if (m.group(1).equals("Alternative spellings")) {
                    leaveOrthoAltBlock(m);
                    gotoOrthoAltBlock(m);
                } else {
                    leaveOrthoAltBlock(m);
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
            leaveTradBlock(m);
            break;
        case ORTHOALTBLOCK:
            leaveOrthoAltBlock(m);
            break;
        default:
            assert false : "Unexpected state while extracting translations from dictionary.";
        } 
        // System.out.println(""+ nbtrad + " Translations extracted");
    }
    
   private void extractOrthoAlt(int startOffset, int endOffset) {
        // TODO: implement alternate spelling extraction.     
    }
    
   private void extractTranslations(int startOffset, int endOffset) {
       Matcher macroMatcher = macroPattern.matcher(pageContent);
       macroMatcher.region(startOffset, endOffset);
       String currentGlose = null;

       while (macroMatcher.find()) {
           String g1 = macroMatcher.group(1);

           if (g1.equals("t+") || g1.equals("t-") || g1.equals("tø")) {
               // TODO: Sometimes translation links have a remaining info after the word
               String g2 = macroMatcher.group(2);
               int i1, i2;
               String lang, word;
               if (g2 != null && (i1 = g2.indexOf('|')) != -1) {
                   lang = g2.substring(0, i1);
                   if ((i2 = g2.indexOf('|', i1+1)) == -1) {
                       word = g2.substring(i1+1);
                   } else {
                       word = g2.substring(i1+1, i2);
                   }
                   String rel = "trad|" + lang + ((currentGlose == null) ? "" : "|" + currentGlose);
                   semnet.addRelation(wiktionaryPageName, new String(lang + "|" + word), 1, rel );
               }
           } else if (g1.equals("trans-top")) {
               // Get the glose that should help disambiguate the source acception
               String g2 = macroMatcher.group(2);
               // Ignore glose if it is a macro
               if (g2 != null && ! g2.startsWith("{{")) {
                   currentGlose = g2;
               }
           } else if (g1.equals("trans-mid")) {
               // just ignore it
           } else if (g1.equals("trans-bottom")) {
               // Forget the current glose
               currentGlose = null;
           }
       }
   }
    
    public static void main(String args[]) throws Exception {
        long startTime = System.currentTimeMillis();
        WiktionaryIndex wi = new WiktionaryIndex(args[0]);
        long endloadTime = System.currentTimeMillis();
        System.out.println("Loaded index in " + (endloadTime - startTime) +"ms.");
         
        EnglishWiktionaryExtractor fwe = new EnglishWiktionaryExtractor(wi);
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
                if (nbrelevantPages == 1000) break;
            }
        }
//        fwe.extractData("dictionnaire", s);
//        fwe.extractData("amour", s);
//        fwe.extractData("bateau", s);
                
        s.dumpToWriter(new PrintStream(args[1] + new Date()));
        System.out.println(nbpages + " entries extracted in : " + (System.currentTimeMillis() - startTime));
        System.out.println("Semnet contains: " + s.getNbNodes() + " nodes and " + s.getNbEdges() + " edges.");
        //for (SemanticNetwork<String,String>.Edge e : s.getEdges("dictionnaire")) {
        //    System.out.println(e.getRelation() + " --> " + e.getDestination());
        //}
    }
    
}

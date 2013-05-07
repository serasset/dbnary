/**
 * 
 */
package org.getalp.dbnary;

import java.io.PrintStream;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.getalp.blexisma.api.ISO639_3;

/**
 * @author serasset
 *
 */
public class PortugueseWiktionaryExtractor extends WiktionaryExtractor {

    //TODO: Handle Wikisaurus entries.
	protected final static String languageSectionPatternString = 
		"(?:=\\s*\\{\\{\\-([^=]*)\\-\\}\\}\\s*=)|(?:={1,5}\\s*([^=\\{\\]\\|\n\r]+)\\s*={1,5})";
	protected final static String sectionPatternString = "={2,4}\\s*([^=]*)\\s*={2,4}";
    private final int NODATA = 0;
    private final int TRADBLOCK = 1;
    private final int DEFBLOCK = 2;
    private final int ORTHOALTBLOCK = 3;
    private final int NYMBLOCK = 4;
    
    
    public PortugueseWiktionaryExtractor(WiktionaryDataHandler wdh) {
        super(wdh);
    }

    // protected final static Pattern languageSectionPattern;
    protected final static Pattern sectionPattern;
    protected final static Pattern languageSectionPattern;
    protected final static HashSet<String> posMarkers;
    protected final static HashSet<String> nymMarkers;
    protected final static HashMap<String, String> nymMarkerToNymName;

    static {
        languageSectionPattern = Pattern.compile(languageSectionPatternString);
       
        sectionPattern = Pattern.compile(sectionPatternString);
        
        posMarkers = new HashSet<String>(20);
        posMarkers.add("Substantivo");
        posMarkers.add("Adjetivo");
        posMarkers.add("Advérbio");
        posMarkers.add("Verbo");
        
        nymMarkers = new HashSet<String>(20);
        nymMarkers.add("Sinônimos");
        nymMarkers.add("Antônimos");
        nymMarkers.add("Hipônimos");
        nymMarkers.add("Hiperônimos");
        nymMarkers.add("Sinónimos");
        nymMarkers.add("Antónimos");
        nymMarkers.add("Hipónimos");
        nymMarkers.add("Hiperónimos");
        
        nymMarkerToNymName = new HashMap<String,String>(20);
        nymMarkerToNymName.put("Sinônimos", "syn");
        nymMarkerToNymName.put("Antônimos", "ant");
        nymMarkerToNymName.put("Hipônimos", "hypo");
        nymMarkerToNymName.put("Hiperônimos", "hyper");
        nymMarkerToNymName.put("Sinónimos", "syn");
        nymMarkerToNymName.put("Antónimos", "ant");
        nymMarkerToNymName.put("Hipónimos", "hypo");
        nymMarkerToNymName.put("Hiperónimos", "hyper");

    }

    int state = NODATA;
    int definitionBlockStart = -1;
    int orthBlockStart = -1;
    int translationBlockStart = -1;
    private int nymBlockStart = -1;
    private String currentNym = null;
    
    //searches for the Portuguese language header
    private boolean isPortugueseSection(Matcher filter) {
    	if (! filter.find()) return true;
    	if (filter.group(1) != null)
    		if (filter.group(1).equals("pt"))
    			return true;
    	if (filter.group(2) != null)
    		if (filter.group().charAt(1) != '=' && filter.group(2).equals("Português"))
    			return true;
    	
    	return false;
    }
    
    //searches for the Portuguese language header
    private boolean isAnotherLanguageSection(Matcher filter) {
    	if (! filter.find()) return true;
    	if (filter.group(1) != null)
    		if (! filter.group(1).equals("pt"))
    			return true;
    	if (filter.group(2) != null)
    		if (filter.group().charAt(1) != '=' && ! filter.group(2).equals("Português"))
    			return true;
    	
    	return false;
    }
    
 
    /* (non-Javadoc)
     * @see org.getalp.dbnary.WiktionaryExtractor#extractData(java.lang.String, org.getalp.blexisma.semnet.SemanticNetwork)
     */
    @Override
    public void extractData() {
        // System.out.println(pageContent);
        Matcher languageFilter = languageSectionPattern.matcher(pageContent);
        while (! isPortugueseSection(languageFilter)) {
            ;
        }
        // Either the filter is at end of sequence or on French language header.
        if (languageFilter.hitEnd()) {
            // There is no french data in this page.
            return ;
        }
        int portugueseSectionStartOffset = languageFilter.end();
        // Advance till end of sequence or new language section
        while (! isAnotherLanguageSection(languageFilter)) {
        	;
        }
        int portugueseSectionEndOffset = languageFilter.hitEnd() ? pageContent.length() : languageFilter.start();
        
        extractPortugueseData(portugueseSectionStartOffset, portugueseSectionEndOffset);
     }

//    private HashSet<String> unsupportedSections = new HashSet<String>(100);
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
        wdh.addPartOfSpeech(m.group(1));
    }
    
    void gotoOrthoAltBlock(Matcher m) {
        state = ORTHOALTBLOCK;    
        orthBlockStart = m.end();
    }
    
    void leaveDefBlock(Matcher m) {
        extractDefinitions(definitionBlockStart, computeRegionEnd(definitionBlockStart, m));
        definitionBlockStart = -1;
    }
    
    void leaveTradBlock(Matcher m) {
        extractTranslations(translationBlockStart, computeRegionEnd(translationBlockStart, m));
        translationBlockStart = -1;
    }

    void leaveOrthoAltBlock(Matcher m) {
        extractOrthoAlt(orthBlockStart, computeRegionEnd(orthBlockStart, m));
        orthBlockStart = -1;
    }

    private void gotoNymBlock(Matcher m) {
        state = NYMBLOCK; 
        currentNym = nymMarkerToNymName.get(m.group(1));
        nymBlockStart = m.end();      
     }

    private void leaveNymBlock(Matcher m) {
        extractNyms(currentNym, nymBlockStart, computeRegionEnd(nymBlockStart, m));
        currentNym = null;
        nymBlockStart = -1;         
     }

    private void extractPortugueseData(int startOffset, int endOffset) {        
        Matcher m = sectionPattern.matcher(pageContent);
        m.region(startOffset, endOffset);
        wdh.initializeEntryExtraction(wiktionaryPageName);
        gotoNoData(m);
        // WONTDO: should I use a macroOrLink pattern to detect translations that are not macro based ?
        // DONE: (priority: top) link the definition node with the current Part of Speech
        // DONE: (priority: top) type all nodes by prefixing it by language, or #pos or #def.
        // DONE: handle alternative spelling
        // DONE: extract synonyms
        // DONE: extract antonyms
        while (m.find()) {
            switch (state) {
            case NODATA:
                if (m.group(1).equals("Tradução")) {
                    gotoTradBlock(m);
                } else if (posMarkers.contains(m.group(1))) {
                    gotoDefBlock(m);
                } else if (m.group(1).equals("Alternative spellings")) {
                    gotoOrthoAltBlock(m);
                } else if (nymMarkers.contains(m.group(1))) {
                    gotoNymBlock(m);
                } 
                
                break;
            case DEFBLOCK:
                // Iterate until we find a new section
                if (m.group(1).equals("Tradução")) {
                    leaveDefBlock(m);
                    gotoTradBlock(m);
                } else if (posMarkers.contains(m.group(1))) {
                    leaveDefBlock(m);
                    gotoDefBlock(m);
                } else if (m.group(1).equals("Alternative spellings")) {
                    leaveDefBlock(m);
                    gotoOrthoAltBlock(m);
                } else if (nymMarkers.contains(m.group(1))) {
                    leaveDefBlock(m);
                    gotoNymBlock(m);
                } else {
                    leaveDefBlock(m);
                    gotoNoData(m);
                }
                break;
            case TRADBLOCK:
                if (m.group(1).equals("Tradução")) {
                    leaveTradBlock(m);
                    gotoTradBlock(m);
                } else if (posMarkers.contains(m.group(1))) {
                    leaveTradBlock(m);
                    gotoDefBlock(m);
                } else if (m.group(1).equals("Alternative spellings")) {
                    leaveTradBlock(m);
                    gotoOrthoAltBlock(m);
                } else if (nymMarkers.contains(m.group(1))) {
                    leaveTradBlock(m);
                    gotoNymBlock(m);
                } else {
                    leaveTradBlock(m);
                    gotoNoData(m);
                }
                break;
            case ORTHOALTBLOCK:
                if (m.group(1).equals("Tradução")) {
                    leaveOrthoAltBlock(m);
                    gotoTradBlock(m);
                } else if (posMarkers.contains(m.group(1))) {
                    leaveOrthoAltBlock(m);
                    gotoDefBlock(m);
                } else if (m.group(1).equals("Alternative spellings")) {
                    leaveOrthoAltBlock(m);
                    gotoOrthoAltBlock(m);
                } else if (nymMarkers.contains(m.group(1))) {
                    leaveOrthoAltBlock(m);
                    gotoNymBlock(m);
                } else {
                    leaveOrthoAltBlock(m);
                    gotoNoData(m);
                }
                break;
            case NYMBLOCK:
                if (m.group(1).equals("Tradução")) {
                    leaveNymBlock(m);
                    gotoTradBlock(m);
                } else if (posMarkers.contains(m.group(1))) {
                    leaveNymBlock(m);
                    gotoDefBlock(m);
                } else if (m.group(1).equals("Alternative spellings")) {
                    leaveNymBlock(m);
                    gotoOrthoAltBlock(m);
                } else if (nymMarkers.contains(m.group(1))) {
                    leaveNymBlock(m);
                    gotoNymBlock(m);
                } else {
                    leaveNymBlock(m);
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
        case NYMBLOCK:
            leaveNymBlock(m);
            break;
        default:
            assert false : "Unexpected state while ending extraction of entry: " + wiktionaryPageName;
        } 
        wdh.finalizeEntryExtraction();
    }
    
    private void extractTranslations(int startOffset, int endOffset) {
        Matcher macroMatcher = macroPattern.matcher(pageContent);
        macroMatcher.region(startOffset, endOffset);
        String currentGlose = null;

        while (macroMatcher.find()) {
            String g1 = macroMatcher.group(1);

            if (g1.equals("t+") || g1.equals("t-") || g1.equals("tø") || g1.equals("trad") || g1.equals("trad-") || g1.equals("trad+")) {
                // DONE: Sometimes translation links have a remaining info after the word, keep it.
                String g2 = macroMatcher.group(2);
                int i1, i2;
                String lang, word;
                if (g2 != null && (i1 = g2.indexOf('|')) != -1) {
                    lang = g2.substring(0, i1);
                    // normalize language code
                    String normLangCode;
                    if ((normLangCode = ISO639_3.sharedInstance.getIdCode(lang)) != null) {
                        lang = normLangCode;
                    } 
                    String usage = null;
                    if ((i2 = g2.indexOf('|', i1+1)) == -1) {
                        word = g2.substring(i1+1);
                    } else {
                        word = g2.substring(i1+1, i2);
                        usage = g2.substring(i2+1);
                    }
                    lang=EnglishLangToCode.triletterCode(lang);
                    if(lang!=null){
                 	   wdh.registerTranslation(lang, currentGlose, usage, word);
                    }
                }
            } else if (g1.equals("tradini")) {
                // Get the glose that should help disambiguate the source acception
                String g2 = macroMatcher.group(2);
                // Ignore glose if it is a macro
                if (g2 != null && ! g2.startsWith("{{")) {
                    currentGlose = g2;
                }
            } else if (g1.equals("tradini-checar")) {
         	   // forget glose.
         	   currentGlose = null;
            } else if (g1.equals("tradmeio")) {
                // just ignore it
            } else if (g1.equals("tradfim")) {
                // Forget the current glose
                currentGlose = null;
            }
        }
    }
    
}
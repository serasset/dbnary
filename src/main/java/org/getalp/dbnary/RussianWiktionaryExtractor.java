/**
 * 
 */
package org.getalp.dbnary;

import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.getalp.blexisma.api.ISO639_3;

/**
 * @author serasset
 *
 */
public class RussianWiktionaryExtractor extends WiktionaryExtractor {

    //TODO: Handle Wikisaurus entries.
	//DONE: extract pronunciation
	//TODO: attach multiple pronounciation correctly
    protected final static String languageSectionPatternString = "=\\s*(\\{\\{-[^=]*\\}\\})\\s*=";

    protected final static String sectionPatternString = "={2,5}\\s*([^=]*)\\s*={2,5}";
    protected final static String pronPatternString = "\\{\\{IPA\\|([^\\}\\|]*)(.*)\\}\\}";
    
    private final int NODATA = 0;
    private final int TRADBLOCK = 1;
    private final int DEFBLOCK = 2;
    private final int ORTHOALTBLOCK = 3;
    private final int NYMBLOCK = 4;
    private final int PRONBLOCK = 5;
        
    public RussianWiktionaryExtractor(WiktionaryDataHandler wdh) {
        super(wdh);
    }

    // protected final static Pattern languageSectionPattern;
    protected final static Pattern languageSectionPattern;
    protected final static Pattern sectionPattern;
    protected final static HashSet<String> posMarkers;
    //protected final static HashSet<String> nymMarkers;
    protected final static HashMap<String, String> nymMarkerToNymName;
	protected final static Pattern pronPattern;

    static {
        // languageSectionPattern = Pattern.compile(languageSectionPatternString);
       
        languageSectionPattern = Pattern.compile(languageSectionPatternString);
        sectionPattern = Pattern.compile(sectionPatternString);
        pronPattern = Pattern.compile(pronPatternString);
        
        posMarkers = new HashSet<String>(20);
        posMarkers.add("Noun");
        posMarkers.add("Adjective");
        posMarkers.add("Adverb");
        posMarkers.add("Verb");
        posMarkers.add("Proper noun");
        
       // nymMarkers = new HashSet<String>(20);
       // nymMarkers.add("Synonyms");
       // nymMarkers.add("Antonyms");
       // nymMarkers.add("Hyponyms");
       // nymMarkers.add("Hypernyms");
       // nymMarkers.add("Meronyms");
        
        nymMarkerToNymName = new HashMap<String,String>(20);
        nymMarkerToNymName.put("Синонимы", "syn");
        nymMarkerToNymName.put("Антонимы", "ant");
        nymMarkerToNymName.put("Гипонимы", "hypo");
        nymMarkerToNymName.put("Гиперонимы", "hyper");
        nymMarkerToNymName.put("Meronyms", "mero");
        nymMarkerToNymName.put("Holonyms", "holo");

    }

    int state = NODATA;
    int definitionBlockStart = -1;
    int orthBlockStart = -1;
    int translationBlockStart = -1;
    private int nymBlockStart = -1;
    private int pronBlockStart = -1;
    private String currentNym = null;
    
    /* (non-Javadoc)
     * @see org.getalp.dbnary.WiktionaryExtractor#extractData(java.lang.String, org.getalp.blexisma.semnet.SemanticNetwork)
     */
    @Override
    public void extractData() {
        
        // System.out.println(pageContent);
        Matcher languageFilter = languageSectionPattern.matcher(pageContent);
        while (languageFilter.find() && ! languageFilter.group(1).startsWith("{{-ru-")) {
            ;
        }
        // Either the filter is at end of sequence or on English language header.
        if (languageFilter.hitEnd()) {
            // There is no english data in this page.
            return ;
        }
        int russianSectionStartOffset = languageFilter.end();
        // Advance till end of sequence or new language section
        while (languageFilter.find() && languageFilter.group().charAt(1) == '=') {
            ;
        }
        // languageFilter.find();
        int russianSectionEndOffset = languageFilter.hitEnd() ? pageContent.length() : languageFilter.start();
        
        System.err.println(pageContent.substring(russianSectionStartOffset, russianSectionEndOffset));
        
        extractRussianData(russianSectionStartOffset, russianSectionEndOffset);
     }
    
    
//    private HashSet<String> unsupportedSections = new HashSet<String>(100);
    void gotoNoData(Matcher m) {
        state = NODATA;
//        try {
//            if (! unsupportedSections.contains(m.group(1))) {
//                unsupportedSections.add(m.group(1));
//                System.out.println(m.group(1));
//            }
//        } catch (IllegalStateException e) {
//            // nop
//        }
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
    	int end = computeRegionEnd(definitionBlockStart, m);
    	System.err.println(pageContent.substring(definitionBlockStart, end));
        extractDefinitions(definitionBlockStart, end);
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

    private void gotoPronBlock(Matcher m) {
        state = PRONBLOCK; 
        pronBlockStart = m.end();      
     }

    private void leavePronBlock(Matcher m) {
        extractPron(pronBlockStart, computeRegionEnd(pronBlockStart, m));
        pronBlockStart = -1;         
     }

	private void extractRussianData(int startOffset, int endOffset) {        
        Matcher m = sectionPattern.matcher(pageContent);
        m.region(startOffset, endOffset);
        wdh.initializeEntryExtraction(wiktionaryPageName);
        gotoNoData(m);
        while (m.find()) {
            switch (state) {
            case NODATA:
                if (m.group(1).equals("Перевод")) {
                    gotoTradBlock(m);
                } else if (m.group(1).equals("Значение")) {
                    gotoDefBlock(m);
                } else if (m.group(1).equals("Alternative spellings")) {
                    gotoOrthoAltBlock(m);
                } else if (nymMarkerToNymName.containsKey(m.group(1))) {
                    gotoNymBlock(m);
                } else if (m.group(1).equals("Pronunciation")) {
                	gotoPronBlock(m);
                }
                
                break;
            case DEFBLOCK:
                // Iterate until we find a new section
                if (m.group(1).equals("Перевод")) {
                    leaveDefBlock(m);
                    gotoTradBlock(m);
                } else if (m.group(1).equals("Значение")) {
                    leaveDefBlock(m);
                    gotoDefBlock(m);
                } else if (m.group(1).equals("Alternative spellings")) {
                    leaveDefBlock(m);
                    gotoOrthoAltBlock(m);
                } else if (nymMarkerToNymName.containsKey(m.group(1))) {
                    leaveDefBlock(m);
                    gotoNymBlock(m);
                } else if (m.group(1).equals("Pronunciation")) {
                    leaveDefBlock(m);
                    gotoPronBlock(m);
                } else {
                    leaveDefBlock(m);
                    gotoNoData(m);
                } 
                break;
            case TRADBLOCK:
                if (m.group(1).equals("Перевод")) {
                    leaveTradBlock(m);
                    gotoTradBlock(m);
                } else if (m.group(1).equals("Значение")) {
                    leaveTradBlock(m);
                    gotoDefBlock(m);
                } else if (m.group(1).equals("Alternative spellings")) {
                    leaveTradBlock(m);
                    gotoOrthoAltBlock(m);
                } else if (nymMarkerToNymName.containsKey(m.group(1))) {
                    leaveTradBlock(m);
                    gotoNymBlock(m);
                } else if (m.group(1).equals("Pronunciation")) {
                    leaveTradBlock(m);
                    gotoPronBlock(m);
                } else {
                    leaveTradBlock(m);
                    gotoNoData(m);
                } 
                break;
            case ORTHOALTBLOCK:
                if (m.group(1).equals("Перевод")) {
                    leaveOrthoAltBlock(m);
                    gotoTradBlock(m);
                } else if (m.group(1).equals("Значение")) {
                    leaveOrthoAltBlock(m);
                    gotoDefBlock(m);
                } else if (m.group(1).equals("Alternative spellings")) {
                    leaveOrthoAltBlock(m);
                    gotoOrthoAltBlock(m);
                } else if (nymMarkerToNymName.containsKey(m.group(1))) {
                    leaveOrthoAltBlock(m);
                    gotoNymBlock(m);
                } else if (m.group(1).equals("Pronunciation")) {
                	leaveOrthoAltBlock(m);
                    gotoPronBlock(m);
                } else {
                    leaveOrthoAltBlock(m);
                    gotoNoData(m);
                }
                break;
            case NYMBLOCK:
                if (m.group(1).equals("Перевод")) {
                    leaveNymBlock(m);
                    gotoTradBlock(m);
                } else if (m.group(1).equals("Значение")) {
                    leaveNymBlock(m);
                    gotoDefBlock(m);
                } else if (m.group(1).equals("Alternative spellings")) {
                    leaveNymBlock(m);
                    gotoOrthoAltBlock(m);
                } else if (nymMarkerToNymName.containsKey(m.group(1))) {
                    leaveNymBlock(m);
                    gotoNymBlock(m);
                } else if (m.group(1).equals("Pronunciation")) {
                	leaveNymBlock(m);
                    gotoPronBlock(m);
                } else {
                    leaveNymBlock(m);
                    gotoNoData(m);
                }
            case PRONBLOCK:
            	if (m.group(1).equals("Перевод")) {
                    leavePronBlock(m);
                    gotoTradBlock(m);
                } else if (m.group(1).equals("Значение")) {
                	leavePronBlock(m);
                    gotoDefBlock(m);
                } else if (m.group(1).equals("Alternative spellings")) {
                	leavePronBlock(m);
                    gotoOrthoAltBlock(m);
                } else if (nymMarkerToNymName.containsKey(m.group(1))) {
                	leavePronBlock(m);
                    gotoNymBlock(m);
                } else if (m.group(1).equals("Pronunciation")) {
                	leavePronBlock(m);
                    gotoPronBlock(m);
                } else {
                	leavePronBlock(m);
                    gotoNoData(m);
                }
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
        case PRONBLOCK:
        	leavePronBlock(m);
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

           if (g1.equals("t+") || g1.equals("t-") || g1.equals("tø")) {
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
           } else if (g1.equals("trans-top")) {
               // Get the glose that should help disambiguate the source acception
               String g2 = macroMatcher.group(2);
               // Ignore glose if it is a macro
               if (g2 != null && ! g2.startsWith("{{")) {
                   currentGlose = g2;
               }
           } else if (g1.equals("checktrans-top")) {
        	   // forget glose.
        	   currentGlose = null;
           } else if (g1.equals("trans-mid")) {
               // just ignore it
           } else if (g1.equals("trans-bottom")) {
               // Forget the current glose
               currentGlose = null;
           }
       }
   }
    
    private void extractPron(int startOffset, int endOffset) {
    	
    	Matcher pronMatcher = pronPattern.matcher(pageContent);
    	while (pronMatcher.find()) {
    		String pron = pronMatcher.group(1);
    		
    		if (null == pron || pron.equals("")) return;
    		
    		if (! pron.equals("")) wdh.registerPronunciation(pron, "en-fonipa");
    	}
	}
    
}

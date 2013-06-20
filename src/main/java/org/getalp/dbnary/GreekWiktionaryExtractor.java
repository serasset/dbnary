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
 * @author Barry
 *
 */
public class GreekWiktionaryExtractor extends AbstractWiktionaryExtractor {

	
    protected final static String SectionPatternString; // "={2,5}\\s*\\{\\{([^=]*)([^\\}\\|\n\r]*)\\s*(?:\\|([^\\}\n\r]*))?)\\s*\\}\\}={2,5}"

	protected final static String languageSectionPatternString = "==\\s*\\{\\{-([^-]*)-\\}\\}\\s*==";

    protected final static String pronPatternString = "\\{\\{ΔΦΑ\\|([^\\|\\}]*)(.*)\\}\\}";

    private final int NODATA = 0;
    private final int TRADBLOCK = 1;
    private final int DEFBLOCK = 2;
    private final int NYMBLOCK = 3;
    private final int PRONBLOCK = 4;
    
    
    
    // protected final static Pattern languageSectionPattern;
   
    private static HashSet<String> defMarkers;
    private static HashSet<String> nymMarkers;
    protected final static Pattern languageSectionPattern;
    private final static HashMap<String, String> nymMarkerToNymName;
    
    
    static {
    	
   
		 
		SectionPatternString = new StringBuilder()
					.append("={2,5}")
					.append("\\{\\{\\s*")
		            .append("([^\\}\\|\n\r]*)(?:([^\\}\n\r]*))?")
		            .append("\\s*\\}\\}")
		            .append("={2,5}")
		            .toString();
    	
    	defMarkers = new HashSet<String>(20);
        defMarkers.add("ουσιαστικό"); 	// Noun 
        defMarkers.add("επίθετο");   		// Adjective
        defMarkers.add("μορφή επιθέτου");   // Adjective
        defMarkers.add("επίρρημα"); 		//Adverb
        defMarkers.add("μορφή ρήματος");	// Verb form
        defMarkers.add("ρήμα"); //  Verb
        defMarkers.add("κύριο όνομα");  	//Proper noun 
        
        
        nymMarkers = new HashSet<String>(20);
        nymMarkers.add("συνώνυμα");// Synonyms 
        nymMarkers.add("αντώνυμα");// Antonyms
        nymMarkers.add("hyponyms");// Hyponyms
        nymMarkers.add("hypernyms");// Hypernyms
        nymMarkers.add("meronyms");// Meronyms
        
        nymMarkerToNymName = new HashMap<String,String>(20);
        nymMarkerToNymName.put("συνώνυμα", "syn");
        nymMarkerToNymName.put("αντώνυμα", "ant");
        nymMarkerToNymName.put("hyponyms", "hypo");
        nymMarkerToNymName.put("hypernyms", "hyper");
        nymMarkerToNymName.put("meronyms", "mero");
        
    }
    
    public GreekWiktionaryExtractor(WiktionaryDataHandler wdh) {
        super(wdh);
    }

    protected final static Pattern SectionPattern;
    protected final static Pattern pronPattern;
    
    static {
    	SectionPattern = Pattern.compile(SectionPatternString);
        pronPattern = Pattern.compile(pronPatternString);
        languageSectionPattern = Pattern.compile(languageSectionPatternString); 
    }

    int state = NODATA;
    int definitionBlockStart = -1;
    int translationBlockStart = -1;
    int pronBlockStart = -1;
    private int nymBlockStart = -1;
    

    private String currentNym = null;
    
    private boolean isnotgreek(Matcher filter) {
    	if (! filter.find()) return false;
    	if (filter.group(1) != null)
    		if (filter.group(1).equals("el"))
    			return false;
    	
    	return true;
    }
    
 public void extractData() {
        
        // System.out.println(pageContent);
        Matcher languageFilter =  languageSectionPattern.matcher(pageContent);
        while (isnotgreek(languageFilter)) {
            ;
        }
        // Either the filter is at end of sequence or on greek language header.
        if (languageFilter.hitEnd()) {
            // There is no greek data in this page.
            return ;
        }
        int greekSectionStartOffset = languageFilter.end();
        // Advance till end of sequence or new language section
      
        languageFilter.find();
        int greekSectionEndOffset = languageFilter.hitEnd() ? pageContent.length() : languageFilter.start();
        
        extractGreekData(greekSectionStartOffset, greekSectionEndOffset);
     }
	   
	  
 
	    void gotoNoData(Matcher m) {
	        state = NODATA;
	    }

	    
	    void gotoTradBlock(Matcher m) {
	        translationBlockStart = m.end();
	        state = TRADBLOCK;
	    }
	    
	    void leaveTradBlock(Matcher m) {
	        extractTranslations(translationBlockStart, computeRegionEnd(translationBlockStart, m));
	        translationBlockStart = -1;
	    }

	    void gotoDefBlock(Matcher m){
	        state = DEFBLOCK;
	        definitionBlockStart = m.end();
	        wdh.addPartOfSpeech(m.group(1));
	    }
	    
	    
	    void leaveDefBlock(Matcher m) {
	        extractDefinitions(definitionBlockStart, computeRegionEnd(definitionBlockStart, m));
	        definitionBlockStart = -1;
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
	    
	    
		private void extractGreekData(int startOffset, int endOffset) {        
	        Matcher m = SectionPattern.matcher(pageContent);
	        m.region(startOffset, endOffset);
	        wdh.initializeEntryExtraction(wiktionaryPageName);
	        gotoNoData(m);
	        
	        while (m.find()) {
	            switch (state) {
	            case NODATA:
	                if (m.group(1).equals("μεταφράσεις")) {
	                    gotoTradBlock(m);
	                } else if (defMarkers.contains(m.group(1))) {
	                    gotoDefBlock(m);
	                } else if (nymMarkers.contains(m.group(1))) {
	                    gotoNymBlock(m);
	                } else if (m.group(1).equals("προφορά")) {
	                	gotoPronBlock(m);
	                }
	                
	                break;
	                
	            case DEFBLOCK:
	                // Iterate until we find a new section
	                if (m.group(1).equals("εταφράσεις")) {
	                    leaveDefBlock(m);
	                    gotoTradBlock(m);
	                } else if (defMarkers.contains(m.group(1))) {
	                    leaveDefBlock(m);
	                    gotoDefBlock(m);
	                } else if (nymMarkers.contains(m.group(1))) {
	                    leaveDefBlock(m);
	                    gotoNymBlock(m);
	                } else if (m.group(1).equals("προφορά")) {
	                    leaveDefBlock(m);
	                    gotoPronBlock(m);
	                } else {
	                    leaveDefBlock(m);
	                    gotoNoData(m);
	                } 
	                break;
	                
	            case TRADBLOCK:
	                if (m.group(1).equals("εταφράσεις")) {
	                    leaveTradBlock(m);
	                    gotoTradBlock(m);
	                } else if (defMarkers.contains(m.group(1))) {
	                    leaveTradBlock(m);
	                    gotoDefBlock(m);
	                } else if (nymMarkers.contains(m.group(1))) {
	                    leaveTradBlock(m);
	                    gotoNymBlock(m);
	                } else if (m.group(1).equals("προφορά")) {
	                    leaveTradBlock(m);
	                    gotoPronBlock(m);
	                } else {
	                    leaveTradBlock(m);
	                    gotoNoData(m);
	                } 
	                break;  
	                
	            case NYMBLOCK:
	                if (m.group(1).equals("εταφράσεις")) {
	                    leaveNymBlock(m);
	                    gotoTradBlock(m);
	                } else if (defMarkers.contains(m.group(1))) {
	                    leaveNymBlock(m);
	                    gotoDefBlock(m);
	                } else if (nymMarkers.contains(m.group(1))) {
	                    leaveNymBlock(m);
	                    gotoNymBlock(m);
	                } else if (m.group(1).equals("προφορά")) {
	                	leaveNymBlock(m);
	                    gotoPronBlock(m);
	                } else {
	                    leaveNymBlock(m);
	                    gotoNoData(m);
	                }
	                break;
	                
	            case PRONBLOCK:
	            	if (m.group(1).equals("εταφράσεις")) {
	                    leavePronBlock(m);
	                    gotoTradBlock(m);
	                } else if (defMarkers.contains(m.group(1))) {
	                	leavePronBlock(m);
	                    gotoDefBlock(m);
	                } else if (nymMarkers.contains(m.group(1))) {
	                	leavePronBlock(m);
	                    gotoNymBlock(m);
	                } else if (m.group(1).equals("προφορά")) {
	                	leavePronBlock(m);
	                    gotoPronBlock(m);
	                } else {
	                	leavePronBlock(m);
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

	    		if (g1.equals("τ")) {
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
	    				 lang=GreekLangtoCode.triletterCode(lang);
	                     if(lang!=null){
	                  	   wdh.registerTranslation(lang, currentGlose, usage, word);
	                     }
	    			}
	    		} else if (g1.equals("μτφ-αρχή") || g1.equals("(")) {
	    			// Get the glose that should help disambiguate the source acception
	    			String g2 = macroMatcher.group(2);
	    			// Ignore glose if it is a macro
	    			if (g2 != null && ! g2.startsWith("{{")) {
	    				currentGlose = g2;
	    			}
	    		} else if (g1.equals("μτφ-μέση")) {
	    			// just ignore it
	    		} else if (g1.equals("μτφ-τέλος") || g1.equals(")")) {
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
		    		
		    		if (! pron.equals("")) wdh.registerPronunciation(pron, "el-fonipa");
		    	}
			}
}

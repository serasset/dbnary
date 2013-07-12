/**
 * 
 */
package org.getalp.dbnary;

import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
import org.getalp.blexisma.api.ISO639_3;

/**
 * @author Barry
 *
 */
public class TurkishWiktionaryExtractor extends AbstractWiktionaryExtractor {
	

    protected final static String languageSectionPatternString = "={2}\\s*\\{\\{Dil\\|([^\\}]*)\\}\\}\\s*={2}";
    protected final static String partOfSpeechPatternString = "={3}([^\\{]*)\\{\\{Söztürü\\|([^\\}\\|]*)(?:\\|([^\\}]*))?\\}\\}.*={3}";
    protected final static String macroPatternString = "\\{\\{([^\\}]*)\\}\\}";
    protected final static String definitionPatternString = "^:{1,3}\\[[^\\]]*]\\s*(.*)$";
    protected final static String pron1PatternString = "\\{\\{Çeviri Yazı\\|([^\\}\\|]*)(.*)\\}\\}";
    protected final static String pron2PatternString = "\\{\\{IPA\\|([^\\}\\|]*)(.*)\\}\\}";
    protected final static String pron3PatternString = ":?([^\\{\\|]*)";
    
    private final int NODATA = 0;
   
    private final int TRADBLOCK = 1;
   
    private final int DEFBLOCK = 2;
    
    private final int NYMBLOCK = 4;
    
    private final int PRONBLOCK = 5;
    


    public TurkishWiktionaryExtractor(WiktionaryDataHandler wdh) {
        super(wdh);
    }
    protected final static Pattern languageSectionPattern;
    protected final static Pattern definitionPattern;
    protected final static HashSet<String> partOfSpeechMarkers;
    protected final static Pattern macroOrPOSPattern; 
    protected final static String macroOrPOSPatternString;
    protected final static Pattern pron1Pattern; 
    protected final static Pattern pron2Pattern; 
    protected final static Pattern pron3Pattern; 

    protected final static Pattern macroPattern; 

    protected final static HashMap<String, String> nymMarkerToNymName;

    static {
     
        macroPattern = Pattern.compile(macroPatternString);
        definitionPattern = Pattern.compile(definitionPatternString, Pattern.MULTILINE);
        languageSectionPattern = Pattern.compile(languageSectionPatternString);        
        
       /* pronPatternString = new StringBuilder()
    	.append("(?:").append(pron1PatternString).append(")")
    	.append("|(?:").append(pron2PatternString).append(")")
    	.append("|(?:").append(pron3PatternString).append(")")
        .toString();

        pronPattern = Pattern.compile(pronPatternString);*/
        
        pron1Pattern = Pattern.compile(pron1PatternString);
        pron2Pattern = Pattern.compile(pron2PatternString);
        pron3Pattern = Pattern.compile(pron3PatternString);


        macroOrPOSPatternString = new StringBuilder()
    	.append("(?:").append(macroPatternString)
    	.append(")|(?:").append(partOfSpeechPatternString).append(")")
        .toString();

        
        macroOrPOSPattern = Pattern.compile(macroOrPOSPatternString);

         partOfSpeechMarkers = new HashSet<String>(20);
        
        partOfSpeechMarkers.add("Fiil");// Verb
        partOfSpeechMarkers.add("Ad"); // Name
        partOfSpeechMarkers.add("Özel ad"); // Name ...
        partOfSpeechMarkers.add("Sıfat"); // Adjective 
        partOfSpeechMarkers.add("Zarf");//Adverb
        
        nymMarkerToNymName = new HashMap<String, String>(20);
        nymMarkerToNymName.put("sinonim", "syn");
        nymMarkerToNymName.put("Eş Anlamlılar", "syn");
        nymMarkerToNymName.put("Eş anlamlılar", "syn");
        nymMarkerToNymName.put("Karşıt Anlamlılar", "ant");
        nymMarkerToNymName.put("Karşıt anlamlılar", "ant");
        nymMarkerToNymName.put("Alt Kavramlar", "hypo");
        nymMarkerToNymName.put("Alt kavramlar", "hypo");
        nymMarkerToNymName.put("Üst Kavramlar", "hyper");
        nymMarkerToNymName.put("Üst kavramlar", "hyper");
        nymMarkerToNymName.put("Meronyms", "mero");
        
       
    	
    }
    	
    public void extractData() {

        // System.out.println(pageContent);
        Matcher languageFilter = languageSectionPattern.matcher(pageContent);
        while (languageFilter.find() && !languageFilter.group(1).equals("Türkçe")) {
            ;
        }
        // Either the filter is at end of sequence or on German language header.
        if (languageFilter.hitEnd()) {
            // There is no German data in this page.
            return;
        }
        int turkishSectionStartOffset = languageFilter.end();
        // Advance till end of sequence or new language section
        // WHY filter on section level ?: while (languageFilter.find() && (languageFilter.start(1) - languageFilter.start()) != 2) {
        languageFilter.find();
        // languageFilter.find();
        int turkishSectionEndOffset = languageFilter.hitEnd() ? pageContent.length() : languageFilter.start();

        extractTurkishData(turkishSectionStartOffset, turkishSectionEndOffset);
    }
	
    int state = NODATA;
    int partOfSpeechBlockStart = -1;
    int translationBlockStart = -1;
    private int nymBlockStart = -1;
    int pronBlockStart = -1;
    int defBlockStart = -1;
    private String currentNym = null;
    
    
    
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
    
    void gotoDefBlock(Matcher m) {
        state = DEFBLOCK;
        defBlockStart = m.end();
    }

    void leaveDefBlock(Matcher m) {
        extractDefinitions(defBlockStart, computeRegionEnd(defBlockStart, m));
        defBlockStart = -1;
    }
    
    // TODO: section {{Kısaltmalar}} gives abbreviations
    // TODO: section Yan Kavramlar gives related concepts (apparently not synonyms).
    private void extractTurkishData(int startOffset, int endOffset) {
        Matcher m = macroOrPOSPattern.matcher(pageContent);
        m.region(startOffset, endOffset);
        wdh.initializeEntryExtraction(wiktionaryPageName);
        gotoNoData(m);
        while (m.find()) {
            switch (state) {
            case NODATA:
                if (m.group(1) != null) {
                    // It's a macro
                    if (m.group(1).equals("Söyleniş")) { // Prononciation
                      gotoPronBlock(m);
                    } else if (m.group(1).equals("Anlamlar")) { // Definition_Meanings
                        gotoDefBlock(m);
                    } else if (m.group(1).equals("Çeviriler")) { // Traduction
                        gotoTradBlock(m);
                    } else if (nymMarkerToNymName.containsKey(m.group(1))) {
                        gotoNymBlock(m);
                    }
                } else if (m.group(3) != null) {
                    if (partOfSpeechMarkers.contains(m.group(3))) {
                    	String def = m.group(3);
                    	        wdh.addPartOfSpeech(def);
                    	}
                	
                } else {
                	
                }

                break;
            case TRADBLOCK:
                if (m.group(1) != null) {
                    // It's a macro
                    if (m.group(1).equals("Söyleniş")) {
                      leaveTradBlock(m);
                       gotoPronBlock(m);
                    } else if (m.group(1).equals("Anlamlar")) {
                        leaveTradBlock(m);
                        gotoDefBlock(m);
                    } else if (m.group(1).equals("Çeviriler")) {
                        leaveTradBlock(m);
                        gotoTradBlock(m);
                    } else if (nymMarkerToNymName.containsKey(m.group(1))) {
                        leaveTradBlock(m);
                        gotoNymBlock(m);
                    }
                } else if (m.group(3) != null) {
                    leaveTradBlock(m);
                    String def = m.group(3);
                	        wdh.addPartOfSpeech(def);
                	gotoNoData(m);
                } else {
                	// Multiline macro
                	// System.out.println(m.group());
                }

                break;
            case NYMBLOCK:
                if (m.group(1) != null) {
                    // It's a macro
                    if (m.group(1).equals("Söyleniş")) {
                       leaveNymBlock(m);
                       gotoPronBlock(m);
                    } else if (m.group(1).equals("Çeviriler")) {
                        leaveNymBlock(m);
                        gotoTradBlock(m);
                    } else if (m.group(1).equals("Anlamlar")) {
                        leaveNymBlock(m);
                        gotoDefBlock(m);
                    } else if (nymMarkerToNymName.containsKey(m.group(1))) {
                        leaveNymBlock(m);
                        gotoNymBlock(m);
                    } 
                } else if (m.group(3) != null) {
                    leaveNymBlock(m);
                    String def = m.group(3);
                	        wdh.addPartOfSpeech(def);
                	gotoNoData(m);
                } else {
                	// Multiline macro
                	// System.out.println(m.group());
                }
                
                break;
                
            case DEFBLOCK:
                if (m.group(1) != null) {
                    // It's a macro
                    if (m.group(1).equals("Söyleniş")) {
                       leaveDefBlock(m);
                        gotoPronBlock(m);
                    } else if (m.group(1).equals("Çeviriler")) {
                        leaveDefBlock(m);
                        gotoTradBlock(m);
                    } else if (m.group(1).equals("Anlamlar")) {
                        leaveDefBlock(m);
                        gotoDefBlock(m);
                    } else if (nymMarkerToNymName.containsKey(m.group(1))) {
                        leaveDefBlock(m);
                        gotoNymBlock(m);
                    } 
                } else if (m.group(3) != null) {
                    leaveDefBlock(m);
                    String def = m.group(3);
                	        wdh.addPartOfSpeech(def);
                	gotoNoData(m);
                } else {
                	// Multiline macro
                	// System.out.println(m.group());
                }
                
                break;

            case PRONBLOCK:
                if (m.group(1) != null) {
                    // It's a macro
                    if (m.group(1).equals("Söyleniş")) {
                       leavePronBlock(m);
                        gotoPronBlock(m);
                    } else if (m.group(1).equals("Çeviriler")) {
                        leavePronBlock(m);
                        gotoTradBlock(m);
                    } else if (m.group(1).equals("Anlamlar")) {
                        leavePronBlock(m);
                        gotoDefBlock(m);
                    }else if (nymMarkerToNymName.containsKey(m.group(1))) {
                        leavePronBlock(m);
                        gotoNymBlock(m);
                    }
                 } else if (m.group(3) != null) {
                    leavePronBlock(m);
                    String def = m.group(3);
                	        wdh.addPartOfSpeech(def);
                	gotoNoData(m);
                } else {
                	// Multiline macro
                	// System.out.println(m.group());
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
        case PRONBLOCK:
            leavePronBlock(m);
            break;
        case TRADBLOCK:
            leaveTradBlock(m);
            break;
        case DEFBLOCK:
            leaveDefBlock(m);
            break;
        case NYMBLOCK:
            leaveNymBlock(m);
            break;
        default:
            assert false : "Unexpected state while extracting translations from dictionary.";
        }
        wdh.finalizeEntryExtraction();
    }
    static final String glossOrMacroPatternString;
    static final Pattern glossOrMacroPattern;

    
    static{   	
    	
 	   glossOrMacroPatternString = "(?:\\[([^\\][a-z]]*)\\])|(?:\\{\\{([^\\}\\|]*)\\|([^\\}\\|]*)\\|([^\\}\\|]*)\\}\\})";
       glossOrMacroPattern = Pattern.compile(glossOrMacroPatternString);
    	    }
    
    private void extractTranslations(int startOffset, int endOffset) {
        Matcher macroMatcher = glossOrMacroPattern.matcher(pageContent);
        macroMatcher.region(startOffset, endOffset);
        String currentGlose = null;
        	
        while (macroMatcher.find()) {
        	String glose = macroMatcher.group(1);
        	
        	if(glose != null){

        		currentGlose = glose ;
        		
        	} else {
          
        		String g1 = macroMatcher.group(2);
        		String g2 = macroMatcher.group(3);
        		String g3 = macroMatcher.group(4);
    
           if (g1.equals("çeviri")) {
            	String lang;
            	String word = null;
            	String usage = null;

            	lang = g2;
        		// normalize language code
                String normLangCode;
                if ((normLangCode = ISO639_3.sharedInstance.getIdCode(lang)) != null) {
                    lang = normLangCode;
                }
                  int i1;    
                if ((i1 = g3.indexOf('|')) == -1) {
                    word = g3;
                } else {
                    word = g3.substring(0, i1);
                    usage = g3.substring(i1+1);
                }
            	lang=TurkishLangtoCode.triletterCode(lang);
                if(lang!=null && word != null){
             	   wdh.registerTranslation(lang, currentGlose, usage, word);
                }
           
            } else if (g1.equals("Üst")) {
                // German wiktionary does not provide a glose to disambiguate.
                // Just ignore this marker.
            } else if (g1.equals("Orta")) {
                // just ignore it
            } else if (g1.equals("Alt")) {
                // Forget the current glose
                currentGlose = null;
            }
        
        }
    }
 }
    
private void extractPron(int startOffset, int endOffset) {
    	
    	Matcher pron1Matcher = pron1Pattern.matcher(pageContent);
    	pron1Matcher.region(startOffset, endOffset);
    	String Pron1 = null;
    	
    	Matcher pron2Matcher = pron2Pattern.matcher(pageContent);
    	pron2Matcher.region(startOffset, endOffset);
    	String Pron2 = null;
    	
    	Matcher pron3Matcher = pron3Pattern.matcher(pageContent);
    	pron3Matcher.region(startOffset, endOffset);
    	String Pron3 = null; 
    	
    	 if (pron1Matcher.find()) {
    		 
    		 if(pron1Matcher.group(1) != null) {
        		 Pron1 = pron1Matcher.group(1);
        		 String pron = StringEscapeUtils.unescapeHtml4(Pron1); // Pour decoder ce qui est codé en caractère exemple: &#x02A7;&#x0251;
        		 
        		 if (null == pron || pron.equals("")) return;
    		
        		 if (! pron.equals("")) wdh.registerPronunciation(pron, "tur-fonipa");
    		 }
    	 } else if (pron2Matcher.find()) {
        	if(pron2Matcher.group(1) != null) {
       		 Pron2 = pron2Matcher.group(1);
       		 String pron = StringEscapeUtils.unescapeHtml4(Pron2); // Pour decoder ce qui est codé en caractère exemple: &#x02A7;&#x0251;

       		 if (null == pron || pron.equals("")) return;
   		
       		 if (! pron.equals("")) wdh.registerPronunciation(pron, "tur-fonipa");
        	} 
        } else if (pron3Matcher.find()) {
    		if(pron3Matcher.group(1) != null) {
    			Pron3 = pron3Matcher.group(1);
    			if(Pron3.indexOf('[') != -1){ // if pron3 = :[[Yardım:Söyleniş|Ses Dosyası]]: {{HP}} [[Media:Ocak.ogg|ocak]],  ''Çoğul:'' {{HP}} 
    				Pron3 = "";
    			}
    			String pron = StringEscapeUtils.unescapeHtml4(Pron3); // Pour decoder ce qui est codé en caractère exemple: &#x02A7;&#x0251;

    			if (null == pron || pron.equals("")) return;
		
    			if (! pron.equals("")) wdh.registerPronunciation(pron, "tur-fonipa");
    		}	
    	}
        	
	}
    
    
@Override
protected void extractDefinitions(int startOffset, int endOffset) {
	// TODO: The definition pattern is the only one that changes. Hence, we should normalize this processing and put the macro in language specific parameters.
	Matcher definitionMatcher = definitionPattern.matcher(this.pageContent);
    definitionMatcher.region(startOffset, endOffset);
    while (definitionMatcher.find()) {
        String def = cleanUpMarkup(definitionMatcher.group(1));
        if (def != null && !def.equals("")) {
        	wdh.registerNewDefinition(definitionMatcher.group(1));
        }
    }
}



}
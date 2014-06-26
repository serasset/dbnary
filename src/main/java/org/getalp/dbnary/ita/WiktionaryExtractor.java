/**
 * 
 */
package org.getalp.dbnary.ita;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.getalp.blexisma.api.ISO639_3;
import org.getalp.dbnary.AbstractWiktionaryExtractor;
import org.getalp.dbnary.WiktionaryDataHandler;
import org.getalp.dbnary.wiki.WikiPatterns;

/**
 * @author serasset
 *
 */
public class WiktionaryExtractor extends AbstractWiktionaryExtractor {



	protected final static String level2HeaderPatternString = "^==([^=].*[^=])==$";
		   
	protected final static String entrySectionPatternString ;	
	 
	static {
		 
		entrySectionPatternString = 
		            new StringBuilder().append("\\{\\{\\s*-")
		            .append("([^\\}\\|\n\r]*)-\\s*(?:\\|([^\\}\n\r]*))?")
		            .append("\\}\\}")
		            .toString();
		
	}

		protected final static String wikiSectionPatternString = "={2,4}\\s*([^=]*)\\s*={2,4}";
	    private final int NODATA = 0;
	    private final int TRADBLOCK = 1;
	    private final int DEFBLOCK = 2;
	    private final int ORTHOALTBLOCK = 3;
	    private final int NYMBLOCK = 4;
	    private final int PRONBLOCK = 5;
	    private final int MORPHOBLOCK = 6;
		private final int IGNOREPOS = 7;

	    // TODO: handle pronounciation
	    protected final static String pronounciationPatternString = "\\{\\{IPA\\|([^\\}\\|]*)(.*)\\}\\}";
	    
    public WiktionaryExtractor(WiktionaryDataHandler wdh) {
        super(wdh);
    }

    // protected final static Pattern languageSectionPattern;
    protected final static HashMap<String,String> posMarkers;
    //protected final static HashSet<String> nymMarkers;
    protected final static HashMap<String, String> nymMarkerToNymName;
    		
    static {
              
    	 posMarkers = new HashMap<String,String>(20);
         posMarkers.put("noun", "Noun");
         posMarkers.put("sost", "Noun");
         posMarkers.put("loc noun", "Noun");
         posMarkers.put("loc nom", "Noun");
         posMarkers.put("nome", "Proper noun");
         posMarkers.put("name", "Proper noun");
         posMarkers.put("adj", "Adjective");
         posMarkers.put("agg", "Adjective");
         posMarkers.put("loc adjc", "Adjective");
         posMarkers.put("loc agg", "Adjective");
         posMarkers.put("avv", "Adverb");
         posMarkers.put("adv", "Adverb");
         posMarkers.put("loc avv", "Adverb");
         posMarkers.put("verb", "Verb");
         posMarkers.put("loc verb", "Verb");
         
         // TODO: -acron-, -acronim-, -acronym-, -espr-, -espress- mark locution as phrases
                  
         nymMarkerToNymName = new HashMap<String,String>(20);
         nymMarkerToNymName.put("syn", "syn");
         nymMarkerToNymName.put("sin", "syn");
         nymMarkerToNymName.put("ant", "ant");
         nymMarkerToNymName.put("Hipônimos", "hypo");
         nymMarkerToNymName.put("Hiperônimos", "hyper");
         nymMarkerToNymName.put("Sinónimos", "syn");
         nymMarkerToNymName.put("Antónimos", "ant");
         nymMarkerToNymName.put("Hipónimos", "hypo");
         nymMarkerToNymName.put("Hiperónimos", "hyper");
       
    }

    protected final static Pattern sectionPattern;

    // TODO: handle pronunciation in italian
	private final static Pattern pronunciationPattern;
    protected final static Pattern level2HeaderPattern;

    static {
        level2HeaderPattern = Pattern.compile(level2HeaderPatternString, Pattern.MULTILINE);

        sectionPattern = Pattern.compile(entrySectionPatternString);
        pronunciationPattern = Pattern.compile(pronounciationPatternString);
    }

    int state = NODATA;
    int definitionBlockStart = -1;
    int translationBlockStart = -1;
    int orthBlockStart = -1;
    private int nymBlockStart = -1;
    private int pronBlockStart = -1;
    private int morphoBlockStart = -1;

    private String currentNym = null;

    protected boolean isCurrentlyExtracting = false;
	private boolean isCorrectPOS;
   
    
    public boolean isCurrentlyExtracting() {
		return isCurrentlyExtracting;
	}

    /* (non-Javadoc)
     * @see org.getalp.dbnary.WiktionaryExtractor#extractData(java.lang.String, org.getalp.blexisma.semnet.SemanticNetwork)
     */
    @Override
    public void extractData() {
    	Matcher l1 = level2HeaderPattern.matcher(pageContent);
    	int itaStart = -1;
        wdh.initializeEntryExtraction(wiktionaryPageName);
    	while (l1.find()) {
    		// System.err.println(l1.group());
    		if (-1 != itaStart) {
    			// System.err.println("Parsing previous portuguese entry");
    			extractItalianData(itaStart, l1.start());
    			itaStart = -1;
    		}
    		if (isItalian(l1)) {
    			itaStart = l1.end();
    		}
    	}
    	if (-1 != itaStart) {
			//System.err.println("Parsing previous portuguese entry");
			extractItalianData(itaStart, pageContent.length());
		}
    	
    }
    
    private boolean isItalian(Matcher l1) {
		if (l1.group(1).trim().startsWith("{{-it-")) return true;
		return false;
	}
    
//    private HashSet<String> unsupportedSections = new HashSet<String>(100);
    void gotoNoData(Matcher m) {
        state = NODATA;
    }

    
    void gotoTradBlock(Matcher m) {
        translationBlockStart = m.start(); // Keep -trad1- in translation block 
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
    	// System.err.println(pageContent.substring(definitionBlockStart, end));
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

	private void gotoIgnorePos() {
		state = IGNOREPOS;
	}

	// TODO: variants, pronunciations and other elements are common to the different entries in the page.
	private void extractItalianData(int startOffset, int endOffset) {        
        Matcher m = sectionPattern.matcher(pageContent);
        m.region(startOffset, endOffset);
        gotoNoData(m);
        while (m.find()) {
            switch (state) {
            case NODATA:
            	if (m.group(1).startsWith("trad1")) {
                    gotoTradBlock(m);
                } else if (posMarkers.containsKey(m.group(1))) {
                    gotoDefBlock(m);
                } else if (m.group(1).equals("var")) {
                    gotoOrthoAltBlock(m);
                } else if (nymMarkerToNymName.containsKey(m.group(1))) {
                    gotoNymBlock(m);
                } else if (m.group(1).equals("pron")) {
                	gotoPronBlock(m);
                } else if (isLevel3Header(m)) {
                	// Level 2 header that are not a correct POS, or Etimology or Pronunciation are considered as ignorable POS.
                	gotoIgnorePos();
                }
                
                break;
            case DEFBLOCK:
                // Iterate until we find a new section
            	if (m.group(1).equals("trad1")) {
                    leaveDefBlock(m);
                    gotoTradBlock(m);
                } else if (posMarkers.containsKey(m.group(1))) {
                    leaveDefBlock(m);
                    gotoDefBlock(m);
                } else if (m.group(1).equals("var")) {
                    leaveDefBlock(m);
                    gotoOrthoAltBlock(m);
                } else if (nymMarkerToNymName.containsKey(m.group(1))) {
                    leaveDefBlock(m);
                    gotoNymBlock(m);
                } else if (m.group(1).equals("pron")) {
                    leaveDefBlock(m);
                    gotoPronBlock(m);
                } else if (isLevel3Header(m)) {
                    leaveDefBlock(m);
                    gotoIgnorePos();
                } else {
                    leaveDefBlock(m);
                    gotoNoData(m);
                } 
                break;
            case TRADBLOCK:
            	if (m.group(1).equals("trad1")) {
                    leaveTradBlock(m);
                    gotoTradBlock(m);
                } else if (posMarkers.containsKey(m.group(1))) {
                    leaveTradBlock(m);
                    gotoDefBlock(m);
                } else if (m.group(1).equals("var")) {
                    leaveTradBlock(m);
                    gotoOrthoAltBlock(m);
                } else if (nymMarkerToNymName.containsKey(m.group(1))) {
                    leaveTradBlock(m);
                    gotoNymBlock(m);
                } else if (m.group(1).equals("pron")) {
                    leaveTradBlock(m);
                    gotoPronBlock(m);
                } else if (isLevel3Header(m)) {
                    leaveTradBlock(m);
                    gotoIgnorePos();
                } else {
                    leaveTradBlock(m);
                    gotoNoData(m);
                } 
                break;
            case ORTHOALTBLOCK:
            	if (m.group(1).equals("trad1")) {
                    leaveOrthoAltBlock(m);
                    gotoTradBlock(m);
                } else if (posMarkers.containsKey(m.group(1))) {
                    leaveOrthoAltBlock(m);
                    gotoDefBlock(m);
                } else if (m.group(1).equals("var")) {
                    leaveOrthoAltBlock(m);
                    gotoOrthoAltBlock(m);
                } else if (nymMarkerToNymName.containsKey(m.group(1))) {
                    leaveOrthoAltBlock(m);
                    gotoNymBlock(m);
                } else if (m.group(1).equals("pron")) {
                	leaveOrthoAltBlock(m);
                    gotoPronBlock(m);
                } else if (isLevel3Header(m)) {
                	leaveOrthoAltBlock(m);
                    gotoIgnorePos();
                } else {
                    leaveOrthoAltBlock(m);
                    gotoNoData(m);
                }
                break;
            case NYMBLOCK:
            	if (m.group(1).equals("trad1")) {
                    leaveNymBlock(m);
                    gotoTradBlock(m);
                } else if (posMarkers.containsKey(m.group(1))) {
                    leaveNymBlock(m);
                    gotoDefBlock(m);
                } else if (m.group(1).equals("var")) {
                    leaveNymBlock(m);
                    gotoOrthoAltBlock(m);
                } else if (nymMarkerToNymName.containsKey(m.group(1))) {
                    leaveNymBlock(m);
                    gotoNymBlock(m);
                } else if (m.group(1).equals("pron")) {
                	leaveNymBlock(m);
                    gotoPronBlock(m);
                } else if (isLevel3Header(m)) {
                	leaveNymBlock(m);
                    gotoIgnorePos();
                } else {
                    leaveNymBlock(m);
                    gotoNoData(m);
                }
            	break;
            case PRONBLOCK:
            	if (m.group(1).equals("trad1")) {
                    leavePronBlock(m);
                    gotoTradBlock(m);
                } else if (posMarkers.containsKey(m.group(1))) {
                	leavePronBlock(m);
                    gotoDefBlock(m);
                } else if (m.group(1).equals("var")) {
                	leavePronBlock(m);
                    gotoOrthoAltBlock(m);
                } else if (nymMarkerToNymName.containsKey(m.group(1))) {
                	leavePronBlock(m);
                    gotoNymBlock(m);
                } else if (m.group(1).equals("pron")) {
                	leavePronBlock(m);
                    gotoPronBlock(m);
                } else if (isLevel3Header(m)) {
                	leavePronBlock(m);
                    gotoIgnorePos();
                } else {
                	leavePronBlock(m);
                    gotoNoData(m);
                }
            	break;
            case IGNOREPOS:
            	if (m.group(1).equals("trad1")) {
                } else if (posMarkers.containsKey(m.group(1))) {
                	gotoDefBlock(m);
                } else if (m.group(1).equals("var")) {
                } else if (nymMarkerToNymName.containsKey(m.group(1))) {
                } else if (m.group(1).equals("pron")) {
                	// gotoPronBlock(m);
                } else if (isLevel3Header(m)) {
                    gotoIgnorePos();
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
        case PRONBLOCK:
        	leavePronBlock(m);
            break;
        case IGNOREPOS:
            break;
       default:
            assert false : "Unexpected state while ending extraction of entry: " + wiktionaryPageName;
        } 
        wdh.finalizeEntryExtraction();
    }
    
	private boolean isLevel3Header(Matcher m) {
		return m.group(0).startsWith("==") && ! m.group(0).startsWith("===") ;
	}

	
	
    protected final static String carPatternString;
	protected final static String macroOrLinkOrcarPatternString;
	
   
    static {
		// les caractères visible 
		carPatternString=
				new StringBuilder().append("(.)")
				.toString();

		// TODO: We should suppress multiline xml comments even if macros or line are to be on a single line.
		macroOrLinkOrcarPatternString = new StringBuilder()
		.append("(?:")
		.append(WikiPatterns.macroPatternString)
		.append(")|(?:")
		.append(WikiPatterns.linkPatternString)
		.append(")|(?:")
		.append("(:*\\*)")
		.append(")|(?:")
		.append(carPatternString)
		.append(")").toString();
    }
    protected final static Pattern macroOrLinkOrcarPattern;
	protected final static Pattern carPattern;
	static {
		carPattern = Pattern.compile(carPatternString);
		macroOrLinkOrcarPattern = Pattern.compile(macroOrLinkOrcarPatternString, Pattern.DOTALL);
	}

	protected final int INIT = 1;
	protected final int LANGUE = 2;
	protected final int TRAD = 3;  

	// TODO: delegate translation extraction to the appropriate wiki model
    private void extractTranslations(int startOffset, int endOffset) {
	Matcher macroOrLinkOrcarMatcher = macroOrLinkOrcarPattern.matcher(pageContent);
		macroOrLinkOrcarMatcher.region(startOffset, endOffset);
		int ETAT = INIT;

		String currentGlose = null;
		String lang=null, word= ""; 
		String usage = "";       
		String langname = "";

        while (macroOrLinkOrcarMatcher.find()) {

			String g1 = macroOrLinkOrcarMatcher.group(1);
			String g3 = macroOrLinkOrcarMatcher.group(3);
			String g5 = macroOrLinkOrcarMatcher.group(5);
			String g6 = macroOrLinkOrcarMatcher.group(6);

			switch (ETAT) {

			case INIT:
				if (g1!=null) {
					if (g1.equalsIgnoreCase("-trad1-"))  {
						if (macroOrLinkOrcarMatcher.group(2) != null) {
							currentGlose = macroOrLinkOrcarMatcher.group(2);
						} else {
							currentGlose = null;
						}

					} else if (g1.equalsIgnoreCase("-trad2-")) {
						currentGlose = null;
					} else if (g1.equalsIgnoreCase("mid")) {
						//ignore
					}
				} else if(g3!=null) {
					//System.err.println("Unexpected link while in INIT state.");
				} else if (g5 != null) {
					ETAT = LANGUE;
				} else if (g6 != null) {
					if (g6.equals(":")) {
						//System.err.println("Skipping ':' while in INIT state.");
					} else if (g6.equals("\n") || g6.equals("\r")) {

					} else if (g6.equals(",")) {
						//System.err.println("Skipping ',' while in INIT state.");
					} else {
						//System.err.println("Skipping " + g5 + " while in INIT state.");
					}
				}

				break;

			case LANGUE:

				if (g1!=null) {
					if (g1.equalsIgnoreCase("-trad1-"))  {
						if (macroOrLinkOrcarMatcher.group(2) != null) {
							currentGlose = macroOrLinkOrcarMatcher.group(2);
						} else {
							currentGlose = null;
						}
						langname = ""; word = ""; usage = "";
						ETAT = INIT;
					} else if (g1.equalsIgnoreCase("-trad2-")) {
						currentGlose = null;
						langname = ""; word = ""; usage = "";
						ETAT = INIT;
					} else if (g1.equalsIgnoreCase("mid")) {
						langname = ""; word = ""; usage = "";
						ETAT = INIT;
					} else {
						langname = g1;
						String l = ISO639_3.sharedInstance.getIdCode(langname);
						if (l != null) {
							langname = l;
						}
					}
				} else if(g3!=null) {
					//System.err.println("Unexpected link while in LANGUE state.");
				} else if (g5 != null) {
					//System.err.println("Skipping '*' while in LANGUE state.");
				} else if (g6 != null) {
					if (g6.equals(":")) {
						lang = langname.trim();
						lang=stripParentheses(lang);
						lang =ItalianLangToCode.triletterCode(lang);
						langname = "";
						ETAT = TRAD;
					} else if (g6.equals("\n") || g6.equals("\r")) {
						//System.err.println("Skipping newline while in LANGUE state.");
					} else if (g6.equals(",")) {
						//System.err.println("Skipping ',' while in LANGUE state.");
					} else {
						langname = langname + g6;
					}
				} 

				break ;
			case TRAD:
				if (g1!=null) {
					if (g1.equalsIgnoreCase("-trad1-"))  {
						if (macroOrLinkOrcarMatcher.group(2) != null) {
							currentGlose = macroOrLinkOrcarMatcher.group(2);
						} else {
							currentGlose = null;
						}
						//if (word != null && word.length() != 0) {
							//lang=stripParentheses(lang);
							//wdh.registerTranslation(lang, currentGlose, usage, word);
						//}
						langname = ""; word = ""; usage = ""; lang=null;
						ETAT = INIT;
					} else if (g1.equalsIgnoreCase("-trad2-")) {
						if (word != null && word.length() != 0) {
							if(lang!=null) {
								wdh.registerTranslation(lang, currentGlose, usage, word);
							}
						}
						currentGlose = null;
						langname = ""; word = ""; usage = ""; lang=null;
						ETAT = INIT;
					} else if (g1.equalsIgnoreCase("mid")) {
						if (word != null && word.length() != 0) {
							if(lang!=null){
							wdh.registerTranslation(lang, currentGlose, usage, word);
							}
						}
						langname = ""; word = ""; usage = ""; lang = null;
						ETAT = INIT;
					} else {
						usage = usage + "{{" + g1 + "}}";
					}
				} else if (g3!=null) {
					word =word+" " +g3;
				} else if (g5 != null) {
					//System.err.println("Skipping '*' while in LANGUE state.");
				} else if (g6 != null) {
					if (g6.equals("\n") || g6.equals("\r")) {
						usage = usage.trim();
						// System.err.println("Registering: " + word + ";" + lang + " (" + usage + ") " + currentGlose);
						if (word != null && word.length() != 0) {
							if(lang!=null){
							wdh.registerTranslation(lang, currentGlose, usage, word);
							}
						}
						lang = null; 
						usage = "";
						word="";
						ETAT = INIT;
					} else if (g6.equals(",")) {
						usage = usage.trim();
						// System.err.println("Registering: " + word + ";" + lang + " (" + usage + ") " + currentGlose);
						if (word != null && word.length() != 0) {
							if (lang!=null){
							wdh.registerTranslation(lang, currentGlose, usage, word);
							}
						}
						usage = "";
						word = "";
					} else {
						usage = usage + g6;
					}
				}
				break;
			default: 
				System.err.println("Unexpected state number:" + ETAT);
				break; 
			}
        	

        }
    }
  
    // TODO: try to use gwtwiki to extract translations
//	private void extractTranslations(int startOffset, int endOffset) {
//       String transCode = pageContent.substring(startOffset, endOffset);
//       ItalianTranslationExtractorWikiModel dbnmodel = new ItalianTranslationExtractorWikiModel(this.wdh, this.wi, new Locale("pt"), "/${image}/"+wiktionaryPageName, "/${title}");
//       dbnmodel.parseTranslationBlock(transCode);
//   }
    
    private void extractPron(int startOffset, int endOffset) {
        String pronCode = pageContent.substring(startOffset, endOffset);
    	ItalianPronunciationExtractorWikiModel dbnmodel = new ItalianPronunciationExtractorWikiModel(this.wdh, this.wi, new Locale("pt"), "/${image}", "/${title}");
        dbnmodel.parsePronunciation(pronCode);
	}
    
    @Override
	public void extractDefinition(String definition) {
		// TODO: properly handle macros in definitions.
        ItalianDefinitionExtractorWikiModel dbnmodel = new ItalianDefinitionExtractorWikiModel(this.wdh, this.wi, new Locale("pt"), "/${image}", "/${title}");
        dbnmodel.parseDefinition(definition);
	}
    

}

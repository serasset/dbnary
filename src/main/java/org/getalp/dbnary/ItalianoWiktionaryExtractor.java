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
public class ItalianoWiktionaryExtractor extends WiktionaryExtractor {

    //TODO: Handle Wikisaurus entries.
	protected final static String languageSectionPatternString = "==\\s*\\{\\{-([^-]*)-\\}\\}\\s*==";
   
	protected final static String sectionPatternString ;
	static {
		 
		sectionPatternString = 
		            new StringBuilder().append("\\{\\{\\s*-")
		            .append("([^\\}\\|\n\r]*)-\\s*(?:\\|([^\\}\n\r]*))?")
		            .append("\\}\\}")
		            .toString();
		
	}
	
	
    private final int NODATA = 0;
    private final int TRADBLOCK = 1;
    private final int DEFBLOCK = 2;
    private final int ORTHOALTBLOCK = 3;
    private final int NYMBLOCK = 4;
    
    
    public ItalianoWiktionaryExtractor(WiktionaryDataHandler wdh) {
        super(wdh);
    }

    // protected final static Pattern languageSectionPattern;
    protected final static Pattern sectionPattern;
    protected final static Pattern linkPattern;
    protected final static Pattern macroPattern;
    protected final static Pattern macroOrLinkPattern;
    protected final static Pattern languageSectionPattern;
    protected final static HashSet<String> posMarkers;
    protected final static HashSet<String> nymMarkers;
    protected final static HashMap<String, String> nymMarkerToNymName;

    static {
    	macroPattern = Pattern.compile(macroPatternString);
        languageSectionPattern = Pattern.compile(languageSectionPatternString);
        linkPattern = Pattern.compile(linkPatternString);
        macroOrLinkPattern = Pattern.compile(macroOrLinkPatternString);
       
        sectionPattern = Pattern.compile(sectionPatternString);
        
        posMarkers = new HashSet<String>(20);
        posMarkers.add("noun");
        posMarkers.add("verb");
        posMarkers.add("adv");
        posMarkers.add("adjc");
        
        nymMarkers = new HashSet<String>(20);
        nymMarkers.add("syn");
        nymMarkers.add("ant");
        nymMarkers.add("hypo");
        nymMarkers.add("hyper");

        
        nymMarkerToNymName = new HashMap<String,String>(20);
        nymMarkerToNymName.put("syn", "syn");
        nymMarkerToNymName.put("ant", "ant");
        nymMarkerToNymName.put("hypo", "hypo");
        nymMarkerToNymName.put("hyper", "hyper");

    }

    int state = NODATA;
    int definitionBlockStart = -1;
    int orthBlockStart = -1;
    int translationBlockStart = -1;
    private int nymBlockStart = -1;
    private String currentNym = null;
    
    //searches for the italian language header
    private boolean keepSearching(Matcher filter) {
    	if (! filter.find()) return false;
    	if (filter.group(1) != null)
    		if (filter.group(1).equals("it"))
    			return false;
    	
    	return true;
    }
    
 
    /* (non-Javadoc)
     * @see org.getalp.dbnary.WiktionaryExtractor#extractData(java.lang.String, org.getalp.blexisma.semnet.SemanticNetwork)
     */
    @Override
    public void extractData() {
        // System.out.println(pageContent);
        Matcher languageFilter = languageSectionPattern.matcher(pageContent);
        while (keepSearching(languageFilter)) {
            ;
        }
        // Either the filter is at end of sequence or on French language header.
        if (languageFilter.hitEnd()) {
            // There is no french data in this page.
            return ;
        }
        int italianSectionStartOffset = languageFilter.end();
        // Advance till end of sequence or new language section
        languageFilter.find();
        int italianSectionEndOffset = languageFilter.hitEnd() ? pageContent.length() : languageFilter.start();
        
        extractItalianData(italianSectionStartOffset, italianSectionEndOffset);
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

    private void extractItalianData(int startOffset, int endOffset) {        
        Matcher m = sectionPattern.matcher(pageContent);
        m.region(startOffset, endOffset);
        // System.err.println(pageContent.substring(startOffset,endOffset));
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
                if (m.group(1).equals("trans1")) {
                    gotoTradBlock(m);
                } else if (posMarkers.contains(m.group(1))) {
                    gotoDefBlock(m);
                } else if (m.group(1).equals("var")) {
                    gotoOrthoAltBlock(m);
                } else if (nymMarkers.contains(m.group(1))) {
                    gotoNymBlock(m);
                } 
                
                break;
            case DEFBLOCK:
                // Iterate until we find a new section
                if (m.group(1).equals("trans1")) {
                    leaveDefBlock(m);
                    gotoTradBlock(m);
                } else if (posMarkers.contains(m.group(1))) {
                    leaveDefBlock(m);
                    gotoDefBlock(m);
                } else if (m.group(1).equals("var")) {
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
                if (m.group(1).equals("trans1")) {
                    leaveTradBlock(m);
                    gotoTradBlock(m);
                } else if (posMarkers.contains(m.group(1))) {
                    leaveTradBlock(m);
                    gotoDefBlock(m);
                } else if (m.group(1).equals("var")) {
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
                if (m.group(1).equals("trans1")) {
                    leaveOrthoAltBlock(m);
                    gotoTradBlock(m);
                } else if (posMarkers.contains(m.group(1))) {
                    leaveOrthoAltBlock(m);
                    gotoDefBlock(m);
                } else if (m.group(1).equals("var")) {
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
                if (m.group(1).equals("trans1")) {
                    leaveNymBlock(m);
                    gotoTradBlock(m);
                } else if (posMarkers.contains(m.group(1))) {
                    leaveNymBlock(m);
                    gotoDefBlock(m);
                } else if (m.group(1).equals("var")) {
                    leaveNymBlock(m);
                    gotoOrthoAltBlock(m);
                } else if (nymMarkers.contains(m.group(1))) {
                    leaveNymBlock(m);
                    gotoNymBlock(m);
                } else {
                    leaveNymBlock(m);
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
        default:
            assert false : "Unexpected state while ending extraction of entry: " + wiktionaryPageName;
        } 
        wdh.finalizeEntryExtraction();
    }
	
    // la fonction de supression des parentheses 

	public static String supParenthese(String s){
		final int A= 0; 
		final int B = 1;

		int ET = A;
		String resultat="";
		int debut =0;
		int fin =0 ;    // la fin de partie qui nous inter
		int i= 0; 

		while(i!=s.length()){
			switch (ET){
			case A:
				if(s.charAt(i)=='('){
					// On a trouvé un debut de parenthese 

					//On place la fin de la partie qui nous interesse
					fin= i;
					//on change d'etat
					ET=B;
					resultat = resultat +s.substring(debut, fin);
				}
				break;
			case B:
				if(s.charAt(i)==')'){
					// On a trouvé la fin du commentaire 

					// on place le debut se le partie qui nous interesse 
					debut= i+1;;
					// on change d'etat 
					ET=A;
				}
				break;

			default:
				System.err.println("Unexpected state number:" + ET);
				break;	
			}

			// On passe au caractère suivant ;
			i=i+1;

		}
		if (i==s.length()) {
			switch (ET){
			case A:
				resultat = resultat +s.substring(debut);
				break;
			case B:
				break;

			default:
				System.err.println("Unexpected state number:" + ET);
				break;	
			}
		}
		return resultat;
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
		.append(macroPatternString)
		.append(")|(?:")
		.append(linkPatternString)
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
    
    
    private void extractTranslations(int startOffset, int endOffset) {
	Matcher macroOrLinkOrcarMatcher = macroOrLinkOrcarPattern.matcher(pageContent);
		macroOrLinkOrcarMatcher.region(startOffset, endOffset);
		int ETAT = INIT;

		String currentGlose = null;
		String lang=null, word= null; 
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
					if (g1.equals("top"))  {
						if (macroOrLinkOrcarMatcher.group(2) != null) {
							currentGlose = macroOrLinkOrcarMatcher.group(2);
						} else {
							currentGlose = null;
						}

					} else if (g1.equals("-trans2-")) {
						currentGlose = null;
					} else if (g1.equals("mid")) {
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
					if (g1.equals("top"))  {
						if (macroOrLinkOrcarMatcher.group(2) != null) {
							currentGlose = macroOrLinkOrcarMatcher.group(2);
						} else {
							currentGlose = null;
						}
						langname = ""; word = ""; usage = "";
						ETAT = INIT;
					} else if (g1.equals("-trans2-")) {
						currentGlose = null;
						langname = ""; word = ""; usage = "";
						ETAT = INIT;
					} else if (g1.equals("mid")) {
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
					if (g1.equals("top"))  {
						if (macroOrLinkOrcarMatcher.group(2) != null) {
							currentGlose = macroOrLinkOrcarMatcher.group(2);
						} else {
							currentGlose = null;
						}
						if (word != null && word.length() != 0) {
							lang=supParenthese(lang);
							wdh.registerTranslation(lang, currentGlose, usage, word);
						}
						langname = ""; word = ""; usage = ""; lang=null;
						ETAT = INIT;
					} else if (g1.equals("-tans2-")) {
						if (word != null && word.length() != 0) {
							lang=supParenthese(lang);
							wdh.registerTranslation(lang, currentGlose, usage, word);
						}
						currentGlose = null;
						langname = ""; word = ""; usage = ""; lang=null;
						ETAT = INIT;
					} else if (g1.equals("mid")) {
						if (word != null && word.length() != 0) {
							lang=supParenthese(lang);
							wdh.registerTranslation(lang, currentGlose, usage, word);
						}
						langname = ""; word = ""; usage = ""; lang = null;
						ETAT = INIT;
					} else {
						usage = usage + "{{" + g1 + "}}";
					}
				} else if (g3!=null) {
					word = g3;
				} else if (g5 != null) {
					//System.err.println("Skipping '*' while in LANGUE state.");
				} else if (g6 != null) {
					if (g6.equals("\n") || g6.equals("\r")) {
						usage = usage.trim();
						// System.err.println("Registering: " + word + ";" + lang + " (" + usage + ") " + currentGlose);
						if (word != null && word.length() != 0) {
							lang=supParenthese(lang);
							wdh.registerTranslation(lang, currentGlose, usage, word);
						}
						lang = null; 
						usage = "";
						word = null;
						ETAT = INIT;
					} else if (g6.equals(",")) {
						usage = usage.trim();
						// System.err.println("Registering: " + word + ";" + lang + " (" + usage + ") " + currentGlose);
						if (word != null && word.length() != 0) {
							lang=supParenthese(lang);
							wdh.registerTranslation(lang, currentGlose, usage, word);
						}
						usage = "";
						word = null;
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
    
}
/**
 * 
 */
package org.getalp.dbnary;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.getalp.blexisma.api.ISO639_3;
import org.getalp.blexisma.api.ISO639_3.Lang;

/**
 * @author serasset
 *
 */
public class SuomiWiktionaryExtractor extends WiktionaryExtractor {

	//TODO: Handle Wikisaurus entries.
	protected final static String sectionPatternString1 =  "={2,5}\\s*([^=]*)\\s*={2,5}";
	protected final static String sectionPatternString2="\\{\\{-([^}]*)-\\}\\}" ;
	protected final static String sectionPatternString;
	protected final static String macroPatternString;
	protected final static String carPatternString;
	protected final static String linkPatternString;
	protected final static String macroOrLinkOrcarPatternString;


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
				.append("([^\\}\\|\n\r]*)(?:\\|([^\\}]*))?")
				.append("\\}\\}")
				.toString();

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


		sectionPatternString = new StringBuilder()
		.append("(?:")
		.append(sectionPatternString1)
		.append(")|(?:")
		.append(sectionPatternString2)
		.append(")|(?:")
		.append("<!--.*-->")
		.append(")").toString();

	}
	protected final static Pattern macroPattern;
	protected final static Pattern linkPattern;
	protected final static Pattern carPattern;
	protected final static Pattern macroOrLinkOrcarPattern;


	static {
		macroPattern = Pattern.compile(macroPatternString);
		linkPattern = Pattern.compile(linkPatternString);
		carPattern = Pattern.compile(carPatternString);
		macroOrLinkOrcarPattern = Pattern.compile(macroOrLinkOrcarPatternString, Pattern.MULTILINE|Pattern.DOTALL);
		
	}

	private final int NODATA = 0;
	private final int TRADBLOCK = 1;
	private final int DEFBLOCK = 2;
	private final int ORTHOALTBLOCK = 3;
	private final int NYMBLOCK = 4;

	public SuomiWiktionaryExtractor(WiktionaryDataHandler wdh) {
		super(wdh);
	}

	// protected final static Pattern languagesectionPattern;
	protected final static Pattern sectionPattern;
	protected final static Pattern languagePattern;
	protected final static Pattern sectionPattern2;
	protected final static HashSet<String> posMarkers;
	protected final static HashSet<String> nymMarkers;
	protected final static HashMap<String, String> nymMarkerToNymName;

	static {
		// languagesectionPattern = Pattern.compile(languagesectionPatternString);

		languagePattern = Pattern.compile(sectionPatternString1);
		sectionPattern2 = Pattern.compile(sectionPatternString2);
		sectionPattern = Pattern.compile(sectionPatternString, Pattern.DOTALL);

		posMarkers = new HashSet<String>(20);
		posMarkers.add("Substantiivi");
		posMarkers.add("Adjektiivi");
		posMarkers.add("adj");
		posMarkers.add("verbi");
		posMarkers.add("subs");
		posMarkers.add("Adverbi");
		posMarkers.add("Verbi");
		posMarkers.add("Erisnimi");


		nymMarkers = new HashSet<String>(20);
		nymMarkers.add("syn");
		nymMarkers.add("vas");
		nymMarkers.add("Synonyymit");
		nymMarkers.add("ala");
		nymMarkers.add("syno");
		nymMarkers.add("vast");


		nymMarkerToNymName = new HashMap<String,String>(20); 
		nymMarkerToNymName.put("syn", "syn");
		nymMarkerToNymName.put("Synonyymit", "syn");
		nymMarkerToNymName.put("syno", "syn");
		nymMarkerToNymName.put("vas", "ant");
		nymMarkerToNymName.put("vast", "ant");
		nymMarkerToNymName.put("ala", "hypo");

		// TODO: metonymie ?


	}



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
	public void afficherLang(){
		Iterator<Lang> it = ISO639_3.sharedInstance.knownLanguagesIterator();
		while (it.hasNext()) {
			Lang l = it.next();
			
		}
	}
	int state = NODATA;
	int definitionBlockStart = -1;
	int orthBlockStart = -1;
	int translationBlockStart = -1;
	private int nymBlockStart = -1;
	private String currentNym = null;

	/* (non-Javadoc)
	 * @see org.getalp.dbnary.WiktionaryExtractor#extractData(java.lang.String, org.getalp.blexisma.semnet.SemanticNetwork)
	 */  // 7630@Override


	public void extractData() {
		
		// System.out.println(pageContent);
		Matcher languageFilter = languagePattern.matcher(pageContent);
		while (languageFilter.find() && ! languageFilter.group(1).equals("Suomi")) {
			;
		}
		// Either the filter is at end of sequence or on suomi language header.
		if (languageFilter.hitEnd()) {
			// There is no Suomi data in this page.
			return ;
		}
		int suomiSectionStartOffset = languageFilter.end();
		// Advance till end of sequence or new language section
		while (languageFilter.find() && languageFilter.group().charAt(2) == '=') {
			;
		}
		// languageFilter.find();
		int suomiSectionEndOffset = languageFilter.hitEnd() ? pageContent.length() : languageFilter.start();

		extractSuomihData(suomiSectionStartOffset, suomiSectionEndOffset);

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
		if(m.group(1)!=null){
			wdh.addPartOfSpeech(m.group(1));
		}else if(m.group(2)!=null){
			wdh.addPartOfSpeech(m.group(2));
		}

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
		if(m.group(2)!=null){
			currentNym = nymMarkerToNymName.get(m.group(2));
			// System.err.println(m.group(2));
		}else if (m.group(1)!=null){
			currentNym = nymMarkerToNymName.get(m.group(1));
			// System.err.println(m.group(1));

		}

		nymBlockStart = m.end();      
	}

	private void leaveNymBlock(Matcher m) {
		extractNyms(currentNym, nymBlockStart, computeRegionEnd(nymBlockStart, m));
		currentNym = null;
		nymBlockStart = -1;         
	}


	private void extractSuomihData(int startOffset, int endOffset) {        
		Matcher m = sectionPattern.matcher(pageContent);
		m.region(startOffset, endOffset);
		//System.err.println(pageContent.substring(startOffset,endOffset));
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
				if (m.group(1)!=null && m.group(1).equals("Käännökset")||(m.group(2)!=null && m.group(2).equals("kään")) ) {
					gotoTradBlock(m);
				} else if ((m.group(1)!=null && posMarkers.contains(m.group(1)))||(m.group(2)!=null && posMarkers.contains(m.group(2)))) {
					gotoDefBlock(m);
				} else if (m.group(2)!=null &&(m.group(2).equals("vaiht-kirj")||m.group(2).equals("vaiht-muo"))) {
					gotoOrthoAltBlock(m);
				} else if ((m.group(1)!=null && nymMarkers.contains(m.group(1)))||(m.group(2)!=null && nymMarkers.contains(m.group(2)))) {
					gotoNymBlock(m);
				} 

				break;
			case DEFBLOCK:
				// Iterate until we find a new section
				if (m.group(1)!=null && m.group(1).equals("Käännökset")||(m.group(2)!=null && m.group(2).equals("kään")) ) {
					leaveDefBlock(m);
					gotoTradBlock(m);
				} else if ((m.group(1)!=null && posMarkers.contains(m.group(1)))||(m.group(2)!=null && posMarkers.contains(m.group(2)))) {
					leaveDefBlock(m);
					gotoDefBlock(m);
				} else if (m.group(1)!=null && (m.group(1).equals("vaiht-kirj")||m.group(1).equals("vaiht-muo"))) {
					leaveDefBlock(m);
					gotoOrthoAltBlock(m);
				} else if ((m.group(1)!=null && nymMarkers.contains(m.group(1)))||(m.group(2)!=null && nymMarkers.contains(m.group(2)))) {
					leaveDefBlock(m);
					gotoNymBlock(m);
				} else {
					leaveDefBlock(m);
					gotoNoData(m);
				}
				break;
			case TRADBLOCK:
				if (m.group(1)!=null && m.group(1).equals("Käännökset")||(m.group(2)!=null && m.group(2).equals("kään")) ) {
					leaveTradBlock(m);
					gotoTradBlock(m);
				} else if ((m.group(1)!=null && posMarkers.contains(m.group(1)))||(m.group(2)!=null && posMarkers.contains(m.group(2)))) {
					leaveTradBlock(m);
					gotoDefBlock(m);
				} else if (m.group(2)!=null &&(m.group(2).equals("vaiht-kirj")||m.group(2).equals("vaiht-muo"))) {
					leaveTradBlock(m);
					gotoOrthoAltBlock(m);
				} else if ((m.group(1)!=null && nymMarkers.contains(m.group(1)))||(m.group(2)!=null && nymMarkers.contains(m.group(2)))) {
					leaveTradBlock(m);
					gotoNymBlock(m);
				} else {
					leaveTradBlock(m);
					gotoNoData(m);
				}
				break;
			case ORTHOALTBLOCK:
				if (m.group(1)!=null && m.group(1).equals("Käännökset")||(m.group(2)!=null && m.group(2).equals("kään")) ) {
					leaveOrthoAltBlock(m);
					gotoTradBlock(m);
				} else if ((m.group(1)!=null && posMarkers.contains(m.group(1)))||(m.group(2)!=null && posMarkers.contains(m.group(2)))) {
					leaveOrthoAltBlock(m);
					gotoDefBlock(m);
				} else if (m.group(2)!=null &&(m.group(2).equals("vaiht-kirj")||m.group(2).equals("vaiht-muo"))) {
					leaveOrthoAltBlock(m);
					gotoOrthoAltBlock(m);
				} else if ((m.group(1)!=null && nymMarkers.contains(m.group(1)))||(m.group(2)!=null && nymMarkers.contains(m.group(2)))) {
					leaveOrthoAltBlock(m);
					gotoNymBlock(m);
				} else {
					leaveOrthoAltBlock(m);
					gotoNoData(m);
				}
				break;
			case NYMBLOCK:
				if (m.group(1)!=null && m.group(1).equals("Käännökset")||(m.group(2)!=null && m.group(2).equals("kään")) ) {
					leaveNymBlock(m);
					gotoTradBlock(m);
				} else if ((m.group(1)!=null && posMarkers.contains(m.group(1)))||(m.group(2)!=null && posMarkers.contains(m.group(2)))) {
					leaveNymBlock(m);
					gotoDefBlock(m);
				} else if (m.group(2)!=null &&(m.group(2).equals("vaiht-kirj")||m.group(2).equals("vaiht-muo"))) {
					leaveNymBlock(m);
					gotoOrthoAltBlock(m);
				} else if ((m.group(1)!=null && nymMarkers.contains(m.group(1)))||(m.group(2)!=null && nymMarkers.contains(m.group(2)))) {
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

	


	private void extractTranslations(int startOffset, int endOffset) {
		Matcher macroOrLinkOrcarMatcher = macroOrLinkOrcarPattern.matcher(pageContent);
		macroOrLinkOrcarMatcher.region(startOffset, endOffset);
		final int ZERO = 0;
		final int INIT = 1;
		final int LANGUE = 2;
		final int TRAD = 3;

	
		
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
					if (g1.equalsIgnoreCase("ylä"))  {
						if (macroOrLinkOrcarMatcher.group(2) != null) {
							currentGlose = macroOrLinkOrcarMatcher.group(2);
						} else {
							currentGlose = null;
						}

					} else if (g1.equalsIgnoreCase("ala")) {
						currentGlose = null;
					} else if (g1.equalsIgnoreCase("keski")) {
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
					if (g1.equalsIgnoreCase("ylä"))  {
						if (macroOrLinkOrcarMatcher.group(2) != null) {
							currentGlose = macroOrLinkOrcarMatcher.group(2);
						} else {
							currentGlose = null;
						}
						langname = ""; word = ""; usage = "";
						ETAT = INIT;
					} else if (g1.equalsIgnoreCase("ala")) {
						currentGlose = null;
						langname = ""; word = ""; usage = "";
						ETAT = INIT;
					} else if (g1.equalsIgnoreCase("keski")) {
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
						lang=supParenthese(lang);
						lang =SuomiLangToCode.triletterCode(lang);
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
					if (g1.equalsIgnoreCase("ylä"))  {
						if (macroOrLinkOrcarMatcher.group(2) != null) {
							currentGlose = macroOrLinkOrcarMatcher.group(2);
						} else {
							currentGlose = null;
						}
						if (word != null && word.length() != 0) {
							if(lang!=null){
								wdh.registerTranslation(lang, currentGlose, usage, word);
							}
						}
						langname = ""; word = ""; usage = ""; lang=null;
						ETAT = INIT;
					} else if (g1.equalsIgnoreCase("ala")) {
						if (word != null && word.length() != 0) {
							if(lang!=null){
								wdh.registerTranslation(lang, currentGlose, usage, word);
							}
						}
						currentGlose = null;
						langname = ""; word = ""; usage = ""; lang=null;
						ETAT = INIT;
					} else if (g1.equalsIgnoreCase("kski")) {
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
					word = g3;
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
						word = null;
						ETAT = INIT;
					} else if (g6.equals(",")) {
						usage = usage.trim();
						// System.err.println("Registering: " + word + ";" + lang + " (" + usage + ") " + currentGlose);
						if (word != null && word.length() != 0) {
							if(lang!=null){
								wdh.registerTranslation(lang, currentGlose, usage, word);
							}
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

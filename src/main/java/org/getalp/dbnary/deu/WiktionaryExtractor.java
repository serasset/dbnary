package org.getalp.dbnary.deu;

import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.getalp.blexisma.api.ISO639_3;
import org.getalp.dbnary.AbstractWiktionaryExtractor;
import org.getalp.dbnary.WiktionaryDataHandler;
import org.getalp.dbnary.wiki.WikiPatterns;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WiktionaryExtractor extends AbstractWiktionaryExtractor {


	private Logger log = LoggerFactory.getLogger(WiktionaryExtractor.class);

	protected final static String senseNumberRegExp = "(?:(?:(?:<tt>)?[IV]+(?:</tt>)?|\\d)*\\.?[abcdefghijklmn]?)";
	protected final static String senseNumberOrRangeRegExp = "(?:(?:(?:<tt>)?[IV]+(?:</tt>)?|\\d|-|–|–|,| |&nbsp;)*\\.?[abcdefghij]?)";
	protected static final String simpleNumListFilter = "^\\s*(" + senseNumberRegExp +"(?:\\s*[\\,\\-–]\\s*" + senseNumberRegExp +")*)\\s*$";

	protected final static String languageSectionPatternString = "={2}\\s*([^\\(]*)\\(\\{\\{Sprache\\|([^\\}]*)\\}\\}\\s*\\)\\s*={2}";
	protected final static String partOfSpeechPatternString = "={3}[^\\{]*\\{\\{Wortart\\|([^\\}\\|]*)(?:\\|([^\\}]*))?\\}\\}.*={3}";
	protected final static String subSection4PatternString = "={4}\\s*(.*)\\s*={4}";
	protected final static String germanDefinitionPatternString = "^:{1,3}\\s*(?:\\[(" + senseNumberRegExp + "*)\\])?([^\n\r]*)$";
	protected final static String germanNymLinePatternString = "^:{1,3}\\s*(?:\\[(" + senseNumberOrRangeRegExp + "*)\\])?([^\n\r]*)$";

	private final int NODATA = 0;
	private final int TRADBLOCK = 1;
	private final int DEFBLOCK = 2;
	private final int ORTHOALTBLOCK = 3;
	private final int NYMBLOCK = 4;

	public WiktionaryExtractor(WiktionaryDataHandler wdh) {
		super(wdh);
	}

	// protected final static Pattern languageSectionPattern;
	
	protected final static String macroOrPOSPatternString;
	protected final static Pattern languageSectionPattern;
	protected final static Pattern germanDefinitionPattern;
	protected final static Pattern germanNymLinePattern;
	protected final static String multilineMacroPatternString;
	protected final static Pattern macroOrPOSPattern; // Combine macro pattern
	// and pos pattern.
	protected final static HashSet<String> posMarkers;
	protected final static HashSet<String> ignorableSectionMarkers;
	protected final static HashSet<String> nymMarkers;
	protected final static HashMap<String, String> nymMarkerToNymName;

	static {
		// languageSectionPattern =
		// Pattern.compile(languageSectionPatternString);

		languageSectionPattern = Pattern.compile(languageSectionPatternString);
		multilineMacroPatternString = 
				new StringBuilder().append("\\{\\{")
				.append("([^\\}\\|]*)(?:\\|([^\\}]*))?")
				.append("\\}\\}")
				.toString();



		macroOrPOSPatternString = new StringBuilder()
		.append("(?:").append(WikiPatterns.macroPatternString)
		.append(")|(?:").append(partOfSpeechPatternString)
		.append(")|(?:").append(subSection4PatternString).append(")")
		.append("|(?:").append(multilineMacroPatternString).append(")")
		.toString();

		macroOrPOSPattern = Pattern.compile(macroOrPOSPatternString);
		germanDefinitionPattern = Pattern.compile(germanDefinitionPatternString, Pattern.MULTILINE);
		germanNymLinePattern = Pattern.compile(germanNymLinePatternString, Pattern.MULTILINE);
		
		posMarkers = new HashSet<String>(20);
		posMarkers.add("Substantiv"); // Should I get the
		// Toponym/Vorname/Nachname additional
		// info ?
		posMarkers.add("Adjektiv");
		posMarkers.add("Absolutadjektiv");
		posMarkers.add("Partizip");
		posMarkers.add("Adverb");
		posMarkers.add("Wortverbindung");
		posMarkers.add("Verb");

		ignorableSectionMarkers = new HashSet<String>(20);
		ignorableSectionMarkers.add("Silbentrennung");
		ignorableSectionMarkers.add("Aussprache");
		ignorableSectionMarkers.add("Herkunft");
		ignorableSectionMarkers.add("Gegenworte");
		ignorableSectionMarkers.add("Beispiele");
		ignorableSectionMarkers.add("Redewendungen");
		ignorableSectionMarkers.add("Abgeleitete Begriffe");
		ignorableSectionMarkers.add("Charakteristische Wortkombinationen");
		ignorableSectionMarkers.add("Dialektausdrücke (Deutsch)");
		ignorableSectionMarkers.add("Referenzen");
		ignorableSectionMarkers.add("Ähnlichkeiten");
		ignorableSectionMarkers.add("Anmerkung");
		ignorableSectionMarkers.add("Anmerkungen");
		ignorableSectionMarkers.add("Alte Rechtschreibung"); // TODO: Integrate
		// these in
		// alternative
		// spelling ?
		ignorableSectionMarkers.add("Nebenformen");
		ignorableSectionMarkers.add("Vokalisierung");
		ignorableSectionMarkers.add("Grammatische Merkmale");
		ignorableSectionMarkers.add("Abkürzungen"); // TODO: Integrate these in
		// alternative spelling ?
		ignorableSectionMarkers.add("Sinnverwandte Wörter"); // TODO: related
		// words (should I
		// keep it ?)
		ignorableSectionMarkers.add("Weibliche Wortformen");
		ignorableSectionMarkers.add("Männliche Wortformen");
		ignorableSectionMarkers.add("Verkleinerungsformen"); // TODO:
		// Diminutif...
		// qu'en faire ?
		ignorableSectionMarkers.add("Vergrößerungsformen");
		ignorableSectionMarkers.add("Kurzformen");
		ignorableSectionMarkers.add("Koseformen");
		ignorableSectionMarkers.add("Kurz- und Koseformen");
		ignorableSectionMarkers.add("Namensvarianten");
		ignorableSectionMarkers.add("Weibliche Namensvarianten");
		ignorableSectionMarkers.add("Männliche Namensvarianten");
		ignorableSectionMarkers.add("Bekannte Namensträger");
		ignorableSectionMarkers.add("Sprichwörter");
		ignorableSectionMarkers.add("Charakteristische Wortkombinationen");
		ignorableSectionMarkers.add("Abgeleitete Begriffe");

		nymMarkers = new HashSet<String>(20);
		nymMarkers.add("Synonyme");
		nymMarkers.add("Gegenwörter");
		nymMarkers.add("Gegenworte");
		nymMarkers.add("Oberbegriffe");
		nymMarkers.add("Unterbegriffe");
		nymMarkers.add("Meronyms"); // TODO: Any meronym/metonym info in German
		// ?

		nymMarkerToNymName = new HashMap<String, String>(20);
		nymMarkerToNymName.put("Synonyme", "syn");
		nymMarkerToNymName.put("Gegenwörter", "ant");
		nymMarkerToNymName.put("Gegenworte", "ant");
		nymMarkerToNymName.put("Unterbegriffe", "hypo");
		nymMarkerToNymName.put("Oberbegriffe", "hyper");
		nymMarkerToNymName.put("Meronyms", "mero");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.getalp.dbnary.WiktionaryExtractor#extractData(java.lang
	 * .String, org.getalp.blexisma.semnet.SemanticNetwork)
	 */
	@Override
	public void extractData() {
		// System.out.println(pageContent);
		Matcher languageFilter = languageSectionPattern.matcher(pageContent);
		while (languageFilter.find() && !isGermanLanguageHeader(languageFilter)) {
			;
		}
		// Either the filter is at end of sequence or on German language header.
		if (languageFilter.hitEnd()) {
			// There is no German data in this page.
			return;
		}
		int germanSectionStartOffset = languageFilter.end();
		// Advance till end of sequence or new language section
		// WHY filter on section level ?: while (languageFilter.find() && (languageFilter.start(1) - languageFilter.start()) != 2) {
		languageFilter.find();
		// languageFilter.find();
		int germanSectionEndOffset = languageFilter.hitEnd() ? pageContent.length() : languageFilter.start();

		extractGermanData(germanSectionStartOffset, germanSectionEndOffset);
	}
	
	private boolean isGermanLanguageHeader(Matcher m) {
		return m.group(2).equals("Deutsch");
	}

	
	/**
	 * @uml.property  name="state"
	 */
	int state = NODATA;
	/**
	 * @uml.property  name="definitionBlockStart"
	 */
	int definitionBlockStart = -1;
	/**
	 * @uml.property  name="orthBlockStart"
	 */
	int orthBlockStart = -1;
	/**
	 * @uml.property  name="translationBlockStart"
	 */
	int translationBlockStart = -1;
	/**
	 * @uml.property  name="nymBlockStart"
	 */
	private int nymBlockStart = -1;
	/**
	 * @uml.property  name="currentNym"
	 * @uml.associationEnd  qualifier="key:java.lang.String java.lang.String"
	 */
	private String currentNym = null;

	void gotoNoData(Matcher m) {
		state = NODATA;
	}

	void gotoTradBlock(Matcher m) {
		translationBlockStart = m.end();
		state = TRADBLOCK;
	}

	void registerNewPartOfSpeech(Matcher m) {
		// if (m.group(4) != null && ! m.group(4).equals("Deutsch"))
		//	System.err.println("lang = " + m.group(4) + " where pos = " + m.group(3) + " in page: " + wiktionaryPageName);
		// TODO: language in group 4 is the language of origin of the entry. Maybe we should keep it.
		// TODO: filter out ignorable part of speech;
		wdh.addPartOfSpeech(m.group(3));
	}

	void gotoDefBlock(Matcher m) {
		state = DEFBLOCK;
		definitionBlockStart = m.end();
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

	// TODO: Prise en compte des diminutifs (Verkleinerungsformen)
	// TODO: Prise en compte des "concepts dérivés" ? (Abgeleitete Begriffe)
	// TODO: supprimer les "Deklinierte Form" des catégories extraites.
	private void extractGermanData(int startOffset, int endOffset) {
		Matcher m = macroOrPOSPattern.matcher(pageContent);
		m.region(startOffset, endOffset);
		wdh.initializeEntryExtraction(wiktionaryPageName);
		wdh.setWiktionaryIndex(wi);
		gotoNoData(m);
		while (m.find()) {
			switch (state) {
			case NODATA:
				if (m.group(1) != null) {
					// It's a macro
					if (m.group(1).equals("Bedeutungen")) {
						// Definitions
						gotoDefBlock(m);
					} else if (m.group(1).equals("Alternative Schreibweisen")) {
						// Alternative spelling
						gotoOrthoAltBlock(m);
					} else if (nymMarkers.contains(m.group(1))) {
						// Nyms
						gotoNymBlock(m);
					} else if (ignorableSectionMarkers.contains(m.group(1))) {
						gotoNoData(m);
					}
				} else if (m.group(3) != null) {
					// partOfSpeech
					registerNewPartOfSpeech(m);
				} else if (m.group(5) != null) {
					// translations
					if (m.group(5).trim().equals("Übersetzungen")) {
						gotoTradBlock(m);
					}
				} else {
					// Multiline macro
					// System.out.println(m.group());
				}

				break;
			case DEFBLOCK:
				if (m.group(1) != null) {
					// It's a macro
					if (m.group(1).equals("Bedeutungen")) {
						// Definitions
						leaveDefBlock(m);
						gotoDefBlock(m);
					} else if (m.group(1).equals("Alternative Schreibweisen")) {
						// Alternative spelling
						leaveDefBlock(m);
						gotoOrthoAltBlock(m);
					} else if (nymMarkers.contains(m.group(1))) {
						// Nyms
						leaveDefBlock(m);
						gotoNymBlock(m);
					} else if (ignorableSectionMarkers.contains(m.group(1))) {
						leaveDefBlock(m);
						gotoNoData(m);
					}
				} else if (m.group(3) != null) {
					// partOfSpeech
					leaveDefBlock(m);
					registerNewPartOfSpeech(m);
					gotoNoData(m);
				} else if (m.group(5) != null) {
					// translations
					if (m.group(5).trim().equals("Übersetzungen")) {
						leaveDefBlock(m);
						gotoTradBlock(m);
					}
				} else {
					// Multiline macro
					// System.out.println(m.group());
				}

				break;
			case TRADBLOCK:
				if (m.group(1) != null) {
					// It's a macro
					if (m.group(1).equals("Bedeutungen")) {
						// Definitions
						leaveTradBlock(m);
						gotoDefBlock(m);
					} else if (m.group(1).equals("Alternative Schreibweisen")) {
						// Alternative spelling
						leaveTradBlock(m);
						gotoOrthoAltBlock(m);
					} else if (nymMarkers.contains(m.group(1))) {
						// Nyms
						leaveTradBlock(m);
						gotoNymBlock(m);
					} else if (ignorableSectionMarkers.contains(m.group(1))) {
						leaveTradBlock(m);
						gotoNoData(m);
					}
				} else if (m.group(3) != null) {
					// partOfSpeech
					leaveTradBlock(m);
					registerNewPartOfSpeech(m);
					gotoNoData(m);
				} else if (m.group(5) != null) {
					// translations
					if (m.group(5).trim().equals("Übersetzungen")) {
						leaveTradBlock(m);
						gotoTradBlock(m);
					}
				} else {
					// Multiline macro
					// System.out.println(m.group());
				}

				break;
			case ORTHOALTBLOCK:
				if (m.group(1) != null) {
					// It's a macro
					if (m.group(1).equals("Bedeutungen")) {
						// Definitions
						leaveOrthoAltBlock(m);
						gotoDefBlock(m);
					} else if (m.group(1).equals("Alternative Schreibweisen")) {
						// Alternative spelling
						leaveOrthoAltBlock(m);
						gotoOrthoAltBlock(m);
					} else if (nymMarkers.contains(m.group(1))) {
						// Nyms
						leaveOrthoAltBlock(m);
						gotoNymBlock(m);
					} else if (ignorableSectionMarkers.contains(m.group(1))) {
						leaveOrthoAltBlock(m);
						gotoNoData(m);
					}
				} else if (m.group(3) != null) {
					// partOfSpeech
					leaveOrthoAltBlock(m);
					registerNewPartOfSpeech(m);
					gotoNoData(m);
				} else if (m.group(5) != null) {
					// translations
					if (m.group(5).trim().equals("Übersetzungen")) {
						leaveOrthoAltBlock(m);
						gotoTradBlock(m);
					}
				} else {
					// Multiline macro
					// System.out.println(m.group());
				}

				break;
			case NYMBLOCK:
				// ICI
				if (m.group(1) != null) {
					// It's a macro
					if (m.group(1).equals("Bedeutungen")) {
						// Definitions
						leaveNymBlock(m);
						gotoDefBlock(m);
					} else if (m.group(1).equals("Alternative Schreibweisen")) {
						// Alternative spelling
						leaveNymBlock(m);
						gotoOrthoAltBlock(m);
					} else if (nymMarkers.contains(m.group(1))) {
						// Nyms
						leaveNymBlock(m);
						gotoNymBlock(m);
					} else if (ignorableSectionMarkers.contains(m.group(1))) {
						leaveNymBlock(m);
						gotoNoData(m);
					}
				} else if (m.group(3) != null) {
					// partOfSpeech
					leaveNymBlock(m);
					registerNewPartOfSpeech(m);
					gotoNoData(m);
				} else if (m.group(5) != null) {
					// translations
					if (m.group(5).trim().equals("Übersetzungen")) {
						leaveNymBlock(m);
						gotoTradBlock(m);
					}
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
			assert false : "Unexpected state while extracting translations from dictionary.";
		}
		wdh.finalizeEntryExtraction();
	}

	static final String glossOrMacroPatternString;
	static final Pattern glossOrMacroPattern;

	static{


		// glossOrMacroPatternString = "(?:\\[([^\\]]*)\\])|(?:\\{\\{([^\\}\\|]*)\\|([^\\}\\|]*)\\|([^\\}\\|]*)\\|?([^\\}]*)\\}\\})";
		glossOrMacroPatternString = "(?:\\[([^\\]]*)\\])|(?:\\{\\{([^\\}\\|]*)\\|([^\\}\\|]*)\\|([^\\}\\|]*)\\|?([^\\}]*)\\}\\})";


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
				String g4 = macroMatcher.group(5);


				if (g1.equals("Ü") || g1.equals("Üxx")) {
					String lang;
					String word = null;
					String trans1 = null;
					String trans2 = null;
					String transcription = null;

					lang = g2;
					// normalize language code
					String normLangCode;
					if ((normLangCode = ISO639_3.sharedInstance.getIdCode(lang)) != null) {
						lang = normLangCode;
					}

					// Extract word and transcription
					// there are three case with 5 "|" : 1-"{{ .. }}'' or 2-"" [[ .. ]]"" or 3-just "|"


					int i1,i2, i3;
					int i4 = 0; 
					int i5 = 0;
					if (g4 != null && (i1 = g4.indexOf('|')) != -1 && (i3 = g4.indexOf('|', i1+1)) == -1 ) { // only 5 "|" 

						if ((i4 = g4.indexOf(']')) != -1 && (i5 = g3.indexOf('[')) != -1 ) {
							i1 = g4.indexOf('|', i4); // the {{..}} can contain more than 1 "|", since the word is after the {{..}}, we ignore the others "|" and match the last one
							trans1 = g3.substring(i5+1);
							trans2 = g4.substring(0, i4+1);
							transcription = trans1 + "|" + trans2;
							word = g4.substring(i1+1);

						} else if (g4 != null && g4.equals("")) {
							word = g3;

						} else {
							transcription =g3;
							word = g4;
						}
					}

					if (g4 != null && g4.equals("")) {
						word = g3;

					} else {
						transcription =g3;
						word = g4;
					}

					lang=GermanLangToCode.triletterCode(lang);
					if(lang!=null){
						wdh.registerTranslation(lang, currentGlose, transcription, word);
					}

				} else if (g1.equals("Ü-links")) {
					// German wiktionary does not provide a glose to disambiguate.
					// Just ignore this marker.
				} else if (g1.equals("Ü-Abstand")) {
					// just ignore it
				} else if (g1.equals("Ü-rechts")) {
					// Forget the current gloss
					currentGlose = null;
				}

			}
		}
	}

    @Override
	protected void extractNyms(String synRelation, int startOffset, int endOffset) {

		Matcher nymLineMatcher = germanNymLinePattern.matcher(this.pageContent);
		nymLineMatcher.region(startOffset, endOffset);

		String currentLevel1SenseNumber = "";
		String currentLevel2SenseNumber = "";
		while (nymLineMatcher.find()) {
			String nymLine = nymLineMatcher.group(2);
			String senseNum = nymLineMatcher.group(1);
			if (null != senseNum) {
				senseNum = senseNum.trim();
				senseNum = senseNum.replaceAll("<[^>]*>", "");
				if (nymLineMatcher.group().length() >= 2 && nymLineMatcher.group().charAt(1) == ':') {
					if (nymLineMatcher.group().length() >= 3 && nymLineMatcher.group().charAt(2) == ':') {
						// Level 3
						log.debug("Level 3 definition: \"{}\" in entry {}", nymLineMatcher.group(), this.wiktionaryPageName);
						if (! senseNum.startsWith(currentLevel2SenseNumber)) {
							senseNum = currentLevel2SenseNumber + senseNum;
						}
						log.debug("Sense number is: {}", senseNum);
					} else {
						// Level 2
						// log.debug("Level 2 definition: \"{}\" in entry {}", definitionMatcher.group(), this.wiktionaryPageName);
						if (! senseNum.startsWith(currentLevel1SenseNumber)) {
							senseNum = currentLevel1SenseNumber + senseNum;
						}
						currentLevel2SenseNumber = senseNum;
					}
				} else {
					// Level 1 definition
					currentLevel1SenseNumber = senseNum;
					currentLevel2SenseNumber = senseNum;
				}
			}
			
			if (nymLine != null && !nymLine.equals("")) {
				
				Matcher linkMatcher = WikiPatterns.linkPattern.matcher(nymLine);
		        while (linkMatcher.find()) {
		            // It's a link, only keep the alternate string if present.
		            String leftGroup = linkMatcher.group(1) ;
		            if (leftGroup != null && ! leftGroup.equals("") && 
		            		! leftGroup.startsWith("Wikisaurus:") &&
		            		! leftGroup.startsWith("Catégorie:") &&
		            		! leftGroup.startsWith("#")) {
		            	if (null == senseNum) {
		            		wdh.registerNymRelation(leftGroup, synRelation);  
		            	} else {
		            		wdh.registerNymRelation(leftGroup, synRelation, senseNum);  
		            	}
		            }
		        }      
				
			}
		}
		
	
    }


	@Override
	protected void extractDefinitions(int startOffset, int endOffset) {
		// TODO: The definition pattern is the only one that changes. Hence, we should normalize this processing and put the macro in language specific parameters.
		Matcher definitionMatcher = germanDefinitionPattern.matcher(this.pageContent);
		definitionMatcher.region(startOffset, endOffset);
		
		String currentLevel1SenseNumber = "";
		String currentLevel2SenseNumber = "";
		while (definitionMatcher.find()) {
			String def = cleanUpMarkup(definitionMatcher.group(2));
			String senseNum = definitionMatcher.group(1);
			if (null == senseNum) {
				log.debug("Null sense number in definition\"{}\" for entry {}", def, this.wiktionaryPageName);
			} else {
				senseNum = senseNum.trim();
				senseNum = senseNum.replaceAll("<[^>]*>", "");
				if (definitionMatcher.group().length() >= 2 && definitionMatcher.group().charAt(1) == ':') {
					if (definitionMatcher.group().length() >= 3 && definitionMatcher.group().charAt(2) == ':') {
						// Level 3
						log.debug("Level 3 definition: \"{}\" in entry {}", definitionMatcher.group(), this.wiktionaryPageName);
						if (! senseNum.startsWith(currentLevel2SenseNumber)) {
							senseNum = currentLevel2SenseNumber + senseNum;
						}
						log.debug("Sense number is: {}", senseNum);
					} else {
						// Level 2
						// log.debug("Level 2 definition: \"{}\" in entry {}", definitionMatcher.group(), this.wiktionaryPageName);
						if (! senseNum.startsWith(currentLevel1SenseNumber)) {
							senseNum = currentLevel1SenseNumber + senseNum;
						}
						currentLevel2SenseNumber = senseNum;
					}
				} else {
					// Level 1 definition
					currentLevel1SenseNumber = senseNum;
					currentLevel2SenseNumber = senseNum;
				}
				
				if (def != null && !def.equals("")) {
					wdh.registerNewDefinition(definitionMatcher.group(2), senseNum);
				}
			}
		}
	}



}

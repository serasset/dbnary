package org.getalp.dbnary.deu;

import java.awt.PageAttributes.OriginType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.getalp.dbnary.LangTools;
import org.getalp.dbnary.AbstractWiktionaryExtractor;
import org.getalp.dbnary.LemonOnt;
import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.WiktionaryDataHandler;
import org.getalp.dbnary.wiki.WikiPatterns;
import org.getalp.dbnary.wiki.WikiTool;
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
//	protected final static HashSet<String> inflectionMarkers;

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
	 * @uml.property  name="currentNym"
	 * @uml.associationEnd  qualifier="key:java.lang.String java.lang.String"
	 */


	void registerNewPartOfSpeech(Matcher m) {
		// if (m.group(4) != null && ! m.group(4).equals("Deutsch"))
		//	System.err.println("lang = " + m.group(4) + " where pos = " + m.group(3) + " in page: " + wiktionaryPageName);
		// TODO: language in group 4 is the language of origin of the entry. Maybe we should keep it.
		// TODO: filter out ignorable part of speech;
		wdh.addPartOfSpeech(m.group(3));
	}

	// TODO: Prise en compte des diminutifs (Verkleinerungsformen)
	// TODO: Prise en compte des "concepts dérivés" ? (Abgeleitete Begriffe)
	// TODO: supprimer les "Deklinierte Form" des catégories extraites.
	
	private enum Block {NOBLOCK, IGNOREPOS, TRADBLOCK, DEFBLOCK, INFLECTIONBLOCK, ORTHOALTBLOCK, NYMBLOCK};

	private Block currentBlock=Block.NOBLOCK;

	int blockStart=-1;
	
	private String currentNym = null;

	
	
	private void extractGermanData(int startOffset, int endOffset){
		Matcher m = macroOrPOSPattern.matcher(pageContent);
		m.region(startOffset, endOffset);
		wdh.initializeEntryExtraction(wiktionaryPageName);
		wdh.setWiktionaryIndex(wi);

		currentBlock=Block.NOBLOCK;
		
		while(m.find()){
//			for(int i=0;i<m.groupCount();i++){
//				System.out.println(i+":"+m.group(i));
//			}
//			System.out.println(currentBlock);
//			System.out.println(m.group());
			if (null != m.group(1) ) {
				//go to the good block
				if (m.group(1).equals("Bedeutungen")) {
					leaveCurrentBlock(m);
					// go to DefBlock
					currentBlock=Block.DEFBLOCK;
					blockStart=m.end();
				} else if (m.group(1).equals("Alternative Schreibweisen")) {
					leaveCurrentBlock(m);
					//go to OrthoAltBlock
					currentBlock=Block.ORTHOALTBLOCK;
					blockStart = m.end();					
				} else if (nymMarkers.contains(m.group(1))) {
					leaveCurrentBlock(m);
					//go to NymBlock
					currentBlock=Block.NYMBLOCK;
					currentNym = nymMarkerToNymName.get(m.group(1));
					blockStart = m.end();
				} else if (ignorableSectionMarkers.contains(m.group(1))) {
					leaveCurrentBlock(m);
					//go to NODATA
					currentBlock=Block.NOBLOCK;
					//inflection block
				} else if(m.group(1).contains("Deutsch")){
						leaveCurrentBlock(m);
						
						currentBlock=Block.INFLECTIONBLOCK;
						blockStart=m.start();
				} else {
				}
			} else if (null != m.group(3)) {
				//part Of speech
				leaveCurrentBlock(m);
				registerNewPartOfSpeech(m);
				//go to the NODATA block
				currentBlock=Block.NOBLOCK;
			} else if (null != m.group(5)) {
				if (m.group(5).trim().equals("Übersetzungen")) {
					leaveCurrentBlock(m);
					//go to the trad block
					currentBlock=Block.TRADBLOCK;
					blockStart = m.end();
				}
				//inflection block
			} else if (null != m.group(6)) {
					if(m.group(6).contains("Deutsch")){
						leaveCurrentBlock(m);
						
						currentBlock=Block.INFLECTIONBLOCK;
						blockStart=m.start();
					}
			} else {
			}
	 	}
		leaveCurrentBlock(m);
		wdh.finalizeEntryExtraction();

			
	}

	private void leaveCurrentBlock(Matcher m){
		if (blockStart == -1) {
				return;
		}
		
			int end = computeRegionEnd(blockStart, m);
			switch (currentBlock) {
				case NOBLOCK:
				case IGNOREPOS:
					break;
				case DEFBLOCK:
					extractDefinitions(blockStart, end);
					break;
				case TRADBLOCK:
					extractTranslations(blockStart, end);
					break;
				case ORTHOALTBLOCK:
					extractOrthoAlt(blockStart, end);
					break;
				case NYMBLOCK:
					extractNyms(currentNym, blockStart, end);
					currentNym = null;
					break;
				case INFLECTIONBLOCK:
		 			extractInflections(blockStart, end);
		 			blockStart=end;
					break;
				default:
					assert false : "Unexpected block while ending extraction of entry: " + wiktionaryPageName;
			}

			blockStart = -1;
		
	}
	
		private static HashSet<String> verbMarker;
	static{
		verbMarker=new HashSet<String>();
		verbMarker.add("Verb");
		verbMarker.add("Hilfsverb");
		
		
		
	}
	private void extractInflections(int startOffset, int endOffset){
		String page=lexEntryToPage(wiktionaryPageName);
		String normalizedPOS=wdh.currentWiktionaryPos();
		//if the currentEntry has a page of conjugation or declination
		GermanExtractorWikiModel gewm = new GermanExtractorWikiModel(wdh, wi, new Locale("de"), "/${Bild}", "/${Titel}");
		if(null!=page){
			if(verbMarker.contains(normalizedPOS)){
				gewm.parseConjugation(page, normalizedPOS);
			}
			else{
				gewm.parseDeclination(page, normalizedPOS);
			}
		}
		else{
		Pattern pattern=Pattern.compile(macroOrPOSPatternString);
		Matcher m=pattern.matcher(pageContent.substring(startOffset, endOffset));
			if(m.find()){
				gewm.parseOtherForm(m.group(0), normalizedPOS);
			}
		}
		
		
		
		
	}
	
	private final static String germanDeclinationSuffix =" (Deklination)";
	private final static String germanConjugationSuffix =" (Konjugation)";
	
	private String lexEntryToPage(String lexEntry){
		int i=0;
		String[] suffix={germanConjugationSuffix,germanDeclinationSuffix};
		String pageContent = null;

			while(null==pageContent && i< suffix.length){
				pageContent=wi.getTextOfPage(lexEntry+suffix[i]);
				i++;
			}
		return pageContent;
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

					lang = LangTools.normalize(g2);

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

					lang=GermanLangToCode.threeLettersCode(lang);
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
//					System.out.println(senseNum+":"+definitionMatcher.group(2));
					wdh.registerNewDefinition(definitionMatcher.group(2), senseNum);
				}
			}
		}
	}

	public void extractOtherForms( int start, int end){
//		Matcher otherFormMatcher = otherFormPattern.matcher(pageContent);
//		otherFormMatcher.region(start, end);
//		GermanExtractorWikiModel gewm = new
//		while(otherFormMatcher.find()){
//		}
	}
	


}

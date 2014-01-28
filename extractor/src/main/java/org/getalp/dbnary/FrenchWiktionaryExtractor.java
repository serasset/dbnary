/**
 * 
 */
package org.getalp.dbnary;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.getalp.blexisma.api.ISO639_3;
import org.getalp.dbnary.wiki.WikiPatterns;
import org.getalp.dbnary.wiki.WikiTool;

/**
 * @author serasset
 *
 */
public class FrenchWiktionaryExtractor extends AbstractWiktionaryExtractor {

	// NOTE: to subclass the extractor, you need to define how a language section is recognized.
	// then, how are sections recognized and what is their semantics.
	// then, how to extract specific elements from the particular sections
    protected final static String languageSectionPatternString;

    protected final static String languageSectionPatternString1 = "==\\s*\\{\\{=([^=]*)=\\}\\}\\s*==";
    protected final static String languageSectionPatternString2 = "==\\s*\\{\\{langue\\|([^\\}]*)\\}\\}\\s*==";
    // TODO: handle morphological informations e.g. fr-rég template ?
    protected final static String pronounciationPatternString = "\\{\\{pron\\|([^\\|\\}]*)(.*)\\}\\}";
    
    private final int NODATA = 0;
    private final int TRADBLOCK = 1;
    protected final int DEFBLOCK = 2;
    private final int ORTHOALTBLOCK = 3;
    private final int NYMBLOCK = 4;
    private final int PRONBLOCK = 5;
	private final int IGNOREPOS = 6;

    private static HashSet<String> posMarkers;
    private static HashSet<String> ignorablePosMarkers;
    private static HashSet<String> sectionMarkers;
    
    private final static HashMap<String, String> nymMarkerToNymName;
    
    private static HashSet<String> unsupportedMarkers = new HashSet<String>();

    
    // private static Set<String> affixesToDiscardFromLinks = null;
    
    static {
    	languageSectionPatternString = new StringBuilder()
        .append("(?:")
        .append(languageSectionPatternString1)
        .append(")|(?:")
        .append(languageSectionPatternString2)
        .append(")").toString();
        
        posMarkers = new HashSet<String>(130);
        ignorablePosMarkers = new HashSet<String>(130);

        posMarkers.add("-déf-");
        posMarkers.add("-déf-/2");
        posMarkers.add("-déf2-");
        posMarkers.add("--");
        posMarkers.add("-adj-");
        posMarkers.add("-adj-/2");
        ignorablePosMarkers.add("-flex-adj-indéf-");
        posMarkers.add("-adj-dém-");
        posMarkers.add("-adj-excl-");
        posMarkers.add("-adj-indéf-");
        posMarkers.add("-adj-int-");
        posMarkers.add("-adj-num-");
        posMarkers.add("-adj-pos-");
        posMarkers.add("-adv-");
        posMarkers.add("-adv-int-");
        posMarkers.add("-adv-pron-");
        posMarkers.add("-adv-rel-");
        posMarkers.add("-aff-");
        posMarkers.add("-art-");
        ignorablePosMarkers.add("-flex-art-déf-");
        ignorablePosMarkers.add("-flex-art-indéf-");
        ignorablePosMarkers.add("-flex-art-part-");
        posMarkers.add("-art-déf-");
        posMarkers.add("-art-indéf-");
        posMarkers.add("-art-part-");
        posMarkers.add("-aux-");
        posMarkers.add("-circonf-");
        posMarkers.add("-class-");
        posMarkers.add("-cpt-");
        posMarkers.add("-conj-");
        posMarkers.add("-conj-coord-");
        posMarkers.add("-cont-");
        posMarkers.add("-copule-");
        posMarkers.add("-corrélatif-");
        posMarkers.add("-erreur-");
        posMarkers.add("-faux-prov-");
        ignorablePosMarkers.add("-flex-adj-");
        ignorablePosMarkers.add("-flex-adj-num-");
        ignorablePosMarkers.add("-flex-adj-pos-");
        ignorablePosMarkers.add("-flex-adv-");
        ignorablePosMarkers.add("-flex-art-");
        ignorablePosMarkers.add("-flex-aux-");
        ignorablePosMarkers.add("-flex-conj-");
        ignorablePosMarkers.add("-flex-interj-");
        ignorablePosMarkers.add("-flex-lettre-");
        ignorablePosMarkers.add("-flex-loc-adj-");
        ignorablePosMarkers.add("-flex-loc-conj-");
        ignorablePosMarkers.add("-flex-loc-nom-");
        ignorablePosMarkers.add("-flex-loc-verb-");
        ignorablePosMarkers.add("-flex-nom-");
        ignorablePosMarkers.add("-flex-nom-fam-");
        ignorablePosMarkers.add("-flex-nom-pr-");
        ignorablePosMarkers.add("-flex-mots-diff-");
        ignorablePosMarkers.add("-flex-prénom-");
        ignorablePosMarkers.add("-flex-prép-");
        ignorablePosMarkers.add("-flex-pronom-");
        ignorablePosMarkers.add("-flex-pronom-indéf-");
        ignorablePosMarkers.add("-flex-pronom-int-");
        ignorablePosMarkers.add("-flex-pronom-pers-");
        ignorablePosMarkers.add("-flex-pronom-rel-");
        ignorablePosMarkers.add("-flex-verb-");
        ignorablePosMarkers.add("-inf-");
        posMarkers.add("-interf-");
        posMarkers.add("-interj-");
        posMarkers.add("-lettre-");
        posMarkers.add("-loc-");
        posMarkers.add("-loc-adj-");
        posMarkers.add("-loc-adv-");
        posMarkers.add("-loc-conj-");
        posMarkers.add("-loc-dét-");
        posMarkers.add("-loc-interj-");
        posMarkers.add("-loc-nom-");
        posMarkers.add("-loc-phr-");
        posMarkers.add("-loc-post-");
        posMarkers.add("-loc-prép-");
        posMarkers.add("-loc-pronom-");
        posMarkers.add("-loc-verb-");
        posMarkers.add("-nom-");
        posMarkers.add("-nom-fam-");
        posMarkers.add("-nom-ni-");
        posMarkers.add("-nom-nu-");
        posMarkers.add("-nom-nn-");
        posMarkers.add("-nom-npl-");
        posMarkers.add("-nom-pr-");
        posMarkers.add("-nom-sciences-");
        posMarkers.add("-numér-");
        posMarkers.add("-onoma-");
        posMarkers.add("-part-");
        posMarkers.add("-post-");
        posMarkers.add("-préf-");
        posMarkers.add("-prénom-");
        posMarkers.add("-prép-");
        posMarkers.add("-pronom-");
        posMarkers.add("-pronom-adj-");
        posMarkers.add("-pronom-dém-");
        posMarkers.add("-pronom-indéf-");
        posMarkers.add("-pronom-int-");
        posMarkers.add("-pronom-pers-");
        posMarkers.add("-pronom-pos-");
        posMarkers.add("-pronom-rel-");
        posMarkers.add("-prov-");
        posMarkers.add("-racine-");
        posMarkers.add("-radical-");
        posMarkers.add("-rimes-");
        posMarkers.add("-signe-");
        posMarkers.add("-sin-");
        posMarkers.add("-subst-pron-pers-");
        ignorablePosMarkers.add("-suf-");
        ignorablePosMarkers.add("-flex-suf-");
        ignorablePosMarkers.add("-symb-");
        posMarkers.add("type");
        posMarkers.add("-var-typo-");
        posMarkers.add("-verb-");
        posMarkers.add("-verb-pr-");
        
        // titres de section S
        // TODO: get alternate from https://fr.wiktionary.org/wiki/Module:types_de_mots/data and normalise the part of speech
        // ADJECTIFS
        posMarkers.add("adjectif");
        posMarkers.add("adj");
        posMarkers.add("adjectif qualificatif");
 
    // ADVERBES
        posMarkers.add("adverbe");
        posMarkers.add("adv");
        posMarkers.add("adverbe interrogatif");
        posMarkers.add("adv-int");
        posMarkers.add("adverbe int");
        posMarkers.add("adverbe pronominal");
        posMarkers.add("adv-pr");
        posMarkers.add("adverbe pro");
        posMarkers.add("adverbe relatif");
        posMarkers.add("adv-rel");
        posMarkers.add("adverbe rel");
 
    // CONJONCTIONS
        posMarkers.add("conjonction");
        posMarkers.add("conj");
        posMarkers.add("conjonction de coordination");
        posMarkers.add("conj-coord");
        posMarkers.add("conjonction coo");
 
        posMarkers.add("copule");
 
    // DÉTERMINANTS
        posMarkers.add("adjectif démonstratif");
        posMarkers.add("adj-dém");
        posMarkers.add("adjectif dém");
        posMarkers.add("déterminant");
        posMarkers.add("dét");
        posMarkers.add("adjectif exclamatif");
        posMarkers.add("adj-excl");
        posMarkers.add("adjectif exc");
        posMarkers.add("adjectif indéfini");
        posMarkers.add("adj-indéf");
        posMarkers.add("adjectif ind");
        posMarkers.add("adjectif interrogatif");
        posMarkers.add("adj-int");
        posMarkers.add("adjectif int");
        posMarkers.add("adjectif numéral");
        posMarkers.add("adj-num");
        posMarkers.add("adjectif num");
        posMarkers.add("adjectif possessif");
        posMarkers.add("adj-pos");
        posMarkers.add("adjectif pos");
 
        posMarkers.add("article");
        posMarkers.add("art");
        posMarkers.add("article défini");
        posMarkers.add("art-déf");
        posMarkers.add("article déf");
        posMarkers.add("article indéfini");
        posMarkers.add("art-indéf");
        posMarkers.add("article ind");
        posMarkers.add("article partitif");
        posMarkers.add("art-part");
        posMarkers.add("article par");
 
    // NOMS
        posMarkers.add("nom");
        posMarkers.add("substantif");
        posMarkers.add("nom commun");
        posMarkers.add("nom de famille");
        posMarkers.add("nom-fam");
        posMarkers.add("patronyme");
        posMarkers.add("nom propre");
        posMarkers.add("nom-pr");
        posMarkers.add("nom scientifique");
        posMarkers.add("nom-sciences");
        posMarkers.add("nom science");
        posMarkers.add("nom scient");
        posMarkers.add("prénom");

    // PRÉPOSITION
        posMarkers.add("préposition");
        posMarkers.add("prép");
 
    // PRONOMS
        posMarkers.add("pronom");
        posMarkers.add("pronom-adjectif");
        posMarkers.add("pronom démonstratif");
        posMarkers.add("pronom-dém");
        posMarkers.add("pronom dém");
        posMarkers.add("pronom indéfini");
        posMarkers.add("pronom-indéf");
        posMarkers.add("pronom ind");
        posMarkers.add("pronom interrogatif");
        posMarkers.add("pronom-int");
        posMarkers.add("pronom int");
        posMarkers.add("pronom personnel");
        posMarkers.add("pronom-pers");
        posMarkers.add("pronom-per");
        posMarkers.add("pronom réf");
        posMarkers.add("pronom-réfl");
        posMarkers.add("pronom réfléchi");
        posMarkers.add("pronom possessif");
        posMarkers.add("pronom-pos");
        posMarkers.add("pronom pos");
        posMarkers.add("pronom relatif");
        posMarkers.add("pronom-rel");
        posMarkers.add("pronom rel");
 
    // VERBES
        posMarkers.add("verbe");
        posMarkers.add("verb");
        posMarkers.add("verbe pronominal");
        posMarkers.add("verb-pr");
        posMarkers.add("verbe pr");
 
    // EXCLAMATIONS
        posMarkers.add("interjection");
        posMarkers.add("interj");
        posMarkers.add("onomatopée");
        posMarkers.add("onoma");
        posMarkers.add("onom");
 
    // PARTIES
//        posMarkers.add("affixe");
//        posMarkers.add("aff");
//        posMarkers.add("circonfixe");
//        posMarkers.add("circonf");
//        posMarkers.add("circon");
//        posMarkers.add("infixe");
//        posMarkers.add("inf");
//        posMarkers.add("interfixe");
//        posMarkers.add("interf");
//        posMarkers.add("particule");
//        posMarkers.add("part");
//        posMarkers.add("particule numérale");
//        posMarkers.add("part-num");
//        posMarkers.add("particule num");
//        posMarkers.add("postposition");
//        posMarkers.add("post");
//        posMarkers.add("postpos");
//        posMarkers.add("préfixe");
//        posMarkers.add("préf");
//        posMarkers.add("radical");
//        posMarkers.add("rad");
//        posMarkers.add("suffixe");
//        posMarkers.add("suff");
//        posMarkers.add("suf");
// 
//        posMarkers.add("pré-verbe");
//        posMarkers.add("pré-nom");
// 
    // PHRASES
        posMarkers.add("locution");
        posMarkers.add("loc");
        posMarkers.add("locution-phrase");
        posMarkers.add("loc-phr");
        posMarkers.add("locution-phrase");
        posMarkers.add("locution phrase");
        posMarkers.add("proverbe");
        posMarkers.add("prov");
 
    // DIVERS
        posMarkers.add("quantificateur");
        posMarkers.add("quantif");
        posMarkers.add("variante typographique");
        posMarkers.add("var-typo");
        posMarkers.add("variante typo");
        posMarkers.add("variante par contrainte typographique");
 
    // CARACTÈRES
        ignorablePosMarkers.add("lettre");
 
        ignorablePosMarkers.add("symbole");
        ignorablePosMarkers.add("symb");
        posMarkers.add("classificateur");
        posMarkers.add("class");
        posMarkers.add("classif");
        posMarkers.add("numéral");
        posMarkers.add("numér");
        posMarkers.add("num");
        posMarkers.add("sinogramme");
        posMarkers.add("sinog");
        posMarkers.add("sino");
 
        posMarkers.add("erreur");
        posMarkers.add("faute");
        posMarkers.add("faute d'orthographe"); 
        posMarkers.add("faute d’orthographe");
 
        // Spéciaux
        posMarkers.add("gismu");
        posMarkers.add("rafsi");
                
        nymMarkerToNymName = new HashMap<String, String>(20);
        nymMarkerToNymName.put("-méro-", "mero");
        nymMarkerToNymName.put("-hyper-", "hyper");
        nymMarkerToNymName.put("-hypo-", "hypo");
        nymMarkerToNymName.put("-holo-", "holo");
        nymMarkerToNymName.put("-méton-", "meto");
        nymMarkerToNymName.put("-syn-", "syn");
        nymMarkerToNymName.put("-q-syn-", "qsyn");
        nymMarkerToNymName.put("-ant-", "ant");
        

        nymMarkerToNymName.put("méronymes", "mero");
        nymMarkerToNymName.put("méro", "mero");
        nymMarkerToNymName.put("hyperonymes", "hyper");
        nymMarkerToNymName.put("hyper", "hyper");
        nymMarkerToNymName.put("hyponymes", "hypo");
        nymMarkerToNymName.put("hypo", "hypo");
        nymMarkerToNymName.put("holonymes", "holo");
        nymMarkerToNymName.put("holo", "holo");
        nymMarkerToNymName.put("-méton-", "meto");
        nymMarkerToNymName.put("synonymes", "syn");
        nymMarkerToNymName.put("syn", "syn");
        nymMarkerToNymName.put("quasi-synonymes", "qsyn");
        nymMarkerToNymName.put("q-syn", "qsyn");
        nymMarkerToNymName.put("quasi-syn", "qsyn");
        nymMarkerToNymName.put("antonymes", "ant");
        nymMarkerToNymName.put("ant", "ant");
        nymMarkerToNymName.put("anto", "ant");
         
        // paronymes, troponymes, gentillés ?
        
        sectionMarkers = new HashSet<String>(200);
        sectionMarkers.addAll(posMarkers);
        sectionMarkers.addAll(nymMarkerToNymName.keySet());
        sectionMarkers.add("-étym-");
        sectionMarkers.add("-voc-");
        sectionMarkers.add("-trad-");
        sectionMarkers.add("-note-");
        sectionMarkers.add("-réf-");
        sectionMarkers.add("clé de tri");
        sectionMarkers.add("-anagr-");
        sectionMarkers.add("-drv-");
        sectionMarkers.add("-voir-");
        sectionMarkers.add("-pron-");
        sectionMarkers.add("-gent-");
        sectionMarkers.add("-apr-");
        sectionMarkers.add("-paro-");
        sectionMarkers.add("-homo-");
        sectionMarkers.add("-exp-");
        sectionMarkers.add("-compos-");
        // DONE: prendre en compte la variante orthographique (différences avec -ortho-alt- ?)
        sectionMarkers.add("-var-ortho-");
        
       // TODO trouver tous les modèles de section...
        
        // affixesToDiscardFromLinks = new HashSet<String>();
        // affixesToDiscardFromLinks.add("s");
    }
    
    public FrenchWiktionaryExtractor(WiktionaryDataHandler wdh) {
        super(wdh);
    }

    protected final static Pattern languageSectionPattern;
	private final static Pattern pronunciationPattern;

    static {
        languageSectionPattern = Pattern.compile(languageSectionPatternString);
        pronunciationPattern = Pattern.compile(pronounciationPatternString);
    }

    int state = NODATA;
    int definitionBlockStart = -1;
    int translationBlockStart = -1;
    int orthBlockStart = -1;
    private int nymBlockStart = -1;

    private String currentNym = null;

    /* (non-Javadoc)
     * @see org.getalp.dbnary.WiktionaryExtractor#extractData(java.lang.String, org.getalp.blexisma.semnet.SemanticNetwork)
     */
    @Override
    public void extractData() {
        // System.out.println(pageContent);
        Matcher languageFilter = languageSectionPattern.matcher(pageContent);
        while (languageFilter.find() && ! isFrenchLanguageHeader(languageFilter)) {
            ;
        }
        // Either the filter is at end of sequence or on French language header.
        if (languageFilter.hitEnd()) {
            // There is no french data in this page.
            return ;
        }
        int frenchSectionStartOffset = languageFilter.end();
        // Advance till end of sequence or new language section
        languageFilter.find();
        int frenchSectionEndOffset = languageFilter.hitEnd() ? pageContent.length() : languageFilter.start();
        
        extractFrenchData(frenchSectionStartOffset, frenchSectionEndOffset);
     }

    
    private boolean isFrenchLanguageHeader(Matcher m) {
		return (null != m.group(1) && m.group(1).equals("fr")) || (null != m.group(2) && m.group(2).equals("fr"));
	}


	void gotoNoData(Matcher m) {
        state = NODATA;
    }

    
    void gotoTradBlock(Matcher m) {
        translationBlockStart = m.end();
        state = TRADBLOCK;
    }

    // TODO: put up in root class extractor.
    void gotoDefBlock(Matcher m, String pos) {
        state = DEFBLOCK;
        definitionBlockStart = m.end();
        wdh.addPartOfSpeech(pos);
    }
    
    void gotoOrthoAltBlock(Matcher m) {
        state = ORTHOALTBLOCK;    
        orthBlockStart = m.end();
    }

    void leaveOrthoAltBlock(Matcher m) {
        extractOrthoAlt(orthBlockStart, computeRegionEnd(orthBlockStart, m));
        orthBlockStart = -1;
    }

    void leaveTradBlock(Matcher m) {
        extractTranslations(translationBlockStart, computeRegionEnd(translationBlockStart, m));
        translationBlockStart = -1;
    }

    
    void leaveDefBlock(Matcher m) {
    	int end = computeRegionEnd(definitionBlockStart, m);
        extractDefinitions(definitionBlockStart, end);
        extractPronounciation(definitionBlockStart, end);
        definitionBlockStart = -1;
    }

	void gotoNymBlock(Matcher m, String nym) {
        state = NYMBLOCK;
        currentNym = nym;
        nymBlockStart = m.end();      
     }

    void gotoIgnorePos() {
        state = IGNOREPOS;
     }

    void leaveNymBlock(Matcher m) {
        extractNyms(currentNym, nymBlockStart, computeRegionEnd(nymBlockStart, m));
        currentNym = null;
        nymBlockStart = -1;         
     }
    
    protected void extractFrenchData(int startOffset, int endOffset) {        
        Matcher m = WikiPatterns.macroPattern.matcher(pageContent);
        m.region(startOffset, endOffset);
        wdh.initializeEntryExtraction(wiktionaryPageName);

        // WONTDO: (priority: low) should I use a macroOrLink pattern to detect translations that are not macro based ?
        // DONE: (priority: top) link the definition node with the current Part of Speech
        // DONE: (priority: top) type all nodes by prefixing it by language, or #pos or #def.
        // DONE: handle alternative spelling
        // DONE: extract synonyms
        // DONE: extract antonyms
        // DONE: add an IGNOREPOS state to ignore the entire part of speech
        
		gotoNoData(m);
		String pos = null;
		String nym = null;
		while (m.find()) {
			switch (state) {
			case NODATA:
				if (isTranslation(m)) {
					gotoTradBlock(m);
				} else if (null != (pos = getValidPOS(m))) {
					if (pos.length()==0) 
						gotoIgnorePos();
					else
						gotoDefBlock(m, pos);
				} else if (isAlternate(m)) {
					gotoOrthoAltBlock(m);
				} else if (null != (nym = isNymHeader(m))) {
					gotoNymBlock(m, nym);
				} else if (isPronounciation(m)) {
					gotoPronBlock(m);
				} else if (isValidSection(m)) {
					gotoNoData(m);
				} else {
					// unknownHeaders.add(m.group(0));
				}
				break;
			case DEFBLOCK:
				// Iterate until we find a new section
				if (isTranslation(m)) {
					leaveDefBlock(m);
					gotoTradBlock(m);
				} else if (null != (pos = getValidPOS(m))) {
					leaveDefBlock(m);
					if (pos.length()==0) 
						gotoIgnorePos();
					else
						gotoDefBlock(m,pos);
				} else if (isAlternate(m)) {
					leaveDefBlock(m);
					gotoOrthoAltBlock(m);
				} else if (null != (nym = isNymHeader(m))) {
					leaveDefBlock(m);
					gotoNymBlock(m, nym);
				} else if (isPronounciation(m)) {
					leaveDefBlock(m);
					gotoPronBlock(m);
				} else if (isValidSection(m)) {
					leaveDefBlock(m);
					gotoNoData(m);
				} else {
					// leaveDefBlock(m);
					// unknownHeaders.add(m.group(0));
					// gotoNoData(m);
				} 
				break;
			case TRADBLOCK:
				if (isTranslation(m)) {
					leaveTradBlock(m);
					gotoTradBlock(m);
				} else if (null != (pos = getValidPOS(m))) {
					leaveTradBlock(m);
					if (pos.length()==0) 
						gotoIgnorePos();
					else
						gotoDefBlock(m, pos);
				} else if (isAlternate(m)) {
					leaveTradBlock(m);
					gotoOrthoAltBlock(m);
				} else if (null != (nym = isNymHeader(m))) {
					leaveTradBlock(m);
					gotoNymBlock(m, nym);
				} else if (isPronounciation(m)) {
					leaveTradBlock(m);
					gotoPronBlock(m);
				} else if (isValidSection(m)) {
					leaveTradBlock(m);
					gotoNoData(m);
				} else {
					//leaveTradBlock(m);
					// unknownHeaders.add(m.group(0));
					// gotoNoData(m);
				} 
				break;
			case ORTHOALTBLOCK:
				if (isTranslation(m)) {
					leaveOrthoAltBlock(m);
					gotoTradBlock(m);
				} else if (null != (pos = getValidPOS(m))) {
					leaveOrthoAltBlock(m);
					if (pos.length()==0) 
						gotoIgnorePos();
					else
						gotoDefBlock(m, pos);
				} else if (isAlternate(m)) {
					leaveOrthoAltBlock(m);
					gotoOrthoAltBlock(m);
				} else if (null != (nym = isNymHeader(m))) {
					leaveOrthoAltBlock(m);
					gotoNymBlock(m, nym);
				} else if (isPronounciation(m)) {
					leaveOrthoAltBlock(m);
					gotoPronBlock(m);
				} else if (isValidSection(m)) {
					leaveOrthoAltBlock(m);
					gotoNoData(m);
				} else {
					// leaveOrthoAltBlock(m);
					// unknownHeaders.add(m.group(0));
					// gotoNoData(m);
				}
				break;
			case NYMBLOCK:
				if (isTranslation(m)) {
					leaveNymBlock(m);
					gotoTradBlock(m);
				} else if (null != (pos = getValidPOS(m))) {
					leaveNymBlock(m);
					if (pos.length()==0) 
						gotoIgnorePos();
					else 
						gotoDefBlock(m, pos);
				} else if (isAlternate(m)) {
					leaveNymBlock(m);
					gotoOrthoAltBlock(m);
				} else if (null != (nym = isNymHeader(m))) {
					leaveNymBlock(m);
					gotoNymBlock(m, nym);
				} else if (isPronounciation(m)) {
					leaveNymBlock(m);
					gotoPronBlock(m);
				} else if (isValidSection(m)) {
					leaveNymBlock(m);
					gotoNoData(m);
				} else {
					// leaveNymBlock(m);
					// unknownHeaders.add(m.group(0));
					// gotoNoData(m);
				}
				break;
			case PRONBLOCK:
				if (isTranslation(m)) {
					leavePronBlock(m);
					gotoTradBlock(m);
				} else if (null != (pos = getValidPOS(m))) {
					leavePronBlock(m);
					if (pos.length()==0) 
						gotoIgnorePos();
					else
						gotoDefBlock(m, pos);
				} else if (isAlternate(m)) {
					leavePronBlock(m);
					gotoOrthoAltBlock(m);
				} else if (null != (nym = isNymHeader(m))) {
					leavePronBlock(m);
					gotoNymBlock(m, nym);
				} else if (isPronounciation(m)) {
					leavePronBlock(m);
					gotoPronBlock(m);
				} else if (isValidSection(m)) {
					leavePronBlock(m);
					gotoNoData(m);
				} else {
					// leavePronBlock(m);
					// unknownHeaders.add(m.group(0));
					// gotoNoData(m);
				}
				break;
			case IGNOREPOS:
				if (isTranslation(m)) {
				} else if (null != (pos = getValidPOS(m))) {
					if (pos.length()==0) 
						gotoIgnorePos();
					else
						gotoDefBlock(m, pos);
				} else if (isAlternate(m)) {
				} else if (null != (nym = isNymHeader(m))) {
				} else if (isPronounciation(m)) {
					// gotoPronBlock(m);
				} else if (isValidSection(m)) {
					// gotoIgnorePos();
				} else {
					// unknownHeaders.add(m.group(0));
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
    




	private boolean isValidSection(Matcher m) {
		if (sectionMarkers.contains(m.group(1))) return true;
		if ("S".equals(m.group(1))) return true;
		return false;
	}


	private void leavePronBlock(Matcher m) {
		// TODO Auto-generated method stub
		
	}


	private void gotoPronBlock(Matcher m) {
		// TODO Auto-generated method stub
		
	}


	private boolean isPronounciation(Matcher m) {
		// TODO Auto-generated method stub
		return false;
	}


	private String getValidPOS(Matcher m) {
		if (posMarkers.contains(m.group(1))) return m.group(1);
		if ("S".equals(m.group(1))) {
			Map<String,String> args = WikiTool.parseArgs(m.group(2));
			String titre = args.get("1");
			String flexion = args.get("3");

			if (null != flexion && "flexion".equals(flexion)) return "";
			if (null != titre && posMarkers.contains(titre)) return titre;
			if (null != titre && ignorablePosMarkers.contains(titre)) return titre;
			
		}
		return null;
	}


	private boolean isTranslation(Matcher m) {
		if (m.group(1).equals("-trad-")) return true;
		if (m.group(1).equals("S")) {
			Map<String,String> args = WikiTool.parseArgs(m.group(2));
			String titre = args.get("1");
			if (null != titre && titre.startsWith("trad")) return true;
		}
		return false;
	}

	private static Set<String> variantSections = new HashSet<String>();
	static {
		variantSections.add("variantes");
		variantSections.add("var");
		variantSections.add("variantes ortho");
		variantSections.add("var-ortho");
		variantSections.add("variantes orthographiques");
		variantSections.add("variantes dialectales");
		variantSections.add("dial");
		variantSections.add("var-dial");
		variantSections.add("variantes dial");
		variantSections.add("variantes dialectes");
		variantSections.add("dialectes");
		variantSections.add("anciennes orthographes");
		variantSections.add("ortho-arch");
		variantSections.add("anciennes ortho");	
	}
	
	private boolean isAlternate(Matcher m) {
		if (m.group(1).equals("-ortho-alt-") || m.group(1).equals("-var-ortho-")) return true;
		if (m.group(1).equals("S")) {
			Map<String,String> args = WikiTool.parseArgs(m.group(2));
			String titre = args.get("1");
			if (null != titre && 
					variantSections.contains(titre)) return true;
		}
		return false;
	}
	


    private String isNymHeader(Matcher m) {
		if (nymMarkerToNymName.containsKey(m.group(1))) return nymMarkerToNymName.get(m.group(1));
		if (m.group(1).equals("S")) {
			Map<String,String> args = WikiTool.parseArgs(m.group(2));
			String titre = args.get("1");
			if (null != titre && 
					nymMarkerToNymName.containsKey(titre)) return nymMarkerToNymName.get(m.group(1));
		}
		return null;
	}


	private void extractTranslations(int startOffset, int endOffset) {
    	Matcher macroMatcher = WikiPatterns.macroPattern.matcher(pageContent);
    	macroMatcher.region(startOffset, endOffset);
    	String currentGlose = null;

    	while (macroMatcher.find()) {
    		String g1 = macroMatcher.group(1);

    		if (g1.equals("trad+") || g1.equals("trad-") || g1.equals("trad") || g1.equals("t+") || g1.equals("t-") || g1.equals("trad--")) {
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
    				 lang=FrenchLangtoCode.triletterCode(lang);
                     if(lang!=null){
                  	   wdh.registerTranslation(lang, currentGlose, usage, word);
                     }
    			}
    		} else if (g1.equals("boîte début") || g1.equals("trad-début") || g1.equals("(")) {
    			// Get the glose that should help disambiguate the source acception
    			String g2 = macroMatcher.group(2);
    			// Ignore glose if it is a macro
    			if (g2 != null && ! g2.startsWith("{{")) {
    				currentGlose = g2;
    			}
    		} else if (g1.equals("-")) {
    			// just ignore it
    		} else if (g1.equals("trad-fin") || g1.equals(")")) {
    			// Forget the current glose
    			currentGlose = null;
    		} else if (g1.equals("T")) {
    			// this a a language identifier, just ignore it as we get the language id from the trad macro parameter.

    		}
    	}
    }
    
    private void extractPronounciation(int startOffset, int endOffset) {
		Matcher pronMatcher = pronunciationPattern.matcher(pageContent);
		pronMatcher.region(startOffset, endOffset);

		while (pronMatcher.find()) {
    		String pron = pronMatcher.group(1);
    		String lang = pronMatcher.group(2);
    		
    		if (null == pron || pron.equals("")) return;
    		if (lang == null || lang.equals("")) return;
    		
    		if (pron.startsWith("1=")) pron = pron.substring(2);
    		if (lang.startsWith("|2=")) lang = lang.substring(2);
    		if (lang.startsWith("|lang=")) lang = lang.substring(5);

    		if (! pron.equals("")) wdh.registerPronunciation(pron, "fr-fonipa");
    		
		}
    }


}

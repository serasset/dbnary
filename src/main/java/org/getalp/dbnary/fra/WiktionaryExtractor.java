/**
 * 
 */
package org.getalp.dbnary.fra;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.getalp.dbnary.LangTools;
import org.getalp.dbnary.AbstractWiktionaryExtractor;
import org.getalp.dbnary.WiktionaryDataHandler;
import org.getalp.dbnary.pol.DefinitionExpanderWikiModel;
import org.getalp.dbnary.fra.FrenchExtractorWikiModel;
import org.getalp.dbnary.wiki.WikiPatterns;
import org.getalp.dbnary.wiki.WikiTool;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

import org.getalp.dbnary.LexinfoOnt;

import org.getalp.dbnary.PropertyResourcePair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author serasset
 *
 */
public class WiktionaryExtractor extends AbstractWiktionaryExtractor {

	private Logger log = LoggerFactory.getLogger(WiktionaryExtractor.class);

	// NOTE: to subclass the extractor, you need to define how a language section is recognized.
	// then, how are sections recognized and what is their semantics.
	// then, how to extract specific elements from the particular sections
	protected final static String languageSectionPatternString;

	protected final static String languageSectionPatternString1 = "==\\s*\\{\\{=([^=]*)=\\}\\}\\s*==";
	protected final static String languageSectionPatternString2 = "==\\s*\\{\\{langue\\|([^\\}]*)\\}\\}\\s*==";
	// TODO: handle morphological informations e.g. fr-rég template ?
	protected final static String pronunciationPatternString = "\\{\\{pron\\|([^\\|\\}]*)\\|([^\\}]*)\\}\\}";

	protected final static String otherFormPatternString = "\\{\\{fr-[^\\}]*\\}\\}";

	private String lastExtractedPronunciationLang = null;

	private static Pattern inflectionMacroNamePattern = Pattern.compile("^fr-");
	protected final static String inflectionDefPatternString = "^\\# ''([^\n]+) de'' \\[\\[([^\n]+)\\]\\]\\.$";
	protected final static Pattern inflectionDefPattern = Pattern.compile(inflectionDefPatternString, Pattern.MULTILINE);

	private static HashMap<String,String> posMarkers;
	private static HashSet<String> ignorablePosMarkers;
	private static HashSet<String> sectionMarkers;
	
	private final static HashMap<String, String> nymMarkerToNymName;
	
	private static HashSet<String> unsupportedMarkers = new HashSet<String>();

	public static final Locale frLocale = new Locale("fr");

	// private static Set<String> affixesToDiscardFromLinks = null;
	private static void addPos(String pos) {posMarkers.put(pos, pos);}
	private static void addPos(String p, String n) {posMarkers.put(p, n);}

	static {
		languageSectionPatternString = "(?:" + languageSectionPatternString1 + ")|(?:" + languageSectionPatternString2 + ")";

		posMarkers = new HashMap<String,String>(130);
		ignorablePosMarkers = new HashSet<String>(130);

		addPos("-déf-");
		addPos("-déf-/2");
		addPos("-déf2-");
		addPos("--");
		addPos("-adj-");
		addPos("-adj-/2");
		ignorablePosMarkers.add("-flex-adj-indéf-");
		addPos("-adj-dém-");
		addPos("-adj-excl-");
		addPos("-adj-indéf-");
		addPos("-adj-int-");
		addPos("-adj-num-");
		addPos("-adj-pos-");
		addPos("-adv-");
		addPos("-adv-int-");
		addPos("-adv-pron-");
		addPos("-adv-rel-");
		addPos("-aff-");
		addPos("-art-");
		ignorablePosMarkers.add("-flex-art-déf-");
		ignorablePosMarkers.add("-flex-art-indéf-");
		ignorablePosMarkers.add("-flex-art-part-");
		addPos("-art-déf-");
		addPos("-art-indéf-");
		addPos("-art-part-");
		addPos("-aux-");
		addPos("-circonf-");
		addPos("-class-");
		addPos("-cpt-");
		addPos("-conj-");
		addPos("-conj-coord-");
		addPos("-cont-");
		addPos("-copule-");
		addPos("-corrélatif-");
		addPos("-erreur-");
		addPos("-faux-prov-");
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
		addPos("-interf-");
		addPos("-interj-");
		addPos("-lettre-");
		addPos("-loc-");
		addPos("-loc-adj-");
		addPos("-loc-adv-");
		addPos("-loc-conj-");
		addPos("-loc-dét-");
		addPos("-loc-interj-");
		addPos("-loc-nom-");
		addPos("-loc-phr-");
		addPos("-loc-post-");
		addPos("-loc-prép-");
		addPos("-loc-pronom-");
		addPos("-loc-verb-");
		addPos("-nom-");
		addPos("-nom-fam-");
		addPos("-nom-ni-");
		addPos("-nom-nu-");
		addPos("-nom-nn-");
		addPos("-nom-npl-");
		addPos("-nom-pr-");
		addPos("-nom-sciences-");
		addPos("-numér-");
		addPos("-onoma-");
		addPos("-part-");
		addPos("-post-");
		addPos("-préf-");
		addPos("-prénom-");
		addPos("-prép-");
		addPos("-pronom-");
		addPos("-pronom-adj-");
		addPos("-pronom-dém-");
		addPos("-pronom-indéf-");
		addPos("-pronom-int-");
		addPos("-pronom-pers-");
		addPos("-pronom-pos-");
		addPos("-pronom-rel-");
		addPos("-prov-");
		addPos("-racine-");
		addPos("-radical-");
		addPos("-rimes-");
		addPos("-signe-");
		addPos("-sin-");
		addPos("-subst-pron-pers-");
		ignorablePosMarkers.add("-suf-");
		ignorablePosMarkers.add("-flex-suf-");
		ignorablePosMarkers.add("-symb-");
		addPos("type");
		addPos("-var-typo-");
		addPos("-verb-");
		addPos("-verb-pr-");
		
		// S section titles
		// TODO: get alternate from https://fr.wiktionary.org/wiki/Module:types_de_mots/data and normalize the part of speech
		// ADJECTIFS
		addPos("adjectif", "-adj-");
		addPos("adj", "-adj-");
		addPos("adjectif qualificatif", "-adj-");
 
	// ADVERBES
		addPos("adverbe", "-adv-");
		addPos("adv", "-adv-");
		addPos("adverbe interrogatif");
		addPos("adv-int");
		addPos("adverbe int");
		addPos("adverbe pronominal");
		addPos("adv-pr");
		addPos("adverbe pro");
		addPos("adverbe relatif");
		addPos("adv-rel");
		addPos("adverbe rel");
 
	// CONJONCTIONS
		addPos("conjonction");
		// addPos("conj");
		addPos("conjonction de coordination");
		addPos("conj-coord");
		addPos("conjonction coo");
 
		addPos("copule");
 
	// DÉTERMINANTS
		addPos("adjectif démonstratif");
		addPos("adj-dém");
		addPos("adjectif dém");
		addPos("déterminant");
		addPos("dét");
		addPos("adjectif exclamatif");
		addPos("adj-excl");
		addPos("adjectif exc");
		addPos("adjectif indéfini");
		addPos("adj-indéf");
		addPos("adjectif ind");
		addPos("adjectif interrogatif");
		addPos("adj-int");
		addPos("adjectif int");
		addPos("adjectif numéral");
		addPos("adj-num");
		addPos("adjectif num");
		addPos("adjectif possessif");
		addPos("adj-pos");
		addPos("adjectif pos");
 
		addPos("article");
		addPos("art");
		addPos("article défini");
		addPos("art-déf");
		addPos("article déf");
		addPos("article indéfini");
		addPos("art-indéf");
		addPos("article ind");
		addPos("article partitif");
		addPos("art-part");
		addPos("article par");
 
	// NOMS
		addPos("nom", "-nom-");
		addPos("substantif", "-nom-");
		addPos("nom commun", "-nom-");
		addPos("nom de famille");
		addPos("nom-fam");
		addPos("patronyme");
		addPos("nom propre", "-nom-pr-");
		addPos("nom-pr", "-nom-pr-");
		addPos("nom scientifique");
		addPos("nom-sciences");
		addPos("nom science");
		addPos("nom scient");
		addPos("prénom");

	// PRÉPOSITION
		addPos("préposition");
		addPos("prép");
 
	// PRONOMS
		addPos("pronom");
		addPos("pronom-adjectif");
		addPos("pronom démonstratif");
		addPos("pronom-dém");
		addPos("pronom dém");
		addPos("pronom indéfini");
		addPos("pronom-indéf");
		addPos("pronom ind");
		addPos("pronom interrogatif");
		addPos("pronom-int");
		addPos("pronom int");
		addPos("pronom personnel");
		addPos("pronom-pers");
		addPos("pronom-per");
		addPos("pronom réf");
		addPos("pronom-réfl");
		addPos("pronom réfléchi");
		addPos("pronom possessif");
		addPos("pronom-pos");
		addPos("pronom pos");
		addPos("pronom relatif");
		addPos("pronom-rel");
		addPos("pronom rel");
 
	// VERBES
		addPos("verbe", "-verb-");
		addPos("verb", "-verb-");
		addPos("verbe pronominal");
		addPos("verb-pr");
		addPos("verbe pr");
 
	// EXCLAMATIONS
		addPos("interjection");
		addPos("interj");
		addPos("onomatopée");
		addPos("onoma");
		addPos("onom");
 
	// PARTIES   TODO: Extract affixes in French
//		addPos("affixe");
//		addPos("aff");
//		addPos("circonfixe");
//		addPos("circonf");
//		addPos("circon");
//		addPos("infixe");
//		addPos("inf");
//		addPos("interfixe");
//		addPos("interf");
//		addPos("particule");
//		addPos("part");
//		addPos("particule numérale");
//		addPos("part-num");
//		addPos("particule num");
//		addPos("postposition");
//		addPos("post");
//		addPos("postpos");
//		addPos("préfixe");
//		addPos("préf");
//		addPos("radical");
//		addPos("rad");
//		addPos("suffixe");
//		addPos("suff");
//		addPos("suf");
//
//		addPos("pré-verbe");
//		addPos("pré-nom");

	// PHRASES
		addPos("locution");
		addPos("loc");
		addPos("locution-phrase");
		addPos("loc-phr");
		addPos("locution-phrase");
		addPos("locution phrase");
		addPos("proverbe");
		addPos("prov");
 
	// DIVERS
		addPos("quantificateur");
		addPos("quantif");
		addPos("variante typographique");
		addPos("var-typo");
		addPos("variante typo");
		addPos("variante par contrainte typographique");
 
	// CARACTÈRES
		ignorablePosMarkers.add("lettre");
 
		ignorablePosMarkers.add("symbole");
		ignorablePosMarkers.add("symb");
		addPos("classificateur");
		addPos("class");
		addPos("classif");
		addPos("numéral");
		addPos("numér");
		addPos("num");
		addPos("sinogramme");
		addPos("sinog");
		addPos("sino");
 
		addPos("erreur");
		addPos("faute");
		addPos("faute d'orthographe"); 
		addPos("faute d’orthographe");
 
		// Spéciaux
		addPos("gismu");
		addPos("rafsi");

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
		sectionMarkers.addAll(posMarkers.keySet());
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
	
	public WiktionaryExtractor(WiktionaryDataHandler wdh) {
		super(wdh);
	}


	protected final static Pattern languageSectionPattern;
	protected final static Pattern pronunciationPattern;
	protected final static Pattern otherFormPattern;

	static {
		languageSectionPattern = Pattern.compile(languageSectionPatternString);
		pronunciationPattern   = Pattern.compile(pronunciationPatternString);
		otherFormPattern	   = Pattern.compile(otherFormPatternString);
	}

	private enum Block {NOBLOCK, IGNOREPOS, TRADBLOCK, DEFBLOCK, INFLECTIONBLOCK, ORTHOALTBLOCK, NYMBLOCK};

	private Block currentBlock = Block.NOBLOCK;
	private int blockStart   = -1;

	private String currentNym = null;

	protected ExampleExpanderWikiModel exampleExpander;

	private Set<String> defTemplates = null;

	protected boolean isFrenchLanguageHeader(Matcher m) {
		return (null != m.group(1) && m.group(1).equals("fr")) || (null != m.group(2) && m.group(2).equals("fr"));
	}

	public String getLanguageInHeader(Matcher m) {
		if (null != m.group(1))
			return m.group(1);

		if (null != m.group(2))
			return m.group(2);

		return null;
	}

	@Override
	public void extractData() {
		extractData(false);
	}

	protected void extractData(boolean extractForeignData) {
		Matcher languageFilter = languageSectionPattern.matcher(pageContent);
		int startSection = -1;

		exampleExpander = new ExampleExpanderWikiModel(wi, frLocale, this.wiktionaryPageName, "");

		String nextLang = null, lang = null;

		while (languageFilter.find()) {
			nextLang = getLanguageInHeader(languageFilter);
			extractData(startSection, languageFilter.start(), lang, extractForeignData);
			lang = nextLang;
			startSection = languageFilter.end();
		}

		// Either the filter is at end of sequence or on French language header.
		if (languageFilter.hitEnd()) {
			extractData(startSection, pageContent.length(), lang, extractForeignData);
		}
	}

	public boolean isInflectionMacro(Matcher m, String macroName) {
		return macroName != null && currentBlock == Block.INFLECTIONBLOCK && inflectionMacroNamePattern.matcher(macroName).matches();
	}

	public HashSet<PropertyResourcePair> morphologicalPropertiesFromWikicode(String wikicodeMophology) {
		HashSet<PropertyResourcePair> infl = new HashSet<PropertyResourcePair>();

		switch(wikicodeMophology) {
		case "ppr":
			infl.add(new PropertyResourcePair(LexinfoOnt.verbFormMood, LexinfoOnt.participle));
			infl.add(new PropertyResourcePair(LexinfoOnt.tense, LexinfoOnt.present));
			// participe présent.
			break;
		case "ppms":
		case "ppm":
		case "pp":
			infl.add(new PropertyResourcePair(LexinfoOnt.verbFormMood, LexinfoOnt.participle));
			infl.add(new PropertyResourcePair(LexinfoOnt.tense,  LexinfoOnt.past));
			infl.add(new PropertyResourcePair(LexinfoOnt.gender, LexinfoOnt.masculine));
			infl.add(new PropertyResourcePair(LexinfoOnt.number, LexinfoOnt.singular));
			// past participle masculine singular (or invariable).
			break;
		case "ppfs":
		case "ppf":
			infl.add(new PropertyResourcePair(LexinfoOnt.verbFormMood, LexinfoOnt.participle));
			infl.add(new PropertyResourcePair(LexinfoOnt.tense,  LexinfoOnt.past));
			infl.add(new PropertyResourcePair(LexinfoOnt.gender, LexinfoOnt.feminine));
			infl.add(new PropertyResourcePair(LexinfoOnt.number, LexinfoOnt.singular));
			// past participle au féminin singulier.
			break;
		case "ppmp":
			infl.add(new PropertyResourcePair(LexinfoOnt.verbFormMood, LexinfoOnt.participle));
			infl.add(new PropertyResourcePair(LexinfoOnt.tense,  LexinfoOnt.past));
			infl.add(new PropertyResourcePair(LexinfoOnt.gender, LexinfoOnt.masculine));
			infl.add(new PropertyResourcePair(LexinfoOnt.number, LexinfoOnt.plural));
			// past participle au masculin pluriel.
			break;
		case "ppfp":
			infl.add(new PropertyResourcePair(LexinfoOnt.verbFormMood, LexinfoOnt.participle));
			infl.add(new PropertyResourcePair(LexinfoOnt.tense,  LexinfoOnt.past));
			infl.add(new PropertyResourcePair(LexinfoOnt.gender, LexinfoOnt.feminine));
			infl.add(new PropertyResourcePair(LexinfoOnt.number, LexinfoOnt.plural));
			// past participle au féminin pluriel.
			break;

		//FIXME: we ignore these morphological informations which describe the entire verb, not only this inflection.
		case "impers": // if verb is pronominal
			return null;
		case "réfl": // if verb is pronominal
			return null;
		case "'": // if "je" is to be written "j'".
			return null;

		default:
			String[] infos = wikicodeMophology.split(".");

			// See http://fr.wiktionary.org/wiki/Mod%C3%A8le:fr-verbe-flexion for documentation about this stuff.
			if (infos.length < 3) {
				log.error("wikicode morphology was not recognized for " + commonInflectionInformations.partOfSpeech + " form in article " + wdh.currentLexEntry());
				return null;
			}

			// Mood
			switch (infos[0]) {
			case "ind":
				infl.add(new PropertyResourcePair(LexinfoOnt.verbFormMood, LexinfoOnt.indicative));
				break;
			case "cond":
				infl.add(new PropertyResourcePair(LexinfoOnt.verbFormMood, LexinfoOnt.conditional));
				break;
			case "imp":
				infl.add(new PropertyResourcePair(LexinfoOnt.verbFormMood, LexinfoOnt.imperative));
				break;
			case "sub":
				infl.add(new PropertyResourcePair(LexinfoOnt.verbFormMood, LexinfoOnt.subjunctive));
				break;
			default:
				log.error("wikicode's mood part was not recognized for " + commonInflectionInformations.partOfSpeech + " form in article " + wdh.currentLexEntry());
				return null;
			}

			// Tense
			switch (infos[1]) {
			case "p":
				infl.add(new PropertyResourcePair(LexinfoOnt.tense, LexinfoOnt.present));
				break;
			case "f":
				infl.add(new PropertyResourcePair(LexinfoOnt.tense, LexinfoOnt.future));
				break;
			case "i":
				infl.add(new PropertyResourcePair(LexinfoOnt.tense, LexinfoOnt.imperfect));
				break;
			case "ps":
				infl.add(new PropertyResourcePair(LexinfoOnt.tense, LexinfoOnt.past));
				break;
			default:
				log.error("wikicode's tense part was not recognized for " + commonInflectionInformations.partOfSpeech + " form in article " + wdh.currentLexEntry());
				return null;
			}

			// Person
			switch (infos[2]) {
			case "1s":
				infl.add(new PropertyResourcePair(LexinfoOnt.person, LexinfoOnt.firstPerson));
				infl.add(new PropertyResourcePair(LexinfoOnt.number, LexinfoOnt.singular));
				break;
			case "2s":
				infl.add(new PropertyResourcePair(LexinfoOnt.person, LexinfoOnt.secondPerson));
				infl.add(new PropertyResourcePair(LexinfoOnt.number, LexinfoOnt.singular));
				break;
			case "3s":
				infl.add(new PropertyResourcePair(LexinfoOnt.person, LexinfoOnt.thirdPerson));
				infl.add(new PropertyResourcePair(LexinfoOnt.number, LexinfoOnt.singular));
				break;
			case "1p":
				infl.add(new PropertyResourcePair(LexinfoOnt.person, LexinfoOnt.firstPerson));
				infl.add(new PropertyResourcePair(LexinfoOnt.number, LexinfoOnt.plural));
				break;
			case "2p":
				infl.add(new PropertyResourcePair(LexinfoOnt.person, LexinfoOnt.secondPerson));
				infl.add(new PropertyResourcePair(LexinfoOnt.number, LexinfoOnt.plural));
				break;
			case "3p":
				infl.add(new PropertyResourcePair(LexinfoOnt.person, LexinfoOnt.thirdPerson));
				infl.add(new PropertyResourcePair(LexinfoOnt.number, LexinfoOnt.plural));
				break;
			default:
				log.error("wikicode's person part was not recognized for " + commonInflectionInformations.partOfSpeech + " form in article " + wdh.currentLexEntry());
				return null;
			}
		}

		return infl;
	}

	public void addInflectionMorphologicalSet(String pos, String canonicalForm, String wikicodeMophology) {
		if (!"verb".equals(pos)) {
			log.error("inflection macro not handled for " + pos + " form in article " + wdh.currentLexEntry());
			return;
		}

		HashSet<PropertyResourcePair> infl = morphologicalPropertiesFromWikicode(wikicodeMophology);

		commonInflectionInformations.inflections.add(infl);
	}

	protected void extractData(int startOffset, int endOffset, String lang, boolean extractForeignData) {
		if (lang == null) {
			return;
		}

		if (extractForeignData) {
			if ("fr".equals(lang))
				return;

			wdh.initializeEntryExtraction(wiktionaryPageName, lang);
		} else {
			if (!"fr".equals(lang))
				return;

			wdh.initializeEntryExtraction(wiktionaryPageName);		
		}

		Matcher m = WikiPatterns.macroPattern.matcher(pageContent);
		m.region(startOffset, endOffset);

		// WONTDO: (priority: low) should I use a macroOrLink pattern to detect translations that are not macro based ?
		// DONE: (priority: top) link the definition node with the current Part of Speech
		// DONE: (priority: top) type all nodes by prefixing it by language, or #pos or #def.
		// DONE: handle alternative spelling
		// DONE: extract synonyms
		// DONE: extract antonyms
		// DONE: add an IGNOREPOS currentBlock to ignore the entire part of speech

		currentBlock = Block.NOBLOCK;

		String pos, nym, sectionTitle;

		Map<String, String> sectionArgs;

		while (m.find()) {
			// Iterate until we find a new section

			if (m.group(1).equals("S")) {
				sectionArgs  = WikiTool.parseArgs(m.group(2));
				sectionTitle = sectionArgs.get("1");
			} else {
				sectionTitle = null;
				sectionArgs  = null;
			}

			if ( (pos = getPOS(m, sectionTitle, sectionArgs)) != null) {
				leaveCurrentBlock(m);
				if (pos.length() == 0)  {
					currentBlock = Block.IGNOREPOS;
				} else {
					blockStart = m.end();

					if (posIsInflection) {
						currentBlock = Block.INFLECTIONBLOCK;
						fillInflectionInformations(pos, sectionArgs);
					} else {
						currentBlock = Block.DEFBLOCK;
						wdh.addPartOfSpeech(pos);
					}
				}
			} else {
				if (currentBlock == Block.IGNOREPOS) {
					continue;
				}

				if (isTranslation(m, sectionTitle)) {
					leaveCurrentBlock(m);
					currentBlock = Block.TRADBLOCK;
				} else if (isAlternate(m, sectionTitle)) {
					leaveCurrentBlock(m);
					currentBlock = Block.ORTHOALTBLOCK;
				} else if (null != (nym = getNymHeader(m, sectionTitle))) {
					leaveCurrentBlock(m);
					currentBlock = Block.NYMBLOCK;
					currentNym = nym;
				} else if (isInflectionMacro(m, sectionTitle)) {
					for (int i = 3; i <= m.groupCount(); i++) {
						// an infection macro can hava several morphological information parameter
						addInflectionMorphologicalSet(pos, m.group(2), m.group(i).substring(0, m.group(i).indexOf('=')));
					}
				} else if (currentBlock == Block.INFLECTIONBLOCK) {
					if ("m".equals(m.group(1)) || "mf".equals(m.group(1))) {
						HashSet<PropertyResourcePair> infl = new HashSet<PropertyResourcePair>();
						infl.add(new PropertyResourcePair(LexinfoOnt.gender, LexinfoOnt.masculine));
						commonInflectionInformations.inflections.add(infl);
					}

					if ("f".equals(m.group(1)) || "mf".equals(m.group(1))) {
						HashSet<PropertyResourcePair> infl = new HashSet<PropertyResourcePair>();
						infl.add(new PropertyResourcePair(LexinfoOnt.gender, LexinfoOnt.feminine));
						commonInflectionInformations.inflections.add(infl);
					}

					continue;
				} else {
					if (isValidSection(m, sectionTitle)) {
						leaveCurrentBlock(m);
						currentBlock = Block.NOBLOCK;
					}

					continue;
				}

				if (currentBlock != Block.NOBLOCK) {
					blockStart = m.end();
				}
			}
		}
		
		// Finalize the entry parsing
		leaveCurrentBlock(m);

		wdh.finalizeEntryExtraction();
	}

	private void leaveCurrentBlock(Matcher m) {
		if (blockStart == -1) {
			return;
		}

		int end = computeRegionEnd(blockStart, m);

		switch (currentBlock) {
		case NOBLOCK:
		case IGNOREPOS:
			break;
		case INFLECTIONBLOCK:
			extractInflections(blockStart, end);
			break;
		case DEFBLOCK:
			extractDefinitions(blockStart, end);
			extractPronunciation(blockStart, end);
			extractOtherForms(blockStart, end);
			extractMorphologicalData(blockStart, end);
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
		default:
			assert false : "Unexpected block while ending extraction of entry: " + wiktionaryPageName;
		}

		blockStart = -1;
	}

	private static ArrayList<String> explode(char sep, String str) {
		int lastI = 0;
		ArrayList<String> res = new ArrayList<String>();
		int pos = str.indexOf(sep, lastI);

		while (pos != -1) {
			res.add(str.substring(lastI, pos));
			lastI = pos + 1;
			pos = str.indexOf(sep, lastI);
		}

		res.add(str.substring(lastI, str.length()));
		return res;
	}

	static void addAtomicMorphologicalInfo(Set<PropertyResourcePair> infos, String word) {
		switch(word) {
		case "singulier":
			infos.add(new PropertyResourcePair(LexinfoOnt.number, LexinfoOnt.singular));
			break;
		case "pluriel":
			infos.add(new PropertyResourcePair(LexinfoOnt.number, LexinfoOnt.plural));
			break;
		case "masculin":
			infos.add(new PropertyResourcePair(LexinfoOnt.gender, LexinfoOnt.masculine));
			break;
		case "féminin":
			infos.add(new PropertyResourcePair(LexinfoOnt.gender, LexinfoOnt.feminine));
			break;
		case "présent":
			infos.add(new PropertyResourcePair(LexinfoOnt.tense, LexinfoOnt.present));
			break;
		case "imparfait":
			infos.add(new PropertyResourcePair(LexinfoOnt.tense, LexinfoOnt.imperfect));
			break;
		case "passé":
			infos.add(new PropertyResourcePair(LexinfoOnt.tense, LexinfoOnt.past));
			break;
		case "futur":
			infos.add(new PropertyResourcePair(LexinfoOnt.tense, LexinfoOnt.future));
			break;
		case "indicatif":
			infos.add(new PropertyResourcePair(LexinfoOnt.mood, LexinfoOnt.indicative));
			break;
		case "subjonctif":
			infos.add(new PropertyResourcePair(LexinfoOnt.mood, LexinfoOnt.subjunctive));
			break;
		case "conditionnel":
			infos.add(new PropertyResourcePair(LexinfoOnt.mood, LexinfoOnt.conditional));
			break;
		case "impératif":
			infos.add(new PropertyResourcePair(LexinfoOnt.mood, LexinfoOnt.imperative));
			break;
		case "première personne":
			infos.add(new PropertyResourcePair(LexinfoOnt.person, LexinfoOnt.firstPerson));
			break;
		case "deuxième personne":
			infos.add(new PropertyResourcePair(LexinfoOnt.person, LexinfoOnt.secondPerson));
			break;
		case "troisième personne":
			infos.add(new PropertyResourcePair(LexinfoOnt.person, LexinfoOnt.thirdPerson));
			break;
		case "futur simple":
			infos.add(new PropertyResourcePair(LexinfoOnt.tense, LexinfoOnt.future));
			infos.add(new PropertyResourcePair(LexinfoOnt.mood, LexinfoOnt.indicative));
			break;
		case "passé simple":
			infos.add(new PropertyResourcePair(LexinfoOnt.tense, LexinfoOnt.past));
			infos.add(new PropertyResourcePair(LexinfoOnt.mood, LexinfoOnt.indicative));
			break;
		case "masculin singulier":
			infos.add(new PropertyResourcePair(LexinfoOnt.gender, LexinfoOnt.masculine));
			infos.add(new PropertyResourcePair(LexinfoOnt.number, LexinfoOnt.singular));
			break;
		case "féminin singulier":
			infos.add(new PropertyResourcePair(LexinfoOnt.gender, LexinfoOnt.feminine));
			infos.add(new PropertyResourcePair(LexinfoOnt.number, LexinfoOnt.singular));
			break;
		case "masculin pluriel":
			infos.add(new PropertyResourcePair(LexinfoOnt.gender, LexinfoOnt.masculine));
			infos.add(new PropertyResourcePair(LexinfoOnt.number, LexinfoOnt.plural));
			break;
		case "féminin pluriel":
			infos.add(new PropertyResourcePair(LexinfoOnt.gender, LexinfoOnt.feminine));
			infos.add(new PropertyResourcePair(LexinfoOnt.number, LexinfoOnt.plural));
			break;
		case "participe passé masculin singulier":
			infos.add(new PropertyResourcePair(LexinfoOnt.gender, LexinfoOnt.masculine));
			infos.add(new PropertyResourcePair(LexinfoOnt.number, LexinfoOnt.singular));
			break;
		case "participe passé féminin singulier":
			infos.add(new PropertyResourcePair(LexinfoOnt.gender, LexinfoOnt.feminine));
			infos.add(new PropertyResourcePair(LexinfoOnt.number, LexinfoOnt.singular));
			break;
		case "participe passé masculin pluriel":
			infos.add(new PropertyResourcePair(LexinfoOnt.verbFormMood, LexinfoOnt.participle));
			infos.add(new PropertyResourcePair(LexinfoOnt.tense,  LexinfoOnt.past));
			infos.add(new PropertyResourcePair(LexinfoOnt.gender, LexinfoOnt.masculine));
			infos.add(new PropertyResourcePair(LexinfoOnt.number, LexinfoOnt.plural));
			break;
		case "participe passé féminin pluriel":
			infos.add(new PropertyResourcePair(LexinfoOnt.verbFormMood, LexinfoOnt.participle));
			infos.add(new PropertyResourcePair(LexinfoOnt.tense,  LexinfoOnt.past));
			infos.add(new PropertyResourcePair(LexinfoOnt.gender, LexinfoOnt.feminine));
			infos.add(new PropertyResourcePair(LexinfoOnt.number, LexinfoOnt.plural));
			break;
		default:
			ArrayList<String> multiwords = explode(' ', word);
			if (multiwords.size() > 1) {
				for (String w : multiwords) {
					addAtomicMorphologicalInfo(infos, w);
				}
			}
		}
	}

	private void extractInflections(int blockStart, int end) {
		Matcher m = inflectionDefPattern.matcher(pageContent);
		m.region(blockStart, end);

		while (m.find()) {

			// Getting the canonical form of the inflection
			String canonicalForm = m.group(2);

			int pipePos = canonicalForm.indexOf('|');
			if (pipePos != -1) {
				canonicalForm = canonicalForm.substring(pipePos + 1);
			}

			Set<PropertyResourcePair> infos = new HashSet<PropertyResourcePair>();

			for (String info : m.group(1).split("de l’|du|de")) {
				addAtomicMorphologicalInfo(infos, info.trim().toLowerCase(frLocale));
			}

			if (commonInflectionInformations.inflections.size() == 0) {
				commonInflectionInformations.inflections.add(new HashSet<PropertyResourcePair>());
			}

			for (HashSet<PropertyResourcePair> inflection : commonInflectionInformations.inflections) {
				HashSet<PropertyResourcePair> union = new HashSet<PropertyResourcePair>(infos);
				union.addAll(inflection);

				wdh.registerInflection(
					commonInflectionInformations.languageCode,
					commonInflectionInformations.partOfSpeech,
					wdh.currentLexEntry(),
					canonicalForm,
					commonInflectionInformations.defNumber,
					union
				);
			}
		}
	}


	private boolean isValidSection(Matcher m, String sectionTitle) {
		return sectionTitle != null || sectionMarkers.contains(m.group(1));
	}

	private boolean posIsInflection;

	private class InflectionSection {
		String partOfSpeech;
		String languageCode;
		int defNumber;
		HashSet<HashSet<PropertyResourcePair>> inflections = new HashSet<HashSet<PropertyResourcePair>>();
	}

	private InflectionSection commonInflectionInformations;

	private void fillInflectionInformations(String pos, Map<String,String> sectionArgs) {
		commonInflectionInformations = new InflectionSection();
		commonInflectionInformations.partOfSpeech = pos;
		commonInflectionInformations.languageCode = LangTools.normalize(sectionArgs.get("2"));

		try {
			commonInflectionInformations.defNumber = Integer.parseInt(sectionArgs.get("num"));
		} catch (java.lang.NumberFormatException e) {
			commonInflectionInformations.defNumber = 0;
		}
	}

	private String getPOS(Matcher m, String sectionTitle, Map<String,String> sectionArgs) {
		if (sectionTitle != null) {
			if("flexion".equals(sectionArgs.get("3"))) {
				posIsInflection = true;
			}

			if (ignorablePosMarkers.contains(sectionTitle)) {
				return "";
			}
			return posMarkers.get(sectionTitle);
		}

		posIsInflection = false;
		return posMarkers.get(m.group(1));
	}

	protected boolean isTranslation(Matcher m, String sectionTitle) {
		if (sectionTitle != null) {
			return sectionTitle.startsWith("trad");
		}

		return m.group(1).equals("-trad-");
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
	
	private boolean isAlternate(Matcher m, String sectionTitle) {
		if (sectionTitle != null) {
			return variantSections.contains(sectionTitle);
		}

		return m.group(1).equals("-ortho-alt-") || m.group(1).equals("-var-ortho-");
	}
	


	private String getNymHeader(Matcher m, String sectionTitle) {
		if (sectionTitle != null) {
			return nymMarkerToNymName.get(sectionTitle);
		}

		return nymMarkerToNymName.get(m.group(1));
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
					lang = LangTools.normalize(g2.substring(0, i1));
					String usage = null;
					if ((i2 = g2.indexOf('|', i1 + 1)) == -1) {
						word = g2.substring(i1 + 1);
					} else {
						word  = g2.substring(i1 + 1, i2);
						usage = g2.substring(i2 + 1);
					}

					lang = FrenchLangtoCode.threeLettersCode(lang);

					if(lang != null) {
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

	protected void extractPronunciation(int startOffset, int endOffset) {
		extractPronunciation(startOffset, endOffset, true);
	}

	private String extractPronunciation(int startOffset, int endOffset, boolean registerPronunciation) {
		Matcher pronMatcher = pronunciationPattern.matcher(pageContent);
		pronMatcher.region(startOffset, endOffset);

		lastExtractedPronunciationLang = null;

		while (pronMatcher.find()) {
			String pron = pronMatcher.group(1);
			String lang = pronMatcher.group(2);

			if (pron == null || pron.equals("")) return null;
			if (lang == null || lang.equals("")) return null;
			
			if (pron.startsWith("1=")) pron = pron.substring(2);
			if (lang.startsWith("|2=")) lang = lang.substring(2);
			if (lang.startsWith("|lang=")) lang = lang.substring(5);

			lastExtractedPronunciationLang = lang;
			if (!pron.equals("")) {
				if (registerPronunciation) {
					wdh.registerPronunciation(pron, lang + "-fonipa");
				}
				return pron;
			}
		}
		return null;
	}

// 	static Pattern conjugationGroup = Pattern.compile("\\{\\{conjugaison\\|fr\\|groupe=(\\d)\\}\\}");

	private void extractMorphologicalData(int blockStart, int end) {
		String block = pageContent.substring(blockStart, end);

		if (block.matches("[\\s\\S]*\\{\\{m\\}\\}(?! *:)[\\s\\S]*") || block.matches("[\\s\\S]*\\{\\{mf\\}\\}(?! *:)[\\s\\S]*")) {
			wdh.registerProperty(LexinfoOnt.gender, LexinfoOnt.masculine);
		}

		if (block.matches("[\\s\\S]*\\{\\{f\\}\\}(?! *:)[\\s\\S]*") || block.matches("[\\s\\S]*\\{\\{mf\\}\\}(?! *:)[\\s\\S]*")) {
			wdh.registerProperty(LexinfoOnt.gender, LexinfoOnt.feminine);
		}

		if (block.matches("[\\s\\S]*\\{\\{plurale tantum|fr\\}\\}(?! *:)[\\s\\S]*")) {
			// plural-only word
			wdh.registerProperty(LexinfoOnt.number, LexinfoOnt.plural);
		}
	}
//
// // 		if (block.indexOf("{{t|fr}}") != -1) {
// // 			//FIXME check conformance
// // 			wdh.registerProperty(LexinfoOnt.property, LexinfoOnt.TransitiveFrame);
// // 		}
// //
// // 		if (block.indexOf("{{i|fr}}") {
// // 			//FIXME check conformance
// // 			wdh.registerProperty(LexinfoOnt.property, LexinfoOnt.IntransitiveFrame);
// // 		}
// //
// // 		Matcher m = conjugationGroup.matcher(block)
// // 		if (m.find()) {
// // 			//FIXME check conformance
// // 			wdh.registerProperty(DBnaryOnt.conjugationGroup, m.group(1));
// // 		}
// 	}

	public void extractExample(String example) {
		Map<Property, String> context = new HashMap<Property, String>();

		String ex = exampleExpander.expandExample(example, defTemplates, context);
		Resource exampleNode = null;
		if (ex != null && ! ex.equals("")) {
			exampleNode = wdh.registerExample(ex, context);
		}
	}

	private void extractOtherForms(int start, int end) {
		Matcher otherFormMatcher = otherFormPattern.matcher(pageContent);
		otherFormMatcher.region(start, end);

		while (otherFormMatcher.find()) {
			FrenchExtractorWikiModel dbnmodel = new FrenchExtractorWikiModel(wdh, wi, frLocale, "/${image}", "/${title}");
			dbnmodel.parseOtherForm(otherFormMatcher.group());
		}
	}
}

package org.getalp.dbnary;

import java.io.OutputStream;
import java.util.*;
import java.util.regex.Pattern;

import org.getalp.blexisma.api.ISO639_3;
import org.getalp.blexisma.api.ISO639_3.Lang;
import org.getalp.dbnary.tools.CounterSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.getalp.dbnary.DbnaryModel;

import org.getalp.dbnary.DBnaryOnt;
import org.getalp.dbnary.LemonOnt;
import org.getalp.dbnary.LexinfoOnt;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ReifiedStatement;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDF;

public class LemonBasedRDFDataHandler extends DbnaryModel implements WiktionaryDataHandler {

	protected static class PosAndType {
		protected Resource pos;
		protected Resource type;
		protected PosAndType(Resource p, Resource t) {this.pos = p; this.type = t;}
	}
	
	private Logger log = LoggerFactory.getLogger(LemonBasedRDFDataHandler.class);
	
	protected Model aBox;

	protected WiktionaryIndex wi;

	// States used for processing
	protected Resource currentLexEntry;
	protected Resource currentLexinfoPos;
	protected String currentWiktionaryPos;
	
	protected Resource currentSense;
	protected int currentSenseNumber;
	protected int currentSubSenseNumber;
	protected CounterSet translationCount = new CounterSet();
	private CounterSet reifiedNymCount = new CounterSet();
	protected String extractedLang;
	protected Resource lexvoExtractedLanguage;

	private Set<Statement> heldBackStatements = new HashSet<Statement>();

	protected int nbEntries = 0;
	protected String NS;
	protected String currentEncodedPageName;
	protected String currentWiktionaryPageName;
	protected CounterSet currentLexieCount = new CounterSet();
	private Resource currentMainLexEntry;
	private Resource currentPreferredWrittenRepresentation;
	
	private static class PrononciationPair {
		String pron, lang;
		public PrononciationPair(String pron, String lang) {
			this.pron = pron; this.lang = lang;
		}
	}

	private Set<PrononciationPair> currentSharedPronunciations;
//	private String currentSharedPronunciation;
//	private String currentSharedPronunciationLang;
	
	private static HashMap<String,Property> nymPropertyMap = new HashMap<String,Property>();
	private static HashMap<String,PosAndType> posAndTypeValueMap = new HashMap<String,PosAndType>();

	// TODO: crappy, please fix
	private static Resource otherPartOfSpeech = tBox.createResource(LexinfoOnt.NS + "otherPartOfSpeech");

	static {
				
		nymPropertyMap.put("syn", DBnaryOnt.synonym);
		nymPropertyMap.put("ant", DBnaryOnt.antonym);
		nymPropertyMap.put("hypo", DBnaryOnt.hyponym);
		nymPropertyMap.put("hyper", DBnaryOnt.hypernym);
		nymPropertyMap.put("mero", DBnaryOnt.meronym);
		nymPropertyMap.put("holo", DBnaryOnt.holonym);
		nymPropertyMap.put("qsyn", DBnaryOnt.approximateSynonym);


		// French
		posAndTypeValueMap.put("-nom-", new PosAndType(LexinfoOnt.noun, LemonOnt.Word));
		posAndTypeValueMap.put("-nom-pr-", new PosAndType(LexinfoOnt.properNoun, LemonOnt.Word));
		posAndTypeValueMap.put("-prénom-", new PosAndType(LexinfoOnt.properNoun, LemonOnt.Word));
		posAndTypeValueMap.put("-adj-", new PosAndType(LexinfoOnt.adjective, LemonOnt.Word));
		posAndTypeValueMap.put("-verb-", new PosAndType(LexinfoOnt.verb, LemonOnt.Word));
		posAndTypeValueMap.put("-adv-", new PosAndType(LexinfoOnt.adverb, LemonOnt.Word));
		posAndTypeValueMap.put("-loc-adv-", new PosAndType(LexinfoOnt.adverb, LemonOnt.Phrase));
		posAndTypeValueMap.put("-loc-adj-", new PosAndType(LexinfoOnt.adjective, LemonOnt.Phrase));
		posAndTypeValueMap.put("-loc-nom-", new PosAndType(LexinfoOnt.noun, LemonOnt.Phrase));
		posAndTypeValueMap.put("-loc-verb-", new PosAndType(LexinfoOnt.verb, LemonOnt.Phrase));

		// Portuguese
		posAndTypeValueMap.put("Substantivo", new PosAndType(LexinfoOnt.noun, LemonOnt.LexicalEntry));
		posAndTypeValueMap.put("Adjetivo", new PosAndType(LexinfoOnt.adjective, LemonOnt.LexicalEntry));
		posAndTypeValueMap.put("Verbo", new PosAndType(LexinfoOnt.verb, LemonOnt.LexicalEntry));
		posAndTypeValueMap.put("Advérbio", new PosAndType(LexinfoOnt.adverb, LemonOnt.LexicalEntry));

		// English
		posAndTypeValueMap.put("Noun", new PosAndType(LexinfoOnt.noun, LemonOnt.LexicalEntry));
		posAndTypeValueMap.put("Proper noun", new PosAndType(LexinfoOnt.properNoun, LemonOnt.LexicalEntry));
		posAndTypeValueMap.put("Adjective", new PosAndType(LexinfoOnt.adjective, LemonOnt.LexicalEntry));
		posAndTypeValueMap.put("Verb", new PosAndType(LexinfoOnt.verb, LemonOnt.LexicalEntry));
		posAndTypeValueMap.put("Adverb", new PosAndType(LexinfoOnt.adverb, LemonOnt.LexicalEntry));


		// German
		posAndTypeValueMap.put("Substantiv", new PosAndType(LexinfoOnt.noun, LemonOnt.LexicalEntry));
		posAndTypeValueMap.put("Nachname", new PosAndType(LexinfoOnt.properNoun, LemonOnt.LexicalEntry));
		posAndTypeValueMap.put("Vorname", new PosAndType(LexinfoOnt.properNoun, LemonOnt.LexicalEntry));
		posAndTypeValueMap.put("Adjektiv", new PosAndType(LexinfoOnt.adjective, LemonOnt.LexicalEntry));
		posAndTypeValueMap.put("Verb", new PosAndType(LexinfoOnt.verb, LemonOnt.LexicalEntry));
		posAndTypeValueMap.put("Adverb", new PosAndType(LexinfoOnt.adverb, LemonOnt.LexicalEntry));

		posAndTypeValueMap.put("Hilfsverb", new PosAndType(LexinfoOnt.verb, LemonOnt.LexicalEntry));
		// Italian
		posAndTypeValueMap.put("noun", new PosAndType(LexinfoOnt.noun, LemonOnt.LexicalEntry));
		posAndTypeValueMap.put("sost", new PosAndType(LexinfoOnt.noun, LemonOnt.LexicalEntry));

		posAndTypeValueMap.put("adjc", new PosAndType(LexinfoOnt.adjective, LemonOnt.LexicalEntry));
		posAndTypeValueMap.put("agg", new PosAndType(LexinfoOnt.adjective, LemonOnt.LexicalEntry));
		posAndTypeValueMap.put("verb", new PosAndType(LexinfoOnt.verb, LemonOnt.LexicalEntry));
		posAndTypeValueMap.put("adv", new PosAndType(LexinfoOnt.adverb, LemonOnt.LexicalEntry));

		// Finnish
		posAndTypeValueMap.put("Substantiivi", new PosAndType(LexinfoOnt.noun, LemonOnt.LexicalEntry));
		posAndTypeValueMap.put("Adjektiivi", new PosAndType(LexinfoOnt.adjective, LemonOnt.LexicalEntry));
		posAndTypeValueMap.put("Verbi", new PosAndType(LexinfoOnt.verb, LemonOnt.LexicalEntry));
		posAndTypeValueMap.put("Adverbi", new PosAndType(LexinfoOnt.adverb, LemonOnt.LexicalEntry));
		posAndTypeValueMap.put("Erisnimi", new PosAndType(LexinfoOnt.properNoun, LemonOnt.LexicalEntry));
		posAndTypeValueMap.put("subs", new PosAndType(LexinfoOnt.noun, LemonOnt.LexicalEntry));
		posAndTypeValueMap.put("adj", new PosAndType(LexinfoOnt.adjective, LemonOnt.LexicalEntry));
		posAndTypeValueMap.put("verbi", new PosAndType(LexinfoOnt.verb, LemonOnt.LexicalEntry));
		posAndTypeValueMap.put("adv", new PosAndType(LexinfoOnt.adverb, LemonOnt.LexicalEntry));
		
		// Greek
		posAndTypeValueMap.put("επίθετο", new PosAndType(LexinfoOnt.adjective, LemonOnt.LexicalEntry));
		posAndTypeValueMap.put("επίρρημα", new PosAndType(LexinfoOnt.adverb, LemonOnt.LexicalEntry));
		posAndTypeValueMap.put("ουσιαστικό", new PosAndType(LexinfoOnt.noun, LemonOnt.LexicalEntry));
		posAndTypeValueMap.put("ρήμα", new PosAndType(LexinfoOnt.verb, LemonOnt.LexicalEntry));
		posAndTypeValueMap.put("κύριο όνομα", new PosAndType(LexinfoOnt.properNoun, LemonOnt.LexicalEntry));

		posAndTypeValueMap.put("", new PosAndType(otherPartOfSpeech, LemonOnt.LexicalEntry));

	}

	// Map of the String to lexvo language entity
	private HashMap<String,Resource> languages = new HashMap<String, Resource>();
	
	public LemonBasedRDFDataHandler(String lang) {
		super();
		
		NS = DBNARY_URL + "/" + lang + "/";
		
		Lang l = ISO639_3.sharedInstance.getLang(lang);
		extractedLang = (null != l.getPart1()) ? l.getPart1() : l.getId();	
		lexvoExtractedLanguage = tBox.createResource(LEXVO + lang);
		
		// Create aBox
		aBox = ModelFactory.createDefaultModel();
			
		aBox.setNsPrefix(lang, NS);
		aBox.setNsPrefix("dbnary", DBnaryOnt.NS);
		aBox.setNsPrefix("lemon", LemonOnt.NS);
		aBox.setNsPrefix("lexinfo", LexinfoOnt.NS);
		aBox.setNsPrefix("rdfs", RDFS);
		aBox.setNsPrefix("dcterms", DCTerms.NS);
		aBox.setNsPrefix("lexvo", LEXVO);
		aBox.setNsPrefix("rdf", RDF.getURI());

	}
	
	@Override
	public void initializeEntryExtraction(String wiktionaryPageName) {
		currentSense = null;
		currentSenseNumber = 0;
		currentSubSenseNumber = 0;
		currentWiktionaryPageName = wiktionaryPageName;
		currentLexinfoPos = null;
		currentWiktionaryPos = null;
		currentLexieCount.resetAll();
		translationCount.resetAll();
		reifiedNymCount.resetAll();
		currentPreferredWrittenRepresentation = null;
		currentSharedPronunciations = new HashSet<PrononciationPair>();

		// Create a dummy lexical entry that points to the one that corresponds to a part of speech
		currentMainLexEntry = getVocable(wiktionaryPageName, true);

		// Create the resource without typing so that the type statement is added only if the currentStatement are added to the model.

		// Retain these statements to be inserted in the model when we know that the entry corresponds to a proper part of speech
		heldBackStatements.add(aBox.createStatement(currentMainLexEntry, RDF.type, DBnaryOnt.Vocable));

		currentEncodedPageName = null;
		currentLexEntry = null;
	}

	@Override
	public void finalizeEntryExtraction() {
		// Clear currentStatements. If statemenents do exist-s in it, it is because, there is no extractable part of speech in the entry.
		heldBackStatements.clear();
		promoteNymProperties();
	}

	
	public void addPartOfSpeech(String originalPOS, Resource normalizedPOS, Resource normalizedType) {
		// DONE: create a LexicalEntry for this part of speech only and attach info to it.
		currentWiktionaryPos = originalPOS;
		currentLexinfoPos = normalizedPOS;
		
		nbEntries++;

		currentEncodedPageName = uriEncode(currentWiktionaryPageName, currentWiktionaryPos) + "__" + currentLexieCount.incr(currentWiktionaryPos);
		currentLexEntry = aBox.createResource(getPrefix() + currentEncodedPageName, normalizedType);

		// All translation numbers are local to a lexEntry
		translationCount.resetAll();
		reifiedNymCount.resetAll();

		currentPreferredWrittenRepresentation = aBox.createResource();

		// If a pronunciation was given before the first part of speech, it means that it is shared amoung pos/etymologies
		for (PrononciationPair p : currentSharedPronunciations) {
			if (null != p.lang && p.lang.length() > 0)  {
				aBox.add(currentPreferredWrittenRepresentation, LexinfoOnt.pronunciation, p.pron, p.lang);
			} else {
				aBox.add(currentPreferredWrittenRepresentation, LexinfoOnt.pronunciation, p.pron);
			}
		}

		aBox.add(currentLexEntry, LemonOnt.canonicalForm, currentPreferredWrittenRepresentation);
		aBox.add(currentPreferredWrittenRepresentation, LemonOnt.writtenRep, currentWiktionaryPageName, extractedLang);
		aBox.add(currentLexEntry, DBnaryOnt.partOfSpeech, currentWiktionaryPos);
		if (null != currentLexinfoPos)
			aBox.add(currentLexEntry, LexinfoOnt.partOfSpeech, currentLexinfoPos);
		aBox.add(currentLexEntry, LemonOnt.language, extractedLang);
		aBox.add(currentLexEntry, DCTerms.language, lexvoExtractedLanguage);

		// Register the pending statements.
		for (Statement s: heldBackStatements) {
			aBox.add(s);
		}
		heldBackStatements.clear();
		aBox.add(currentMainLexEntry, DBnaryOnt.refersTo, currentLexEntry);
	}
	
	@Override
	public void addPartOfSpeech(String pos) {
		PosAndType pat = posAndTypeValueMap.get(pos);
		Resource lexinfoPOS = (null == pat) ? null : pat.pos;
		Resource entryType = (null == pat) ? LemonOnt.LexicalEntry : pat.type;

		addPartOfSpeech(pos, lexinfoPOS, entryType);
	}

	
	@Override
	public void registerAlternateSpelling(String alt) {
		if (null == currentLexEntry) {
			log.debug("Registering Alternate Spelling when lex entry is null in \"{}\".", this.currentMainLexEntry);
			return; // Don't register anything if current lex entry is not known.
		}

		Resource altlemma = aBox.createResource();
		aBox.add(currentLexEntry, LemonOnt.lexicalVariant, altlemma);
		aBox.add(altlemma, LemonOnt.writtenRep, alt, extractedLang);
	}

	@Override
	public void registerNewDefinition(String def) {
		this.registerNewDefinition(def, 1);
	}

	@Override
	public void registerNewDefinition(String def, int lvl) {
		if (null == currentLexEntry) {
			log.debug("Registering Word Sense when lex entry is null in \"{}\".", this.currentMainLexEntry);
			return; // Don't register anything if current lex entry is not known.
		}
		if (lvl > 1) {
			currentSubSenseNumber++;
		} else {
			currentSenseNumber++;
			currentSubSenseNumber = 0;
		}
		registerNewDefinition(def, computeSenseNum());
	}

	public void registerNewDefinition(String def, String senseNumber) {
		if (null == currentLexEntry) {
			log.debug("Registering Word Sense when lex entry is null in \"{}\".", this.currentMainLexEntry);
			return; // Don't register anything if current lex entry is not known.
		}
		
		// Create new word sense + a definition element 
		currentSense = aBox.createResource(computeSenseId(senseNumber), LemonOnt.LexicalSense);
		aBox.add(currentLexEntry, LemonOnt.sense, currentSense);
		aBox.add(aBox.createLiteralStatement(currentSense, DBnaryOnt.senseNumber, aBox.createTypedLiteral(senseNumber)));
		// pos is not usefull anymore for word sense as they should be correctly linked to an entry with only one pos.
		// if (currentPos != null && ! currentPos.equals("")) {
		//	aBox.add(currentSense, LexinfoOnt.partOfSpeech, currentPos);
		//}

		Resource defNode = aBox.createResource();
		aBox.add(currentSense, LemonOnt.definition, defNode);
		// Keep a human readable version of the definition, removing all links annotations.
		aBox.add(defNode, LemonOnt.value, AbstractWiktionaryExtractor.cleanUpMarkup(def, true), extractedLang);

		// TODO: Extract domain/usage field from the original definition.

	}

	private String computeSenseId(String senseNumber) {
		return getPrefix() + "__ws_" + senseNumber + "_" + currentEncodedPageName;
	}
	
	private String computeSenseNum() {
		return "" + currentSenseNumber + ((currentSubSenseNumber == 0) ? "" : (char) ('a' + currentSubSenseNumber - 1));
	}

    protected Resource registerTranslationToEntity(Resource entity, String lang, String currentGlose, String usage, String word) {
		if (null == entity) {
			log.debug("Registering Translation when lex entry is null in \"{}\".", this.currentMainLexEntry);
			return null; // Don't register anything if current lex entry is not known.
		}
		word = word.trim();
		// Do not register empty translations
		if (word.length() == 0) {
			return null;
		}
		// Ensure language is in its standard form.
		Lang t = ISO639_3.sharedInstance.getLang(lang);
		if (null != t) lang = t.getId();
		Resource trans = aBox.createResource(computeTransId(lang, entity), DBnaryOnt.Translation);
    	aBox.add(trans, DBnaryOnt.isTranslationOf, entity);
		aBox.add(createTargetLanguageProperty(trans, lang));

		if (null == t) {
			aBox.add(trans, DBnaryOnt.writtenForm, word);
		} else {
			String tl = (null != t.getPart1()) ? t.getPart1() : t.getId();
			aBox.add(trans, DBnaryOnt.writtenForm, word, tl);
		}

		if (currentGlose != null && ! currentGlose.equals("")) {
			aBox.add(trans, DBnaryOnt.gloss, currentGlose, extractedLang);
		}

		if (usage != null && ! usage.equals("")) {
			aBox.add(trans, DBnaryOnt.usage, usage);
		}
    	return trans;
	}

	@Override
    public void registerTranslation(String lang, String currentGlose, String usage, String word) {
		registerTranslationToEntity(currentLexEntry, lang, currentGlose, usage, word);
	}

	public Resource getVocable(String vocable, boolean dontLinkWithType) {
		if (dontLinkWithType) {
			return aBox.createResource(getPrefix() + uriEncode(vocable));
		}
		return aBox.createResource(getPrefix() + uriEncode(vocable), DBnaryOnt.Vocable);
	}

	public Resource getVocable(String vocable) {
		return getVocable(vocable, false);
	}

// 	private void addInflectionMorphology(Resource inflectionResource, String wikicodeMorphology) {
// 		aBox.add(inflectionResource, DBnaryOnt.wikiMorphology, wikicodeMorphology);
// 	}

	// The InflectionIdentity class is here to provide handle inflection unicity.
	// An inflection i1 is equivalent to another inflection i2 iff :
	//  - the part of speech
	//  - the written form
	//  - the morphological data associated to the form
	//  - the dictionary entry to which it is associated
	// are all equal between the two inflections.

// 	private class InflectionIdentity {
// 		private final Resource r;
// 		private final InflectionData i;
// 		private final Set<String> pronunciations = new HashSet<String>();
// 		private final Resource representation;
// 
// 		InflectionIdentity(InflectionData infl, Resource referenceEntry) {
// 			r = referenceEntry;
// 			i = infl;
// 
// 			representation = aBox.createResource();
// 		}
// 
// 		@Override
// 		public boolean equals(Object ob) {
// 			InflectionIdentity o = (InflectionIdentity) ob;
// 
// 			if (o == null) {
// 				return false;
// 			}
// 
// 			return this == o || (
// 				     r == o.r
// 				  && i.equals(o.i)
// 			);
// 		}
// 	}
// 
// 	private Map<infl, InflectionIdentity> registeredInflections = new HashMap<infl, InflectionIdentity>();
// 
// 
// 	public void registerInflection(InflectionData infl) {
// 		registerInflection(infl, getVocable(infl.canonicalForm));
// 	}
// 
// 	public void registerInflection(InflectionData infl, Resource referenceEntry) {
// 		if (infl.canonicalForm == null) {
// 			if (null == currentLexEntry) {
// 				log.debug("Registering inflection when lex entry is null in \"{}\".", this.currentMainLexEntry);
// 				return; // Don't register anything if current lex entry is not known.
// 			}
// 
// 			infl.canonicalForm = currentLexEntry();
// 		}
// 
// 		if (infl.normalizedInflectionPOS == null) {
// 			PosAndType pat = posAndTypeValueMap.get(infl.pos);
// 			infl.normalizedInflectionPOS = (null == pat) ? null : pat.pos;
// 		}
// 
//         InflectionIdentity ii = registeredInflections.get(infl);
// 		if (ii != null) {
// 			if (infl.pronunciation != null && infl.pronunciation.length() != 0 && !ii.pronunciations.contains(infl.pronunciation)) {
// 				ii.pronunciations.add(infl.pronunciation);
// 				registerPronunciation(ii.representation, infl.pronunciation, infl.pronunciationLang);
// 			}
// 			return;
// 		}
// 		
// 		String inflectionEntryName = NS + "inflection__" + inflectionPOS + "__" + canonicalForm;
// 
// 		Resource inflectionEntry = aBox.createResource(
// 		                             inflectionEntryName,
// 		                             DBnaryOnt.Inflection
// 		                         );
// 
// 		ii = new InflectionIdentity(infl, referenceEntry);
// 
// 		registeredInflections.put(infl, ii);
// 
// 		aBox.add(inflectionEntry, DBnaryOnt.inflectionOf, referenceEntry);
// 		aBox.add(inflectionEntry, DBnaryOnt.inflectionType, normalizedInflectionPOS);
// 
// 		aBox.add(ii.representation, LemonOnt.writtenRep, inflectionForm);
// 
// 		addInflectionMorphology(representation, wikicodeMorphology);
// 
// 		aBox.add(inflectionEntry, DBnaryOnt.inflectionForm, ii.representation);
// 	}

	private Statement createTargetLanguageProperty(Resource trans, String lang) {
		lang = lang.trim();
		if (isAnISO639_3Code(lang)) {
			return aBox.createStatement(trans, DBnaryOnt.targetLanguage, getLexvoLanguageResource(lang));
		} else {
			return aBox.createStatement(trans, DBnaryOnt.targetLanguageCode, lang);
		}
	}

 	private final static Pattern iso3letters = Pattern.compile("\\w{3}");

	private boolean isAnISO639_3Code(String lang) {
		// TODO For the moment, only check if the code is a 3 letter code...
		return iso3letters.matcher(lang).matches();
	}

	private String computeTransId(String lang, Resource entity) {
		lang = uriEncode(lang);
		return getPrefix() + "__tr_" + lang + "_" + translationCount.incr(lang) + "_" + entity.getURI().substring(getPrefix().length());
	}

	private Resource getLexvoLanguageResource(String lang) {
		Resource res = languages.get(lang);
		if (res == null) {
			res = tBox.createResource(LEXVO + lang);
			languages.put(lang, res);
		}
		return res;
	}

	public void registerNymRelationToEntity(String target, String synRelation, Resource entity) {
		if (null == entity) {
			log.debug("Registering Lexical Relation when lex entry is null in \"{}\".", this.currentMainLexEntry);
			return; // Don't register anything if current lex entry is not known.
		}
		// Some links point to Annex pages or Images, just ignore these.
		int colon = target.indexOf(':');
		if (colon != -1) {
			return;
		}
		int hash = target.indexOf('#');
		if (hash != -1) {
			// The target contains an intra page href. Remove it from the target uri and keep it in the relation.
			target = target.substring(0,hash);
			// TODO: keep additional intra-page href
			// aBox.add(nym, isAnnotatedBy, target.substring(hash));
		}
		
		Property nymProperty = nymPropertyMap.get(synRelation);
		
		Resource targetResource = getVocable(target);
		
		aBox.add(entity, nymProperty, targetResource);
	}

	@Override
	public void registerNymRelation(String target, String synRelation) {
		registerNymRelationToEntity(target, synRelation, currentLexEntry);
	}
	
	@Override
	public void registerNymRelation(String target, String synRelation, String gloss) {
		if (null == currentLexEntry) {
			log.debug("Registering Lexical Relation when lex entry is null in \"{}\".", this.currentMainLexEntry);
			return; // Don't register anything if current lex entry is not known.
		}
		// Some links point to Annex pages or Images, just ignore these.
		int colon = target.indexOf(':');
		if (colon != -1) {
			return;
		}
		int hash = target.indexOf('#');
		if (hash != -1) {
			// The target contains an intra page href. Remove it from the target uri and keep it in the relation.
			target = target.substring(0,hash);
			// TODO: keep additional intra-page href
			// aBox.add(nym, isAnnotatedBy, target.substring(hash));
		}
		Property nymProperty = nymPropertyMap.get(synRelation);
		
		Resource targetResource = getVocable(target);
		
		Statement nymR = aBox.createStatement(currentLexEntry, nymProperty, targetResource);
		aBox.add(nymR);
		ReifiedStatement rnymR = nymR.createReifiedStatement(computeNymId(synRelation));
		rnymR.addProperty(DBnaryOnt.gloss, gloss);
		
	}

	private String computeNymId(String nym) {
		return getPrefix() + "__" + nym + "_" + reifiedNymCount.incr(nym) + "_" + currentEncodedPageName;
	}

	@Override
	public void registerNymRelationOnCurrentSense(String target, String synRelation) {
		if (null == currentSense) {
			log.debug("Registering Lexical Relation when current sense is null in \"{}\".", this.currentMainLexEntry);
			registerNymRelation(target, synRelation);
			return ; // Don't register anything if current lex entry is not known.
		}
		// Some links point to Annex pages or Images, just ignore these.
		int colon = target.indexOf(':');
		if (colon != -1) {
			return;
		}
		int hash = target.indexOf('#');
		if (hash != -1) {
			// The target contains an intra page href. Remove it from the target uri and keep it in the relation.
			target = target.substring(0,hash);
			// TODO: keep additional intra-page href
			// aBox.add(nym, isAnnotatedBy, target.substring(hash));
		}
		
		Property nymProperty = nymPropertyMap.get(synRelation);
		
		Resource targetResource = getVocable(target);

		aBox.add(currentSense, nymProperty, targetResource);
	}

	@Override
	public void registerPronunciation(String pron, String lang) {
		if (null == currentPreferredWrittenRepresentation) {
			currentSharedPronunciations.add(new PrononciationPair(pron, lang));
		} else {
			registerPronunciation(currentPreferredWrittenRepresentation, pron, lang);
		}
	}

	private void registerPronunciation(Resource writtenRepresentation, String pron, String lang) {
		if (null != lang && lang.length() > 0) {
			aBox.add(writtenRepresentation, LexinfoOnt.pronunciation, pron, lang);
		} else {
			aBox.add(writtenRepresentation, LexinfoOnt.pronunciation, pron);
		}
	}

	private void promoteNymProperties() {
		StmtIterator entries = currentMainLexEntry.listProperties(DBnaryOnt.refersTo);
		HashSet<Statement> toBeRemoved = new HashSet<Statement>();
		while (entries.hasNext()) {
			Resource lu = entries.next().getResource();
			List<Statement> senses = lu.listProperties(LemonOnt.sense).toList();
			if (senses.size() == 1) {
				Resource s = senses.get(0).getResource();
				HashSet<Property> alreadyProcessedNyms = new HashSet<Property>();
				for (Property nymProp: nymPropertyMap.values()) {
					if (alreadyProcessedNyms.contains(nymProp)) continue;
					alreadyProcessedNyms.add(nymProp);
					StmtIterator nyms = lu.listProperties(nymProp);
					while (nyms.hasNext()) {
						Statement nymRel = nyms.next();
						aBox.add(s, nymProp, nymRel.getObject());
						toBeRemoved.add(nymRel);
					}
				}
			}
		}
		for (Statement s: toBeRemoved) {
			s.remove();
		}
	}

	public void setWiktionaryIndex(WiktionaryIndex wi) {
		this.wi = wi;
	}
	
	public void dump(OutputStream out) {
		dump(out, null);
	}

	/**
	 * Write a serialized representation of this model in a specified language.
	 * The language in which to write the model is specified by the lang argument. 
	 * Predefined values are "RDF/XML", "RDF/XML-ABBREV", "N-TRIPLE", "TURTLE", (and "TTL") and "N3". 
	 * The default value, represented by null, is "RDF/XML".
	 * @param out
	 * @param format
	 */
	public void dump(OutputStream out, String format) {
		aBox.write(out, format);
	}

	@Override
	public int nbEntries() {
		return nbEntries;
	}

	@Override
	public String currentLexEntry() {
		// TODO Auto-generated method stub
		return currentWiktionaryPageName;
	}

	public String getPrefix() {
		return NS;
	}

	@Override
	public void initializeEntryExtraction(String wiktionaryPageName, String lang) {
		// TODO Auto-generated method stub
		throw new RuntimeException("Cannot initialize a foreign language entry.");
	}

	@Override
	public Resource registerExample(String ex, Map<Property, String> context) {
		if (null == currentSense) {
			log.debug("Registering example when lex sense is null in \"{}\".", this.currentMainLexEntry);
			return null; // Don't register anything if current lex entry is not known.
		}
		
		// Create new word sense + a definition element 
    	Resource example = aBox.createResource();	
    	aBox.add(aBox.createStatement(example, LemonOnt.value, ex, extractedLang));
        if (null != context) {
            for (Map.Entry<Property, String> c : context.entrySet()) {
                aBox.add(aBox.createStatement(example,c.getKey(),c.getValue(),extractedLang));
            }
        }
    	aBox.add(aBox.createStatement(currentSense, LemonOnt.example, example));
		return example;

	}

	public void registerOtherForm(String form)  {
        Resource otherForm = aBox.createResource();
        aBox.add(currentLexEntry, LemonOnt.otherForm, otherForm);
        aBox.add(otherForm, LemonOnt.writtenRep, form, extractedLang);
	}
}

package org.getalp.dbnary;

import java.io.OutputStream;
import java.util.*;
import java.util.regex.Pattern;

import org.getalp.blexisma.api.ISO639_3;
import org.getalp.blexisma.api.ISO639_3.Lang;
import org.getalp.dbnary.tools.CounterSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	static {
				
		nymPropertyMap.put("syn", synonymProperty);
		nymPropertyMap.put("ant", antonymProperty);
		nymPropertyMap.put("hypo", hyponymProperty);
		nymPropertyMap.put("hyper", hypernymProperty);
		nymPropertyMap.put("mero", meronymProperty);
		nymPropertyMap.put("holo", holonymProperty);
		nymPropertyMap.put("qsyn", nearSynonymProperty);


		// French
		posAndTypeValueMap.put("-nom-", new PosAndType(nounPOS, wordEntryType));
		posAndTypeValueMap.put("-nom-pr-", new PosAndType(properNounPOS, wordEntryType));
		posAndTypeValueMap.put("-prénom-", new PosAndType(properNounPOS, wordEntryType));
		posAndTypeValueMap.put("-adj-", new PosAndType(adjPOS, wordEntryType));
		posAndTypeValueMap.put("-verb-", new PosAndType(verbPOS, wordEntryType));
		posAndTypeValueMap.put("-adv-", new PosAndType(adverbPOS, wordEntryType));
		posAndTypeValueMap.put("-loc-adv-", new PosAndType(adverbPOS, phraseEntryType));
		posAndTypeValueMap.put("-loc-adj-", new PosAndType(adjPOS, phraseEntryType));
		posAndTypeValueMap.put("-loc-nom-", new PosAndType(nounPOS, phraseEntryType));
		posAndTypeValueMap.put("-loc-verb-", new PosAndType(verbPOS, phraseEntryType));

		// Portuguese
		posAndTypeValueMap.put("Substantivo", new PosAndType(nounPOS, lexEntryType));
		posAndTypeValueMap.put("Adjetivo", new PosAndType(adjPOS, lexEntryType));
		posAndTypeValueMap.put("Verbo", new PosAndType(verbPOS, lexEntryType));
		posAndTypeValueMap.put("Advérbio", new PosAndType(adverbPOS, lexEntryType));

		// English
		posAndTypeValueMap.put("Noun", new PosAndType(nounPOS, lexEntryType));
		posAndTypeValueMap.put("Proper noun", new PosAndType(properNounPOS, lexEntryType));
		posAndTypeValueMap.put("Adjective", new PosAndType(adjPOS, lexEntryType));
		posAndTypeValueMap.put("Verb", new PosAndType(verbPOS, lexEntryType));
		posAndTypeValueMap.put("Adverb", new PosAndType(adverbPOS, lexEntryType));


		// German
		posAndTypeValueMap.put("Substantiv", new PosAndType(nounPOS, lexEntryType));
		posAndTypeValueMap.put("Nachname", new PosAndType(properNounPOS, lexEntryType));
		posAndTypeValueMap.put("Vorname", new PosAndType(properNounPOS, lexEntryType));
		posAndTypeValueMap.put("Adjektiv", new PosAndType(adjPOS, lexEntryType));
		posAndTypeValueMap.put("Verb", new PosAndType(verbPOS, lexEntryType));
		posAndTypeValueMap.put("Adverb", new PosAndType(adverbPOS, lexEntryType));

		posAndTypeValueMap.put("Hilfsverb", new PosAndType(verbPOS, lexEntryType));
		// Italian
		posAndTypeValueMap.put("noun", new PosAndType(nounPOS, lexEntryType));
		posAndTypeValueMap.put("sost", new PosAndType(nounPOS, lexEntryType));

		posAndTypeValueMap.put("adjc", new PosAndType(adjPOS, lexEntryType));
		posAndTypeValueMap.put("agg", new PosAndType(adjPOS, lexEntryType));
		posAndTypeValueMap.put("verb", new PosAndType(verbPOS, lexEntryType));
		posAndTypeValueMap.put("adv", new PosAndType(adverbPOS, lexEntryType));

		// Finnish
		posAndTypeValueMap.put("Substantiivi", new PosAndType(nounPOS, lexEntryType));
		posAndTypeValueMap.put("Adjektiivi", new PosAndType(adjPOS, lexEntryType));
		posAndTypeValueMap.put("Verbi", new PosAndType(verbPOS, lexEntryType));
		posAndTypeValueMap.put("Adverbi", new PosAndType(adverbPOS, lexEntryType));
		posAndTypeValueMap.put("Erisnimi", new PosAndType(properNounPOS, lexEntryType));
		posAndTypeValueMap.put("subs", new PosAndType(nounPOS, lexEntryType));
		posAndTypeValueMap.put("adj", new PosAndType(adjPOS, lexEntryType));
		posAndTypeValueMap.put("verbi", new PosAndType(verbPOS, lexEntryType));
		posAndTypeValueMap.put("adv", new PosAndType(adverbPOS, lexEntryType));
		
		// Greek
		posAndTypeValueMap.put("επίθετο", new PosAndType(adjPOS, lexEntryType));
		posAndTypeValueMap.put("επίρρημα", new PosAndType(adverbPOS, lexEntryType));
		posAndTypeValueMap.put("ουσιαστικό", new PosAndType(nounPOS, lexEntryType));
		posAndTypeValueMap.put("ρήμα", new PosAndType(verbPOS, lexEntryType));
		posAndTypeValueMap.put("κύριο όνομα", new PosAndType(properNounPOS, lexEntryType));

		posAndTypeValueMap.put("", new PosAndType(otherPOS, lexEntryType));

	}
	
	// Map of the String to lexvo language entity
	private HashMap<String,Resource> languages = new HashMap<String, Resource>();
	
	public LemonBasedRDFDataHandler(String lang) {
		super();
		
		NS = DBNARY_NS_PREFIX + "/" + lang + "/";
		
		Lang l = ISO639_3.sharedInstance.getLang(lang);
		extractedLang = (null != l.getPart1()) ? l.getPart1() : l.getId();	
		lexvoExtractedLanguage = tBox.createResource(LEXVO + lang);
		
		// Create aBox
		aBox = ModelFactory.createDefaultModel();
			
		aBox.setNsPrefix(lang, NS);
		aBox.setNsPrefix("dbnary", DBNARY);
		aBox.setNsPrefix("lemon", LEMON);
		aBox.setNsPrefix("lexinfo", LEXINFO);
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
		heldBackStatements.add(aBox.createStatement(currentMainLexEntry, RDF.type, vocableEntryType));

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
				aBox.add(currentPreferredWrittenRepresentation, pronProperty, p.pron, p.lang);
			} else {
				aBox.add(currentPreferredWrittenRepresentation, pronProperty, p.pron);
			}
		}

		aBox.add(currentLexEntry, canonicalFormProperty, currentPreferredWrittenRepresentation);
		aBox.add(currentPreferredWrittenRepresentation, writtenRepProperty, currentWiktionaryPageName, extractedLang);
		aBox.add(currentLexEntry, dbnaryPosProperty, currentWiktionaryPos);
		if (null != currentLexinfoPos)
			aBox.add(currentLexEntry, posProperty, currentLexinfoPos);
		aBox.add(currentLexEntry, languageProperty, extractedLang);
		aBox.add(currentLexEntry, DCTerms.language, lexvoExtractedLanguage);

		// Register the pending statements.
		for (Statement s: heldBackStatements) {
			aBox.add(s);
		}
		heldBackStatements.clear();
		aBox.add(currentMainLexEntry, refersTo, currentLexEntry);
	}
	
	@Override
	public void addPartOfSpeech(String pos) {
		PosAndType pat = posAndTypeValueMap.get(pos);
		Resource lexinfoPOS = (null == pat) ? null : pat.pos;
		Resource entryType = (null == pat) ? lexEntryType : pat.type;

		addPartOfSpeech(pos, lexinfoPOS, entryType);
	}

	
	@Override
	public void registerAlternateSpelling(String alt) {
		if (null == currentLexEntry) {
			log.debug("Registering Alternate Spelling when lex entry is null in \"{}\".", this.currentMainLexEntry);
			return; // Don't register anything if current lex entry is not known.
		}

		Resource altlemma = aBox.createResource();
		aBox.add(currentLexEntry, lexicalVariantProperty, altlemma);
		aBox.add(altlemma, writtenRepProperty, alt, extractedLang);
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
		currentSense = aBox.createResource(computeSenseId(senseNumber), lexicalSenseType);
		aBox.add(currentLexEntry, lemonSenseProperty, currentSense);
		aBox.add(aBox.createLiteralStatement(currentSense, senseNumberProperty, aBox.createTypedLiteral(senseNumber)));
		// pos is not usefull anymore for word sense as they should be correctly linked to an entry with only one pos.
		// if (currentPos != null && ! currentPos.equals("")) {
		//	aBox.add(currentSense, posProperty, currentPos);
		//}

		Resource defNode = aBox.createResource();
		aBox.add(currentSense, lemonDefinitionProperty, defNode);
		// Keep a human readable version of the definition, removing all links annotations.
		aBox.add(defNode, lemonValueProperty, AbstractWiktionaryExtractor.cleanUpMarkup(def, true), extractedLang);

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
		Resource trans = aBox.createResource(computeTransId(lang, entity), translationType);
    	aBox.add(trans, isTranslationOf, entity);
		aBox.add(createTargetLanguageProperty(trans, lang));

		if (null == t) {
			aBox.add(trans, equivalentTargetProperty, word);
		} else {
			String tl = (null != t.getPart1()) ? t.getPart1() : t.getId();
			aBox.add(trans, equivalentTargetProperty, word, tl);
		}

		if (currentGlose != null && ! currentGlose.equals("")) {
			aBox.add(trans, glossProperty, currentGlose, extractedLang);
		}

		if (usage != null && ! usage.equals("")) {
			aBox.add(trans, usageProperty, usage);
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
		return aBox.createResource(getPrefix() + uriEncode(vocable), vocableEntryType);
	}

	public Resource getVocable(String vocable) {
		return getVocable(vocable, false);
	}

// 	private void addInflectionMorphology(Resource inflectionResource, String wikicodeMorphology) {
// 		aBox.add(inflectionResource, hasWikiCodeMorphology, wikicodeMorphology);
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
// 		                             inflectionType
// 		                         );
// 
// 		ii = new InflectionIdentity(infl, referenceEntry);
// 
// 		registeredInflections.put(infl, ii);
// 
// 		aBox.add(inflectionEntry, isInflectionOf, referenceEntry);
// 		aBox.add(inflectionEntry, isInflectionType, normalizedInflectionPOS);
// 
// 		aBox.add(ii.representation, writtenRepProperty, inflectionForm);
// 
// 		addInflectionMorphology(representation, wikicodeMorphology);
// 
// 		aBox.add(inflectionEntry, hasInflectionForm, ii.representation);
// 	}

	private Statement createTargetLanguageProperty(Resource trans, String lang) {
		lang = lang.trim();
		if (isAnISO639_3Code(lang)) {
			return aBox.createStatement(trans, targetLanguageProperty, getLexvoLanguageResource(lang));
		} else {
			return aBox.createStatement(trans, targetLanguageCodeProperty, lang);
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
		rnymR.addProperty(glossProperty, gloss);
		
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
			aBox.add(writtenRepresentation, pronProperty, pron, lang);
		} else {
			aBox.add(writtenRepresentation, pronProperty, pron);
		}
	}

	private void promoteNymProperties() {
		StmtIterator entries = currentMainLexEntry.listProperties(refersTo);
		HashSet<Statement> toBeRemoved = new HashSet<Statement>();
		while (entries.hasNext()) {
			Resource lu = entries.next().getResource();
			List<Statement> senses = lu.listProperties(lemonSenseProperty).toList();
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
		aBox.add(currentLexEntry, otherFormProperty, form);
	}
}

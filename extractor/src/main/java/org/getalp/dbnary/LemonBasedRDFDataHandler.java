package org.getalp.dbnary;

import java.io.OutputStream;
import java.util.*;
import java.util.regex.Pattern;

import com.hp.hpl.jena.vocabulary.RDFS;
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

public class LemonBasedRDFDataHandler extends DbnaryModel implements IWiktionaryDataHandler {

	protected static class PosAndType {
		protected Resource pos;
		protected Resource type;
		public PosAndType(Resource p, Resource t) {this.pos = p; this.type = t;}
	}
	
	private Logger log = LoggerFactory.getLogger(LemonBasedRDFDataHandler.class);
	
	protected Model aBox;

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
	protected static HashMap<String,PosAndType> posAndTypeValueMap = new HashMap<String,PosAndType>();

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

        // German
		posAndTypeValueMap.put("Substantiv", new PosAndType(LexinfoOnt.noun, LemonOnt.LexicalEntry));
		posAndTypeValueMap.put("Nachname", new PosAndType(LexinfoOnt.properNoun, LemonOnt.LexicalEntry));
		posAndTypeValueMap.put("Vorname", new PosAndType(LexinfoOnt.properNoun, LemonOnt.LexicalEntry));
		posAndTypeValueMap.put("Adjektiv", new PosAndType(LexinfoOnt.adjective, LemonOnt.LexicalEntry));
		posAndTypeValueMap.put("Verb", new PosAndType(LexinfoOnt.verb, LemonOnt.LexicalEntry));
		posAndTypeValueMap.put("Adverb", new PosAndType(LexinfoOnt.adverb, LemonOnt.LexicalEntry));
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

	//	posAndTypeValueMap.put("", new PosAndType(null, LemonOnt.LexicalEntry)); // other Part of Speech

	}
	
	// Map of the String to lexvo language entity
	private HashMap<String,Resource> languages = new HashMap<String, Resource>();
	
	public LemonBasedRDFDataHandler(String lang) {
		super();
		
		NS = DBNARY_NS_PREFIX + "/" + lang + "/";
		
		Lang l = ISO639_3.sharedInstance.getLang(lang);
		extractedLang = (null != l.getPart1()) ? l.getPart1() : l.getId();
        // TODO: Use lexvo as a class
		lexvoExtractedLanguage = tBox.getResource(LEXVO + lang); 
		
		// Create aBox
		aBox = ModelFactory.createDefaultModel();
			
		aBox.setNsPrefix(lang, NS);
		aBox.setNsPrefix("dbnary", DBnaryOnt.getURI());
		aBox.setNsPrefix("lemon", LemonOnt.getURI());
		aBox.setNsPrefix("lexinfo", LexinfoOnt.getURI());
		aBox.setNsPrefix("rdfs", RDFS.getURI());
		aBox.setNsPrefix("dcterms", DCTerms.getURI());
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
        String encodedPageName = uriEncode(wiktionaryPageName);
        currentMainLexEntry = aBox.createResource(getPrefix() + encodedPageName);
        
        // Create the resource without typing so that the type statement is added only if the currentStatement are added to the model.
        // Resource lemma = aBox.createResource(encodedPageName);
                
        // Retain these statements to be inserted in the model when we will know that the entry corresponds to a proper part of speech
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

        if (! normalizedType.equals(LemonOnt.LexicalEntry)) {
            // Add the Lexical Entry type so that users may refer to all entries using the top hierarchy without any reasoner.
            aBox.add(aBox.createStatement(currentLexEntry, RDF.type, LemonOnt.LexicalEntry));
        }
        
        // All translation numbers are local to a lexEntry
        translationCount.resetAll();
        reifiedNymCount.resetAll();

        currentPreferredWrittenRepresentation = aBox.createResource(); 
        
        // If a pronunciation was given before the first part of speech, it means that it is shared amoung pos/etymologies
        for (PrononciationPair p : currentSharedPronunciations) {
        	if (null != p.lang && p.lang.length() > 0) 
        		aBox.add(aBox.createStatement(currentPreferredWrittenRepresentation, LexinfoOnt.pronunciation, p.pron, p.lang));
        	else
        		aBox.add(aBox.createStatement(currentPreferredWrittenRepresentation, LexinfoOnt.pronunciation, p.pron));
        }
        	

    	aBox.add(aBox.createStatement(currentLexEntry, LemonOnt.canonicalForm, currentPreferredWrittenRepresentation));
    	aBox.add(aBox.createStatement(currentPreferredWrittenRepresentation, LemonOnt.writtenRep, currentWiktionaryPageName, extractedLang));
    	aBox.add(aBox.createStatement(currentLexEntry, DBnaryOnt.partOfSpeech, currentWiktionaryPos));
    	if (null != currentLexinfoPos)
    		aBox.add(aBox.createStatement(currentLexEntry, LexinfoOnt.partOfSpeech, currentLexinfoPos));
    	aBox.add(aBox.createStatement(currentLexEntry, LemonOnt.language, extractedLang));
    	aBox.add(aBox.createStatement(currentLexEntry, DCTerms.language, lexvoExtractedLanguage));

    	// Register the pending statements.
        for (Statement s: heldBackStatements) {
        	aBox.add(s);
        }
        heldBackStatements.clear();
        aBox.add(aBox.createStatement(currentMainLexEntry, DBnaryOnt.refersTo, currentLexEntry));
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
    	aBox.add(aBox.createStatement(currentLexEntry, LemonOnt.lexicalVariant , altlemma));
    	aBox.add(aBox.createStatement(altlemma, LemonOnt.writtenRep, alt, extractedLang));
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
    	aBox.add(aBox.createStatement(currentLexEntry, LemonOnt.sense, currentSense));
    	aBox.add(aBox.createLiteralStatement(currentSense, DBnaryOnt.senseNumber, aBox.createTypedLiteral(senseNumber)));
    	// pos is not usefull anymore for word sense as they should be correctly linked to an entry with only one pos.
    	// if (currentPos != null && ! currentPos.equals("")) {
        //	aBox.add(aBox.createStatement(currentSense, posProperty, currentPos));
        //}
    	
    	Resource defNode = aBox.createResource();
    	aBox.add(aBox.createStatement(currentSense, LemonOnt.definition, defNode));
    	// Keep a human readable version of the definition, removing all links annotations.
    	aBox.add(aBox.createStatement(defNode, LemonOnt.value, AbstractWiktionaryExtractor.cleanUpMarkup(def, true), extractedLang));

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
		if (word.length() == 0 && (usage == null || usage.length() == 0)) {
			return null;
		}
		// Ensure language is in its standard form.
    	Lang t = ISO639_3.sharedInstance.getLang(lang);
    	if (null != t) lang = t.getId();
		Resource trans = aBox.createResource(computeTransId(lang, entity), DBnaryOnt.Translation);
    	aBox.add(aBox.createStatement(trans, DBnaryOnt.isTranslationOf, entity));
    	aBox.add(createTargetLanguageProperty(trans, lang));
    	if (null == t) {
        	aBox.add(aBox.createStatement(trans, DBnaryOnt.writtenForm, word));
    	} else {
    		String tl = (null != t.getPart1()) ? t.getPart1() : t.getId();	
    		aBox.add(aBox.createStatement(trans, DBnaryOnt.writtenForm, word, tl));
    	}
    	if (currentGlose != null && ! currentGlose.equals("")) {
        	aBox.add(aBox.createStatement(trans, DBnaryOnt.gloss, currentGlose, extractedLang));
    	}
    	if (usage != null && ! usage.equals("")) {
        	aBox.add(aBox.createStatement(trans, DBnaryOnt.usage, usage));
    	}
    	return trans;
    }

	@Override
    public void registerTranslation(String lang, String currentGlose, String usage, String word) {
		registerTranslationToEntity(currentLexEntry, lang, currentGlose, usage, word);
	}


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
	    	// aBox.add(aBox.createStatement(nym, isAnnotatedBy, target.substring(hash)));
		}
		
		Property nymProperty = nymPropertyMap.get(synRelation);
		
		Resource targetResource = aBox.createResource(getPrefix() + uriEncode(target), DBnaryOnt.Vocable);
		
    	aBox.add(aBox.createStatement(entity, nymProperty, targetResource));
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
	    	// aBox.add(aBox.createStatement(nym, isAnnotatedBy, target.substring(hash)));
		}
		Property nymProperty = nymPropertyMap.get(synRelation);
		
		Resource targetResource = aBox.createResource(getPrefix() + uriEncode(target), DBnaryOnt.Vocable);
		
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
	    	// aBox.add(aBox.createStatement(nym, isAnnotatedBy, target.substring(hash)));
		}
		
		Property nymProperty = nymPropertyMap.get(synRelation);
		
		Resource targetResource = aBox.createResource(getPrefix() + uriEncode(target), DBnaryOnt.Vocable);
		
    	aBox.add(aBox.createStatement(currentSense, nymProperty, targetResource));
    }
	
	
	@Override
	public void registerPronunciation(String pron, String lang) {
		
		if (null == currentPreferredWrittenRepresentation) {
			currentSharedPronunciations.add(new PrononciationPair(pron, lang));
		} else {
			if (null != lang && lang.length() > 0)
				aBox.add(aBox.createStatement(currentPreferredWrittenRepresentation, LexinfoOnt.pronunciation, pron, lang));
			else
				aBox.add(aBox.createStatement(currentPreferredWrittenRepresentation, LexinfoOnt.pronunciation, pron));
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
						aBox.add(aBox.createStatement(s, nymProp, nymRel.getObject()));
						toBeRemoved.add(nymRel);
					}
				}
			}
		}
		for (Statement s: toBeRemoved) {
			s.remove();
		}
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
	
}

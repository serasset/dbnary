package org.getalp.dbnary;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.getalp.blexisma.api.ISO639_3;
import org.getalp.blexisma.api.ISO639_3.Lang;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

public class LemonBasedRDFDataHandler implements WiktionaryDataHandler {

	protected static class PosAndType {
		protected Resource pos;
		protected Resource type;
		protected PosAndType(Resource p, Resource t) {this.pos = p; this.type = t;}
	}
	
	protected static final String NSprefix = "http://kaiko.getalp.org/dbnary";
	protected static final String DBNARY = NSprefix + "#";
	// protected static final String LMF = "http://www.lexicalmarkupframework.org/lmf/r14#";
	protected static final String LEMON = "http://www.monnetproject.eu/lemon#";
	protected static final String LEXINFO = "http://www.lexinfo.net/ontology/2.0/lexinfo#";
	protected static final String RDFS = "http://www.w3.org/2000/01/rdf-schema#";
	
	protected static final Resource lexEntryType;
	protected static final Resource lexicalFormType;
	protected static final Resource translationType;
	protected static final Resource lexicalSenseType;
	// protected Resource definitionType;
	// protected Resource lexicalEntryRelationType;

	protected static final Property canonicalFormProperty;
	protected static final Property lexicalVariantProperty;
	protected static final Property writtenRepresentationProperty;
	
	// DBNARY properties
	protected static final Property dbnaryPosProperty;
	protected static final Resource vocableEntryType;
	protected static final Property refersTo;

	// LEMON properties
	protected static final Property posProperty;
	protected static final Property lemonSenseProperty;
	protected static final Property lemonDefinitionProperty;
	protected static final Property lemonValueProperty;
	protected static final Property languageProperty;
	protected static final Property pronProperty;

	//LMF properties
	// protected Property formProperty;
	protected static final Property isTranslationOf;
	protected static final Property targetLanguageProperty;
	protected static final Property equivalentTargetProperty;
	protected static final Property gloseProperty;
	protected static final Property usageProperty;
	// protected static final Property textProperty;
	protected static final Property senseNumberProperty;
	// protected static final Property entryRelationTargetProperty;

	// protected static final Property entryRelationLabelProperty;

	static Model tBox;

	Model aBox;

	// States used for processing
	protected Resource currentLexEntry;
	private Resource currentLexinfoPos;
	private String currentWiktionaryPos;
	
	private Resource currentSense;
	private int currentSenseNumber;
	private int currentTranslationNumber;
	protected String extractedLang;

	private Set<Statement> heldBackStatements = new HashSet<Statement>();

	protected int nbEntries = 0;
	protected String NS;
	protected String currentEncodedPageName;
	private String currentWiktionaryPageName;
	private HashMap<String,Integer> currentLexieCount = new HashMap<String,Integer>();
	private Resource currentMainLexEntry;
	private Resource currentPreferredWrittenRepresentation;
	private String currentSharedPronunciation;
	private String currentSharedPronunciationLang;
	
	private static HashMap<String,Property> nymPropertyMap = new HashMap<String,Property>();
	private static HashMap<String,PosAndType> posAndTypeValueMap = new HashMap<String,PosAndType>();

	static {
		// Create T-Box and read rdf schema associated to it.
		tBox = ModelFactory.createDefaultModel();
		// InputStream fis = LemonBasedRDFDataHandler.class.getResourceAsStream("LMF-rdf-rev14.xml");
		// tBox.read( fis, LMF );
		InputStream lis = LemonBasedRDFDataHandler.class.getResourceAsStream("lemon.ttl");
		tBox.read( lis, LEMON, "TURTLE");

		lexEntryType = tBox.getResource(LEMON + "LexicalEntry");
		lexicalFormType = tBox.getResource(LEMON + "LexicalForm");
		lexicalSenseType = tBox.getResource(LEMON + "LexicalSense");
		canonicalFormProperty = tBox.getProperty(LEMON + "canonicalForm");
		lemonSenseProperty = tBox.getProperty(LEMON + "sense");
		lexicalVariantProperty = tBox.getProperty(LEMON + "lexicalVariant");
		writtenRepresentationProperty =  tBox.getProperty(LEMON + "writtenRep");
		lemonDefinitionProperty = tBox.getProperty(LEMON + "definition");
		lemonValueProperty = tBox.getProperty(LEMON + "value");
		languageProperty = tBox.getProperty(LEMON + "language");
		
		vocableEntryType = tBox.getResource(DBNARY + "vocable");

		translationType = tBox.getResource(DBNARY + "Equivalent");
		// definitionType = tBox.getResource(LMF + "Definition");
		// lexicalEntryRelationType = tBox.getResource(NS + "LexicalEntryRelation");

		// formProperty = tBox.getProperty(NS + "writtenForm");
		targetLanguageProperty = tBox.getProperty(DBNARY + "targetLanguage");
		equivalentTargetProperty = tBox.getProperty(DBNARY + "writtenForm");
		gloseProperty = tBox.getProperty(DBNARY + "glose");
		usageProperty = tBox.getProperty(DBNARY + "usage");
		// textProperty = tBox.getProperty(DBNARY + "text");
		senseNumberProperty = tBox.getProperty(DBNARY + "senseNumber");
		// entryRelationLabelProperty = tBox.getProperty(DBNARY + "label");
		// entryRelationTargetProperty = tBox.getProperty(DBNARY + "target");
		refersTo = tBox.getProperty(DBNARY + "refersTo");
		isTranslationOf = tBox.getProperty(DBNARY + "isTranslationOf");
				
		posProperty = tBox.getProperty(LEXINFO + "partOfSpeech");
		dbnaryPosProperty = tBox.getProperty(DBNARY + "partOfSpeech");
		
		pronProperty = tBox.getProperty(LEXINFO + "pronunciation");
		
		Property synonymProperty = tBox.getProperty(DBNARY + "synonym");
		Property antonymProperty = tBox.getProperty(DBNARY + "antonym");
		Property hypernymProperty = tBox.getProperty(DBNARY + "hypernym");
		Property hyponymProperty = tBox.getProperty(DBNARY + "hyponym");
		Property nearSynonymProperty = tBox.getProperty(DBNARY + "approximateSynonym");

		Property lxfSynonymProperty = tBox.getProperty(LEXINFO + "synonym");
		Property lxfAntonymProperty = tBox.getProperty(LEXINFO + "antonym");
		Property lxfHypernymProperty = tBox.getProperty(LEXINFO + "hypernym");
		Property lxfHyponymProperty = tBox.getProperty(LEXINFO + "hyponym");
		Property lxfNearSynonymProperty = tBox.getProperty(LEXINFO + "approximateSynonym");

		// non standard nym (not in lexinfo);
		Property meronymProperty = tBox.getProperty(DBNARY + "meronym");
		Property holonymProperty = tBox.getProperty(DBNARY + "holonym");
		
		nymPropertyMap.put("syn", synonymProperty);
		nymPropertyMap.put("ant", antonymProperty);
		nymPropertyMap.put("hypo", hyponymProperty);
		nymPropertyMap.put("hyper", hypernymProperty);
		nymPropertyMap.put("mero", meronymProperty);
		nymPropertyMap.put("holo", holonymProperty);
		nymPropertyMap.put("qsyn", nearSynonymProperty);

		Resource nounPOS = tBox.getResource(LEXINFO + "noun");
		Resource adjPOS = tBox.getResource(LEXINFO + "adj");
		Resource properNounPOS = tBox.getResource(LEXINFO + "properNoun");
		Resource verbPOS = tBox.getResource(LEXINFO + "verb");
		Resource adverbPOS = tBox.getResource(LEXINFO + "adverb");
		Resource otherPOS = tBox.getResource(LEXINFO + "otherPartOfSpeech");

		Resource word = tBox.getResource(LEMON + "Word");
		Resource phrase = tBox.getResource(LEMON + "Phrase");

		// French
		posAndTypeValueMap.put("-nom-", new PosAndType(nounPOS, word));
		posAndTypeValueMap.put("-nom-pr-", new PosAndType(properNounPOS, word));
		posAndTypeValueMap.put("-prénom-", new PosAndType(properNounPOS, word));
		posAndTypeValueMap.put("-adj-", new PosAndType(adjPOS, word));
		posAndTypeValueMap.put("-verb-", new PosAndType(verbPOS, word));
		posAndTypeValueMap.put("-adv-", new PosAndType(adverbPOS, word));
		posAndTypeValueMap.put("-loc-adv-", new PosAndType(adverbPOS, phrase));
		posAndTypeValueMap.put("-loc-adj-", new PosAndType(adjPOS, phrase));
		posAndTypeValueMap.put("-loc-nom-", new PosAndType(nounPOS, phrase));
		posAndTypeValueMap.put("-loc-verb-", new PosAndType(verbPOS, phrase));
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
		// Italian
		posAndTypeValueMap.put("noun", new PosAndType(nounPOS, lexEntryType));
		posAndTypeValueMap.put("adjc", new PosAndType(adjPOS, lexEntryType));
		posAndTypeValueMap.put("verb", new PosAndType(verbPOS, lexEntryType));
		posAndTypeValueMap.put("adv", new PosAndType(adverbPOS, lexEntryType));
		
		posAndTypeValueMap.put("", new PosAndType(otherPOS, lexEntryType));

	}
	
	public LemonBasedRDFDataHandler(String lang) {
		super();
		
		NS = NSprefix + "/" + lang + "/";
		
		Lang l = ISO639_3.sharedInstance.getLang(lang);
		extractedLang = (null != l.getPart1()) ? l.getPart1() : l.getId();
				

		// Create aBox
		aBox = ModelFactory.createDefaultModel();
			
		aBox.setNsPrefix(lang, NS);
		aBox.setNsPrefix("dbnary", DBNARY);
		// aBox.setNsPrefix("lmf", LMF);
		aBox.setNsPrefix("lemon", LEMON);
		aBox.setNsPrefix("lexinfo", LEXINFO);
		aBox.setNsPrefix("rdfs", RDFS);

	}
	
	@Override
	public void initializeEntryExtraction(String wiktionaryPageName) {
        currentSense = null;
        currentSenseNumber = 1;
        currentTranslationNumber = 1;
        currentWiktionaryPageName = wiktionaryPageName;
        currentLexinfoPos = null;
        currentWiktionaryPos = null;
        currentLexieCount.clear();
        currentPreferredWrittenRepresentation = null;
        currentSharedPronunciation = null;
        currentSharedPronunciationLang = null;
        
        // Create a dummy lexical entry that points to the one that corresponds to a part of speech
        String encodedPageName = uriEncode(wiktionaryPageName);
        currentMainLexEntry = aBox.createResource(NS + encodedPageName);
        
        // Create the resource without typing so that the type statement is added only if the currentStatement are added to the model.
        // Resource lemma = aBox.createResource(encodedPageName);
        
        // Retain these statements to be inserted in the model when we will know that the entry corresponds to a proper part of speech
        heldBackStatements.add(aBox.createStatement(currentMainLexEntry, RDF.type, vocableEntryType));
    }
    
	
	@Override
	public void finalizeEntryExtraction() {
		// Clear currentStatements. If statemenents do exist-s in it, it is because, there is no extractable part of speech in the entry.
		heldBackStatements.clear();
		promoteNymProperties();
	}

	@Override
	public void addPartOfSpeech(String pos) {
    	// DONE: create a LexicalEntry for this part of speech only and attach info to it.
		currentWiktionaryPos = pos;
		PosAndType pat = posAndTypeValueMap.get(pos);
    	currentLexinfoPos = (null == pat) ? null : pat.pos;
    	Resource entryType = (null == pat) ? lexEntryType : pat.type;
    	nbEntries++;
    	
        currentEncodedPageName = uriEncode(currentWiktionaryPageName, currentWiktionaryPos) + "__" + getCurrentLexieCount(currentWiktionaryPos);
        currentLexEntry = aBox.createResource(NS + currentEncodedPageName, entryType);

        currentPreferredWrittenRepresentation = aBox.createResource(); 
        
        // If a pronunciation was given before the first part of speech, it means that it is shared amoung pos/etymologies
        if (null != currentSharedPronunciation && currentSharedPronunciation.length() > 0) {
        	if (null != currentSharedPronunciationLang && currentSharedPronunciationLang.length() > 0) 
        		aBox.add(aBox.createStatement(currentPreferredWrittenRepresentation, pronProperty, currentSharedPronunciation, currentSharedPronunciationLang));
    		else
    			aBox.add(aBox.createStatement(currentPreferredWrittenRepresentation, pronProperty, currentSharedPronunciation));
        }
			

    	heldBackStatements.add(aBox.createStatement(currentLexEntry, canonicalFormProperty, currentPreferredWrittenRepresentation));
    	heldBackStatements.add(aBox.createStatement(currentPreferredWrittenRepresentation, writtenRepresentationProperty, currentWiktionaryPageName, extractedLang));
    	aBox.add(aBox.createStatement(currentLexEntry, dbnaryPosProperty, currentWiktionaryPos));
    	if (null != currentLexinfoPos)
    		aBox.add(aBox.createStatement(currentLexEntry, posProperty, currentLexinfoPos));
    	aBox.add(aBox.createStatement(currentLexEntry, languageProperty, extractedLang));

    	// Register the pending statements.
        for (Statement s: heldBackStatements) {
        	aBox.add(s);
        }
        heldBackStatements.clear();
        aBox.add(aBox.createStatement(currentMainLexEntry, refersTo, currentLexEntry));
    }

	private int getCurrentLexieCount(String pos) {
		Integer v = currentLexieCount.get(pos);
		if (null == v) {
			currentLexieCount.put(pos, 1);
		} else {
			currentLexieCount.put(pos, ++v);
		}
		return currentLexieCount.get(pos);
	}

	@Override
    public void registerAlternateSpelling(String alt) {
    	Resource altlemma = aBox.createResource();
    	aBox.add(aBox.createStatement(currentLexEntry, lexicalVariantProperty, altlemma));
    	aBox.add(aBox.createStatement(altlemma, writtenRepresentationProperty, alt, extractedLang));
    }
    
    @Override
	public void registerNewDefinition(String def) {
    	
    	// Create new word sense + a definition element 
    	currentSense = aBox.createResource(computeSenseId(), lexicalSenseType);
    	aBox.add(aBox.createStatement(currentLexEntry, lemonSenseProperty, currentSense));
    	aBox.add(aBox.createLiteralStatement(currentSense, senseNumberProperty, aBox.createTypedLiteral(currentSenseNumber)));
    	// pos is not usefull anymore for word sense as they should be correctly linked to an entry with only one pos.
    	// if (currentPos != null && ! currentPos.equals("")) {
        //	aBox.add(aBox.createStatement(currentSense, posProperty, currentPos));
        //}
    	
    	Resource defNode = aBox.createResource();
    	aBox.add(aBox.createStatement(currentSense, lemonDefinitionProperty, defNode));
    	// Keep a human readable version of the definition, removing all links annotations.
    	aBox.add(aBox.createStatement(defNode, lemonValueProperty, WiktionaryExtractor.cleanUpMarkup(def, true), extractedLang)); 

    	currentSenseNumber++;
    	// TODO: Extract domain/usage field from the original definition.
    }

	private String computeSenseId() {
		return NS + "__ws_" + currentSenseNumber + "_" + currentEncodedPageName;
	}

	@Override
    public void registerTranslation(String lang, String currentGlose,
			String usage, String word) {
		word = word.trim();
    	Resource trans = aBox.createResource(computeTransId(lang), translationType);
    	aBox.add(aBox.createStatement(trans, isTranslationOf, currentLexEntry));
    	aBox.add(aBox.createStatement(trans, targetLanguageProperty, lang));
    	aBox.add(aBox.createStatement(trans, equivalentTargetProperty, word));
    	if (currentGlose != null && ! currentGlose.equals("")) {
        	aBox.add(aBox.createStatement(trans, gloseProperty, currentGlose));
    	}
    	if (usage != null && ! usage.equals("")) {
        	aBox.add(aBox.createStatement(trans, usageProperty, usage));
    	}	
	}

    private String computeTransId(String lang) {
		return NS + "__tr_" + uriEncode(lang) + "_" + (currentTranslationNumber++) + "_" + currentEncodedPageName;
	}

	@Override
	public void registerNymRelation(String target, String synRelation) {
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
		
		Resource targetResource = aBox.createResource(NS + uriEncode(target), vocableEntryType);
		
    	aBox.add(aBox.createStatement(currentLexEntry, nymProperty, targetResource));
    }

	@Override
	public void registerPronunciation(String pron, String lang) {
		
		if (null == currentPreferredWrittenRepresentation) {
			currentSharedPronunciation = pron;
			currentSharedPronunciationLang = lang;
		} else {
			if (null != lang && lang.length() > 0)
				aBox.add(aBox.createStatement(currentPreferredWrittenRepresentation, pronProperty, pron, lang));
			else
				aBox.add(aBox.createStatement(currentPreferredWrittenRepresentation, pronProperty, pron));
		}
	}
	
	protected String uriEncode(String s) {
		StringBuffer res = new StringBuffer();
		uriEncode(s, res);
		return res.toString();
	}
	
	protected void uriEncode(String s, StringBuffer res) {
		int i = 0;
		while (i != s.length()) {
			char c = s.charAt(i);
			if (Character.isSpaceChar(c))
				res.append('_');
			else if ((c >= '\u00A0' && c <= '\u00BF') ||
					(c == '<') || (c == '>') || (c == '%') ||
					(c == '"') || (c == '#') || (c == '[') || 
					(c == ']') || (c == '\\') || (c == '^') ||
					(c == '`') || (c == '{') || (c == '|') || 
					(c == '}') || (c == '\u00D7') || (c == '\u00F7')
					)
				try {
					res.append(URLEncoder.encode("" + c, "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					// Should never happen
					e.printStackTrace();
				}
			else if (Character.isISOControl(c))
				; // nop
			else
				res.append(c);
			i++;
		}
	}
	
	protected String uriEncode(String s, String pos) {
		StringBuffer res = new StringBuffer();
		uriEncode(s, res);
		res.append("__");
		int i = 0;
		while (i != pos.length()) {
			char c = pos.charAt(i);
			if (Character.isSpaceChar(c))
				res.append('_');
			else if ((c >= '\u00A0' && c <= '\u00BF') ||
					(c == '<') || (c == '>') || (c == '%') ||
					(c == '"') || (c == '#') || (c == '[') || 
					(c == ']') || (c == '\\') || (c == '^') ||
					(c == '`') || (c == '{') || (c == '|') || 
					(c == '}') || (c == '\u00D7') || (c == '\u00F7') || 
					(c == '-') || (c == '_') || 
					Character.isISOControl(c))
				; // nop
			else
				res.append(c);
			i++;
		}
		return res.toString();
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
	 * Write a serialized represention of this model in a specified language.
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



	
}

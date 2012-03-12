package org.getalp.blexisma.wiktionary;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Formatter;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;

import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;

public class FrenchRDFWiktionaryExtractor implements WiktionaryDataHandler {

	protected static final String NS = "http://getalp.org/dbnary#";
	protected static final String LMF = "http://www.lexicalmarkupframework.org/lmf/r14#";
	
	protected Resource lexEntryType;
	protected Resource lemmaType;
	protected Resource translationType;
	protected Resource senseType;
	protected Resource definitionType;
	protected Resource lexicalEntryRelationType;

	protected Property posProperty;
	protected Property formProperty;
	protected Property isPartOf;
	protected Property langProperty;
	protected Property equivalentTargetProperty;
	protected Property gloseProperty;
	protected Property usageProperty;
	protected Property textProperty;
	protected Property senseNumberProperty;
	protected Property entryRelationTargetProperty;
	protected Property entryRelationLabelProperty;

	Model aBox;
	Model tBox;

	// States used for processing
	protected Resource currentLexEntry;
	private Resource currentSense;
	private int currentSenseNumber;
	private int currentTranslationNumber;
	private int currentRelationNumber;

	private Set<Statement> currentStatements = new HashSet<Statement>();
	private String currentPos;

	protected int nbEntries = 0;
	private String encodedPageName;
	
	public FrenchRDFWiktionaryExtractor() {
		super();
		
		// Create T-Box and read rdf schema associated to it.
		tBox = ModelFactory.createDefaultModel();
		InputStream fis = FrenchRDFWiktionaryExtractor.class.getResourceAsStream("LMF-rdf-rev14.xml");
		tBox.read( fis, LMF );
			
		// Create aBox
		aBox = ModelFactory.createDefaultModel();
			
		aBox.setNsPrefix("dbnary", NS);
		aBox.setNsPrefix("lmf", LMF);
			
		lexEntryType = tBox.getResource(LMF + "LexicalEntry");
		lemmaType = tBox.getResource(LMF + "Lemma");

		translationType = tBox.getResource(LMF + "Equivalent");
		senseType = tBox.getResource(LMF + "Sense");
		definitionType = tBox.getResource(LMF + "Definition");
		lexicalEntryRelationType = tBox.getResource(NS + "LexicalEntryRelation");

		posProperty = tBox.getProperty(NS + "partOfSpeech");
		formProperty = tBox.getProperty(NS + "writtenForm");
		langProperty = tBox.getProperty(NS + "language");
		equivalentTargetProperty = formProperty;
		gloseProperty = tBox.getProperty(NS + "glose");
		usageProperty = tBox.getProperty(NS + "usage");
		textProperty = tBox.getProperty(NS + "text");
		senseNumberProperty = tBox.getProperty(NS + "senseNumber");
		entryRelationLabelProperty = tBox.getProperty(NS + "label");
		entryRelationTargetProperty = tBox.getProperty(NS + "target");
		
		isPartOf = tBox.getProperty(LMF + "isPartOf");
	}
	
	@Override
	public void initializeEntryExtraction(String wiktionaryPageName) {
        currentSense = null;
        currentSenseNumber = 1;
        currentTranslationNumber = 1;
        currentRelationNumber = 1;
        encodedPageName = uriEncode(wiktionaryPageName);
        currentLexEntry = aBox.createResource(NS + encodedPageName);

        // DONE: Do not create anonymous nodes to avoid pb with the interpretation of blank nodes.
        // Create the ressource without typing so that the type statement is added only if the currentStatement are added to the model.
        Resource lemma = aBox.createResource(computeLemmaId(encodedPageName));

        // Retain these statements to be inserted in the model when we will know that the entry corresponds to a proper part of speech
        currentStatements.add(aBox.createStatement(lemma, RDF.type, lemmaType));
        currentStatements.add(aBox.createStatement(currentLexEntry, RDF.type, lexEntryType));
    	currentStatements.add(aBox.createStatement(lemma, isPartOf, currentLexEntry));
    	currentStatements.add(aBox.createStatement(lemma, formProperty, wiktionaryPageName));
    }
    
	private String computeLemmaId(String lemma) {
		return NS + "__lem_" + lemma;
	}

	protected String uriEncode(String s) {
		StringBuffer res = new StringBuffer();
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
		return res.toString();
	}
	
	@Override
	public void finalizeEntryExtraction() {
		// Clear currentStatements. If statemenents do exist-s in it, it is because, there is no extractable part of speech in the entry.
		currentStatements.clear();
	}

    @Override
	public
    void addPartOfSpeech(String pos) {
    	currentPos = pos;
    	nbEntries++;
        aBox.add(aBox.createStatement(currentLexEntry, posProperty, pos));
        // Register the pending statements.
        for (Statement s: currentStatements) {
        	aBox.add(s);
        }
        currentStatements.clear();
    }

    @Override
    public void registerAlternateSpelling(String alt) {
    	Resource altlemma = aBox.createResource(computeLemmaId(uriEncode(alt)), lemmaType);
    	aBox.add(aBox.createStatement(altlemma, isPartOf, currentLexEntry));
    	aBox.add(aBox.createStatement(altlemma, formProperty, alt));
    }
    
    @Override
	public void registerNewDefinition(String def) {
    	
    	// Create new word sense + a definition element 
    	// DONE: give an ID to all resources to avoid blank node interpretation
    	currentSense = aBox.createResource(computeSenseId(), senseType);
    	aBox.add(aBox.createStatement(currentSense, isPartOf, currentLexEntry));
    	aBox.add(aBox.createLiteralStatement(currentSense, senseNumberProperty, currentSenseNumber));
    	if (currentPos != null && ! currentPos.equals("")) {
        	aBox.add(aBox.createLiteralStatement(currentSense, posProperty, currentPos));
        }
    	
    	Resource defNode = aBox.createResource(computeDefId(), definitionType);
    	aBox.add(aBox.createStatement(defNode, isPartOf, currentSense));
    	// Keep a human readable version of the definition, removing all links annotations.
    	aBox.add(aBox.createStatement(defNode, textProperty, WiktionaryExtractor.cleanUpMarkup(def, true))); 

    	currentSenseNumber++;
    	// TODO: Extract domain/usage field from the original definition.
    }

    private String computeDefId() {
		return NS + "__def_" + currentSenseNumber + "_" + encodedPageName;
	}

	private String computeSenseId() {
		return NS + "__ws_" + currentSenseNumber + "_" + encodedPageName;
	}

	@Override
    public void registerTranslation(String lang, String currentGlose,
			String usage, String word) {
    	Resource trans = aBox.createResource(computeTransId(lang), translationType);
    	aBox.add(aBox.createStatement(trans, isPartOf, currentLexEntry));
    	aBox.add(aBox.createStatement(trans, langProperty, lang));
    	aBox.add(aBox.createStatement(trans, equivalentTargetProperty, word));
    	if (currentGlose != null && ! currentGlose.equals("")) {
        	aBox.add(aBox.createStatement(trans, gloseProperty, currentGlose));
    	}
    	if (usage != null && ! usage.equals("")) {
        	aBox.add(aBox.createStatement(trans, usageProperty, usage));
    	}	
	}

    private String computeTransId(String lang) {
		return NS + "__tr_" + uriEncode(lang) + "_" + (currentTranslationNumber++) + "_" + encodedPageName;
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
		
		Resource nym = aBox.createResource(createRelationId(synRelation), lexicalEntryRelationType);
		Resource targetResource = aBox.createResource(NS + uriEncode(target), lexEntryType);
    	aBox.add(aBox.createStatement(nym, isPartOf, currentLexEntry));
    	aBox.add(aBox.createStatement(nym, entryRelationLabelProperty, synRelation));
    	aBox.add(aBox.createStatement(nym, entryRelationTargetProperty, targetResource));
    }
   
	private String createRelationId(String rel) {
		return NS + "__" + rel + "_" + (currentRelationNumber++) + "_" + encodedPageName;
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

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
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDF;

public class DbnaryModel {

	
	public static final String DBNARY_NS_PREFIX = "http://kaiko.getalp.org/dbnary";
	public static final String DBNARY = DBNARY_NS_PREFIX + "#";
	// protected static final String LMF = "http://www.lexicalmarkupframework.org/lmf/r14#";
	public static final String LEMON = "http://www.lemon-model.net/lemon#";
	public static final String LEXINFO = "http://www.lexinfo.net/ontology/2.0/lexinfo#";
	public static final String RDFS = "http://www.w3.org/2000/01/rdf-schema#";
	public static final String LEXVO = "http://lexvo.org/id/iso639-3/";

	public static final Resource lexEntryType;
	public static final Resource wordEntryType;
	public static final Resource phraseEntryType;

	public static final Resource lexicalFormType;
	public static final Resource translationType;
	public static final Resource lexicalSenseType;
	// protected Resource definitionType;
	// protected Resource lexicalEntryRelationType;

	public static final Property canonicalFormProperty;
	public static final Property lexicalVariantProperty;
	public static final Property writtenRepresentationProperty;
	
	// DBNARY properties
	public static final Property dbnaryPosProperty;
	public static final Resource vocableEntryType;
	public static final Property refersTo;

	// LEMON properties
	public static final Property posProperty;
	public static final Property lemonSenseProperty;
	public static final Property lemonDefinitionProperty;
	public static final Property lemonValueProperty;
	public static final Property languageProperty;
	public static final Property pronProperty;

	//LMF properties
	// protected Property formProperty;
	public static final Property isTranslationOf;
	public static final Property targetLanguageProperty;
	public static final Property targetLanguageCodeProperty;
	public static final Property equivalentTargetProperty;
	public static final Property glossProperty;
	public static final Property usageProperty;
	// protected static final Property textProperty;
	public static final Property senseNumberProperty;
	// protected static final Property entryRelationTargetProperty;

	// protected static final Property entryRelationLabelProperty;

	public static Model tBox;
	
	public static Property synonymProperty ;
	public static Property antonymProperty ;
	public static Property hypernymProperty ;
	public static Property hyponymProperty ;
	public static Property nearSynonymProperty ;

	// non standard nym (not in lexinfo);
	public static Property meronymProperty ;
	public static Property holonymProperty ;

	public static Resource nounPOS;
	public static Resource adjPOS;
	public static Resource properNounPOS ;
	public static Resource verbPOS ;
	public static Resource adverbPOS ;
	public static Resource otherPOS ;

	static {
		// Create T-Box and read rdf schema associated to it.
		tBox = ModelFactory.createDefaultModel();
		// InputStream fis = LemonBasedRDFDataHandler.class.getResourceAsStream("LMF-rdf-rev14.xml");
		// tBox.read( fis, LMF );
		InputStream lis = DbnaryModel.class.getResourceAsStream("lemon.ttl");
		tBox.read( lis, LEMON, "TURTLE");

		lexEntryType = tBox.getResource(LEMON + "LexicalEntry");
		wordEntryType = tBox.getResource(LEMON + "Word");
		phraseEntryType = tBox.getResource(LEMON + "Phrase");

		lexicalFormType = tBox.getResource(LEMON + "LexicalForm");
		lexicalSenseType = tBox.getResource(LEMON + "LexicalSense");
		canonicalFormProperty = tBox.getProperty(LEMON + "canonicalForm");
		lemonSenseProperty = tBox.getProperty(LEMON + "sense");
		lexicalVariantProperty = tBox.getProperty(LEMON + "lexicalVariant");
		writtenRepresentationProperty =  tBox.getProperty(LEMON + "writtenRep");
		lemonDefinitionProperty = tBox.getProperty(LEMON + "definition");
		lemonValueProperty = tBox.getProperty(LEMON + "value");
		languageProperty = tBox.getProperty(LEMON + "language");
		
		vocableEntryType = tBox.getResource(DBNARY + "Vocable");

		translationType = tBox.getResource(DBNARY + "Translation");
		// definitionType = tBox.getResource(LMF + "Definition");
		// lexicalEntryRelationType = tBox.getResource(NS + "LexicalEntryRelation");

		// formProperty = tBox.getProperty(NS + "writtenForm");
		targetLanguageProperty = tBox.getProperty(DBNARY + "targetLanguage");
		targetLanguageCodeProperty = tBox.getProperty(DBNARY + "targetLanguageCode");
		equivalentTargetProperty = tBox.getProperty(DBNARY + "writtenForm");
		glossProperty = tBox.getProperty(DBNARY + "gloss");
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

		synonymProperty = tBox.getProperty(DBNARY + "synonym");
		antonymProperty = tBox.getProperty(DBNARY + "antonym");
		hypernymProperty = tBox.getProperty(DBNARY + "hypernym");
		hyponymProperty = tBox.getProperty(DBNARY + "hyponym");
		nearSynonymProperty = tBox.getProperty(DBNARY + "approximateSynonym");

		meronymProperty = tBox.getProperty(DBNARY + "meronym");
		holonymProperty = tBox.getProperty(DBNARY + "holonym");

		nounPOS = tBox.getResource(LEXINFO + "noun");
		adjPOS = tBox.getResource(LEXINFO + "adj");
		properNounPOS = tBox.getResource(LEXINFO + "properNoun");
		verbPOS = tBox.getResource(LEXINFO + "verb");
		adverbPOS = tBox.getResource(LEXINFO + "adverb");
		otherPOS = tBox.getResource(LEXINFO + "otherPartOfSpeech");

	}
	
	
	protected static String uriEncode(String s) {
		StringBuffer res = new StringBuffer();
		uriEncode(s, res);
		return res.toString();
	}
	
	protected static void uriEncode(String s, StringBuffer res) {
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
			else if (c == '\u200e' || c == '\u200f') {
				; // ignore rRLM and LRM.
			} else
				res.append(c);
			i++;
		}
	}
	
	protected static String uriEncode(String s, String pos) {
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
			else if (c == '\u200e' || c == '\u200f') {
				; // ignore rRLM and LRM.
			} else
				res.append(c);
			i++;
		}
		return res.toString();
	}

}

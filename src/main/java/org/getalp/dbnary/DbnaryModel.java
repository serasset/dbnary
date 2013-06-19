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

	
	protected static final String NSprefix = "http://kaiko.getalp.org/dbnary";
	protected static final String DBNARY = NSprefix + "#";
	// protected static final String LMF = "http://www.lexicalmarkupframework.org/lmf/r14#";
	protected static final String LEMON = "http://www.monnetproject.eu/lemon#";
	protected static final String LEXINFO = "http://www.lexinfo.net/ontology/2.0/lexinfo#";
	protected static final String RDFS = "http://www.w3.org/2000/01/rdf-schema#";
	protected static final String LEXVO = "http://lexvo.org/id/iso639-3/";

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
	protected static final Property targetLanguageCodeProperty;
	protected static final Property equivalentTargetProperty;
	protected static final Property glossProperty;
	protected static final Property usageProperty;
	// protected static final Property textProperty;
	protected static final Property senseNumberProperty;
	// protected static final Property entryRelationTargetProperty;

	// protected static final Property entryRelationLabelProperty;

	static Model tBox;
	

	static {
		// Create T-Box and read rdf schema associated to it.
		tBox = ModelFactory.createDefaultModel();
		// InputStream fis = LemonBasedRDFDataHandler.class.getResourceAsStream("LMF-rdf-rev14.xml");
		// tBox.read( fis, LMF );
		InputStream lis = DbnaryModel.class.getResourceAsStream("lemon.ttl");
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
			else
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
			else
				res.append(c);
			i++;
		}
		return res.toString();
	}

}

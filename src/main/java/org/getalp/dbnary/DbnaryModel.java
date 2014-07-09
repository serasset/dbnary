package org.getalp.dbnary;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.Normalizer;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

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
	public static final Property writtenRepProperty;
	
	// DBNARY properties
	public static final Property dbnaryPosProperty;
	public static final Resource vocableEntryType;
	public static final Property refersTo;
	public static final Property otherFormProperty;

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
	public static final Property fromProperty;
	public static final Property toProperty;
	
	// protected static final Property textProperty;
	public static final Property senseNumberProperty;
	// protected static final Property entryRelationTargetProperty;

	// protected static final Property entryRelationLabelProperty;

	public static final Property synonymReifiedRelation ;
	public static final Property antonymReifiedRelation ;
	public static final Property hypernymReifiedRelation ;
	public static final Property hyponymReifiedRelation ;
	public static final Property nearSynonymReifiedRelation ;
	public static final Property meronymReifiedRelation ;
	public static final Property holonymReifiedRelation ;

	public static Model tBox;
	
	public static final Property synonymProperty ;
	public static final Property antonymProperty ;
	public static final Property hypernymProperty ;
	public static final Property hyponymProperty ;
	public static final Property nearSynonymProperty ;

	// non standard nym (not in lexinfo);
	public static final Property meronymProperty ;
	public static final Property holonymProperty ;

	public static final Resource nounPOS;
	public static final Resource adjPOS;
	public static final Resource properNounPOS ;
	public static final Resource verbPOS ;
	public static final Resource adverbPOS ;
	public static final Resource otherPOS ;

	public static final Resource genderProperty;
	public static final Resource masculine;
	public static final Resource feminine;
	public static final Resource neuter;

	public static final Resource animacyProperty;
	public static final Resource inanimate;
	public static final Resource animate;

	public static final Resource inflectionType;
	public static final Property hasInflectionForm;
	public static final Property hasWikiCodeMorphology;
	public static final Property isInflectionOf;
	public static final Property isInflectionType;


	static {
		// Create T-Box and read rdf schema associated to it.
		tBox = ModelFactory.createDefaultModel();

		InputStream lis = DbnaryModel.class.getResourceAsStream("lemon.ttl");
		tBox.read(lis, LEMON,  "TURTLE");

		lis = DbnaryModel.class.getResourceAsStream("lexinfo.owl");
		tBox.read(lis, LEMON);

		lexEntryType               = tBox.createResource(LEMON   + "LexicalEntry");
		wordEntryType              = tBox.createResource(LEMON   + "Word");
		phraseEntryType            = tBox.createResource(LEMON   + "Phrase");
		vocableEntryType           = tBox.createResource(DBNARY  + "Vocable");

		lexicalFormType            = tBox.createResource(LEMON   + "LexicalForm");
		lexicalSenseType           = tBox.createResource(LEMON   + "LexicalSense");

		canonicalFormProperty      = tBox.createProperty(LEMON,    "canonicalForm");
		lemonSenseProperty         = tBox.createProperty(LEMON,    "sense");
		lexicalVariantProperty     = tBox.createProperty(LEMON,    "lexicalVariant");
		lemonDefinitionProperty    = tBox.createProperty(LEMON,    "definition");
		lemonValueProperty         = tBox.createProperty(LEMON,    "value");
		languageProperty           = tBox.createProperty(LEMON,    "language");
		
		writtenRepProperty         = tBox.createProperty(LEMON,    "writtenRep");

		translationType            = tBox.createResource(DBNARY +  "Translation");

		targetLanguageProperty     = tBox.createProperty(DBNARY,   "targetLanguage");
		targetLanguageCodeProperty = tBox.createProperty(DBNARY,   "targetLanguageCode");
		equivalentTargetProperty   = tBox.createProperty(DBNARY,   "writtenForm");

		glossProperty              = tBox.createProperty(DBNARY,   "gloss");
		usageProperty              = tBox.createProperty(DBNARY,   "usage");
		fromProperty               = tBox.createProperty(DBNARY,   "from");
		toProperty                 = tBox.createProperty(DBNARY,   "to");

		refersTo                   = tBox.createProperty(DBNARY,   "refersTo");
		isTranslationOf            = tBox.createProperty(DBNARY,   "isTranslationOf");
		senseNumberProperty        = tBox.createProperty(DBNARY,   "senseNumber");
		otherFormProperty          = tBox.createProperty(DBNARY,   "otherForm");
		pronProperty               = tBox.createProperty(LEXINFO,  "pronunciation");


		synonymReifiedRelation     = tBox.createProperty(DBNARY,   "Synonym");
		antonymReifiedRelation     = tBox.createProperty(DBNARY,   "Antonym");
		hypernymReifiedRelation    = tBox.createProperty(DBNARY,   "Hypernym");
		hyponymReifiedRelation     = tBox.createProperty(DBNARY,   "Hyponym");
		nearSynonymReifiedRelation = tBox.createProperty(DBNARY,   "ApproximateSynonym");
		meronymReifiedRelation     = tBox.createProperty(DBNARY,   "Meronym");
		holonymReifiedRelation     = tBox.createProperty(DBNARY,   "Holonym");

		synonymProperty            = tBox.createProperty(DBNARY,   "synonym");
		antonymProperty            = tBox.createProperty(DBNARY,   "antonym");
		hypernymProperty           = tBox.createProperty(DBNARY,   "hypernym");
		hyponymProperty            = tBox.createProperty(DBNARY,   "hyponym");
		nearSynonymProperty        = tBox.createProperty(DBNARY,   "approximateSynonym");
		meronymProperty            = tBox.createProperty(DBNARY,   "meronym");
		holonymProperty            = tBox.createProperty(DBNARY,   "holonym");


		posProperty                = tBox.createProperty(LEXINFO,  "partOfSpeech");
		dbnaryPosProperty          = tBox.createProperty(DBNARY,   "partOfSpeech");
		nounPOS                    = tBox.createResource(LEXINFO + "noun");
		adjPOS                     = tBox.createResource(LEXINFO + "adjective");
		properNounPOS              = tBox.createResource(LEXINFO + "properNoun");
		verbPOS                    = tBox.createResource(LEXINFO + "verb");
		adverbPOS                  = tBox.createResource(LEXINFO + "adverb");
		otherPOS                   = tBox.createResource(LEXINFO + "otherPartOfSpeech");

		
		genderProperty             = tBox.createResource(LEXINFO + "gender");
		masculine                  = tBox.createResource(LEXINFO + "masculine");
		feminine                   = tBox.createResource(LEXINFO + "feminine");
		neuter                     = tBox.createResource(LEXINFO + "neuter");
		
		animacyProperty            = tBox.createResource(LEXINFO + "animacy");
		animate                    = tBox.createResource(LEXINFO + "animate");
		inanimate                  = tBox.createResource(LEXINFO + "inanimate");


		inflectionType             = tBox.createResource(DBNARY  + "Inflection");
		hasInflectionForm          = tBox.createProperty(DBNARY,   "inflectionForm");
		hasWikiCodeMorphology      = tBox.createProperty(DBNARY  + "wikiMorphology");
		isInflectionOf             = tBox.createProperty(DBNARY  + "inflectionOf");
		isInflectionType           = tBox.createProperty(DBNARY  + "inflectionType");
	}
	
	public static String uriEncode(String s) {
		StringBuffer res = new StringBuffer();
		uriEncode(s, res);
		return res.toString();
	}
	
	protected static void uriEncode(String s, StringBuffer res) {
		int i = 0;
		s = Normalizer.normalize(s, Normalizer.Form.NFKC);
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
		pos = Normalizer.normalize(pos, Normalizer.Form.NFKC);
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

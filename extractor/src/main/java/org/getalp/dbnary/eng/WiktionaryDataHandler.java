package org.getalp.dbnary.eng;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import org.getalp.dbnary.*;
import org.getalp.dbnary.wiki.WikiTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by serasset on 17/09/14, pantaleo
 */
public class WiktionaryDataHandler extends LemonBasedRDFDataHandler {

    private Logger log = LoggerFactory.getLogger(WiktionaryDataHandler.class);

    /**
     * a HashSet to store the name of etymtree pages that have already been extracted
     */ 
    static HashSet etymtreeHashSet = new HashSet();

    public ArrayList<Resource> ancestors;
    /**
     * A Resource containing the current Etymology Entry.
     */
    public Resource currentEtymologyEntry;
    /**
     * An integer counting the number of alternative etymologies for the same entry.
     */
    protected int currentEtymologyNumber;

    private HashMap<String,String> prefixes = new HashMap<String,String>();


    static {
        // English
        posAndTypeValueMap.put("Noun", new PosAndType(LexinfoOnt.noun, LemonOnt.LexicalEntry));
        posAndTypeValueMap.put("Proper noun", new PosAndType(LexinfoOnt.properNoun, LemonOnt.LexicalEntry));
        posAndTypeValueMap.put("Proper Noun", new PosAndType(LexinfoOnt.properNoun, LemonOnt.LexicalEntry));

        posAndTypeValueMap.put("Adjective", new PosAndType(LexinfoOnt.adjective, LemonOnt.LexicalEntry));
        posAndTypeValueMap.put("Verb", new PosAndType(LexinfoOnt.verb, LemonOnt.LexicalEntry));
        posAndTypeValueMap.put("Adverb", new PosAndType(LexinfoOnt.adverb, LemonOnt.LexicalEntry));
        posAndTypeValueMap.put("Article", new PosAndType(LexinfoOnt.article, LexinfoOnt.Article));
        posAndTypeValueMap.put("Conjunction", new PosAndType(LexinfoOnt.conjunction, LexinfoOnt.Conjunction));
        posAndTypeValueMap.put("Determiner", new PosAndType(LexinfoOnt.determiner, LexinfoOnt.Determiner));

        posAndTypeValueMap.put("Numeral", new PosAndType(LexinfoOnt.numeral, LexinfoOnt.Numeral));
        posAndTypeValueMap.put("Cardinal numeral", new PosAndType(LexinfoOnt.cardinalNumeral, LexinfoOnt.Numeral));
        posAndTypeValueMap.put("Cardinal number", new PosAndType(LexinfoOnt.cardinalNumeral, LexinfoOnt.Numeral));

        posAndTypeValueMap.put("Number", new PosAndType(LexinfoOnt.numeral, LexinfoOnt.Number));
        posAndTypeValueMap.put("Particle", new PosAndType(LexinfoOnt.particle, LexinfoOnt.Particle));
        posAndTypeValueMap.put("Preposition", new PosAndType(LexinfoOnt.preposition, LexinfoOnt.Preposition));
        posAndTypeValueMap.put("Postposition", new PosAndType(LexinfoOnt.postposition, LexinfoOnt.Postposition));

        posAndTypeValueMap.put("Prepositional phrase", new PosAndType(null, LemonOnt.Phrase));

        posAndTypeValueMap.put("Pronoun", new PosAndType(LexinfoOnt.pronoun, LexinfoOnt.Pronoun));
        posAndTypeValueMap.put("Symbol", new PosAndType(LexinfoOnt.symbol, LexinfoOnt.Symbol));

        posAndTypeValueMap.put("Prefix", new PosAndType(LexinfoOnt.prefix, LexinfoOnt.Prefix));
        posAndTypeValueMap.put("Suffix", new PosAndType(LexinfoOnt.suffix, LexinfoOnt.Suffix));
        posAndTypeValueMap.put("Affix", new PosAndType(LexinfoOnt.affix, LexinfoOnt.Affix));
        posAndTypeValueMap.put("Infix", new PosAndType(LexinfoOnt.infix, LexinfoOnt.Infix));
        posAndTypeValueMap.put("Interfix", new PosAndType(LexinfoOnt.affix, LexinfoOnt.Affix));
        posAndTypeValueMap.put("Circumfix", new PosAndType(LexinfoOnt.affix, LexinfoOnt.Affix));

        posAndTypeValueMap.put("Proverb", new PosAndType(LexinfoOnt.proverb, LemonOnt.Phrase));
        posAndTypeValueMap.put("Interjection", new PosAndType(LexinfoOnt.interjection, LexinfoOnt.Interjection));
        posAndTypeValueMap.put("Phrase", new PosAndType(LexinfoOnt.phraseologicalUnit, LemonOnt.Phrase));
        posAndTypeValueMap.put("Idiom", new PosAndType(LexinfoOnt.idiom, LemonOnt.Phrase));

        // Initialism ?
    }

    public WiktionaryDataHandler(String lang) {
        super(lang);
    }

    @Override
    public void initializeEntryExtraction(String wiktionaryPageName) {
	initializeEntryExtraction(wiktionaryPageName, "eng");
    }
    
    public void initializeEntryExtraction(String wiktionaryPageName, String lang) {
        currentEtymologyNumber = 0;
        currentEtymologyEntry = null;
	super.initializeEntryExtraction(wiktionaryPageName); 
    }	

    public static boolean isValidPOS(String pos) {
        return posAndTypeValueMap.containsKey(pos);
    }

    public void registerEtymologyPos(){
	registerEtymologyPos("eng");
    }
    
    public void registerEtymologyPos(String lang){
	if (currentEtymologyEntry == null){//there is no etymology section
	    currentEtymologyEntry = aBox.createResource(getPrefixe(lang) + "__ee_" + uriEncode(currentWiktionaryPageName), DBnaryEtymologyOnt.EtymologyEntry);
	}
	aBox.add(currentEtymologyEntry, DBnaryOnt.refersTo, currentLexEntry);
    }
    
    @Override
    public Resource addPartOfSpeech(String originalPOS, Resource normalizedPOS, Resource normalizedType) {
        Resource lexEntry = super.addPartOfSpeech(originalPOS, normalizedPOS, normalizedType);
        return lexEntry;
    }

    private String computeEtymologyId(int etymologyNumber, String lang) {
        return getPrefixe(lang) + "__ee_" + etymologyNumber + "_" + uriEncode(currentWiktionaryPageName);
    }

    public String getPrefixe(String lang){
	lang = lang.trim();
	if (lang.equals("eng")){
	    return NS;
	}
	if (this.prefixes.containsKey(lang)){
	    return this.prefixes.get(lang);
	}
	String tmp = LangTools.normalize(lang);
	if (tmp != null){
	    lang = tmp;
	}
	String prefix = DBNARY_NS_PREFIX + "/eng/" + lang + "/";
	prefixes.put(lang, prefix);
	aBox.setNsPrefix(lang + "-eng", prefix);
	return prefix;
    }

    public void registerDerived(Etymology etymology){
	if (etymology.symbols == null || etymology.symbols.size() == 0){
	    return;
	}
	String lang = null;
	Resource vocable0 = null;
	int counter = 0;
	for (Symbols b : etymology.symbols){
	    String word = uriEncode(b.args.get("word1").split(",")[0].trim());
	    if (counter == 0){
	       	lang = b.args.get("lang");
		if (word.equals("")){
		    log.debug("Error: empty lemma found while processing derived words of {}, language {}", currentWiktionaryPageName, lang);
		}
		//register derives_from
	        vocable0 = aBox.createResource(getPrefixe(lang) + "__ee_" + word, DBnaryEtymologyOnt.EtymologyEntry);
	        aBox.add(vocable0, DBnaryEtymologyOnt.derivesFrom, currentEtymologyEntry);
	    } else {
	        //register etymologically_equivalent_to
     	        Resource vocable2 = aBox.createResource(getPrefixe(lang) + "__ee_" + word, DBnaryEtymologyOnt.EtymologyEntry);
		aBox.add(vocable2, DBnaryEtymologyOnt.etymologicallyEquivalentTo, vocable0);
	    }
	    counter ++;
	}
    }
        
    public void registerCurrentEtymologyEntry(String lang){
	if (currentEtymologyEntry != null) {
	    return;
	}
	currentEtymologyNumber ++;
	currentEtymologyEntry = aBox.createResource(computeEtymologyId(currentEtymologyNumber, lang), DBnaryEtymologyOnt.EtymologyEntry);
    }

    public Resource createEtymologyEntryResource(String e, String lang){
	if (e.equals("")){
	    log.debug("Error: empty lemma found while processing word {}, language {}", currentWiktionaryPageName, lang);
	}
	return aBox.createResource(getPrefixe(lang) + "__ee_" + uriEncode(e).split(",")[0].trim(), DBnaryEtymologyOnt.EtymologyEntry);
    }
        
    public void registerEtymology(Etymology etymology){
	currentEtymologyEntry = null;	
	
	if (etymology.symbols == null || etymology.symbols.size() == 0){
	    return;
	}
	
	registerCurrentEtymologyEntry(etymology.lang);
	Resource vocable0 = currentEtymologyEntry, vocable = null;
	String lang0 = etymology.lang, lang = null;
	for (int j = 0; j < etymology.symbols.size(); j ++){
	    Symbols b = etymology.symbols.get(j);
	    for (String values : b.values) {
	    	if (values.equals("LEMMA")){
		    boolean isEquivalent = false;
		    lang = b.args.get("lang");
		    //handle etymologically equivalent words (i.e., isEquivalent = true)
		    if (lang != null && lang0 != null){
                        if (lang0.equals(lang)){
			    if (j > 1){
				if (etymology.symbols.get(j - 1).values.get(0).equals("COMMA")){
				    isEquivalent = true;
				}
			    }
			}
		    }
	            if (isEquivalent){//etymologically equivalent words
			vocable = createEtymologyEntryResource(b.args.get("word1"), lang0);
	                aBox.add(vocable0, DBnaryEtymologyOnt.etymologicallyEquivalentTo, vocable);
	            } else {
			boolean compound = false;
			for (int kk = 1; kk < 12; kk ++){
		      	    String word = b.args.get("word" + Integer.toString(kk));
			    lang = b.args.get("lang");//reset lang
			    if (lang == null){
				lang = b.args.get("lang" + Integer.toString(kk));
			    }
			    if (word != null && lang != null){
				if (kk > 1){//it's some kind of compound
				    compound = true;
				}
				vocable = createEtymologyEntryResource(word, lang);
			        aBox.add(vocable0, DBnaryEtymologyOnt.etymologicallyDerivesFrom, vocable);
			    }   
			}
			if (compound){
			    return;
			}
	       	    }
		    vocable0 = vocable;
		    lang0 = lang; 
	        }
	    }
        }
    }

    public void initializeAncestors(){
	ancestors = new ArrayList<Resource>();
    }

    public void finalizeAncestors(){
	ancestors.clear();
    }
    
    public void addAncestorsAndRegisterDescendants(Etymology etymology){
	if (etymology.symbols == null || etymology.symbols.size() == 0){
	    return;
	}
	int counter = 0; //number of etymology.symbols	
	for (Symbols b : etymology.symbols) {
	    if (b.values != null) {
	        if (b.values.get(0).equals("LEMMA")) {
		    Resource vocable = aBox.createResource(getPrefixe(b.args.get("lang")) + "__ee_" + uriEncode(b.args.get("word1").split(",")[0].trim()), DBnaryEtymologyOnt.EtymologyEntry);
		    if (counter == 0){
			if (ancestors.size() > 0){
			    aBox.add(vocable, DBnaryEtymologyOnt.descendsFrom, ancestors.get(ancestors.size() - 1));
			}
			ancestors.add(vocable);
		    } else {
			if (ancestors.size() > 1){
			    aBox.add(vocable, DBnaryEtymologyOnt.descendsFrom, ancestors.get(ancestors.size() - 2));
			}
		    }
		    counter ++;
		}
	    }
	}
    }
    
    @Override
    public void registerInflection(String languageCode,
                                   String pos,
                                   String inflection,
                                   String canonicalForm,
                                   int defNumber,
                                   HashSet<PropertyObjectPair> props,
                                   HashSet<PronunciationPair> pronunciations) {

        if (pronunciations != null) {
            for (PronunciationPair pronunciation : pronunciations) {
                props.add(PropertyObjectPair.get(LexinfoOnt.pronunciation, aBox.createLiteral(pronunciation.pron, pronunciation.lang)));
            }
        }

        registerInflection(languageCode, pos, inflection, canonicalForm, defNumber, props);
    }

    @Override
    protected void addOtherFormPropertiesToLexicalEntry(Resource lexEntry, HashSet<PropertyObjectPair> properties) {
        // Do not try to merge new form with an existing compatible one in English.
        // This would lead to a Past becoming a PastParticiple when registering the past participle form.
        Model morphoBox = featureBoxes.get(Feature.MORPHOLOGY);

        if (null == morphoBox) return;

        lexEntry = lexEntry.inModel(morphoBox);

        String otherFormNodeName = computeOtherFormResourceName(lexEntry,properties);
        Resource otherForm = morphoBox.createResource(getPrefix() + otherFormNodeName, LemonOnt.Form);
        morphoBox.add(lexEntry, LemonOnt.otherForm, otherForm);
        mergePropertiesIntoResource(properties, otherForm);

    }

    public void registerInflection(String inflection,
                                   String note,
                                   HashSet<PropertyObjectPair> props) {

        // Keep it simple for english: register forms on the current lexical entry
        if (null != note) {
            PropertyObjectPair p = PropertyObjectPair.get(DBnaryOnt.note, aBox.createLiteral(note, extractedLang));
            props.add(p);
        }
        PropertyObjectPair p = PropertyObjectPair.get(LemonOnt.writtenRep, aBox.createLiteral(inflection, extractedLang));
        props.add(p);

        addOtherFormPropertiesToLexicalEntry(currentLexEntry, props);

    }

    @Override
    public void registerInflection(String languageCode,
                                   String pos,
                                   String inflection,
                                   String canonicalForm,
                                   int defNumber,
                                   HashSet<PropertyObjectPair> props) {

        // Keep it simple for english: register forms on the current lexical entry
        // FIXME: check what is provided when we have different lex entries with the same pos and morph.

        PropertyObjectPair p = PropertyObjectPair.get(LemonOnt.writtenRep, aBox.createLiteral(inflection, extractedLang));

        props.add(p);

        addOtherFormPropertiesToLexicalEntry(currentLexEntry, props);

    }

    public void uncountable() {
        if (currentLexEntry == null) {
            log.debug("Registering countability on non existant lex entry in  {}", currentWiktionaryPageName);
            return;
        }
        aBox.add(aBox.createStatement(currentLexEntry, OliaOnt.hasCountability, OliaOnt.Uncountable));
    }

    public void countable() {
        if (currentLexEntry == null) {
            log.debug("Registering countability on non existant lex entry in  {}", currentWiktionaryPageName);
            return;
        }
        aBox.add(aBox.createStatement(currentLexEntry, OliaOnt.hasCountability, OliaOnt.Countable));
    }

    public void comparable() {
        if (currentLexEntry == null) {
            log.debug("Registering comparativity on non existant lex entry in  {}", currentWiktionaryPageName);
            return;
        }
        // TODO: do we have a mean to say that an adjective is comparable ?
        // aBox.add(aBox.createStatement(currentLexEntry, OliaOnt., OliaOnt.Uncountable));
    }

    public void notComparable() {
        if (currentLexEntry == null) {
            log.debug("Registering comparativity on non existant lex entry in  {}", currentWiktionaryPageName);
            return;
        }
        // TODO: do we have a mean to say that an adjective is not comparable ?
        // aBox.add(aBox.createStatement(currentLexEntry, OliaOnt., OliaOnt.Uncountable));
    }

    public void addInflectionOnCanonicalForm(EnglishInflectionData infl) {
        this.mergePropertiesIntoResource(infl.toPropertyObjectMap(), currentCanonicalForm);
    }
}

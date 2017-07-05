package org.getalp.dbnary.eng;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import org.getalp.dbnary.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

/**
 * Created by serasset on 17/09/14, pantaleo
 */
public class WiktionaryDataHandler extends OntolexBasedRDFDataHandler {

    private Logger log = LoggerFactory.getLogger(WiktionaryDataHandler.class);

    /**
     * a HashSet to store the name of etymtree pages that have already been extracted
     */
    static HashSet etymtreeHashSet = new HashSet();

    /**
     * Resources containing Etymology Entries.
     */
    public ArrayList<Resource> ancestors;
    public Resource currentEtymologyEntry;
    public Resource currentGlobalEtymologyEntry;
    /**
     * An integer counting the number of alternative etymologies for the same entry.
     */
    protected int currentEtymologyNumber;


    protected String currentEntryLanguage = null;
    protected String currentEntryLanguageName = null;
    
    static {
        // English
        posAndTypeValueMap.put("Noun", new PosAndType(LexinfoOnt.noun, OntolexOnt.LexicalEntry));
        posAndTypeValueMap.put("Proper noun", new PosAndType(LexinfoOnt.properNoun, OntolexOnt.LexicalEntry));
        posAndTypeValueMap.put("Proper Noun", new PosAndType(LexinfoOnt.properNoun, OntolexOnt.LexicalEntry));

        posAndTypeValueMap.put("Adjective", new PosAndType(LexinfoOnt.adjective, OntolexOnt.LexicalEntry));
        posAndTypeValueMap.put("Verb", new PosAndType(LexinfoOnt.verb, OntolexOnt.LexicalEntry));
        posAndTypeValueMap.put("Adverb", new PosAndType(LexinfoOnt.adverb, OntolexOnt.LexicalEntry));
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

        posAndTypeValueMap.put("Prepositional phrase", new PosAndType(null, OntolexOnt.MultiWordExpression));

        posAndTypeValueMap.put("Pronoun", new PosAndType(LexinfoOnt.pronoun, LexinfoOnt.Pronoun));
        posAndTypeValueMap.put("Symbol", new PosAndType(LexinfoOnt.symbol, LexinfoOnt.Symbol));

        posAndTypeValueMap.put("Prefix", new PosAndType(LexinfoOnt.prefix, LexinfoOnt.Prefix));
        posAndTypeValueMap.put("Suffix", new PosAndType(LexinfoOnt.suffix, LexinfoOnt.Suffix));
        posAndTypeValueMap.put("Affix", new PosAndType(LexinfoOnt.affix, LexinfoOnt.Affix));
        posAndTypeValueMap.put("Infix", new PosAndType(LexinfoOnt.infix, LexinfoOnt.Infix));
        posAndTypeValueMap.put("Interfix", new PosAndType(LexinfoOnt.affix, LexinfoOnt.Affix));
        posAndTypeValueMap.put("Circumfix", new PosAndType(LexinfoOnt.affix, LexinfoOnt.Affix));

        posAndTypeValueMap.put("Proverb", new PosAndType(LexinfoOnt.proverb, OntolexOnt.MultiWordExpression));
        posAndTypeValueMap.put("Interjection", new PosAndType(LexinfoOnt.interjection, LexinfoOnt.Interjection));
        posAndTypeValueMap.put("Phrase", new PosAndType(LexinfoOnt.phraseologicalUnit, OntolexOnt.MultiWordExpression));
        posAndTypeValueMap.put("Idiom", new PosAndType(LexinfoOnt.idiom, OntolexOnt.MultiWordExpression));

        // Initialism ?
    }

    public WiktionaryDataHandler(String lang) {
        super(lang);
    }

    @Override
    public void initializeEntryExtraction(String wiktionaryPageName) {
	    currentEntryLanguage = "en";
	    currentEntryLanguageName = "English";
        initializeEntryExtraction(wiktionaryPageName, currentEntryLanguage, currentEntryLanguageName);
    }

    public void initializeEntryExtraction(String wiktionaryPageName, String lang, String languageName){
        currentEtymologyNumber = 0;
        currentEtymologyEntry = null;
        currentGlobalEtymologyEntry = createGlobalEtymologyResource(wiktionaryPageName, lang);
        super.initializeEntryExtraction(wiktionaryPageName);
    }
    
    private Resource createGlobalEtymologyResource(String wiktionaryPageName, String lang) {
        Model eBox = null;
        if ((eBox = featureBoxes.get(Feature.ETYMOLOGY)) != null) {
            // TODO : should I check that getPrefix returns null ?
	    lang = EnglishLangToCode.threeLettersCode(lang);
	    Resource r = eBox.createResource(getPrefix(eBox, lang) + "__ee_" + uriEncode(wiktionaryPageName), DBnaryEtymologyOnt.EtymologyEntry);
	    Resource w = ResourceFactory.createResource(WIKT + uriEncode(wiktionaryPageName) + "#" + uriEncode(currentEntryLanguageName));
	    eBox.add(r, RDFS.seeAlso, w);
	    eBox.add(r, RDFS.label, wiktionaryPageName, lang);
	    
            return r;
        }
        return null;
    }

    public static boolean isValidPOS(String pos) {
        return posAndTypeValueMap.containsKey(pos);
    }

    public void registerEtymologyPos(String wiktionaryPageName) {
        registerEtymologyPos("eng", "English", wiktionaryPageName);
    }

    public void registerEtymologyPos(String lang, String languageName, String wiktionaryPageName) {
        Model eBox = null;
        if ((eBox = featureBoxes.get(Feature.ETYMOLOGY)) == null) return;
        if (currentEtymologyEntry == null) {//there is no etymology section
            currentEtymologyEntry = eBox.createResource(getPrefix(eBox, lang) + "__ee_" + uriEncode(currentWiktionaryPageName), DBnaryEtymologyOnt.EtymologyEntry);
	    Resource w = ResourceFactory.createResource(WIKT + uriEncode(currentWiktionaryPageName) + "#" + uriEncode(currentEntryLanguageName));
	    eBox.add(currentEtymologyEntry, RDFS.seeAlso, w);
	    eBox.add(currentEtymologyEntry, RDFS.label, currentWiktionaryPageName, lang);
	}
	eBox.add(currentEtymologyEntry, DBnaryOnt.refersTo, currentLexEntry);
    }

    @Override
    public Resource addPartOfSpeech(String originalPOS, Resource normalizedPOS, Resource normalizedType) {
        Resource lexEntry = super.addPartOfSpeech(originalPOS, normalizedPOS, normalizedType);
        return lexEntry;
    }

    private String computeEtymologyId(Model box, int etymologyNumber, String lang) {
        return getPrefix(box, lang) + "__ee_" + etymologyNumber + "_" + uriEncode(currentWiktionaryPageName);
    }

    // TODO : check if we should create the prefixes in the aBox or in the eBox
    // TODO: getPrefix should never return null. It should return "unknown" if there is no language
    public String getPrefix(Model box, String lang) {
        // TODO : lang may be null ?
        // TODO : what if the
        if (lang == null) {
            log.debug("Null input language to function getPrefix.");
            lang = "unknown";
        }
        String code = EnglishLangToCode.threeLettersCode(lang);
        if (code == null) code = "unknown";
        lang = LangTools.normalize(code);
        lang = lang.trim();
        if (lang.equals("eng")) {
            return super.getPrefix();
        }
        Map<String, String> pMap = box.getNsPrefixMap();
        String pName = lang + "-eng";
        if (pMap.containsKey(pName)) {
            return pMap.get(pName);
        }
        String tmp = LangTools.normalize(lang);
        if (tmp != null) {
            lang = tmp;
        }
        String prefix = DBNARY_NS_PREFIX + "/eng/" + lang + "/";
        box.setNsPrefix(pName, prefix);
        return prefix;
    }

    public void registerDerived(Etymology etymology) {
        Model eBox = null;
        if ((eBox = featureBoxes.get(Feature.ETYMOLOGY)) == null) return;

        if (etymology.symbols == null || etymology.symbols.size() == 0) {
            return;
        }

        String lang = null;
        Resource vocable0 = null;
        int counter = 0;
        for (Symbols b : etymology.symbols) {
            String word = b.args.get("word1").split(",")[0].trim();
            if (word.equals("")) {
                log.debug("Error: empty lemma found while processing derived words of {} in string {}", currentWiktionaryPageName, etymology.string);
            } else {
                if (counter == 0) {
                    lang = b.args.get("lang");
                    //register derives_from
                    vocable0 = eBox.createResource(getPrefix(eBox, lang) + "__ee_" + uriEncode(word), DBnaryEtymologyOnt.EtymologyEntry);
                    eBox.add(vocable0, DBnaryEtymologyOnt.etymologicallyRelatedTo, currentEtymologyEntry);
		    Resource w = ResourceFactory.createResource(WIKT + uriEncode(currentWiktionaryPageName) + "#" + uriEncode(currentEntryLanguageName));
		    eBox.add(vocable0, RDFS.seeAlso, w);
		    eBox.add(vocable0, RDFS.label, word, lang);
                } else {
                    //register etymologically_equivalent_to
                    Resource vocable2 = eBox.createResource(getPrefix(eBox, lang) + "__ee_" + uriEncode(word), DBnaryEtymologyOnt.EtymologyEntry);
		    eBox.add(vocable2, DBnaryEtymologyOnt.etymologicallyRelatedTo, vocable0);
		    eBox.add(vocable2, DBnaryEtymologyOnt.etymologicallyEquivalentTo, vocable0);
		    Resource w = ResourceFactory.createResource(WIKT + uriEncode(currentWiktionaryPageName) + "#" + uriEncode(currentEntryLanguageName));
		    eBox.add(vocable2, RDFS.seeAlso, w);
		    eBox.add(vocable2, RDFS.label, word, lang);
                }		
                counter++;
            }
        }
    }

    public void registerCurrentEtymologyEntry(String lang) {
        Model eBox = null;
        if ((eBox = featureBoxes.get(Feature.ETYMOLOGY)) == null) return;

        if (currentEtymologyEntry != null) {
            return;
        }
        currentEtymologyNumber++;
        currentEtymologyEntry = eBox.createResource(computeEtymologyId(eBox, currentEtymologyNumber, lang), DBnaryEtymologyOnt.EtymologyEntry);
        eBox.add(currentGlobalEtymologyEntry, DBnaryOnt.refersTo, currentEtymologyEntry);
    }

    public Resource createEtymologyEntryResource(Model eBox, String e, String lang) {
        String word = e.split(",")[0].trim();
        return eBox.createResource(getPrefix(eBox, lang) + "__ee_" + uriEncode(word), DBnaryEtymologyOnt.EtymologyEntry);
    }

    public void registerEtymology(Etymology etymology) {
        Model eBox = null;
        if ((eBox = featureBoxes.get(Feature.ETYMOLOGY)) == null) return;

        currentEtymologyEntry = null;

        if (etymology.symbols == null || etymology.symbols.size() == 0) {
            return;
        }

        registerCurrentEtymologyEntry(etymology.lang);
        Resource vocable0 = currentEtymologyEntry, vocable = null;
        String lang0 = etymology.lang, lang = null;
        for (int j = 0; j < etymology.symbols.size(); j++) {
            Symbols b = etymology.symbols.get(j);
            for (String values : b.values) {
                if (values.equals("LEMMA")) {
                    boolean isEquivalent = false;
                    lang = b.args.get("lang");
                    //handle etymologically equivalent words (i.e., isEquivalent = true)
                    if (lang != null && lang0 != null) {
                        if (lang0.equals(lang)) {
                            if (j > 1) {
                                if (etymology.symbols.get(j - 1).values.get(0).equals("COMMA")) {
                                    isEquivalent = true;
                                }
                            }
                        }
                    }
                    if (isEquivalent) {//etymologically equivalent words
			String word1 = b.args.get("word1");
                        if (word1 != null && !word1.equals("")) {
                            vocable = createEtymologyEntryResource(eBox, word1, lang0);
                            eBox.add(vocable0, DBnaryEtymologyOnt.etymologicallyEquivalentTo, vocable);
			    eBox.add(vocable0, DBnaryEtymologyOnt.etymologicallyRelatedTo, vocable);
			    Resource w = ResourceFactory.createResource(WIKT + uriEncode(currentWiktionaryPageName) + "#" + uriEncode(currentEntryLanguageName));
			    eBox.add(vocable, RDFS.seeAlso, w);			    
			    eBox.add(vocable, RDFS.label, word1, lang0);
                        } else {
                            log.debug("empty word in symbol %s\n", b.string);
                        }
                    } else {
                        boolean compound = false;
                        for (int kk = 1; kk < 12; kk++) {
                            String word = b.args.get("word" + Integer.toString(kk));
			    lang = b.args.get("lang" + Integer.toString(kk));
			    if (lang == null) {
                                lang = b.args.get("lang");
                            }
                            // TODO: When word is empty (but not null), it means same string as current entry
                            if ("".equals(word)) word = currentWiktionaryPageName;
                            if (word != null && !word.equals("") && lang != null) {
                                if (kk > 1) {//it's some kind of compound
                                    compound = true;
                                }
				
                                vocable = createEtymologyEntryResource(eBox, word, lang);
                                eBox.add(vocable0, DBnaryEtymologyOnt.etymologicallyRelatedTo, vocable);
				Resource w = ResourceFactory.createResource(WIKT + uriEncode(currentWiktionaryPageName) + "#" + uriEncode(currentEntryLanguageName));
				eBox.add(vocable, RDFS.seeAlso, w);
				eBox.add(vocable, RDFS.label, word, lang);	   
                            }
                        }
                        if (compound) {
                            return;
                        }
                    }
                    vocable0 = vocable;
                    lang0 = lang;
                }
            }
        }
    }

    public void initializeAncestors() {
        ancestors = new ArrayList<Resource>();
    }

    public void finalizeAncestors() {
        ancestors.clear();
    }

    public void addAncestorsAndRegisterDescendants(Etymology etymology) {
        Model eBox = null;
        if ((eBox = featureBoxes.get(Feature.ETYMOLOGY)) == null) return;

        if (etymology.symbols == null || etymology.symbols.size() == 0) {
            Resource vocable = eBox.createResource("");
            ancestors.add(vocable);//add an empty vocable and don't register it
            return;
        }
        Resource ancestor = null;
        for (int i = 0; i < ancestors.size(); i++) {
            String a = ancestors.get(ancestors.size() - 1 - i).toString();
            if (!a.equals("")) {
                ancestor = ancestors.get(ancestors.size() - 1 - i);
                break;
            }
        }

        int counter = 0; //number of etymology.symbols
        for (Symbols b : etymology.symbols) {
            if (b.values != null) {
                if (b.values.get(0).equals("LEMMA")) {
                    String word = b.args.get("word1").split(",")[0].trim();
		    String lang = b.args.get("lang");
                    Resource vocable = eBox.createResource(getPrefix(eBox, lang) + "__ee_" + uriEncode(word), DBnaryEtymologyOnt.EtymologyEntry);
                    if (counter == 0) {
                        if (ancestor != null) {
                            eBox.add(vocable, DBnaryEtymologyOnt.etymologicallyRelatedTo, ancestor);
			    Resource w = ResourceFactory.createResource(WIKT + uriEncode(currentWiktionaryPageName) + "#" + uriEncode(currentEntryLanguageName));
			    eBox.add(vocable, RDFS.seeAlso, w);			    
			    eBox.add(vocable, RDFS.label, word, lang);
                        }
                        ancestors.add(vocable);
                    } else {
                        eBox.add(vocable, DBnaryEtymologyOnt.etymologicallyEquivalentTo, ancestors.get(ancestors.size() - 1));
			eBox.add(vocable, DBnaryEtymologyOnt.etymologicallyRelatedTo, ancestors.get(ancestors.size() - 1));
			Resource w = ResourceFactory.createResource(WIKT + uriEncode(currentWiktionaryPageName) + "#" + uriEncode(currentEntryLanguageName));
			eBox.add(vocable, RDFS.seeAlso, w);			
			eBox.add(vocable, RDFS.label, word, lang);
                    }
                    counter++;
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

        String otherFormNodeName = computeOtherFormResourceName(lexEntry, properties);
        Resource otherForm = morphoBox.createResource(getPrefix() + otherFormNodeName, OntolexOnt.Form);
        morphoBox.add(lexEntry, OntolexOnt.otherForm, otherForm);
        mergePropertiesIntoResource(properties, otherForm);

    }

    public void registerInflection(String inflection,
                                   String note,
                                   HashSet<PropertyObjectPair> props) {

        // Keep it simple for english: register forms on the current lexical entry
        if (null != note) {
            PropertyObjectPair p = PropertyObjectPair.get(SkosOnt.note, aBox.createLiteral(note, extractedLang));
            props.add(p);
        }
        PropertyObjectPair p = PropertyObjectPair.get(OntolexOnt.writtenRep, aBox.createLiteral(inflection, extractedLang));
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

        PropertyObjectPair p = PropertyObjectPair.get(OntolexOnt.writtenRep, aBox.createLiteral(inflection, extractedLang));

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

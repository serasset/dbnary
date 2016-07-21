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
import java.util.Map;

/**
 * Created by serasset on 17/09/14.
 */
public class WiktionaryDataHandler extends LemonBasedRDFDataHandler {

    private Logger log = LoggerFactory.getLogger(WiktionaryDataHandler.class);

    /**
     * An entry in a specific language can have multiple etymologies that correspond to multiple POS-s.
     * e.g.: the English noun and the English verb "tear" ("To cause to lose some kind of unity or coherence") derive from Proto-Indo-European "*der-", the noun "tear" ("A drop of clear, salty liquid produced from the eyes by crying or irritation.") derives from Proto-Indo-European  "*dáḱru-". see https://en.wiktionary.org/wiki/tear
     * This ArrayList contains the list of POS associated to each etymology.
     */
    public ArrayList<Resource> etymologyPos = new ArrayList<Resource>();

    /**
     * A String containing the etymological definition.
     */
    public String etymologyString;
    /**
     * A Resource containing the current Etymology Entry.
     */
    protected Resource currentEtymologyEntry;
    /**
     * An integer counting the number of alternative etymologies for the same entry.
     */
    protected int currentEtymologyNumber;

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
        super.initializeEntryExtraction(wiktionaryPageName);
        etymologyString = null;
        etymologyPos.clear();
        currentEtymologyNumber = 0;
        currentEtymologyEntry = null;
    }

    public static boolean isValidPOS(String pos) {
        return posAndTypeValueMap.containsKey(pos);
    }

    @Override
    public Resource addPartOfSpeech(String originalPOS, Resource normalizedPOS, Resource normalizedType) {
        Resource lexEntry = super.addPartOfSpeech(originalPOS, normalizedPOS, normalizedType);
        etymologyPos.add(currentLexEntry);
        return lexEntry;
    }

    /**
     * This functions extracts from the page content the text contained
     * in the Etymology Section
     * @param pageContent the page content
     * @param pageContent the page content
     * @param end position where the Etymology Section starts
     */
    public void extractEtymology(String pageContent, int start, int end) {
        etymologyString = WikiTool.removeReferencesIn(pageContent.substring(start, end));
        //System.out.format("ety=%s\n", etymologyString);
    }

    /**
     * This function sets etymologyString and currentEtymologyEbtry to null and clears the ArrayList etymologyPos.
     */
    public void cleanEtymology() {
        etymologyString = null;
        etymologyPos.clear();
        currentEtymologyEntry = null;
    }

    /**
     * @return the String containing the etymology section extracted by
     * function extractEtymology
     */
    public String getEtymology() {
        return etymologyString;
    }

    private String computeEtymologyId(int etymologyNumber) {
        return getPrefix() + "__ee_" + etymologyNumber + uriEncode(currentWiktionaryPageName) ;
    }

    //type = 0 etymologically equivalent
    //type = 1 etymologically derives from
    //type = 2 derives from
    //type = 3 descendent of
    public boolean registerEtymology(Map<String, String> args1, Map<String, String> args2, int type) {
        // String tmp = currentEntryLanguage;
        if (args1.size() == 0 || args2.size() == 0) {
            return false;
        }

        if (currentEtymologyEntry == null) {
            currentEtymologyNumber++;
            currentEtymologyEntry = aBox.createResource(computeEtymologyId(currentEtymologyNumber), DBnaryEtymologyOnt.EtymologyEntry);
            for (int i = 0; i < etymologyPos.size(); i++) {
                aBox.add(currentEtymologyEntry, DBnaryOnt.refersTo, etymologyPos.get(i));
            }
        }

        Resource vocable1;
        if (args1.get("isCurrentEtymologyEntry") != null) {
            vocable1 = currentEtymologyEntry;
        } else {
            vocable1 = getVocableResource(args1.get("word1").split(",")[0].trim(), true);
        }
        //if args2 represents a compound word return true
        int counter = 0;
        for (String key : args2.keySet()) {
            if (key.startsWith("word")) {
                counter++;
                if (type == 0) {
                    if (counter > 1) {
                        //it cannot be a compound word
                        System.out.format("Warning: word etymologically equivalent to a compound word; returning\n");
                        break;
                    }
                } else if (type == 1 || type == 2 || type == 3) {
                    // setCurrentLanguage(args2.get("lang"));
                }
                if (type == 0 || type == 1) {
                    //split args2.get(key) and for each entry register it as an etymology entry and as etymologically equivalent to entry 0
                    String[] words = args2.get(key).split(",");
                    //entry 0
                    Resource vocable2_0 = getVocableResource(words[0].trim(), true);
                    aBox.add(vocable2_0, RDF.type, DBnaryEtymologyOnt.EtymologyEntry);
                    if (type == 0) {
                        aBox.add(vocable1, DBnaryEtymologyOnt.etymologicallyEquivalentTo, vocable2_0);
                    } else if (type == 1) {
                        aBox.add(vocable1, DBnaryEtymologyOnt.etymologicallyDerivesFrom, vocable2_0);
                    }
                    if (words.length > 1) {
                        for (int i = 1; i < words.length; i++) {
                            Resource vocable2 = getVocableResource(words[i].trim(), true);
                            aBox.add(vocable2, RDF.type, DBnaryEtymologyOnt.EtymologyEntry);
                            aBox.add(vocable2_0, DBnaryEtymologyOnt.etymologicallyEquivalentTo, vocable2);
                        }
                    }
                } else if (type == 2 || type == 3) {
                    Resource vocable2 = getVocableResource(args2.get(key), true);
                    aBox.add(vocable2, RDF.type, DBnaryEtymologyOnt.EtymologyEntry);
                    if (type == 2) {
                        aBox.add(vocable2, DBnaryEtymologyOnt.derivesFrom, vocable1);
                    } else if (type == 3) {
                        aBox.add(vocable2, DBnaryEtymologyOnt.descendsFrom, vocable1);
                    }
                }
            }
        }

        //set language back to initial value
        // setCurrentLanguage(tmp);
        return counter > 1 ? true : false;
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

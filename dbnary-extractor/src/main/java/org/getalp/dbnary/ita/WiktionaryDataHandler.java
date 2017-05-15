package org.getalp.dbnary.ita;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;
import org.getalp.dbnary.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.math.BigInteger;
import java.util.*;


public class WiktionaryDataHandler extends OntolexBasedRDFDataHandler {

    private Logger log = LoggerFactory.getLogger(WiktionaryDataHandler.class);

    static {

        posAndTypeValueMap.put("noun", new PosAndType(LexinfoOnt.noun, OntolexOnt.Word));
        posAndTypeValueMap.put("sost", new PosAndType(LexinfoOnt.noun, OntolexOnt.Word));
        posAndTypeValueMap.put("loc noun", new PosAndType(LexinfoOnt.noun, OntolexOnt.MultiWordExpression));
        posAndTypeValueMap.put("loc nom", new PosAndType(LexinfoOnt.noun, OntolexOnt.MultiWordExpression));
        posAndTypeValueMap.put("nome", new PosAndType(LexinfoOnt.properNoun, OntolexOnt.LexicalEntry));
        posAndTypeValueMap.put("name", new PosAndType(LexinfoOnt.properNoun, OntolexOnt.LexicalEntry));
        posAndTypeValueMap.put("adj", new PosAndType(LexinfoOnt.adjective, OntolexOnt.Word));
        posAndTypeValueMap.put("adjc", new PosAndType(LexinfoOnt.adjective, OntolexOnt.Word));
        posAndTypeValueMap.put("agg", new PosAndType(LexinfoOnt.adjective, OntolexOnt.Word));
        posAndTypeValueMap.put("loc adjc", new PosAndType(LexinfoOnt.adjective, OntolexOnt.MultiWordExpression));
        posAndTypeValueMap.put("loc agg", new PosAndType(LexinfoOnt.adjective, OntolexOnt.MultiWordExpression));
        posAndTypeValueMap.put("avv", new PosAndType(LexinfoOnt.adverb, OntolexOnt.Word));
        posAndTypeValueMap.put("adv", new PosAndType(LexinfoOnt.adverb, OntolexOnt.Word));
        posAndTypeValueMap.put("loc avv", new PosAndType(LexinfoOnt.adverb, OntolexOnt.MultiWordExpression));
        posAndTypeValueMap.put("verb", new PosAndType(LexinfoOnt.verb, OntolexOnt.Word));
        posAndTypeValueMap.put("loc verb", new PosAndType(LexinfoOnt.verb, OntolexOnt.MultiWordExpression));

        posAndTypeValueMap.put("agg num", new PosAndType(LexinfoOnt.numeral, OntolexOnt.Word));
        posAndTypeValueMap.put("agg poss", new PosAndType(LexinfoOnt.possessiveAdjective, OntolexOnt.Word));
        // card/ord is not a Part of speech, but an information added to aggetivi numerali
        //        posAndTypeValueMap.put("card", new PosAndType(LexinfoOnt.cardinalNumeral, OntolexOnt.Word));
        //        posAndTypeValueMap.put("ord", new PosAndType(LexinfoOnt.ordinalAdjective, OntolexOnt.Word));
        posAndTypeValueMap.put("agg nom", new PosAndType(LexinfoOnt.adjective, OntolexOnt.Word));

        posAndTypeValueMap.put("art", new PosAndType(LexinfoOnt.article, OntolexOnt.Word));
        posAndTypeValueMap.put("cong", new PosAndType(LexinfoOnt.conjunction, OntolexOnt.Word));
        posAndTypeValueMap.put("conj", new PosAndType(LexinfoOnt.conjunction, OntolexOnt.Word));
        posAndTypeValueMap.put("inter", new PosAndType(LexinfoOnt.interjection, OntolexOnt.Word));
        posAndTypeValueMap.put("interj", new PosAndType(LexinfoOnt.interjection, OntolexOnt.Word));
        posAndTypeValueMap.put("loc cong", new PosAndType(LexinfoOnt.conjunction, OntolexOnt.MultiWordExpression));
        posAndTypeValueMap.put("loc conj", new PosAndType(LexinfoOnt.conjunction, OntolexOnt.MultiWordExpression));
        posAndTypeValueMap.put("loc inter", new PosAndType(LexinfoOnt.interjection, OntolexOnt.MultiWordExpression));
        posAndTypeValueMap.put("loc interj", new PosAndType(LexinfoOnt.interjection, OntolexOnt.MultiWordExpression));
        posAndTypeValueMap.put("loc prep", new PosAndType(LexinfoOnt.preposition, OntolexOnt.MultiWordExpression));
        posAndTypeValueMap.put("posp", new PosAndType(LexinfoOnt.postposition, OntolexOnt.Word));
        posAndTypeValueMap.put("prep", new PosAndType(LexinfoOnt.preposition, OntolexOnt.Word));
        posAndTypeValueMap.put("pron poss", new PosAndType(LexinfoOnt.possessivePronoun, OntolexOnt.Word));
        posAndTypeValueMap.put("pronome poss", new PosAndType(LexinfoOnt.possessivePronoun, OntolexOnt.Word));
        posAndTypeValueMap.put("pronome", new PosAndType(LexinfoOnt.pronoun, OntolexOnt.Word));
        posAndTypeValueMap.put("pron dim", new PosAndType(LexinfoOnt.demonstrativePronoun, OntolexOnt.Word));

        // TODO: -acron-, -acronim-, -acronym-, -espr-, -espress- mark locution as phrases

        // Template:-abbr-

        // Template redirects
//        Template:-abbr-
//                Template:-acronim-
//                Template:-acronym-
//                Template:-esclam-
//                Template:-espress-
//                Template:-let-
//                Template:-loc noun form-
//                Template:-name form-
//                Template:-noun form-
//                Template:-prefix-
//                Template:-pronome form-
//                Template:-pronoun form-
        posAndTypeValueMap.put("pref", new PosAndType(LexinfoOnt.prefix, OntolexOnt.Affix));
        posAndTypeValueMap.put("prefix", new PosAndType(LexinfoOnt.prefix, OntolexOnt.Affix));
        posAndTypeValueMap.put("suff", new PosAndType(LexinfoOnt.suffix, OntolexOnt.Affix));
        posAndTypeValueMap.put("suffix", new PosAndType(LexinfoOnt.suffix, OntolexOnt.Affix));


        posAndTypeValueMap.put("acron", new PosAndType(null, OntolexOnt.LexicalEntry));
        posAndTypeValueMap.put("acronim", new PosAndType(null, OntolexOnt.LexicalEntry));
        posAndTypeValueMap.put("acronym", new PosAndType(null, OntolexOnt.LexicalEntry));
        posAndTypeValueMap.put("espr", new PosAndType(null, OntolexOnt.MultiWordExpression));
        posAndTypeValueMap.put("espress", new PosAndType(null, OntolexOnt.MultiWordExpression));


        // For translation glosses

    }

    private String encodedWiktionaryPageName;

    public static boolean isValidPOS(String pos) {
        return posAndTypeValueMap.containsKey(pos);
    }

    Map<Resource, Set<Resource>> lexEntries = new HashMap<>();

    public WiktionaryDataHandler(String lang) {
        super(lang);
    }

    @Override
    public void initializePageExtraction(String wiktionaryPageName) {
        super.initializePageExtraction(wiktionaryPageName);

    }

    @Override
    public void initializeEntryExtraction(String wiktionaryPageName) {
        super.initializeEntryExtraction(wiktionaryPageName);
        lexEntries.clear();
        encodedWiktionaryPageName = uriEncode(currentWiktionaryPageName);
    }

    @Override
    public void finalizePageExtraction() {
        super.finalizePageExtraction();
    }

    @Override
    public void finalizeEntryExtraction() {
        super.finalizeEntryExtraction();
        encodedWiktionaryPageName = null;
    }

    @Override
    public void registerTranslation(String lang, Resource currentGloss, String usage, String word) {
        if (lexEntries.size() == 0) {
            log.debug("Registering Translation when no lexical entry is defined in {}", currentWiktionaryPageName);
        } else if (lexEntries.size() == 1) {
            super.registerTranslation(lang, currentGloss, usage, word);
        } else if (null == currentGloss) {
            log.debug("Attaching translations to Vocable (Null gloss and several lexical entries) in {}", currentWiktionaryPageName);
            super.registerTranslationToEntity(currentMainLexEntry, lang, currentGloss, usage, word);
        } else {
            // TODO: guess which translation is to be attached to which entry/sense
            List<Resource> entries = getLexicalEntryUsingPartOfSpeech(currentGloss);
            if (entries.size() != 0) {
                log.trace("Attaching translations using part of speech in gloss : {}", currentWiktionaryPageName);
                if (entries.size() > 1) {
                    log.trace("Attaching translations to several entries in {}", currentWiktionaryPageName);
                }
                for (Resource entry : entries) {
                    super.registerTranslationToEntity(entry, lang, currentGloss, usage, word);
                }
            } else {
                Statement s = currentGloss.getProperty(RDF.value);
                String g = (null == s) ? "" : s.getString();
                log.debug("Several entries are defined in {} // {}", currentWiktionaryPageName, g);
                // TODO: disambiguate and attach to the correct entry.
                super.registerTranslationToEntity(currentMainLexEntry, lang, currentGloss, usage, word);
            }
        }
    }

    protected String getGlossResourceName(StructuredGloss gloss) {
        String key = gloss.getGloss() + gloss.getSenseNumber();
        key = DatatypeConverter.printBase64Binary(BigInteger.valueOf(key.hashCode()).toByteArray()).replaceAll("[/=\\+]", "-");
        return getPrefix() + "__" + extractedLang + "_gloss_" + key + "_" + encodedWiktionaryPageName ;
    }

    private List<Resource> getLexicalEntryUsingPartOfSpeech(Resource structuredGloss) {
        ArrayList<Resource> res = new ArrayList<>();
        Statement s = structuredGloss.getProperty(RDF.value);
        if (null == s) return res;
        String gloss = s.getString();
        if (null == gloss) return res;
        gloss = gloss.trim();
        if (gloss.startsWith("aggettivo numerale")) {
            addAllResourceOfPoS(res, LexinfoOnt.numeral);
        } else if (gloss.startsWith("aggettivo")) {
            addAllResourceOfPoS(res, LexinfoOnt.adjective);
        } else if (gloss.startsWith("avverbio")) {
            addAllResourceOfPoS(res, LexinfoOnt.adverb);
        } else if (gloss.startsWith("pronome")) {
            addAllResourceOfPoS(res, LexinfoOnt.pronoun);
        } else if (gloss.startsWith("sostantivo")) {
            addAllResourceOfPoS(res, LexinfoOnt.noun);
        } else if (gloss.startsWith("verbo")) {
            addAllResourceOfPoS(res, LexinfoOnt.verb);
        } else if (gloss.startsWith("agg. e sost.")) {
            addAllResourceOfPoS(res, LexinfoOnt.adjective);
            addAllResourceOfPoS(res, LexinfoOnt.noun);
        }
        return res;
    }

    private void addAllResourceOfPoS(ArrayList<Resource> res, Resource pos) {
        Set<Resource> ares = lexEntries.get(pos);
        if (ares != null)
            res.addAll(ares);
    }

    @Override
    public void addPartOfSpeech(String pos) {
        // TODO: Italian sometimes define translations for noun forms. If an entry is ambiguous,
        // TODO: then translations could be wrongly attached. The forms should be kept in lex entries
        // TODO: but not correspond to a valid resource. This will be usefull for later
        // drop of non useful translations.
        PosAndType pat = posAndTypeValueMap.get(pos);
        Resource entry = addPartOfSpeech(pos, posResource(pat), typeResource(pat));
        addLexEntry(posResource(pat), entry);
    }

    private void addLexEntry(Resource pos, Resource entry) {
        Set<Resource> entrySet;
        if (null != (entrySet = lexEntries.get(pos))) {
            entrySet.add(entry);
        } else {
            entrySet = new HashSet<Resource>();
            entrySet.add(entry);
            lexEntries.put(pos, entrySet);
        }
    }
}

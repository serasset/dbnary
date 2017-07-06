package org.getalp.dbnary.lat;

import com.hp.hpl.jena.rdf.model.Resource;
import org.getalp.dbnary.OntolexBasedRDFDataHandler;
import org.getalp.dbnary.OntolexOnt;
import org.getalp.dbnary.LexinfoOnt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by serasset on 17/09/14.
 */
public class WiktionaryDataHandler extends OntolexBasedRDFDataHandler {

    private Logger log = LoggerFactory.getLogger(WiktionaryDataHandler.class);

    static {

        // French
        posAndTypeValueMap.put("nomen-subst", new PosAndType(LexinfoOnt.noun, OntolexOnt.Word));
        posAndTypeValueMap.put("nomen", new PosAndType(LexinfoOnt.noun, OntolexOnt.Word));
        posAndTypeValueMap.put("nomen-prop", new PosAndType(LexinfoOnt.properNoun, OntolexOnt.Word));
        // posAndTypeValueMap.put("-prénom-", new PosAndType(LexinfoOnt.properNoun, OntolexOnt.Word));
        posAndTypeValueMap.put("nomen-adj", new PosAndType(LexinfoOnt.adjective, OntolexOnt.Word));
        posAndTypeValueMap.put("verbum", new PosAndType(LexinfoOnt.verb, OntolexOnt.Word));
        // TODO: check how to encode transitivity/intransitivity
        posAndTypeValueMap.put("verbum-tr", new PosAndType(LexinfoOnt.verb, OntolexOnt.Word));
        posAndTypeValueMap.put("verbum-intr", new PosAndType(LexinfoOnt.verb, OntolexOnt.Word));
        posAndTypeValueMap.put("adverbium", new PosAndType(LexinfoOnt.adverb, OntolexOnt.Word));
        //posAndTypeValueMap.put("-loc-adv-", new PosAndType(LexinfoOnt.adverb, OntolexOnt.Phrase));
        //posAndTypeValueMap.put("-loc-adj-", new PosAndType(LexinfoOnt.adjective, OntolexOnt.Phrase));
        //posAndTypeValueMap.put("-loc-nom-", new PosAndType(LexinfoOnt.noun, OntolexOnt.Phrase));
        //posAndTypeValueMap.put("-loc-verb-", new PosAndType(LexinfoOnt.verb, OntolexOnt.Phrase));
        posAndTypeValueMap.put("coniunctio", new PosAndType(LexinfoOnt.conjunction, OntolexOnt.Word));
        posAndTypeValueMap.put("pronomen", new PosAndType(LexinfoOnt.pronoun, OntolexOnt.Word));
        posAndTypeValueMap.put("participium", new PosAndType(LexinfoOnt.participle, OntolexOnt.Word));
        posAndTypeValueMap.put("wikt-praep", new PosAndType(LexinfoOnt.preposition, OntolexOnt.Word));
    }

    public WiktionaryDataHandler(String lang) {
        super(lang);
    }

    @Override
    public void addPartOfSpeech(String pos) {
        // DONE: compute if the entry is a phrase or a word.
        PosAndType pat = posAndTypeValueMap.get(pos);
        Resource typeR = typeResource(pat);
        if (currentWiktionaryPageName.startsWith("se ")) {
            if (currentWiktionaryPageName.substring(2).trim().contains(" ")) {
                typeR = OntolexOnt.MultiWordExpression;
            }
        } else if (currentWiktionaryPageName.contains(" ")) {
            typeR = OntolexOnt.MultiWordExpression;
        }
        addPartOfSpeech(pos, posResource(pat), typeR);
    }


}

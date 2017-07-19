package org.getalp.dbnary.spa;

import org.apache.jena.rdf.model.Resource;
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

    public WiktionaryDataHandler(String lang) {
        super(lang);
    }

    @Override
    public void addPartOfSpeech(String pos) {
        if (pos.startsWith("{{")) pos = pos.substring(2).trim();
        pos = pos.split("\\|")[0];

        PosAndType pat = null;
        String spos = pos;
        // TODO : handle locucions

        if (pos.startsWith("adjetivo cardinal")) {
            pat = new PosAndType(LexinfoOnt.adjective, OntolexOnt.Word);
        } else if (pos.startsWith("adjetivo ordinal")) {
            pat = new PosAndType(LexinfoOnt.ordinalAdjective, OntolexOnt.Word);
        } else if (pos.startsWith("adjetivo posesivo")) {
            pat = new PosAndType(LexinfoOnt.possessiveAdjective, OntolexOnt.Word);
        } else if (pos.startsWith("adjetivo")) {
            pat = new PosAndType(LexinfoOnt.adjective, OntolexOnt.Word);
        } else if (pos.startsWith("sustantivo propio")) {
            pat = new PosAndType(LexinfoOnt.properNoun, OntolexOnt.Word);
        } else if (pos.startsWith("sustantivo femenino y masculino")) {
            pat = new PosAndType(LexinfoOnt.noun, OntolexOnt.Word);
            // TODO: add proper gender
        } else if (pos.startsWith("sustantivo femenino")) {
            pat = new PosAndType(LexinfoOnt.noun, OntolexOnt.Word);
            // TODO: add proper gender
        } else if (pos.startsWith("sustantivo masculino")) {
            pat = new PosAndType(LexinfoOnt.noun, OntolexOnt.Word);
            // TODO: add proper gender
        } else if (pos.startsWith("sustantivo")) {
            pat = new PosAndType(LexinfoOnt.noun, OntolexOnt.Word);
        } else if (pos.startsWith("adverbio")) {
            pat = new PosAndType(LexinfoOnt.adverb, OntolexOnt.Word);
        } else if (pos.startsWith("verbo transitivo")) {
            pat = new PosAndType(LexinfoOnt.verb, OntolexOnt.Word);
            // TODO: add proper syntactive frame
        } else if (pos.startsWith("verbo intransitivo")) {
            pat = new PosAndType(LexinfoOnt.verb, OntolexOnt.Word);
            // TODO: add proper syntactive frame
        } else if (pos.startsWith("verbo pronominal")) {
            pat = new PosAndType(LexinfoOnt.verb, OntolexOnt.Word);
            // TODO: add proper syntactive frame
        } else if (pos.startsWith("verbo impersonal")) {
            pat = new PosAndType(LexinfoOnt.verb, OntolexOnt.Word);
            // TODO: add proper syntactive frame
        } else if (pos.startsWith("verbo auxiliar")) {
            pat = new PosAndType(LexinfoOnt.verb, OntolexOnt.Word);
            // TODO: add proper syntactive frame
        } else if (pos.startsWith("verbo")) {
            pat = new PosAndType(LexinfoOnt.verb, OntolexOnt.Word);
        }

        // TODO handle extra information (genre, ...) from pos
        log.debug("Handling POS String: {} --> {} || in {}", pos, posResource(pat), currentWiktionaryPageName);
        // PosAndType pat = posAndTypeValueMap.get(pos);
        Resource typeR = typeResource(pat);
        addPartOfSpeech(spos, posResource(pat), typeR);
    }


}

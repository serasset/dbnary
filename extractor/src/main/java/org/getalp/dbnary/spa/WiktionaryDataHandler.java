package org.getalp.dbnary.spa;

import com.hp.hpl.jena.rdf.model.Resource;
import org.getalp.dbnary.LemonBasedRDFDataHandler;
import org.getalp.dbnary.LemonOnt;
import org.getalp.dbnary.LexinfoOnt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by serasset on 17/09/14.
 */
public class WiktionaryDataHandler extends LemonBasedRDFDataHandler {

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
            pat = new PosAndType(LexinfoOnt.adjective, LemonOnt.Word);
        } else if (pos.startsWith("adjetivo ordinal")) {
            pat = new PosAndType(LexinfoOnt.ordinalAdjective, LemonOnt.Word);
        } else if (pos.startsWith("adjetivo posesivo")) {
            pat = new PosAndType(LexinfoOnt.possessiveAdjective, LemonOnt.Word);
        } else if (pos.startsWith("adjetivo")) {
            pat = new PosAndType(LexinfoOnt.adjective, LemonOnt.Word);
        } else if (pos.startsWith("sustantivo propio")) {
            pat = new PosAndType(LexinfoOnt.properNoun, LemonOnt.Word);
        } else if (pos.startsWith("sustantivo femenino y masculino")) {
            pat = new PosAndType(LexinfoOnt.noun, LemonOnt.Word);
            // TODO: add proper gender
        } else if (pos.startsWith("sustantivo femenino")) {
            pat = new PosAndType(LexinfoOnt.noun, LemonOnt.Word);
            // TODO: add proper gender
        } else if (pos.startsWith("sustantivo masculino")) {
            pat = new PosAndType(LexinfoOnt.noun, LemonOnt.Word);
            // TODO: add proper gender
        } else if (pos.startsWith("sustantivo")) {
            pat = new PosAndType(LexinfoOnt.noun, LemonOnt.Word);
        } else if (pos.startsWith("adverbio")) {
            pat = new PosAndType(LexinfoOnt.adverb, LemonOnt.Word);
        } else if (pos.startsWith("verbo transitivo")) {
            pat = new PosAndType(LexinfoOnt.verb, LemonOnt.Word);
            // TODO: add proper syntactive frame
        } else if (pos.startsWith("verbo intransitivo")) {
            pat = new PosAndType(LexinfoOnt.verb, LemonOnt.Word);
            // TODO: add proper syntactive frame
        } else if (pos.startsWith("verbo pronominal")) {
            pat = new PosAndType(LexinfoOnt.verb, LemonOnt.Word);
            // TODO: add proper syntactive frame
        } else if (pos.startsWith("verbo impersonal")) {
            pat = new PosAndType(LexinfoOnt.verb, LemonOnt.Word);
            // TODO: add proper syntactive frame
        } else if (pos.startsWith("verbo auxiliar")) {
            pat = new PosAndType(LexinfoOnt.verb, LemonOnt.Word);
            // TODO: add proper syntactive frame
        } else if (pos.startsWith("verbo")) {
            pat = new PosAndType(LexinfoOnt.verb, LemonOnt.Word);
        }

        // TODO handle extra information (genre, ...) from pos
        log.debug("Handling POS String: {} --> {} || in {}", pos, posResource(pat), currentWiktionaryPageName);
        // PosAndType pat = posAndTypeValueMap.get(pos);
        Resource typeR = typeResource(pat);
        addPartOfSpeech(spos, posResource(pat), typeR);
    }


}

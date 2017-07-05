package org.getalp.dbnary.por;

import org.getalp.dbnary.OntolexBasedRDFDataHandler;
import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.OntolexOnt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by serasset on 17/09/14.
 */
public class WiktionaryDataHandler extends OntolexBasedRDFDataHandler {

    private Logger log = LoggerFactory.getLogger(WiktionaryDataHandler.class);

    static {

        // Portuguese
        posAndTypeValueMap.put("Substantivo", new PosAndType(LexinfoOnt.noun, OntolexOnt.LexicalEntry));
        posAndTypeValueMap.put("Adjetivo", new PosAndType(LexinfoOnt.adjective, OntolexOnt.LexicalEntry));
        posAndTypeValueMap.put("Verbo", new PosAndType(LexinfoOnt.verb, OntolexOnt.LexicalEntry));
        posAndTypeValueMap.put("Advérbio", new PosAndType(LexinfoOnt.adverb, OntolexOnt.LexicalEntry));


    }

    public WiktionaryDataHandler(String lang) {
        super(lang);
    }

}

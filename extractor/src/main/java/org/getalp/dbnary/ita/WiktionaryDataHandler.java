package org.getalp.dbnary.ita;

import org.getalp.dbnary.LemonBasedRDFDataHandler;
import org.getalp.dbnary.LemonOnt;
import org.getalp.dbnary.LexinfoOnt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class WiktionaryDataHandler extends LemonBasedRDFDataHandler {

    private Logger log = LoggerFactory.getLogger(WiktionaryDataHandler.class);

    static {

        posAndTypeValueMap.put("noun", new PosAndType(LexinfoOnt.noun, LemonOnt.Word));
        posAndTypeValueMap.put("sost", new PosAndType(LexinfoOnt.noun, LemonOnt.Word));
        posAndTypeValueMap.put("loc noun", new PosAndType(LexinfoOnt.noun, LemonOnt.Phrase));
        posAndTypeValueMap.put("loc nom", new PosAndType(LexinfoOnt.noun, LemonOnt.Phrase));
        posAndTypeValueMap.put("nome", new PosAndType(LexinfoOnt.properNoun, LemonOnt.LexicalEntry));
        posAndTypeValueMap.put("name", new PosAndType(LexinfoOnt.properNoun, LemonOnt.LexicalEntry));
        posAndTypeValueMap.put("adj", new PosAndType(LexinfoOnt.adjective, LemonOnt.Word));
        posAndTypeValueMap.put("agg", new PosAndType(LexinfoOnt.adjective, LemonOnt.Word));
        posAndTypeValueMap.put("loc adjc", new PosAndType(LexinfoOnt.adjective, LemonOnt.Phrase));
        posAndTypeValueMap.put("loc agg", new PosAndType(LexinfoOnt.adjective, LemonOnt.Phrase));
        posAndTypeValueMap.put("avv", new PosAndType(LexinfoOnt.adverb, LemonOnt.Word));
        posAndTypeValueMap.put("adv", new PosAndType(LexinfoOnt.adverb, LemonOnt.Word));
        posAndTypeValueMap.put("loc avv", new PosAndType(LexinfoOnt.adverb, LemonOnt.Phrase));
        posAndTypeValueMap.put("verb", new PosAndType(LexinfoOnt.verb, LemonOnt.Word));
        posAndTypeValueMap.put("loc verb", new PosAndType(LexinfoOnt.verb, LemonOnt.Phrase));
        // TODO: -acron-, -acronim-, -acronym-, -espr-, -espress- mark locution as phrases

    }
    public static boolean isValidPOS(String pos) {
        return posAndTypeValueMap.containsKey(pos);
    }

    public WiktionaryDataHandler(String lang) {
        super(lang);
    }


}

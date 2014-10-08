package org.getalp.dbnary.fra;

import org.getalp.dbnary.LemonBasedRDFDataHandler;
import org.getalp.dbnary.LemonOnt;
import org.getalp.dbnary.LexinfoOnt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by serasset on 17/09/14.
 */
public class WiktionaryDataHandler extends LemonBasedRDFDataHandler {

    private Logger log = LoggerFactory.getLogger(WiktionaryDataHandler.class);

    static {

        // French
        posAndTypeValueMap.put("-nom-", new PosAndType(LexinfoOnt.noun, LemonOnt.Word));
        posAndTypeValueMap.put("-nom-pr-", new PosAndType(LexinfoOnt.properNoun, LemonOnt.Word));
        posAndTypeValueMap.put("-prénom-", new PosAndType(LexinfoOnt.properNoun, LemonOnt.Word));
        posAndTypeValueMap.put("-adj-", new PosAndType(LexinfoOnt.adjective, LemonOnt.Word));
        posAndTypeValueMap.put("-verb-", new PosAndType(LexinfoOnt.verb, LemonOnt.Word));
        posAndTypeValueMap.put("-adv-", new PosAndType(LexinfoOnt.adverb, LemonOnt.Word));
        posAndTypeValueMap.put("-loc-adv-", new PosAndType(LexinfoOnt.adverb, LemonOnt.Phrase));
        posAndTypeValueMap.put("-loc-adj-", new PosAndType(LexinfoOnt.adjective, LemonOnt.Phrase));
        posAndTypeValueMap.put("-loc-nom-", new PosAndType(LexinfoOnt.noun, LemonOnt.Phrase));
        posAndTypeValueMap.put("-loc-verb-", new PosAndType(LexinfoOnt.verb, LemonOnt.Phrase));
    }

    public WiktionaryDataHandler(String lang) {
        super(lang);
    }

    @Override
    public void addPartOfSpeech(String pos) {
        // TODO: compute if the entry is a phrase or a word.
        PosAndType pat = posAndTypeValueMap.get(pos);
        addPartOfSpeech(pos, posResource(pat), typeResource(pat));
    }

    public static PosAndType getPosAndType(String pos) {
        return posAndTypeValueMap.get(pos);
    }

}

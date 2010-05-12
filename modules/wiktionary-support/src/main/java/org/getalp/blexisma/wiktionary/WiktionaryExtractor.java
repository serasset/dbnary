package org.getalp.blexisma.wiktionary;

import org.getalp.blexisma.api.SemanticNetwork;

public abstract class WiktionaryExtractor {
    
    protected WiktionaryIndex wiktionaryIndex;

    public WiktionaryExtractor(WiktionaryIndex wi) {
        super();
        this.wiktionaryIndex = wi;
    }

    /**
     * @return the wiktionaryIndex
     */
    public WiktionaryIndex getWiktionaryIndex() {
        return wiktionaryIndex;
    }
    
    public abstract void extractData(String wiktionaryPageName, SemanticNetwork semnet);
    
}

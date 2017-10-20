package org.getalp.dbnary.fra;

import org.getalp.LangTools;
import org.getalp.dbnary.OntolexBasedRDFDataHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class ForeignLanguagesWiktionaryDataHandler extends OntolexBasedRDFDataHandler {


    private Logger log = LoggerFactory.getLogger(ForeignLanguagesWiktionaryDataHandler.class);

    private HashMap<String, String> prefixes = new HashMap<String, String>();

    private String currentPrefix = null;

    public ForeignLanguagesWiktionaryDataHandler(String lang) {
        super(lang);

    }

    public void initializeEntryExtraction(String wiktionaryPageName, String lang) {
        currentPrefix = getPrefix(lang);
        super.initializeEntryExtraction(wiktionaryPageName);
    }

    @Override
    public void finalizeEntryExtraction() {
        currentPrefix = null;
    }


    @Override
    public String currentLexEntry() {
        // TODO Auto-generated method stub
        return currentWiktionaryPageName;
    }

    @Override
    public String getPrefix() {
        return currentPrefix;
    }

    public String getPrefix(String lang) {
        if (this.prefixes.containsKey(lang))
            return this.prefixes.get(lang);
        else {
            lang = LangTools.normalize(lang);
            String prefix = DBNARY_NS_PREFIX + "/fra/" + lang +"/";
            prefixes.put(lang, prefix);
            aBox.setNsPrefix(lang + "-fra", prefix);
            return prefix;
        }
    }
}

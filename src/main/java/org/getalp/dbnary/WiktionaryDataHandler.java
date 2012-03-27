package org.getalp.dbnary;

public interface WiktionaryDataHandler {

    public void initializeEntryExtraction(String wiktionaryPageName);

    public void addPartOfSpeech(String pos);

    /**
     * 
     * @param def the not cleaned up version of the definition. This version contains macros (that may represent subject fields) and links.
     */
    // TODO: maybe pass the cleaned up and the original def, so that the extractor takes what fits its requirements.
    public void registerNewDefinition(String def);

    public void registerAlternateSpelling(String alt);
    
    public void registerNymRelation(String leftGroup, String synRelation);

    public void registerTranslation(String lang, String currentGlose, String usage, String word);

	public void finalizeEntryExtraction();

	public int nbEntries();
}

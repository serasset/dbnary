package org.getalp.dbnary;

import java.io.OutputStream;

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

    public void registerPronunciation(String pron, String lang);

	public void finalizeEntryExtraction();

	public int nbEntries();
	
	public String currentLexEntry();
	
	public void dump(OutputStream out);
    
	/**
	 * Write a serialized represention of this model in a specified language.
	 * The language in which to write the model is specified by the lang argument. 
	 * Predefined values are "RDF/XML", "RDF/XML-ABBREV", "N-TRIPLE", "TURTLE", (and "TTL") and "N3". 
	 * The default value, represented by null, is "RDF/XML".
	 * @param out
	 * @param format
	 */
	public void dump(OutputStream out, String format);

	
}

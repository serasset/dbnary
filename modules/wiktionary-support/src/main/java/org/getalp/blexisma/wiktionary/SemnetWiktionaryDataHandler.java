package org.getalp.blexisma.wiktionary;

import org.getalp.blexisma.api.ISO639_3;
import org.getalp.blexisma.api.SemanticNetwork;
import org.getalp.blexisma.semnet.SimpleSemanticNetwork;

public class SemnetWiktionaryDataHandler implements WiktionaryDataHandler {

    protected static  String langPrefix = "no-language-specified";
    /**
	 * @uml.property  name="lang"
	 */
    protected  String lang = "no-language-specified";
        
    protected final static String POS_RELATION = "pos";
    protected final static String DEF_RELATION = "def";
    protected final static String ALT_RELATION = "alt";
    protected final static String SYN_RELATION = "syn";
    protected final static String ANT_RELATION = "ant";
    protected final static String TRANSLATION_RELATION = "trad";
    protected final static String POS_PREFIX = "#" + POS_RELATION + "|";
    protected final static String DEF_PREFIX = "#" + DEF_RELATION + "|";
        
    // protected WiktionaryIndex wiktionaryIndex;
    /**
	 * @uml.property  name="semnet"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
    protected SemanticNetwork<String, String> semnet;
    /**
	 * @uml.property  name="wiktionaryPageNameWithLangPrefix"
	 */
    protected String wiktionaryPageNameWithLangPrefix;
    /**
	 * @uml.property  name="wiktionaryPageName"
	 */
    protected String wiktionaryPageName;

    /**
	 * @uml.property  name="currentPos"
	 */
    protected String currentPos = "";
    /**
	 * @uml.property  name="nbEntries"
	 */
    protected int nbEntries = 0;

    public SemnetWiktionaryDataHandler(String lang) {
    	this(new SimpleSemanticNetwork<String, String>(), lang);
    }
    
    public SemnetWiktionaryDataHandler(SemanticNetwork<String, String> semnet, String lang) {
    	this.lang = ISO639_3.sharedInstance.getIdCode(lang);
        langPrefix = "#" + lang + "|";
        
    	this.semnet = semnet;
    }
    
    public void initializeEntryExtraction(String wiktionaryPageName) {
    	this.wiktionaryPageName = wiktionaryPageName;
        this.wiktionaryPageNameWithLangPrefix = langPrefix + wiktionaryPageName;
    }

    public void addPartOfSpeech(String pos) {
    	nbEntries++;
        currentPos = pos;
        semnet.addRelation(wiktionaryPageNameWithLangPrefix, POS_PREFIX + pos, 1, POS_RELATION);
    }

    /**
     * 
     * @param def the not cleaned up version of the definition. This version contains macros (that may represent subject fields) and links.
     */
    // TODO: maybe pass the cleaned up and the original def, so that the extractor takes what fits its requirements.
    public void registerNewDefinition(String def) {
    	def = WiktionaryExtractor.cleanUpMarkup(def);
    	def = DEF_PREFIX + def;
        this.semnet.addRelation(this.wiktionaryPageNameWithLangPrefix, def, 1, DEF_RELATION);
        if (currentPos != null && ! currentPos.equals("")) {
            this.semnet.addRelation(def, POS_PREFIX + currentPos, 1, POS_RELATION);
        }
	}

    public void registerAlternateSpelling(String alt) {
    	alt = langPrefix + alt;
        this.semnet.addRelation(this.wiktionaryPageNameWithLangPrefix, alt, 1, ALT_RELATION);
    }
    
	public void registerNymRelation(String leftGroup, String synRelation) {
		 leftGroup = langPrefix + leftGroup;
        this.semnet.addRelation(this.wiktionaryPageNameWithLangPrefix, leftGroup, 1, synRelation);
	}
	
    public void registerTranslation(String lang, String currentGlose,
			String usage, String word) {
    	String rel = "trad|#" + lang + ((currentGlose == null || currentGlose.equals("")) ? "" : "|" + currentGlose);
        rel = rel + ((usage == null) ? "" : "|" + usage);
        semnet.addRelation(wiktionaryPageNameWithLangPrefix, new String("#" + lang + "|" + word), 1, rel );
	}

	public void finalizeEntryExtraction() {
		// nop
	}

	public SemanticNetwork<? extends String, ? extends String> getSemnet() {
		// TODO Auto-generated method stub
		return semnet;
	}
   
	@Override
	public int nbEntries() {
		return nbEntries;
	}

}

/**
 * 
 */
package org.getalp.dbnary.fra;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.getalp.dbnary.AbstractWiktionaryExtractor;
import org.getalp.dbnary.IWiktionaryDataHandler;
import org.getalp.dbnary.LangTools;
import org.getalp.dbnary.WiktionaryIndex;
import org.getalp.dbnary.wiki.WikiPatterns;
import org.getalp.dbnary.wiki.WikiTool;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * @author serasset
 *
 */
public class WiktionaryExtractor extends AbstractWiktionaryExtractor {

	// NOTE: to subclass the extractor, you need to define how a language section is recognized.
	// then, how are sections recognized and what is their semantics.
	// then, how to extract specific elements from the particular sections
    protected final static String languageSectionPatternString;

    protected final static String languageSectionPatternString1 = "==\\s*\\{\\{=([^=]*)=\\}\\}\\s*==";
    protected final static String languageSectionPatternString2 = "==\\s*\\{\\{langue\\|([^\\}]*)\\}\\}\\s*==";
    // TODO: handle morphological informations e.g. fr-rég template ?
    protected final static String pronounciationPatternString = "\\{\\{pron\\|([^\\|\\}]*)\\|([^\\}]*)\\}\\}";
    
    protected static final int NODATA = 0;
    protected static final int TRADBLOCK = 1;
    protected static final int DEFBLOCK = 2;
    protected static final int ORTHOALTBLOCK = 3;
    protected static final int NYMBLOCK = 4;
    protected static final int PRONBLOCK = 5;
    protected static final int IGNOREPOS = 6;

    private static HashMap<String,String> posMarkers;
    private static HashSet<String> ignorablePosMarkers;
    private static HashSet<String> sectionMarkers;
    
    private final static HashMap<String, String> nymMarkerToNymName;
    
    private static HashSet<String> unsupportedMarkers = new HashSet<String>();

    
    // private static Set<String> affixesToDiscardFromLinks = null;
    private static void addPos(String pos) {posMarkers.put(pos, pos);}
    private static void addPos(String p, String n) {posMarkers.put(p, n);}
    
    static {
    	languageSectionPatternString = new StringBuilder()
        .append("(?:")
        .append(languageSectionPatternString1)
        .append(")|(?:")
        .append(languageSectionPatternString2)
        .append(")").toString();
        
        posMarkers = new HashMap<String,String>(130);
        ignorablePosMarkers = new HashSet<String>(130);

        addPos("-déf-");
        addPos("-déf-/2");
        addPos("-déf2-");
        addPos("--");
        addPos("-adj-");
        addPos("-adj-/2");
        ignorablePosMarkers.add("-flex-adj-indéf-");
        addPos("-adj-dém-");
        addPos("-adj-excl-");
        addPos("-adj-indéf-");
        addPos("-adj-int-");
        addPos("-adj-num-");
        addPos("-adj-pos-");
        addPos("-adv-");
        addPos("-adv-int-");
        addPos("-adv-pron-");
        addPos("-adv-rel-");
        addPos("-aff-");
        addPos("-art-");
        ignorablePosMarkers.add("-flex-art-déf-");
        ignorablePosMarkers.add("-flex-art-indéf-");
        ignorablePosMarkers.add("-flex-art-part-");
        addPos("-art-déf-");
        addPos("-art-indéf-");
        addPos("-art-part-");
        addPos("-aux-");
        addPos("-circonf-");
        addPos("-class-");
        addPos("-cpt-");
        addPos("-conj-");
        addPos("-conj-coord-");
        addPos("-cont-");
        addPos("-copule-");
        addPos("-corrélatif-");
        addPos("-erreur-");
        addPos("-faux-prov-");
        ignorablePosMarkers.add("-flex-adj-");
        ignorablePosMarkers.add("-flex-adj-num-");
        ignorablePosMarkers.add("-flex-adj-pos-");
        ignorablePosMarkers.add("-flex-adv-");
        ignorablePosMarkers.add("-flex-art-");
        ignorablePosMarkers.add("-flex-aux-");
        ignorablePosMarkers.add("-flex-conj-");
        ignorablePosMarkers.add("-flex-interj-");
        ignorablePosMarkers.add("-flex-lettre-");
        ignorablePosMarkers.add("-flex-loc-adj-");
        ignorablePosMarkers.add("-flex-loc-conj-");
        ignorablePosMarkers.add("-flex-loc-nom-");
        ignorablePosMarkers.add("-flex-loc-verb-");
        ignorablePosMarkers.add("-flex-nom-");
        ignorablePosMarkers.add("-flex-nom-fam-");
        ignorablePosMarkers.add("-flex-nom-pr-");
        ignorablePosMarkers.add("-flex-mots-diff-");
        ignorablePosMarkers.add("-flex-prénom-");
        ignorablePosMarkers.add("-flex-prép-");
        ignorablePosMarkers.add("-flex-pronom-");
        ignorablePosMarkers.add("-flex-pronom-indéf-");
        ignorablePosMarkers.add("-flex-pronom-int-");
        ignorablePosMarkers.add("-flex-pronom-pers-");
        ignorablePosMarkers.add("-flex-pronom-rel-");
        ignorablePosMarkers.add("-flex-verb-");
        ignorablePosMarkers.add("-inf-");
        addPos("-interf-");
        addPos("-interj-");
        addPos("-lettre-");
        addPos("-loc-");
        addPos("-loc-adj-");
        addPos("-loc-adv-");
        addPos("-loc-conj-");
        addPos("-loc-dét-");
        addPos("-loc-interj-");
        addPos("-loc-nom-");
        addPos("-loc-phr-");
        addPos("-loc-post-");
        addPos("-loc-prép-");
        addPos("-loc-pronom-");
        addPos("-loc-verb-");
        addPos("-nom-");
        addPos("-nom-fam-");
        addPos("-nom-ni-");
        addPos("-nom-nu-");
        addPos("-nom-nn-");
        addPos("-nom-npl-");
        addPos("-nom-pr-");
        addPos("-nom-sciences-");
        addPos("-numér-");
        addPos("-onoma-");
        addPos("-part-");
        addPos("-post-");
        addPos("-préf-");
        addPos("-prénom-");
        addPos("-prép-");
        addPos("-pronom-");
        addPos("-pronom-adj-");
        addPos("-pronom-dém-");
        addPos("-pronom-indéf-");
        addPos("-pronom-int-");
        addPos("-pronom-pers-");
        addPos("-pronom-pos-");
        addPos("-pronom-rel-");
        addPos("-prov-");
        addPos("-racine-");
        addPos("-radical-");
        addPos("-rimes-");
        addPos("-signe-");
        addPos("-sin-");
        addPos("-subst-pron-pers-");
        ignorablePosMarkers.add("-suf-");
        ignorablePosMarkers.add("-flex-suf-");
        ignorablePosMarkers.add("-symb-");
        addPos("type");
        addPos("-var-typo-");
        addPos("-verb-");
        addPos("-verb-pr-");
        
        // titres de section S
        // TODO: get alternate from https://fr.wiktionary.org/wiki/Module:types_de_mots/data and normalise the part of speech
        // ADJECTIFS
        addPos("adjectif", "-adj-");
        addPos("adj", "-adj-");
        addPos("adjectif qualificatif", "-adj-");
 
    // ADVERBES
        addPos("adverbe", "-adv-");
        addPos("adv", "-adv-");
        addPos("adverbe interrogatif");
        addPos("adv-int");
        addPos("adverbe int");
        addPos("adverbe pronominal");
        addPos("adv-pr");
        addPos("adverbe pro");
        addPos("adverbe relatif");
        addPos("adv-rel");
        addPos("adverbe rel");
 
    // CONJONCTIONS
        addPos("conjonction");
        // addPos("conj");
        addPos("conjonction de coordination");
        addPos("conj-coord");
        addPos("conjonction coo");
 
        addPos("copule");
 
    // DÉTERMINANTS
        addPos("adjectif démonstratif");
        addPos("adj-dém");
        addPos("adjectif dém");
        addPos("déterminant");
        addPos("dét");
        addPos("adjectif exclamatif");
        addPos("adj-excl");
        addPos("adjectif exc");
        addPos("adjectif indéfini");
        addPos("adj-indéf");
        addPos("adjectif ind");
        addPos("adjectif interrogatif");
        addPos("adj-int");
        addPos("adjectif int");
        addPos("adjectif numéral");
        addPos("adj-num");
        addPos("adjectif num");
        addPos("adjectif possessif");
        addPos("adj-pos");
        addPos("adjectif pos");
 
        addPos("article");
        addPos("art");
        addPos("article défini");
        addPos("art-déf");
        addPos("article déf");
        addPos("article indéfini");
        addPos("art-indéf");
        addPos("article ind");
        addPos("article partitif");
        addPos("art-part");
        addPos("article par");
 
    // NOMS
        addPos("nom", "-nom-");
        addPos("substantif", "-nom-");
        addPos("nom commun", "-nom-");
        addPos("nom de famille");
        addPos("nom-fam");
        addPos("patronyme");
        addPos("nom propre", "-nom-pr-");
        addPos("nom-pr", "-nom-pr-");
        addPos("nom scientifique");
        addPos("nom-sciences");
        addPos("nom science");
        addPos("nom scient");
        addPos("prénom");

    // PRÉPOSITION
        addPos("préposition");
        addPos("prép");
 
    // PRONOMS
        addPos("pronom");
        addPos("pronom-adjectif");
        addPos("pronom démonstratif");
        addPos("pronom-dém");
        addPos("pronom dém");
        addPos("pronom indéfini");
        addPos("pronom-indéf");
        addPos("pronom ind");
        addPos("pronom interrogatif");
        addPos("pronom-int");
        addPos("pronom int");
        addPos("pronom personnel");
        addPos("pronom-pers");
        addPos("pronom-per");
        addPos("pronom réf");
        addPos("pronom-réfl");
        addPos("pronom réfléchi");
        addPos("pronom possessif");
        addPos("pronom-pos");
        addPos("pronom pos");
        addPos("pronom relatif");
        addPos("pronom-rel");
        addPos("pronom rel");
 
    // VERBES
        addPos("verbe", "-verb-");
        addPos("verb", "-verb-");
        addPos("verbe pronominal");
        addPos("verb-pr");
        addPos("verbe pr");
 
    // EXCLAMATIONS
        addPos("interjection");
        addPos("interj");
        addPos("onomatopée");
        addPos("onoma");
        addPos("onom");
 
    // PARTIES   TODO: Extract affixes in French
//        addPos("affixe");
//        addPos("aff");
//        addPos("circonfixe");
//        addPos("circonf");
//        addPos("circon");
//        addPos("infixe");
//        addPos("inf");
//        addPos("interfixe");
//        addPos("interf");
//        addPos("particule");
//        addPos("part");
//        addPos("particule numérale");
//        addPos("part-num");
//        addPos("particule num");
//        addPos("postposition");
//        addPos("post");
//        addPos("postpos");
//        addPos("préfixe");
//        addPos("préf");
//        addPos("radical");
//        addPos("rad");
//        addPos("suffixe");
//        addPos("suff");
//        addPos("suf");
//
//        addPos("pré-verbe");
//        addPos("pré-nom");

    // PHRASES
        addPos("locution");
        addPos("loc");
        addPos("locution-phrase");
        addPos("loc-phr");
        addPos("locution-phrase");
        addPos("locution phrase");
        addPos("proverbe");
        addPos("prov");
 
    // DIVERS
        addPos("quantificateur");
        addPos("quantif");
        addPos("variante typographique");
        addPos("var-typo");
        addPos("variante typo");
        addPos("variante par contrainte typographique");
 
    // CARACTÈRES
        ignorablePosMarkers.add("lettre");
 
        ignorablePosMarkers.add("symbole");
        ignorablePosMarkers.add("symb");
        addPos("classificateur");
        addPos("class");
        addPos("classif");
        addPos("numéral");
        addPos("numér");
        addPos("num");
        addPos("sinogramme");
        addPos("sinog");
        addPos("sino");
 
        addPos("erreur");
        addPos("faute");
        addPos("faute d'orthographe"); 
        addPos("faute d’orthographe");
 
        // Spéciaux
        addPos("gismu");
        addPos("rafsi");
                
        nymMarkerToNymName = new HashMap<String, String>(20);
        nymMarkerToNymName.put("-méro-", "mero");
        nymMarkerToNymName.put("-hyper-", "hyper");
        nymMarkerToNymName.put("-hypo-", "hypo");
        nymMarkerToNymName.put("-holo-", "holo");
        nymMarkerToNymName.put("-méton-", "meto");
        nymMarkerToNymName.put("-syn-", "syn");
        nymMarkerToNymName.put("-q-syn-", "qsyn");
        nymMarkerToNymName.put("-ant-", "ant");
        

        nymMarkerToNymName.put("méronymes", "mero");
        nymMarkerToNymName.put("méro", "mero");
        nymMarkerToNymName.put("hyperonymes", "hyper");
        nymMarkerToNymName.put("hyper", "hyper");
        nymMarkerToNymName.put("hyponymes", "hypo");
        nymMarkerToNymName.put("hypo", "hypo");
        nymMarkerToNymName.put("holonymes", "holo");
        nymMarkerToNymName.put("holo", "holo");
        nymMarkerToNymName.put("-méton-", "meto");
        nymMarkerToNymName.put("synonymes", "syn");
        nymMarkerToNymName.put("syn", "syn");
        nymMarkerToNymName.put("quasi-synonymes", "qsyn");
        nymMarkerToNymName.put("q-syn", "qsyn");
        nymMarkerToNymName.put("quasi-syn", "qsyn");
        nymMarkerToNymName.put("antonymes", "ant");
        nymMarkerToNymName.put("ant", "ant");
        nymMarkerToNymName.put("anto", "ant");
         
        // paronymes, troponymes, gentillés ?
        
        sectionMarkers = new HashSet<String>(200);
        sectionMarkers.addAll(posMarkers.keySet());
        sectionMarkers.addAll(nymMarkerToNymName.keySet());
        sectionMarkers.add("-étym-");
        sectionMarkers.add("-voc-");
        sectionMarkers.add("-trad-");
        sectionMarkers.add("-note-");
        sectionMarkers.add("-réf-");
        sectionMarkers.add("clé de tri");
        sectionMarkers.add("-anagr-");
        sectionMarkers.add("-drv-");
        sectionMarkers.add("-voir-");
        sectionMarkers.add("-pron-");
        sectionMarkers.add("-gent-");
        sectionMarkers.add("-apr-");
        sectionMarkers.add("-paro-");
        sectionMarkers.add("-homo-");
        sectionMarkers.add("-exp-");
        sectionMarkers.add("-compos-");
        // DONE: prendre en compte la variante orthographique (différences avec -ortho-alt- ?)
        sectionMarkers.add("-var-ortho-");
        
       // TODO trouver tous les modèles de section...
        
        // affixesToDiscardFromLinks = new HashSet<String>();
        // affixesToDiscardFromLinks.add("s");
    }
    
    public WiktionaryExtractor(IWiktionaryDataHandler wdh) {
        super(wdh);
    }


	protected final static Pattern languageSectionPattern;
	protected final static Pattern pronunciationPattern;

    static {
        languageSectionPattern = Pattern.compile(languageSectionPatternString);
        pronunciationPattern = Pattern.compile(pronounciationPatternString);
    }

    int state = NODATA;
    int definitionBlockStart = -1;
    int translationBlockStart = -1;
    int orthBlockStart = -1;
    private int nymBlockStart = -1;

    private String currentNym = null;

	private ExampleExpanderWikiModel exampleExpander;

    @Override
    public void setWiktionaryIndex(WiktionaryIndex wi) {
        super.setWiktionaryIndex(wi);
        exampleExpander = new ExampleExpanderWikiModel(wi, new Locale("fr"), "--DO NOT USE IMAGE BASE URL FOR DEBUG--", "");
    }

    private Set<String> defTemplates = null;

    /* (non-Javadoc)
     * @see org.getalp.dbnary.WiktionaryExtractor#extractData(java.lang.String, org.getalp.blexisma.semnet.SemanticNetwork)
     */
    @Override
    public void extractData() {
        // test swebble
        // testSwebble();

        // System.out.println(pageContent);
        Matcher languageFilter = languageSectionPattern.matcher(pageContent);

        exampleExpander.setPageName(this.wiktionaryPageName);

        while (languageFilter.find() && ! isFrenchLanguageHeader(languageFilter)) {
            ;
        }
        // Either the filter is at end of sequence or on French language header.
        if (languageFilter.hitEnd()) {
            // There is no french data in this page.
            return ;
        }
        int frenchSectionStartOffset = languageFilter.end();
        // Advance till end of sequence or new language section
        languageFilter.find();
        int frenchSectionEndOffset = languageFilter.hitEnd() ? pageContent.length() : languageFilter.start();
        
        extractFrenchData(frenchSectionStartOffset, frenchSectionEndOffset);
     }

//    private void testSwebble() {
//        try {
//            SimpleWikiConfiguration config = new SimpleWikiConfiguration(
//                    "classpath:/org/sweble/wikitext/engine/SimpleWikiConfiguration.xml"
//            );
//
//            // Instantiate a compiler for wiki pages
//            Compiler compiler = new Compiler(config);
//            EntityMap entityMap= new EntityMap();
//
//            // Retrieve a page
//            PageTitle pageTitle = PageTitle.make(config, this.wiktionaryPageName);
//
//            PageId pageId = new PageId(pageTitle, 1l);
//
//            CompiledPage page = compiler.parse(pageId, this.pageContent, null);
//
////            printOutCompiledPage(page);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (JAXBException e) {
//            e.printStackTrace();
//        } catch (CompilerException e) {
//            e.printStackTrace();
//        } catch (LinkTargetException e) {
//            e.printStackTrace();
//        }
//    }

//    protected static void printOutCompiledPage(CompiledPage page) {
//        System.err.println(page.getLog());
//        System.err.println("\n");
//        System.err.println(page.getWarnings());
//        System.err.println("\n");
//        for (AstNode astNode : page) {
//            printOutCompiledPage(astNode);
//        }
//    }

 //   private static void printOutCompiledPage(AstNode astNode) {
//        System.err.println("Node");
//    }

//    protected static void printOutCompiledPage(Page page) {
//        System.err.println("Page");
//        for (AstNode astNode : page) {
//            printOutCompiledPage(astNode);
//        }
//    }


    protected boolean isFrenchLanguageHeader(Matcher m) {
		return (null != m.group(1) && m.group(1).equals("fr")) || (null != m.group(2) && m.group(2).equals("fr"));
	}


	void gotoNoData(Matcher m) {
        state = NODATA;
    }

    
    void gotoTradBlock(Matcher m) {
        translationBlockStart = m.end();
        state = TRADBLOCK;
    }

    // TODO: put up in root class extractor.
    void gotoDefBlock(Matcher m, String pos) {
        state = DEFBLOCK;
        definitionBlockStart = m.end();
        wdh.addPartOfSpeech(pos);
    }
    
    void gotoOrthoAltBlock(Matcher m) {
        state = ORTHOALTBLOCK;    
        orthBlockStart = m.end();
    }

    void leaveOrthoAltBlock(Matcher m) {
        extractOrthoAlt(orthBlockStart, computeRegionEnd(orthBlockStart, m));
        orthBlockStart = -1;
    }

    void leaveTradBlock(Matcher m) {
        extractTranslations(translationBlockStart, computeRegionEnd(translationBlockStart, m));
        translationBlockStart = -1;
    }

    
    void leaveDefBlock(Matcher m) {
    	int end = computeRegionEnd(definitionBlockStart, m);
        extractDefinitions(definitionBlockStart, end);
        extractPronunciation(definitionBlockStart, end);
        definitionBlockStart = -1;
    }
    
	void gotoNymBlock(Matcher m, String nym) {
        state = NYMBLOCK;
        currentNym = nym;
        nymBlockStart = m.end();      
     }

    void gotoIgnorePos() {
        state = IGNOREPOS;
     }

    void leaveNymBlock(Matcher m) {
        extractNyms(currentNym, nymBlockStart, computeRegionEnd(nymBlockStart, m));
        currentNym = null;
        nymBlockStart = -1;         
     }
    
    protected void extractFrenchData(int startOffset, int endOffset) {        
        Matcher m = WikiPatterns.macroPattern.matcher(pageContent);
        m.region(startOffset, endOffset);
        wdh.initializeEntryExtraction(wiktionaryPageName);

        // WONTDO: (priority: low) should I use a macroOrLink pattern to detect translations that are not macro based ?
        // DONE: (priority: top) link the definition node with the current Part of Speech
        // DONE: (priority: top) type all nodes by prefixing it by language, or #pos or #def.
        // DONE: handle alternative spelling
        // DONE: extract synonyms
        // DONE: extract antonyms
        // DONE: add an IGNOREPOS state to ignore the entire part of speech
        
		gotoNoData(m);
		String pos = null;
		String nym = null;
		while (m.find()) {
			switch (state) {
			case NODATA:
				if (isTranslation(m)) {
					gotoTradBlock(m);
				} else if (null != (pos = getValidPOS(m))) {
					if (pos.length()==0) 
						gotoIgnorePos();
					else
						gotoDefBlock(m, pos);
				} else if (isAlternate(m)) {
					gotoOrthoAltBlock(m);
				} else if (null != (nym = isNymHeader(m))) {
					gotoNymBlock(m, nym);
				} else if (isPronounciation(m)) {
					gotoPronBlock(m);
				} else if (isValidSection(m)) {
					gotoNoData(m);
				} else {
					// unknownHeaders.add(m.group(0));
				}
				break;
			case DEFBLOCK:
				// Iterate until we find a new section
				if (isTranslation(m)) {
					leaveDefBlock(m);
					gotoTradBlock(m);
				} else if (null != (pos = getValidPOS(m))) {
					leaveDefBlock(m);
					if (pos.length()==0) 
						gotoIgnorePos();
					else
						gotoDefBlock(m,pos);
				} else if (isAlternate(m)) {
					leaveDefBlock(m);
					gotoOrthoAltBlock(m);
				} else if (null != (nym = isNymHeader(m))) {
					leaveDefBlock(m);
					gotoNymBlock(m, nym);
				} else if (isPronounciation(m)) {
					leaveDefBlock(m);
					gotoPronBlock(m);
				} else if (isValidSection(m)) {
					leaveDefBlock(m);
					gotoNoData(m);
				} else {
					// leaveDefBlock(m);
					// unknownHeaders.add(m.group(0));
					// gotoNoData(m);
				} 
				break;
			case TRADBLOCK:
				if (isTranslation(m)) {
					leaveTradBlock(m);
					gotoTradBlock(m);
				} else if (null != (pos = getValidPOS(m))) {
					leaveTradBlock(m);
					if (pos.length()==0) 
						gotoIgnorePos();
					else
						gotoDefBlock(m, pos);
				} else if (isAlternate(m)) {
					leaveTradBlock(m);
					gotoOrthoAltBlock(m);
				} else if (null != (nym = isNymHeader(m))) {
					leaveTradBlock(m);
					gotoNymBlock(m, nym);
				} else if (isPronounciation(m)) {
					leaveTradBlock(m);
					gotoPronBlock(m);
				} else if (isValidSection(m)) {
					leaveTradBlock(m);
					gotoNoData(m);
				} else {
					//leaveTradBlock(m);
					// unknownHeaders.add(m.group(0));
					// gotoNoData(m);
				} 
				break;
			case ORTHOALTBLOCK:
				if (isTranslation(m)) {
					leaveOrthoAltBlock(m);
					gotoTradBlock(m);
				} else if (null != (pos = getValidPOS(m))) {
					leaveOrthoAltBlock(m);
					if (pos.length()==0) 
						gotoIgnorePos();
					else
						gotoDefBlock(m, pos);
				} else if (isAlternate(m)) {
					leaveOrthoAltBlock(m);
					gotoOrthoAltBlock(m);
				} else if (null != (nym = isNymHeader(m))) {
					leaveOrthoAltBlock(m);
					gotoNymBlock(m, nym);
				} else if (isPronounciation(m)) {
					leaveOrthoAltBlock(m);
					gotoPronBlock(m);
				} else if (isValidSection(m)) {
					leaveOrthoAltBlock(m);
					gotoNoData(m);
				} else {
					// leaveOrthoAltBlock(m);
					// unknownHeaders.add(m.group(0));
					// gotoNoData(m);
				}
				break;
			case NYMBLOCK:
				if (isTranslation(m)) {
					leaveNymBlock(m);
					gotoTradBlock(m);
				} else if (null != (pos = getValidPOS(m))) {
					leaveNymBlock(m);
					if (pos.length()==0) 
						gotoIgnorePos();
					else 
						gotoDefBlock(m, pos);
				} else if (isAlternate(m)) {
					leaveNymBlock(m);
					gotoOrthoAltBlock(m);
				} else if (null != (nym = isNymHeader(m))) {
					leaveNymBlock(m);
					gotoNymBlock(m, nym);
				} else if (isPronounciation(m)) {
					leaveNymBlock(m);
					gotoPronBlock(m);
				} else if (isValidSection(m)) {
					leaveNymBlock(m);
					gotoNoData(m);
				} else {
					// leaveNymBlock(m);
					// unknownHeaders.add(m.group(0));
					// gotoNoData(m);
				}
				break;
			case PRONBLOCK:
				if (isTranslation(m)) {
					leavePronBlock(m);
					gotoTradBlock(m);
				} else if (null != (pos = getValidPOS(m))) {
					leavePronBlock(m);
					if (pos.length()==0) 
						gotoIgnorePos();
					else
						gotoDefBlock(m, pos);
				} else if (isAlternate(m)) {
					leavePronBlock(m);
					gotoOrthoAltBlock(m);
				} else if (null != (nym = isNymHeader(m))) {
					leavePronBlock(m);
					gotoNymBlock(m, nym);
				} else if (isPronounciation(m)) {
					leavePronBlock(m);
					gotoPronBlock(m);
				} else if (isValidSection(m)) {
					leavePronBlock(m);
					gotoNoData(m);
				} else {
					// leavePronBlock(m);
					// unknownHeaders.add(m.group(0));
					// gotoNoData(m);
				}
				break;
			case IGNOREPOS:
				if (isTranslation(m)) {
				} else if (null != (pos = getValidPOS(m))) {
					if (pos.length()==0) 
						gotoIgnorePos();
					else
						gotoDefBlock(m, pos);
				} else if (isAlternate(m)) {
				} else if (null != (nym = isNymHeader(m))) {
				} else if (isPronounciation(m)) {
					// gotoPronBlock(m);
				} else if (isValidSection(m)) {
					// gotoIgnorePos();
				} else {
					// unknownHeaders.add(m.group(0));
				}
				break;
			default:
				assert false : "Unexpected state while extracting translations from dictionary.";
			} 
		}
		// Finalize the entry parsing
		switch (state) {
		case NODATA:
			break;
		case DEFBLOCK:
			leaveDefBlock(m);
			break;
		case TRADBLOCK:
			leaveTradBlock(m);
			break;
		case ORTHOALTBLOCK:
			leaveOrthoAltBlock(m);
			break;
		case NYMBLOCK:
			leaveNymBlock(m);
			break;
		case PRONBLOCK:
			leavePronBlock(m);
			break;
		case IGNOREPOS:
			break;
		default:
			assert false : "Unexpected state while ending extraction of entry: " + wiktionaryPageName;
		} 
       
        wdh.finalizeEntryExtraction();
    }
    




	protected boolean isValidSection(Matcher m) {
		if (sectionMarkers.contains(m.group(1))) return true;
		if ("S".equals(m.group(1))) return true;
		return false;
	}


	protected void leavePronBlock(Matcher m) {
		// TODO Auto-generated method stub
		
	}


	protected void gotoPronBlock(Matcher m) {
		// TODO Auto-generated method stub
		
	}


	protected boolean isPronounciation(Matcher m) {
		// TODO Auto-generated method stub
		return false;
	}


	protected String getValidPOS(Matcher m) {
		String pos;
		if (null != (pos = posMarkers.get(m.group(1)))) return pos;
		if ("S".equals(m.group(1))) {
			Map<String,String> args = WikiTool.parseArgs(m.group(2));
			String titre = args.get("1");
			String flexion = args.get("3");

			if (null != flexion && "flexion".equals(flexion)) return "";
			if (null != titre && null != (pos = posMarkers.get(titre))) return pos;
			if (null != titre && ignorablePosMarkers.contains(titre)) return "";
			
		}
		return null;
	}


	protected boolean isTranslation(Matcher m) {
		if (m.group(1).equals("-trad-")) return true;
		if (m.group(1).equals("S")) {
			Map<String,String> args = WikiTool.parseArgs(m.group(2));
			String titre = args.get("1");
			if (null != titre && titre.startsWith("trad")) return true;
		}
		return false;
	}

	private static Set<String> variantSections = new HashSet<String>();
	static {
		variantSections.add("variantes");
		variantSections.add("var");
		variantSections.add("variantes ortho");
		variantSections.add("var-ortho");
		variantSections.add("variantes orthographiques");
		variantSections.add("variantes dialectales");
		variantSections.add("dial");
		variantSections.add("var-dial");
		variantSections.add("variantes dial");
		variantSections.add("variantes dialectes");
		variantSections.add("dialectes");
		variantSections.add("anciennes orthographes");
		variantSections.add("ortho-arch");
		variantSections.add("anciennes ortho");	
	}
	
	protected boolean isAlternate(Matcher m) {
		if (m.group(1).equals("-ortho-alt-") || m.group(1).equals("-var-ortho-")) return true;
		if (m.group(1).equals("S")) {
			Map<String,String> args = WikiTool.parseArgs(m.group(2));
			String titre = args.get("1");
			if (null != titre && 
					variantSections.contains(titre)) return true;
		}
		return false;
	}
	


    protected String isNymHeader(Matcher m) {
		if (nymMarkerToNymName.containsKey(m.group(1))) return nymMarkerToNymName.get(m.group(1));
		if (m.group(1).equals("S")) {
			Map<String,String> args = WikiTool.parseArgs(m.group(2));
			String titre = args.get("1");
			if (null != titre && 
					nymMarkerToNymName.containsKey(titre)) return nymMarkerToNymName.get(titre);
		}
		return null;
	}

    
	private void extractTranslations(int startOffset, int endOffset) {
    	Matcher macroMatcher = WikiPatterns.macroPattern.matcher(pageContent);
    	macroMatcher.region(startOffset, endOffset);
    	String currentGlose = null;

    	while (macroMatcher.find()) {
    		String g1 = macroMatcher.group(1);

    		if (g1.equals("trad+") || g1.equals("trad-") || g1.equals("trad") || g1.equals("t+") || g1.equals("t-") || g1.equals("trad--")) {
    			// DONE: Sometimes translation links have a remaining info after the word, keep it.
    			String g2 = macroMatcher.group(2);
    			int i1, i2;
    			String lang, word;
    			if (g2 != null && (i1 = g2.indexOf('|')) != -1) {
    				lang = g2.substring(0, i1);
    				// normalize language code
    				String normLangCode;
    				if ((normLangCode = LangTools.getCode(lang)) != null) {
    					lang = normLangCode;
    				} 
    				String usage = null;
    				if ((i2 = g2.indexOf('|', i1+1)) == -1) {
    					word = g2.substring(i1+1);
    				} else {
    					word = g2.substring(i1+1, i2);
    					usage = g2.substring(i2+1);
    				}
    				lang=FrenchLangtoCode.threeLettersCode(lang);
                    if(lang!=null){
                  	    wdh.registerTranslation(lang, currentGlose, usage, word);
                    }
    			}
    		} else if (g1.equals("boîte début") || g1.equals("trad-début") || g1.equals("(")) {
    			// Get the glose that should help disambiguate the source acception
    			String g2 = macroMatcher.group(2);
    			// Ignore glose if it is a macro
    			if (g2 != null && ! g2.startsWith("{{")) {
    				currentGlose = g2;
    			}
    		} else if (g1.equals("-")) {
    			// just ignore it
    		} else if (g1.equals("trad-fin") || g1.equals(")")) {
    			// Forget the current glose
    			currentGlose = null;
    		} else if (g1.equals("T")) {
    			// this a a language identifier, just ignore it as we get the language id from the trad macro parameter.

    		}
    	}
    }
    
    private void extractPronunciation(int startOffset, int endOffset) {
		Matcher pronMatcher = pronunciationPattern.matcher(pageContent);
		pronMatcher.region(startOffset, endOffset);

		while (pronMatcher.find()) {
    		String pron = pronMatcher.group(1);
    		String lang = pronMatcher.group(2);
    		
    		if (null == pron || pron.equals("")) return;
    		if (lang == null || lang.equals("")) return;
    		
    		if (pron.startsWith("1=")) pron = pron.substring(2);
    		if (lang.startsWith("|2=")) lang = lang.substring(2);
    		if (lang.startsWith("|lang=")) lang = lang.substring(5);

    		if (! pron.equals("")) wdh.registerPronunciation(pron, "fr-fonipa");
    		
		}
    }

    @Override
	public void extractExample(String example) {
        Map<Property, String> context = new HashMap<Property, String>();

        String ex = exampleExpander.expandExample(example, defTemplates, context);
		Resource exampleNode = null;
        if (ex != null && ! ex.equals("")) {
        	exampleNode = wdh.registerExample(ex, context);
        }
    }

}

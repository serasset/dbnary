package org.getalp.blexisma.wiktionary;

import java.util.HashSet;
import java.util.regex.Matcher;

import org.getalp.blexisma.api.SemanticNetwork;

public class FrenchMacroExtractor extends FrenchWiktionaryExtractor {

    public FrenchMacroExtractor(WiktionaryIndex wi) {
        super(wi);
    }
    
    
    private HashSet<String> macros = new HashSet<String>();
   
    public void extractMacros(String wiktionaryPageName) {
        String pageContent = wiktionaryIndex.getTextOfPage(wiktionaryPageName);
        
        if (pageContent == null) return;
        
        // System.out.println(pageContent);
        Matcher languageFilter = languageSectionPattern.matcher(pageContent);
        while (languageFilter.find() && ! languageFilter.group(1).equals("fr")) {
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
        
        extractAllMacros(wiktionaryPageName, pageContent, frenchSectionStartOffset, frenchSectionEndOffset);
     }

    public void extractAllMacros(String wiktionaryPageName, String pageContent, int startOffset, int endOffset) {
        Matcher macroMatcher = macroPattern.matcher(pageContent);
        macroMatcher.region(startOffset, endOffset);
        while (macroMatcher.find()) {
            String def = macroMatcher.group(1);
            macros.add(def);
        }      
    }   
    
    /**
     * @param args
     * @throws WiktionaryIndexerException 
     */
    public static void main(String[] args) throws WiktionaryIndexerException {
        long startTime = System.currentTimeMillis();
        WiktionaryIndex wi = new WiktionaryIndex(args[0]);
        long endloadTime = System.currentTimeMillis();
        System.out.println("Loaded index in " + (endloadTime - startTime) +"ms.");
         int nbpages = 0;
        FrenchMacroExtractor fwe = new FrenchMacroExtractor(wi);
        
        for (String page : wi.keySet()) {
            // System.out.println("Extracting: " + page);
            fwe.extractMacros(page); 
            nbpages ++;
            if (nbpages == 100000) break;
        }
        System.out.println(fwe.macros);
    }

}

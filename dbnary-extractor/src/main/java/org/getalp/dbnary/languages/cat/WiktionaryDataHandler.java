package org.getalp.dbnary.languages.cat;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.getalp.dbnary.ExtractionFeature;
import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.OntolexOnt;
import org.getalp.dbnary.languages.OntolexBasedRDFDataHandler;
import org.getalp.model.ontolex.LexicalForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class WiktionaryDataHandler extends OntolexBasedRDFDataHandler {

    private static final Logger log = LoggerFactory.getLogger(WiktionaryDataHandler.class);

    private static final HashMap<String, String> nymRelation = new HashMap<>();

    static {
        nymRelation.put("sin", "syn");
        nymRelation.put("ant", "ant");
        nymRelation.put("hipo", "hypo");
        nymRelation.put("hiper", "hyper");
        nymRelation.put("mero", "mero");
        nymRelation.put("holo", "holo");

        nymRelation.put("Antònims", "ant");
        nymRelation.put("Sinònims", "syn");
        nymRelation.put("Hipònims", "hypo");
        nymRelation.put("Hiperònims", "hyper");
        nymRelation.put("Parònims", "qsyn");

        posAndTypeValueMap.put("Nom", new PosAndType(LexinfoOnt.noun, OntolexOnt.Word));
        posAndTypeValueMap.put("nom", new PosAndType(LexinfoOnt.noun, OntolexOnt.Word));
        posAndTypeValueMap.put("Verb", new PosAndType(LexinfoOnt.verb, OntolexOnt.Word));
        posAndTypeValueMap.put("verb", new PosAndType(LexinfoOnt.verb, OntolexOnt.Word));
        posAndTypeValueMap.put("Adjectiu", new PosAndType(LexinfoOnt.adjective, OntolexOnt.Word));
        posAndTypeValueMap.put("Adjetiu", new PosAndType(LexinfoOnt.adjective, OntolexOnt.Word));
        posAndTypeValueMap.put("Sigles", new PosAndType(LexinfoOnt.acronym, OntolexOnt.Word));
        posAndTypeValueMap.put("Adverbi", new PosAndType(LexinfoOnt.adverb, OntolexOnt.Word));
        posAndTypeValueMap.put("Nom propi", new PosAndType(LexinfoOnt.properNoun, OntolexOnt.Word));
        posAndTypeValueMap.put("Numeral", new PosAndType(LexinfoOnt.number, OntolexOnt.Word));
        posAndTypeValueMap.put("Conjunció", new PosAndType(LexinfoOnt.conjunction, OntolexOnt.Word));
        posAndTypeValueMap.put("Abreviatura", new PosAndType(LexinfoOnt.abbreviation, OntolexOnt.Word));
        posAndTypeValueMap.put("Lletra", new PosAndType(LexinfoOnt.letter, OntolexOnt.Word));
        posAndTypeValueMap.put("Preposició", new PosAndType(LexinfoOnt.preposition, OntolexOnt.Word));
        posAndTypeValueMap.put("Interjecció", new PosAndType(LexinfoOnt.Interjection, OntolexOnt.Word));
        posAndTypeValueMap.put("Pronom", new PosAndType(LexinfoOnt.Pronoun, OntolexOnt.Word));
        posAndTypeValueMap.put("Article", new PosAndType(LexinfoOnt.Article, OntolexOnt.Word));
        posAndTypeValueMap.put("Contracció", new PosAndType(LexinfoOnt.contraction, OntolexOnt.Word));
        posAndTypeValueMap.put("Prefix", new PosAndType(LexinfoOnt.Prefix, OntolexOnt.Affix));
        posAndTypeValueMap.put("Sufix", new PosAndType(LexinfoOnt.Suffix, OntolexOnt.Affix));
        posAndTypeValueMap.put("Infix", new PosAndType(LexinfoOnt.Infix, OntolexOnt.Affix));
        posAndTypeValueMap.put("Símbol", new PosAndType(LexinfoOnt.Symbol, LexinfoOnt.Symbol));
        posAndTypeValueMap.put("Forma verbal", new PosAndType(LexinfoOnt.verb, OntolexOnt.Word));
        posAndTypeValueMap.put("Frase feta", new PosAndType(LexinfoOnt.idiom, OntolexOnt.MultiWordExpression));
        posAndTypeValueMap.put("Desinència", new PosAndType(LexinfoOnt.suffix, OntolexOnt.Affix));

    }


    public WiktionaryDataHandler(String longEditionLanguageCode, String tdbDir) {
        super(longEditionLanguageCode, tdbDir);
    }

    @Override
    public void registerNymRelation(String target, String synRelation) {
        super.registerNymRelation(target, nymRelation.get(synRelation));
    }

    @Override
    public void initializeLexicalEntry(String headingTitle) {
        String pos = parseSectionName(headingTitle);

        if (posAndTypeValueMap.get(pos) == null)
            log.warn("UNHANDLED LEXICAL TYPE : " + pos + " name -> " + this.currentPage.getName());

        PosAndType pat = posAndTypeValueMap.get(pos);
        Resource typeR = typeResource(pat);
        initializeLexicalEntry(pos, posResource(pat), typeR);
    }

    public void addLexicalForm(LexicalForm form) {
        Model morphoBox = this.getFeatureBox(ExtractionFeature.MORPHOLOGY);

        if (null == morphoBox)
            return;

        form.attachTo(currentLexEntry.inModel(morphoBox));
    }

    public String parseSectionName(final String str) {
        int level = 0;
        while (str.charAt(level) == '=')
            level++;
        return str.substring(level, str.length() - level).trim();
    }
}

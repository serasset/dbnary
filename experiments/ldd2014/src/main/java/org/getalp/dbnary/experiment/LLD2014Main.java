package org.getalp.dbnary.experiment;

import org.getalp.dbnary.experiment.disambiguation.Ambiguity;
import org.getalp.dbnary.experiment.disambiguation.Disambiguable;
import org.getalp.dbnary.experiment.disambiguation.Disambiguator;
import org.getalp.dbnary.experiment.disambiguation.translations.DisambiguableSense;
import org.getalp.dbnary.experiment.disambiguation.translations.TranslationAmbiguity;
import org.getalp.dbnary.experiment.disambiguation.translations.TranslationDisambiguator;
import org.getalp.lexsema.lexicalresource.lemon.LexicalEntry;
import org.getalp.lexsema.lexicalresource.lemon.LexicalSense;
import org.getalp.lexsema.lexicalresource.lemon.dbnary.DBNary;
import org.getalp.lexsema.lexicalresource.lemon.dbnary.Translation;
import org.getalp.lexsema.lexicalresource.lemon.dbnary.Vocable;
import org.getalp.lexsema.lexicalresource.lemon.dbnary.relations.DBNaryRelationType;
import org.getalp.lexsema.ontology.OWLTBoxModel;
import org.getalp.lexsema.ontology.OntologyModel;
import org.getalp.lexsema.ontology.graph.Relation;
import org.getalp.lexsema.ontology.graph.RelationIface;
import org.getalp.lexsema.ontology.storage.JenaMemoryStore;
import org.getalp.lexsema.ontology.storage.Store;
import org.getalp.lexsema.ontology.storage.StoreHandler;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class LLD2014Main {

    private LLD2014Main() {
    }

    public static void main(String[] args) throws IOException {

        //Store vts = new VirtuosoTripleStore("jdbc:virtuoso://kopi.imag.fr:1982", "dba", "dba");
        System.err.println("Loading Jena aBox model from file " + args[0] + " ...");
        Store vts = new JenaMemoryStore(args[0]);
        StoreHandler.registerStoreInstance(vts);
        System.err.println("Loading Jena tBox model ...");
        OntologyModel tBox = new OWLTBoxModel();

        DBNary lr = new DBNary(tBox, Locale.FRENCH);
        Disambiguator disamb = new TranslationDisambiguator();
        List<Ambiguity> results = new ArrayList<>();
        System.err.println("Fetching Vocables ...");
        List<Vocable> vocables = lr.getVocables();
        int currentVocable = 0;
        int progress = 0;
        int previousProgress = -1;
        int disambiguated = 0;
        System.err.println("Processing start ...");
        for (Vocable v : vocables) {
            List<LexicalEntry> entries = v.getLexicalEntries();
            for (LexicalEntry le : entries) {
                List<Disambiguable> choices = new ArrayList<>();
                List<LexicalSense> senses = le.getSenses();
                for (LexicalSense ls : senses) {
                    choices.add(new DisambiguableSense(ls));
                }
                List<RelationIface> rels = Relation.findRelationsForTarget(le, DBNaryRelationType.isTranslationOf);
                for (RelationIface rel : rels) {
                    Translation t = new Translation(lr, rel.getStart(), le);
                    String gloss = t.getGloss();
                    Integer tnum = t.getTranslationNumber();
                    if (gloss != null) {
                        Ambiguity ambiguity = new TranslationAmbiguity(gloss, String.valueOf(tnum));
                        disamb.disambiguate(ambiguity, choices);
                        results.add(ambiguity);
                        disambiguated++;
                    }
                }
            }
            progress = (int) (((double) currentVocable / (double) vocables.size()) * 100.0);
            System.err.println("\r Processing Vocables" + "[" + ((double) currentVocable / (double) vocables.size()) * 100 + "%] -- D: " + disambiguated);
            if (progress > previousProgress) {
                System.err.println(" \r Processing Vocables" + "[" + progress + "%] -- D: " + disambiguated);
                previousProgress = progress;
            }
            currentVocable++;
        }

        Collections.sort(results);
        FileOutputStream fos = new FileOutputStream("french_results.res");
        PrintStream ps = new PrintStream(fos, true);
        int current = 0;
        for (Ambiguity a : results) {
            System.err.println("Writing " + ((double) current / (double) results.size()) * 100.0 + "%");
            ps.println(a);
            current++;
        }
        ps.println();
        ps.flush();
        ps.close();
    }
}
    /* select distinct count(?a),?a,?s where {?a ?r ?s. FILTER (regex(?r, "^.*nym$"))}
         */

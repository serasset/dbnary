package org.getalp.dbnary.experiment;

import org.getalp.dbnary.experiment.disambiguation.Ambiguity;
import org.getalp.dbnary.experiment.disambiguation.Disambiguable;
import org.getalp.dbnary.experiment.disambiguation.Disambiguator;
import org.getalp.dbnary.experiment.disambiguation.translations.DisambiguableSense;
import org.getalp.dbnary.experiment.disambiguation.translations.TranslationAmbiguity;
import org.getalp.dbnary.experiment.disambiguation.translations.TranslationDisambiguator;
import org.getalp.dbnary.experiment.similarity.SimilarityMeasure;
import org.getalp.dbnary.experiment.similarity.TsverskiIndex;
import org.getalp.lexsema.ontology.OntologyModel;
import org.getalp.lexsema.ontology.storage.Store;
import org.getalp.lexsema.ontology.storage.StoreHandler;
import org.getalp.lexsema.ontology.storage.VirtuosoTripleStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class LLD2014Main {

    private LLD2014Main() {
    }

    public static void main(String[] args) throws IOException {

        SimilarityMeasure s = new TsverskiIndex(0.5, 0.5, true);
        String a = "eternal sun's shine gives you a wonderful smile";
        String b = "eternally shining sunny giving wonder smiling";
        System.err.println(s.compute(a, b));


        //Store vts = new VirtuosoTripleStore("jdbc:virtuoso://kopi:1111", "dba", "dba");
        Store vts = new VirtuosoTripleStore("jdbc:virtuoso://kopi:1982", "dba", "dba");
        StoreHandler.registerStoreInstance(vts);

        OntologyModel otm = vts.getModel();

        // Creating DBnary wrapper
        /*DBNary lr = new DBNary(otm, Locale.ENGLISH);

        List<Vocable> vocables = lr.getVocables();

        for (Vocable v : vocables) {
            List<LexicalEntry> entries = v.getLexicalEntries();
            System.err.println(v);
            for (LexicalEntry le : entries) {
                String lemma = le.getLemma();
                String PoS = le.getPartOfSpeech();
                int number = le.getNumber();
                List<LexicalSense> senses = le.getSenses();
                for(LexicalSense ls: senses){
                    System.out.println(ls);
                }
            }
        }*/

        //Query q =
        //vts.runQuery();

            /*System.out.println(v);
            List<LexicalEntry> les = v.getLexicalEntries();
            for (LexicalEntry le : les) {
                List<LexicalSense> senses;
                System.err.println(le);
            }*/

        /*
            select distinct count(?a),?a,?s where {?a ?r ?s. FILTER (regex(?r, "^.*nym$"))}
         */

        Ambiguity ambi = new TranslationAmbiguity("insect", "__tr_alt_1_butterfly__Noun__1");

        List<Disambiguable> choices = new ArrayList<>();
        choices.add(new DisambiguableSense("A flying insect of the order Lepidoptera, distinguished from moths by their diurnal activity and generally brighter colouring.", "dbnary-eng:__ws_1_butterfly__Noun__1"));
        choices.add(new DisambiguableSense("Someone seen as being unserious and (originally) dressed gaudily; someone flighty and unreliable.", "dbnary-eng:__ws_2_butterfly__Noun__1"));
        choices.add(new DisambiguableSense("The butterfly stroke.", "dbnary-eng:__ws_3_butterfly__Noun__1"));
        choices.add(new DisambiguableSense("A use of surgical tape, cut into thin strips and placed across an open wound to hold it closed.", "dbnary-eng:__ws_4_butterfly__Noun__1"));

        Disambiguator disamb = new TranslationDisambiguator();
        disamb.disambiguate(ambi, choices);

        System.err.println(ambi);


    }
}

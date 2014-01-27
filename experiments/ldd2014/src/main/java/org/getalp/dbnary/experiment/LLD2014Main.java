package org.getalp.dbnary.experiment;

import org.getalp.dbnary.experiment.similarity.SimilarityMeasure;
import org.getalp.dbnary.experiment.similarity.TsverskiIndex;
import org.getalp.lexsema.lexicalresource.lemon.LexicalEntry;
import org.getalp.lexsema.lexicalresource.lemon.dbnary.DBNary;
import org.getalp.lexsema.lexicalresource.lemon.dbnary.Vocable;
import org.getalp.lexsema.ontology.OntologyModel;
import org.getalp.lexsema.ontology.storage.Store;
import org.getalp.lexsema.ontology.storage.StoreHandler;
import org.getalp.lexsema.ontology.storage.VirtuosoTripleStore;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LLD2014Main {

    public static void main(String[] args) throws IOException {

        SimilarityMeasure s = new TsverskiIndex(0.5, 0.5, true);
        String a = "eternal sun's shine gives you a wonderful smile";
        String b = "eternally shining sunny giving wonder smiling";
        System.err.println(s.compute(a, b));


        Store vts = new VirtuosoTripleStore("jdbc:virtuoso://kopi:1111", "dba", "dba");
        StoreHandler.registerStoreInstance(vts);

        OntologyModel otm = vts.getModel();

        // Creating DBnary wrapper
        DBNary lr = new DBNary(otm, Locale.ENGLISH);

        List<Vocable> vocables = lr.getVocables();

        for (Vocable v : vocables) {
            List<LexicalEntry> entries = v.getLexicalEntries();
            for (LexicalEntry le : entries) {
                String lemma = le.getLemma();
                String PoS = le.getPartOfSpeech();
                int number = le.getNumber();
                System.err.println(lemma + "|" + PoS + "|" + number);
            }
        }


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
    }
}

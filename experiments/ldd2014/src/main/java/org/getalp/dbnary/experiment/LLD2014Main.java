package org.getalp.dbnary.experiment;

import com.hp.hpl.jena.rdf.model.*;
import org.getalp.dbnary.DbnaryModel;
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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class LLD2014Main {

    private Property senseNumProperty;
    private Property transNumProperty;
    private String NS;

    private LLD2014Main() {
    }

    public static void main(String[] args) throws IOException {

        //Store vts = new VirtuosoTripleStore("jdbc:virtuoso://kopi.imag.fr:1982", "dba", "dba");
        System.err.println("Loading Jena aBox model from file " + args[0] + " ...");
        Store vts = new JenaMemoryStore(args[0]);
        StoreHandler.registerStoreInstance(vts);

        int nrunners = Integer.valueOf(args[1]);

        System.err.println("Loading Jena tBox model ...");
        OntologyModel tBox = new OWLTBoxModel();

        DBNary lr = new DBNary(tBox, Locale.FRENCH);

        LLD2014Main lld = new LLD2014Main();

        List<Vocable> vocables = lr.getVocables();
       /* List<Thread> threads = new ArrayList<>();
        List<SubsetVocableProcessor> runners = new ArrayList<>();

        for(int i=0;i<nrunners;i++){
            int sliceWidth = vocables.size()/nrunners;
            if(i==nrunners-1){
                runners.add(new SubsetVocableProcessor(lr,i*sliceWidth,(i+1)*sliceWidth -1 +vocables.size()%nrunners,vocables));
            } else {
                runners.add(new SubsetVocableProcessor(lr,i*sliceWidth,(i+1)*sliceWidth-1,vocables));
            }
        }

        for(SubsetVocableProcessor runner: runners){
            Thread t = new Thread(runner);
            threads.add(t);
            t.start();
        }
        boolean done=false;
        while(!done){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            int vocablesProcessed = 0;
            int disambiguated = 0;
            int i=0;
            for(SubsetVocableProcessor r: runners){
                vocablesProcessed+=r.getCurrentVocable();
                disambiguated+=r.getDisambiguated();
                if(r.isTerminated()){
                    i++;
                }
            }
            if(i==runners.size()){
                done =true;
            }
            System.err .println("Processing Vocables" + "[" + (((double)vocablesProcessed/(double)vocables.size())*100.0) + "%] -- D: " + disambiguated+"                                             \r");
        }

        FileOutputStream fos = new FileOutputStream("french_results.res");
        PrintStream ps = new PrintStream(fos, true);

        for(SubsetVocableProcessor r: runners){
            for(Ambiguity a : r.getResults()){
                ps.println(a);
            }
            ps.println();
            fos.flush();
            ps.close();
        }*/

        lld.processTranslations();

    }

    public void processWithLexsema(DBNary lr) throws FileNotFoundException {
        Disambiguator disamb = new TranslationDisambiguator();
        List<Ambiguity> results = new ArrayList<>();
        System.err.println("Fetching Vocables ...");
        List<Vocable> vocables = lr.getVocables();
        int currentVocable = 0;
        int progress = 0;
        int previousProgress = -1;
        int disambiguated = 0;
        FileOutputStream fos = new FileOutputStream("french_results.res");
        PrintStream ps = new PrintStream(fos, true);
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

    private void initializeTBox(String lang) {
        NS = DbnaryModel.DBNARY_NS_PREFIX + "/" + lang + "/";
        senseNumProperty = DbnaryModel.tBox.getProperty(DbnaryModel.DBNARY + "translationSenseNumber");
        transNumProperty = DbnaryModel.tBox.getProperty(DbnaryModel.DBNARY + "translationNumber");
    }

    private void processTranslations() throws FileNotFoundException {
        initializeTBox("fra");
        // Iterate over all translations
        Store store = StoreHandler.getStore();
        Model m1 = store.getABox();
        List<String> gsEntries = new ArrayList<>();

        Disambiguator disamb = new TranslationDisambiguator();

        StmtIterator translations = m1.listStatements((Resource) null, DbnaryModel.isTranslationOf, (RDFNode) null);
        FileOutputStream fos = new FileOutputStream("french_results.res");
        PrintStream ps = new PrintStream(fos, true);

        while (translations.hasNext()) {
            Statement next = translations.next();

            Resource e = next.getSubject();

            Statement n = e.getProperty(transNumProperty);
            Statement s = e.getProperty(senseNumProperty);
            Statement g = e.getProperty(DbnaryModel.glossProperty);

            if (null != s && null != n && null != g) {
                Ambiguity ambiguity = new TranslationAmbiguity(g.getObject().toString(), n.getObject().toString().split("\\^\\^")[0]);
                Resource lexicalEntry = next.getObject().asResource();
                List<String> senseIds = new ArrayList<>();
                StmtIterator senses = m1.listStatements(lexicalEntry, DbnaryModel.lemonSenseProperty, (RDFNode) null);
                List<Disambiguable> choices = new ArrayList<>();
                while (senses.hasNext()) {
                    Statement nextSense = senses.next();
                    String sstr = nextSense.getObject().toString();
                    Statement dRef = nextSense.getProperty(DbnaryModel.lemonDefinitionProperty);
                    Statement dVal = dRef.getProperty(DbnaryModel.lemonValueProperty);
                    String deftext = dVal.getObject().toString();
                    choices.add(new DisambiguableSense(deftext, sstr));
                }
                disamb.disambiguate(ambiguity, choices);
                ps.println(ambiguity);
                System.out.println(ambiguity);
            }

            //System.out.println(n.getObject().toString().split("\\^\\^")[0] + " 0 " + senseIds.get(num) + " " + rank);
        }
        ps.println();
        ps.flush();
        ps.close();
    }
}


    /* select distinct count(?a),?a,?s where {?a ?r ?s. FILTER (regex(?r, "^.*nym$"))}
         */

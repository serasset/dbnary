package org.getalp.dbnary.experiment;

import com.hp.hpl.jena.rdf.model.*;
import com.wcohen.ss.Level2JaroWinkler;
import com.wcohen.ss.MongeElkan;
import com.wcohen.ss.ScaledLevenstein;
import org.getalp.dbnary.DbnaryModel;
import org.getalp.dbnary.experiment.disambiguation.Ambiguity;
import org.getalp.dbnary.experiment.disambiguation.Disambiguable;
import org.getalp.dbnary.experiment.disambiguation.Disambiguator;
import org.getalp.dbnary.experiment.disambiguation.translations.DisambiguableSense;
import org.getalp.dbnary.experiment.disambiguation.translations.MFSTranslationDisambiguator;
import org.getalp.dbnary.experiment.disambiguation.translations.TranslationAmbiguity;
import org.getalp.dbnary.experiment.disambiguation.translations.TranslationDisambiguator;
import org.getalp.dbnary.experiment.fuzzystring.JaroWinklerUnicode;
import org.getalp.dbnary.experiment.similarity.TsverskiIndex;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class LLD2014Main {

    private Property senseNumProperty;
    private Property transNumProperty;

    private LLD2014Main() {
    }

    public static void main(String[] args) throws IOException {

        //Store vts = new VirtuosoTripleStore("jdbc:virtuoso://kopi.imag.fr:1982", "dba", "dba");
        System.err.println("Loading Jena aBox model from file " + args[0] + " ...");

        Model model = ModelFactory.createOntologyModel();
        model.read(args[0]);
        //Store vts = new JenaMemoryStore(args[0]);
        //StoreHandler.registerStoreInstance(vts);
        LLD2014Main lld = new LLD2014Main();

        lld.processTranslations(model);
    }


    private void initializeTBox(String lang) {
        senseNumProperty = DbnaryModel.tBox.getProperty(DbnaryModel.DBNARY + "translationSenseNumber");
        transNumProperty = DbnaryModel.tBox.getProperty(DbnaryModel.DBNARY + "translationNumber");
    }

    private void processTranslations(Model m1) throws FileNotFoundException {
        initializeTBox("fra");

        MFSTranslationDisambiguator mfs = new MFSTranslationDisambiguator();
        Disambiguator disamb = new TranslationDisambiguator();
        FileOutputStream mfsfos = new FileOutputStream("french_results_MFS.res");
        PrintStream psmfs = new PrintStream(mfsfos, true);

        FileOutputStream votefos = new FileOutputStream("french_results_Vote.res");
        PrintStream psvote = new PrintStream(votefos, true);
        for (int i = 0; i < 11; i++) {
            double w1, w2;
            w1 = (double) i / 10.0;
            w2 = 1 - w1;
            String mstr = String.format("_%f_%f", w1, w2);

            disamb.registerSimilarity("FTiJW" + mstr, new TsverskiIndex(w2, w1, true, new JaroWinklerUnicode()));
            disamb.registerSimilarity("FTiLs" + mstr, new TsverskiIndex(w2, w1, true, new ScaledLevenstein()));
            //disamb.registerSimilarity("FTiJMjs"+mstr, new TsverskiIndex(w2, w1, true, new JelinekMercerJS()));
            //disamb.registerSimilarity("FTiDjs"+mstr, new TsverskiIndex(w2, w1, true, new DirichletJS()));
            disamb.registerSimilarity("FTiME" + mstr, new TsverskiIndex(w2, w1, true, new MongeElkan()));
            disamb.registerSimilarity("FTiJW2" + mstr, new TsverskiIndex(w2, w1, true, new Level2JaroWinkler()));
            disamb.registerSimilarity("FTiLcss" + mstr, new TsverskiIndex(w2, w1, true));
        }


        Map<String, PrintStream> streams = new HashMap<>();
        for (String m : disamb.getMethods()) {
            FileOutputStream fos = new FileOutputStream(String.format("french_results_%s.res", m));
            streams.put(m, new PrintStream(fos, true));
        }


        StmtIterator translations = m1.listStatements(null, DbnaryModel.isTranslationOf, (RDFNode) null);


        while (translations.hasNext()) {
            Statement next = translations.next();

            Resource e = next.getSubject();

            Statement n = e.getProperty(transNumProperty);
            Statement s = e.getProperty(senseNumProperty);
            Statement g = e.getProperty(DbnaryModel.glossProperty);

            if (null != s && null != n && null != g) {
                String url = g.getObject().toString();
                Ambiguity ambiguity = new TranslationAmbiguity(url, n.getObject().toString().split("\\^\\^")[0]);
                Ambiguity ambiguity2 = new TranslationAmbiguity(url, n.getObject().toString().split("\\^\\^")[0]);
                Resource lexicalEntry = next.getObject().asResource();
                StmtIterator senses = m1.listStatements(lexicalEntry, DbnaryModel.lemonSenseProperty, (RDFNode) null);
                List<Disambiguable> choices = new ArrayList<>();
                while (senses.hasNext()) {
                    Statement nextSense = senses.next();
                    String sstr = nextSense.getObject().toString();
                    sstr = sstr.substring(sstr.indexOf("__ws_"));
                    Statement dRef = nextSense.getProperty(DbnaryModel.lemonDefinitionProperty);
                    Statement dVal = dRef.getProperty(DbnaryModel.lemonValueProperty);
                    String deftext = dVal.getObject().toString();
                    choices.add(new DisambiguableSense(deftext, sstr));
                }
                disamb.disambiguate(ambiguity, choices);
                mfs.disambiguate(ambiguity2, choices);
                for (String m : ambiguity.getMethods()) {
                    streams.get(m).println(ambiguity.toString(m));
                }
                psmfs.println(ambiguity2.toString("MFS"));
                psvote.println(ambiguity.toStringVote());
            }

            //System.out.println(n.getObject().toString().split("\\^\\^")[0] + " 0 " + senseIds.get(num) + " " + rank);
        }
        System.out.println(mfs);
        psmfs.close();
        psvote.close();
        for (String m : disamb.getMethods()) {
            streams.get(m).close();
        }
    }
}


    /* select distinct count(?a),?a,?s where {?a ?r ?s. FILTER (regex(?r, "^.*nym$"))}
         */

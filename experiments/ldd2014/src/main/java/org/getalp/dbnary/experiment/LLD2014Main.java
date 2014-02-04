package org.getalp.dbnary.experiment;

import com.hp.hpl.jena.rdf.model.*;
import org.getalp.dbnary.DbnaryModel;
import org.getalp.dbnary.experiment.disambiguation.Ambiguity;
import org.getalp.dbnary.experiment.disambiguation.Disambiguable;
import org.getalp.dbnary.experiment.disambiguation.Disambiguator;
import org.getalp.dbnary.experiment.disambiguation.translations.DisambiguableSense;
import org.getalp.dbnary.experiment.disambiguation.translations.TranslationAmbiguity;
import org.getalp.dbnary.experiment.disambiguation.translations.TranslationDisambiguator;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

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

        Disambiguator disamb = new TranslationDisambiguator();

        StmtIterator translations = m1.listStatements(null, DbnaryModel.isTranslationOf, (RDFNode) null);
        FileOutputStream fos = new FileOutputStream("french_results.res");
        PrintStream ps = new PrintStream(fos, true);

        while (translations.hasNext()) {
            Statement next = translations.next();

            Resource e = next.getSubject();

            Statement n = e.getProperty(transNumProperty);
            Statement s = e.getProperty(senseNumProperty);
            Statement g = e.getProperty(DbnaryModel.glossProperty);

            if (null != s && null != n && null != g) {
                String url = g.getObject().toString();
                Ambiguity ambiguity = new TranslationAmbiguity(url, n.getObject().toString().split("\\^\\^")[0]);
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
                ps.println(ambiguity);
                System.out.println(ambiguity);
            }

            //System.out.println(n.getObject().toString().split("\\^\\^")[0] + " 0 " + senseIds.get(num) + " " + rank);
        }
    }
}


    /* select distinct count(?a),?a,?s where {?a ?r ?s. FILTER (regex(?r, "^.*nym$"))}
         */

package org.getalp.jdm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Scanner;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseTDB {

  private static final Logger log = LoggerFactory.getLogger(BaseTDB.class);

  public static void main(String[] args) {

    // INITIALISATION

    Scanner sc = new Scanner(System.in);
    Dataset dataset = TDBFactory.createDataset("C:\\Users\\Edzil\\workspace\\jdm2rdf\\TDB");
    Model tdb = dataset.getDefaultModel();
    dataset.begin(ReadWrite.WRITE);

    Model model = ModelFactory.createDefaultModel();
    System.out.println("modèle vide ? " + tdb.isEmpty());

    // CREATION REQUETE

    UpdateRequest request = UpdateFactory.create();
    request.add("CREATE GRAPH <http://example/g2>")
        .add("prefix jdm_property:  <http://kaiko.getalp.org/jdm#> ")
        .add("prefix jdm_mot:  <http://kaiko.getalp.org/jdm/>")
        .add("prefix lemon: <http://lemon-model.net/lemon#> ")
        .add("INSERT DATA" + " {?nom a lemon:LexicalEntry .} ")
        .add("SELECT ?nom {" + "?nom jdm_property:type 1");// TODO réussir à introduire un triplet
    // ied(variable) a lemon:LexicalEntry ,
    // pour chaque triplet venant de JDM dont
    // le type (jdm_property:type) vaut 1

    // TODO réussir à implanter les modifications dans le Model model pour le sortir dans un fichier
    UpdateAction.execute(request, dataset);

    // FERMETURE
    // TODO erreur si on relance le programme sans répondre : com.hp.hpl.jena.tdb.TDBException:
    // Can't open database at location ... as it is already locked by the process with PID 10736.

    dataset.commit();
    sc.close();

    dataset.end(); // TODO ligne inutile ?
    dataset.close();
    System.out.println("Tout est fermé");
    try {
      RDFDataMgr.write(new FileOutputStream(new File("modifs.ttl"), true), tdb,
          RDFFormat.TURTLE_BLOCKS);

    } catch (FileNotFoundException e) {
      log.error(e.getLocalizedMessage());
    }
    tdb.close();
    System.out.println("FIN");
  }// FIN MAIN
}// FIN CLASSE


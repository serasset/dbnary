package org.getalp.dbnary.cli;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.io.FileUtils;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RiotNotFoundException;
import org.apache.jena.tdb2.TDB2Factory;
import org.apache.jena.util.iterator.ExtendedIterator;

public class RDFDiff extends VerboseCommand {

  private final TreeMap<String, String> anodes2id = new TreeMap<>();
  public static final Resource me = ResourceFactory.createResource("#me");
  public static final Property diffRate =
      ResourceFactory.createProperty("http://kaiko.getalp.org/dbnary/diffs/", "diffRate");

  public RDFDiff(String[] args) {
    this.loadArgs(args);
  }

  @Override
  protected void loadArgs(CommandLine cmd) {
    if (remainingArgs.length < 2 || remainingArgs.length > 3) {
      printUsage();
      System.exit(1);
    }
  }

  @Override
  protected void printUsage() {
    HelpFormatter formatter = new HelpFormatter();
    PrintWriter pw = new PrintWriter(System.err);
    formatter
        .printHelp(pw, formatter.getWidth(),
            "java -cp /path/to/dbnary.jar " + this.getClass().getCanonicalName()
                + " [OPTIONS] from.ttl to.ttl",
            "With OPTIONS in:", options, formatter.getLeftPadding(), formatter.getDescPadding(),
            "Computes the difference between from.ttl and to.ttl. The command will output the "
                + "model resulting from the removal of to.ttl to the model from.ttl in stdout.",
            false);
    pw.flush();
  }

  public static void main(String[] args) throws FileNotFoundException {
    RDFDiff cli = new RDFDiff(args);
    cli.diff();
  }

  public void diff() throws FileNotFoundException {
    String fromFile = remainingArgs[0];
    String toFile = remainingArgs[1];
    String outFile = null;
    if (remainingArgs.length == 3)
      outFile = remainingArgs[2];
    Model fromModel;
    Model toModel;
    Model diffModel;
    Dataset dataset1 = null;
    Dataset dataset2 = null;
    Dataset diffDataset = null;
    boolean useTDB = false;

    if (remainingArgs[0].endsWith(".tdb")) {
      // read the RDF/XML files
      if (verbose)
        System.err.println("Handling first model from TDB database: " + fromFile);
      dataset1 = TDB2Factory.connectDataset(fromFile);
      dataset1.begin(ReadWrite.READ);
      // Get model inside the transaction
      fromModel = dataset1.getDefaultModel();
      useTDB = true;
    } else {
      if (verbose)
        System.err.println("Handling first model from turtle: " + fromFile);
      fromModel = ModelFactory.createDefaultModel();
      try {
        RDFDataMgr.read(fromModel, fromFile);
      } catch (RiotNotFoundException e) {
        // Don't do anything as the model will simply be empty
      }
    }

    if (remainingArgs[1].endsWith(".tdb")) {
      // read the RDF/XML files
      if (verbose)
        System.err.println("Handling second model from TDB database: " + toFile);
      dataset2 = TDB2Factory.connectDataset(toFile);
      dataset2.begin(ReadWrite.READ);
      // Get model inside the transaction
      toModel = dataset2.getDefaultModel();
      useTDB = true;
    } else {
      if (verbose)
        System.err.println("Handling second model from turtle: " + toFile);
      toModel = ModelFactory.createDefaultModel();
      try {
        RDFDataMgr.read(toModel, toFile);
      } catch (RiotNotFoundException e) {
        // Don't do anything as the model will simply be empty
      }
    }

    String tdbDir = null;
    if (useTDB) {
      try {
        Path temp = Files.createTempDirectory("dbnary-diff");
        temp.toFile().deleteOnExit();
        tdbDir = temp.toAbsolutePath().toString();
        if (verbose) {
          System.err.println("Using temp TDB at " + tdbDir);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
          try {
            FileUtils.deleteDirectory(temp.toFile());
          } catch (IOException e) {
            System.err.println("Caught " + e.getClass()
                + " when attempting to delete the temporary TDB directory " + temp);
            System.err.println(e.getLocalizedMessage());
          }
        }));
      } catch (IOException e) {
        System.err.println("Could not create temporary TDB directory. Exiting...");
        System.exit(-1);
      }
    }
    if (null != tdbDir) {
      diffDataset = TDB2Factory.connectDataset(tdbDir);
      diffDataset.begin(ReadWrite.WRITE); // UGLY: using one LONG write transaction
      diffModel = diffDataset.getDefaultModel();
    } else {
      diffModel = ModelFactory.createDefaultModel();
    }

    if (verbose)
      System.err.println("Building bindings.");
    buildBinding(fromModel, toModel);

    if (verbose)
      System.err.println("Computing differences.");

    // merge the Models
    difference(fromModel, toModel, diffModel);

    for (Entry<String, String> e : fromModel.getNsPrefixMap().entrySet()) {
      diffModel.setNsPrefix(e.getKey(), e.getValue());
    }
    if (null != diffDataset)
      diffDataset.commit();

    if (null != dataset1)
      dataset1.end();
    if (null != dataset2)
      dataset2.end();
    if (null != diffDataset)
      diffDataset.end();


    if (null != diffDataset)
      diffDataset.begin(ReadWrite.READ);
    PrintStream out = System.out;
    if (null != outFile) {
      out = new PrintStream(outFile);
    }
    RDFDataMgr.write(out, diffModel, Lang.TURTLE);
    out.flush();
    if (null != diffDataset)
      diffDataset.end();
    if (null != outFile)
      out.close();
  }

  public void buildBinding(Model m1, Model m2) {
    // Creates a binding between equivalent blank nodes in m1 and m2
    indexBlankNodes(m1);
    indexBlankNodes(m2);
  }

  private void indexBlankNodes(Model m) {
    Node s;
    ExtendedIterator<Node> iter = null;
    try {
      iter = GraphUtil.listSubjects(m.getGraph(), Node.ANY, Node.ANY);
      int nbtriple = 0;
      int nbBlank = 0;
      while (iter.hasNext()) {
        s = iter.next();
        nbtriple++;
        if (s.isBlank()) {
          nbBlank++;
          ExtendedIterator<Triple> it = m.getGraph().find(s, Node.ANY, Node.ANY);
          SortedSet<String> signature = new TreeSet<>();
          while (it.hasNext()) {
            Triple t = it.next();
            signature.add(t.getPredicate().toString() + "+" + t.getObject().toString());
          }
          StringBuilder b = new StringBuilder();
          for (String r : signature) {
            b.append(r).append("|");
          }
          String key = b.toString();
          assert anodes2id.get(s.getBlankNodeLabel()) == null;
          anodes2id.put(s.getBlankNodeLabel(), key);
        }
        if (verbose && nbtriple % 1000 == 0) {
          System.err.print("Indexed " + nbBlank + " blank nodes /" + nbtriple + "\r");
        }
      }
      if (verbose)
        System.err.println();
    } finally {
      if (null != iter) {
        iter.close();
      }
    }
  }

  private Model difference(Model from, Model to, Model diff) {
    ExtendedIterator<Triple> iter = null;
    Triple triple;
    int nbprocessed = 0;
    int nbdiffs = 0;
    try {
      iter = GraphUtil.findAll(from.getGraph());
      while (iter.hasNext()) {
        triple = iter.next();
        nbprocessed++;
        if (triple.getSubject().isBlank() && triple.getObject().isBlank()) {
          // We assume this is not the case.
        } else if (triple.getSubject().isBlank()) {
          ExtendedIterator<Node> it = null;
          try {
            it = GraphUtil.listSubjects(to.getGraph(), triple.getPredicate(), triple.getObject());
            if (it.hasNext()) {
              Node ec = it.next();
              while (it.hasNext() && !bound(triple.getSubject(), ec)) {
                ec = it.next();
              }
              if (!bound(triple.getSubject(), ec)) {
                diff.getGraph().add(triple);
                nbdiffs++;
              }
            } else {
              diff.getGraph().add(triple);
              nbdiffs++;
            }
          } finally {
            if (null != it) {
              it.close();
            }
          }

        } else if (triple.getObject().isBlank()) {
          ExtendedIterator<Node> it = null;
          try {
            it = GraphUtil.listObjects(to.getGraph(), triple.getSubject(), triple.getPredicate());
            if (it.hasNext()) {
              Node ec = it.next();
              while (it.hasNext() && !bound(triple.getObject(), ec)) {
                ec = it.next();
              }
              if (!bound(triple.getObject(), ec)) {
                diff.getGraph().add(triple);
                nbdiffs++;
              }
            } else {
              diff.getGraph().add(triple);
              nbdiffs++;
            }
          } finally {
            if (null != it) {
              it.close();
            }
          }

        } else if (!to.getGraph().contains(triple)) {
          diff.getGraph().add(triple);
          nbdiffs++;
        }
        if (verbose && nbprocessed % 1000 == 0)
          System.err.print("" + nbdiffs + "/" + nbprocessed / 1000 + " k\r");
      }
      if (verbose)
        System.err.print("" + nbdiffs + "/" + nbprocessed + "\n");
    } finally {
      iter.close();
    }

    // Add statistics about the diff
    diff.add(diff.createStatement(me, diffRate,
        diff.createTypedLiteral(nbdiffs / (double) nbprocessed)));

    return diff;
  }


  private boolean bound(Node n1, Node n2) {
    if (n2.isBlank()) {
      String k1 = anodes2id.get(n1.getBlankNodeLabel());
      String k2 = anodes2id.get(n2.getBlankNodeLabel());
      return Objects.equals(k1, k2);
    }
    return false;
  }

}

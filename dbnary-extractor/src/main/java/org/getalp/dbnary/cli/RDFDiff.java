package org.getalp.dbnary.cli;

import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.util.iterator.ExtendedIterator;

public class RDFDiff {

  private static boolean verbose = true;

  public static void main(String args[]) {
    Model m1, m2;
    Dataset dataset1 = null, dataset2 = null;

    if (args.length != 2) {
      usage();
      System.exit(1);
    }

    if (args[0].endsWith(".tdb")) {
      // read the RDF/XML files
      System.err.println("Handling first model from TDB database: " + args[0]);
      dataset1 = TDBFactory.createDataset(args[0]);
      dataset1.begin(ReadWrite.READ);
      // Get model inside the transaction
      m1 = dataset1.getDefaultModel();
    } else {
      System.err.println("Handling first model from turtle: " + args[0]);
      m1 = ModelFactory.createDefaultModel();
      m1.read(args[0], "TURTLE");
    }

    if (args[1].endsWith(".tdb")) {
      // read the RDF/XML files
      System.err.println("Handling second model from TDB database: " + args[1]);
      dataset2 = TDBFactory.createDataset(args[1]);
      dataset2.begin(ReadWrite.READ);
      // Get model inside the transaction
      m2 = dataset2.getDefaultModel();
    } else {
      System.err.println("Handling second model from turtle: " + args[1]);
      m2 = ModelFactory.createDefaultModel();
      m2.read(args[1], "TURTLE");
    }

    System.err.println("Building bindings.");
    buildBinding(m1, m2);

    System.err.println("Computing differences.");

    // merge the Models
    Model model = difference(m1, m2);

    if (null != dataset1)
      dataset1.end();
    if (null != dataset2)
      dataset2.end();

    for (Entry<String, String> e : m1.getNsPrefixMap().entrySet()) {
      model.setNsPrefix(e.getKey(), e.getValue());
    }
    // print the Model as RDF/XML
    model.write(System.out, "TURTLE");
  }

  static TreeMap<String, String> anodes2id = new TreeMap<>();


  public static void buildBinding(Model m1, Model m2) {
    // Creates a binding between equivalent blank nodes in m1 and m2;
    ExtendedIterator<Node> iter = null;
    Node s;
    indexBlankNodes(m1);
    indexBlankNodes(m2);
  }

  private static void indexBlankNodes(Model m) {
    Node s;
    ExtendedIterator<Node> iter = null;
    try {
      iter = GraphUtil.listSubjects(m.getGraph(), Node.ANY, Node.ANY);
      int nbtriple = 0, nbBlank = 0;
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
          StringBuffer b = new StringBuffer();
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

  private static Model difference(Model m1, Model m2) {
    Model resultModel = ModelFactory.createDefaultModel();
    ExtendedIterator<Triple> iter = null;
    Triple triple;
    int nbprocessed = 0, nbdiffs = 0;
    try {
      iter = GraphUtil.findAll(m1.getGraph());
      while (iter.hasNext()) {
        triple = iter.next();
        nbprocessed++;
        if (triple.getSubject().isBlank() && triple.getObject().isBlank()) {
          // TODO
        } else if (triple.getSubject().isBlank()) {
          ExtendedIterator<Node> it = null;
          try {
            it = GraphUtil.listSubjects(m2.getGraph(), triple.getPredicate(), triple.getObject());
            if (it.hasNext()) {
              Node ec = it.next();
              while (it.hasNext() && !bound(triple.getSubject(), ec)) {
                ec = it.next();
              }
              if (!bound(triple.getSubject(), ec)) {
                resultModel.getGraph().add(triple);
                nbdiffs++;
              }
            } else {
              resultModel.getGraph().add(triple);
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
            it = GraphUtil.listObjects(m2.getGraph(), triple.getSubject(), triple.getPredicate());
            if (it.hasNext()) {
              Node ec = it.next();
              while (it.hasNext() && !bound(triple.getObject(), ec)) {
                ec = it.next();
              }
              if (!bound(triple.getObject(), ec)) {
                resultModel.getGraph().add(triple);
                nbdiffs++;
              }
            } else {
              resultModel.getGraph().add(triple);
              nbdiffs++;
            }
          } finally {
            if (null != it) {
              it.close();
            }
          }

        } else if (!m2.getGraph().contains(triple)) {
          resultModel.getGraph().add(triple);
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
    return resultModel;
  }


  private static boolean bound(Node n1, Node n2) {
    if (n2.isBlank()) {
      String k1 = anodes2id.get(n1.getBlankNodeLabel());
      String k2 = anodes2id.get(n2.getBlankNodeLabel());
      return k1 == k2 || (k1 != null && k1.equals(k2));
    }
    return false;
  }

  private static void usage() {
    System.err.println("Usage: java -Xmx8G " + RDFDiff.class.getCanonicalName() + " url1 url2");
  }
}

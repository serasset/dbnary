package org.getalp.dbnary.cli;

import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class RDFDiff {
	
	public static void main(String args[]) {
		Model m1, m2;
		if (args.length != 2) {
			usage();
			System.exit(1);
		}
		
		m1 = ModelFactory.createDefaultModel();
		m2 = ModelFactory.createDefaultModel();
		
		// read the RDF/XML files
		System.err.println("Reading first model.");
		m1.read(args[0], "TURTLE");
		System.err.println("Reading second model.");
		m2.read(args[1], "TURTLE");
		System.err.println("Building bindings.");
		buildBinding(m1,m2);
		
		System.err.println("Computing differences.");

		// merge the Models
		Model model = difference(m1,m2);
		
		for (Entry<String, String> e : m1.getNsPrefixMap().entrySet()) {
			model.setNsPrefix(e.getKey(), e.getValue());
		}
		// print the Model as RDF/XML
		model.write(System.out, "TURTLE");
	}

	static 	TreeMap<String, String> anodes2id = new TreeMap<String,String>();


	public static void buildBinding(Model m1, Model m2) {
		// Creates a binding between equivalent blank nodes in m1 and m2;
		ResIterator iter = null;
		Resource s;
		try {
			iter =  m1.listSubjects();
			while (iter.hasNext()) {
				s = iter.nextResource();
				if (s.isAnon()) {
					StmtIterator stmts = m1.listStatements(s, null, (RDFNode)null);
					SortedSet<String> signature = new TreeSet<String>();
					while (stmts.hasNext()) {
						Statement stmt = stmts.nextStatement();
						signature.add(stmt.getPredicate().toString() + "+" + stmt.getObject().toString());
					}
					StringBuffer b = new StringBuffer();
					for (String r: signature) {
						b.append(r).append("|");
					}
					String key = b.toString();
					assert anodes2id.get(s.getId().getLabelString()) == null;
					anodes2id.put(s.getId().getLabelString(), key);
				}
			}
		} finally {
			if (null != iter) iter.close();
		}
		try {
			iter = m2.listSubjects();
			long tt1 = 0, tt2=0, tt3 = 0, tt4 = 0, tt5 = 0;
			while (iter.hasNext()) {
				s = iter.nextResource();
				if (s.isAnon()) {
					long t = System.nanoTime();
					StmtIterator stmts = m2.listStatements(s, null, (RDFNode)null);
					SortedSet<String> signature = new TreeSet<String>();
					while (stmts.hasNext()) {
						Statement stmt = stmts.nextStatement();
						signature.add(stmt.getPredicate().toString() + "+" + stmt.getObject().toString());
					}
					StringBuffer b = new StringBuffer();
					for (String r: signature) {
						b.append(r).append("|");
					}
					String key = b.toString();
					assert anodes2id.get(s.getId().getLabelString()) == null;
					anodes2id.put(s.getId().getLabelString(), key);
				}
			}
		} finally {
			if (null != iter) iter.close();
		}
	}
	
	private static Model difference(Model m1, Model m2) {
        Model resultModel = ModelFactory.createDefaultModel();
        StmtIterator iter = null;
        Statement stmt;
        try {
            iter = m1.listStatements();
            while (iter.hasNext()) {
                stmt = iter.nextStatement();
                if (stmt.getSubject().isAnon() && stmt.getObject().isAnon()) {
                	// TODO
                } else if (stmt.getSubject().isAnon()) {
                	StmtIterator stmts = null;
                	try {
                		stmts = m2.listStatements(null, stmt.getPredicate(), stmt.getObject());
                		if (stmts.hasNext()) {
                			Statement ec = stmts.nextStatement();
                			while (stmts.hasNext() && ! bound(stmt.getSubject(),ec.getSubject())) {
                				ec = stmts.nextStatement();
                			}
                			if (! bound(stmt.getSubject(),ec.getSubject())) {
                				resultModel.add(stmt);
                			}
                		}
                	} finally {
                		stmts.close();
                	}
                	
                } else if (stmt.getObject().isAnon()) {
                	StmtIterator stmts = null;
                	try {
                		stmts = m2.listStatements(stmt.getSubject(), stmt.getPredicate(), (RDFNode)null);
                		if (stmts.hasNext()) {
                			Statement ec = stmts.nextStatement();
                			while (stmts.hasNext() && ! bound(stmt.getObject().asResource(),ec.getObject().asResource())) {
                				ec = stmts.nextStatement();
                			}
                			if (! bound(stmt.getObject().asResource(),ec.getObject().asResource())) {
                				resultModel.add(stmt);
                			}
                		}
                	} finally {
                		stmts.close();
                	}
                	
                } else if (! m2.contains(stmt)) {
                    resultModel.add(stmt);
                }
            }
        } finally {
            iter.close();
        }
		return resultModel;
	}


	private static boolean bound(Resource n1, Resource n2) {
		if (n2.isAnon()) {
			String k1 = anodes2id.get(n1.getId().getLabelString());
			String k2 = anodes2id.get(n2.getId().getLabelString());
			return k1 == k2 || (k1 != null && k1.equals(k2));
		}
		return false;
	}

	private static void usage() {
		System.out.println("Usage: java -Xmx8G " + RDFDiff.class.getCanonicalName() + " url1 url2");
	}
}

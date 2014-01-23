package org.getalp.dbnary.cli;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.SortedSet;
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

	static HashMap<String, HashSet<String>> binding = new HashMap<String, HashSet<String>>();

	public static void buildBinding(Model m1, Model m2) {
		// Creates a binding between equivalent blank nodes in m1 and m2;
		HashMap<String,HashSet<String>> rel2anodes = new HashMap<String,HashSet<String>>();
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
					HashSet<String> nodes = rel2anodes.get(key);
					if (null != nodes) {
						nodes.add(s.getId().getLabelString());
					} else {
						nodes = new HashSet<String>();
						nodes.add(s.getId().getLabelString());
						rel2anodes.put(key, nodes);
					}
				}
			}
		} finally {
			if (null != iter) iter.close();
		}
		try {
			iter = m2.listSubjects();
			while (iter.hasNext()) {
				s = iter.nextResource();
				if (s.isAnon()) {
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
					HashSet<String> m1ids = rel2anodes.get(key);
					if (null != m1ids) {
						for (String m1id : m1ids) {
							HashSet<String> equivs1 = binding.get(m1id);
							if (null == equivs1) equivs1 = new HashSet<String>();
							equivs1.add(s.getId().getLabelString());
							binding.put(m1id, equivs1);
						}
					}
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
			HashSet<String> b = binding.get(n1.getId().getLabelString());
			if (null != b) return b.contains(n2.getId().getLabelString());
		}
		return false;
	}

	private static void usage() {
		System.out.println("Usage: java -Xmx8G " + RDFDiff.class.getCanonicalName() + " url1 url2");
	}
}

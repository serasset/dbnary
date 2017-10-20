package org.getalp.dbnary.experiment;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.rdf.model.impl.StatementImpl;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.vocabulary.RDF;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.getalp.dbnary.OntolexOnt;
import org.getalp.dbnary.VarTransOnt;

import java.io.*;
import java.util.*;

public class DisambiguateTargets {
    Model graph ;
    Model noisyGraph ;
    Map<String,Model> initialModels ;
    int delta ;
    String outputModelFileName ;

    public DisambiguateTargets(String directoryGraph, String directoryModels, String outputModelFileName){
        System.err.println("Reading Graph model") ;
        Dataset dataset = TDBFactory.createDataset(directoryGraph) ;
        dataset.begin(ReadWrite.READ) ;
        graph = dataset.getDefaultModel() ;

        initialModels = new HashMap<String,Model>() ;

        System.err.println("Reading initial models") ;
        String[] directories = new File(directoryModels).list();
        for(int i = 0 ; i<directories.length ; i++){
            if(directories[i].length()==3) {
                Dataset dat = TDBFactory.createDataset(directoryModels + "/" + directories[i]);
                dat.begin(ReadWrite.READ);
                Model m = dat.getDefaultModel();
                initialModels.put(directories[i], m);
            }
        }

        System.err.println("Creating noisy graph") ;
        noisyGraph = ModelFactory.createDefaultModel();
        createNoisyGraph();
        System.err.println("Noisy graph created") ;

        delta = 4 ;
        this.outputModelFileName = outputModelFileName ;
    }

    public void disambiguateTargets(){
        // disambiguate all target senses, use disambiguateTarget(sense) for each each source sense (graph.add(Model m))
        Model outputModel = ModelFactory.createDefaultModel() ;
        ResIterator resIter = noisyGraph.listSubjects();
        while(resIter.hasNext()){
            Resource source = resIter.next() ;
            System.err.println("Choosing senses for "+source.getLocalName()) ;
            Model m = getLinksFromSense(source) ;
            outputModel.add(m) ;
        }

        this.output(outputModel);
    }

    private Model getLinksFromSense(Resource sense){ // disambiguate target sense for a given source sense
        Model res = ModelFactory.createDefaultModel() ;
        Map<Resource,Double> mu = disambiguateTarget(sense) ;
        for(Resource r : mu.keySet()){
            res.add(res.createStatement(sense,VarTransOnt.translatableAs,r)) ;
        }
        return res ;
    }

    private Map<Resource,Double> disambiguateTarget(Resource sense){ // The score can be used for other purposes
        StmtIterator stmtIter = graph.listStatements(sense,VarTransOnt.translatableAs, (RDFNode) null) ;
        Map<Resource,Double> mu = new HashMap<>() ;
        while(stmtIter.hasNext()){
            List<List<Statement>> allPaths = new ArrayList<>();
            Map<Resource,List<List<Statement>>> paths = new HashMap<>() ;
            Statement stmt = stmtIter.next() ;
            Resource transLE = stmt.getResource() ;
            StmtIterator stmtIterInitial = initialModels.get(getLanguage(transLE)).listStatements(transLE, OntolexOnt.sense, (RDFNode) null);
            while(stmtIterInitial.hasNext()){
                Statement stmtInit = stmtIterInitial.next() ;
                Resource s = stmtInit.getResource();
                Statement pathS = new StatementImpl(sense, VarTransOnt.translatableAs,s) ;
                paths.put(s,dfs(pathS)) ;
                allPaths.addAll(paths.get(s));
            }

            Map<Resource,Double> score = new HashMap<>() ;
            stmtIterInitial = initialModels.get(getLanguage(transLE)).listStatements(transLE, OntolexOnt.sense, (RDFNode) null);
            while(stmtIterInitial.hasNext()){
                Statement stmtInit = stmtIterInitial.next() ;
                Resource s = stmtInit.getResource();
                score.put(s,Double.valueOf(0));
                for(List<Statement> p : paths.get(s)){
                    int l = p.size() ;
                    Double v = w(l)*(1/((double)numPaths(allPaths,l))) ;
                    Double d = score.get(s) ;
                    d = d+v ;
                    score.put(s,d) ;
                }
            }
            if(!score.isEmpty()){
                Resource bestr = null ;
                Double bests = Double.valueOf(-1) ;
                for(Resource r : score.keySet()){
                    if(Double.compare(score.get(r),bests)>0){
                        bestr = r ;
                        bests = score.get(r) ;
                    }
                }
                mu.put(bestr,bests);
            }
        }
        return mu ;
    }

    private static int numPaths(List<List<Statement>> paths, int l){
        int res = 0 ;
        for(List<Statement> path : paths){
            if(path.size()==l){
                res = res+1 ;
            }
        }
        return res ;
    }

    private static double w(int l){
        return 1/(Math.exp(l)) ;
    }

    private void createNoisyGraph(){
        // add statements of noisy graph in this.noisyGraph
        StmtIterator stmtIter = graph.listStatements() ;
        while(stmtIter.hasNext()){
            Statement stmt = stmtIter.next() ;
            Resource sourceSense = stmt.getSubject() ;
            if(isWordSense(sourceSense)){
                Resource le = stmt.getResource() ;
                for(Resource sense : getSenses(le)){
                    noisyGraph.add(noisyGraph.createStatement(sourceSense,VarTransOnt.translatableAs,sense)) ;
                }
            }
        }
    }

    private List<Resource> getSenses(Resource le){
        List<Resource> res = new ArrayList<>() ;
        Model modelLE = initialModels.get(getLanguage(le)) ;
        StmtIterator stmtIter = modelLE.listStatements(le, RDF.type, OntolexOnt.LexicalEntry) ;
        while(stmtIter.hasNext()){
            Statement stmt = stmtIter.next() ;
            Resource r = stmt.getSubject();
            StmtIterator stmtIter2 = modelLE.listStatements(r, OntolexOnt.sense, (RDFNode) null) ;
            while(stmtIter2.hasNext()){
                Statement stmt2 = stmtIter2.next() ;
                Resource sense = stmt2.getResource() ;
                res.add(sense) ;
            }
        }
        return res ;
    }

    private static String getLanguage(Resource r){
        String[] split = r.toString().split("/") ;
        return split[4] ;
    }

    private static boolean isWordSense(Resource r){
        return r.toString().charAt(35) == '_' ;
    }

    private List<List<Statement>> dfs(Statement stm){ // list de paths (path = list de statement)
        List<List<Statement>> paths = new ArrayList<>() ;
        Stack<Resource> visited = new Stack<>() ;
        Resource potentialTarget = stm.getResource() ;
        Resource source = stm.getSubject() ;
        List<Statement> l = new ArrayList<>() ;
        l.add(stm) ;
        paths.addAll(recDFS(potentialTarget,source,l,visited)) ;
        return paths ;
    }

    private List<List<Statement>> recDFS(Resource potentialTarget, Resource source, List<Statement> path, Stack visited){
        List<List<Statement>> res = new ArrayList<>() ;
        if(visited.contains(potentialTarget) || path.size()>delta){
            return new ArrayList<>() ;
        }
        if(potentialTarget.equals(source)){
            List<List<Statement>> p = new ArrayList<>() ;
            p.add(path) ;
            return p ;
        }
        visited.push(potentialTarget) ;
        StmtIterator stm = noisyGraph.listStatements(potentialTarget, VarTransOnt.translatableAs,(RDFNode)null) ;
        while(stm.hasNext()){
            Statement stmt = stm.next() ;
            List<Statement> path2 = new ArrayList<>() ;
            path2.addAll(path) ;
            path2.add(stmt) ;
            Resource sense2 = stmt.getResource() ;
            res.addAll(recDFS(sense2,source,path2,visited)) ;
        }
        stm = noisyGraph.listStatements(null, VarTransOnt.translatableAs,potentialTarget) ;
        while(stm.hasNext()){
            Statement stmt = stm.next() ;
            List<Statement> path2 = new ArrayList<>() ;
            path2.addAll(path) ;
            path2.add(stmt) ;
            Resource sense2 = stmt.getSubject() ;
            if(reversedEdgesConsecutive(path2)) {
                res.addAll(recDFS(sense2, source, path2, visited));
            }
        }
        visited.pop() ;
        return res ;
    }

    private boolean reversedEdgesConsecutive(List<Statement> path){ // TODO implement
        List<Statement> copy = new ArrayList<>() ;
        for(Statement s : path){
            copy.add(s) ;
        }
        List<Statement> normal = new ArrayList<>() ;
        List<Statement> reverse = new ArrayList<>() ;
        boolean forward = true ;
        boolean backwards = true ;
        // NORMAL
        if(!copy.isEmpty()) {
            Statement s = copy.remove(0);
            Resource first = s.getResource();
            normal.add(s);
            Resource r = s.getResource();
            while (forward) {
                s = lookForward(copy, r);
                if (s != null) {
                    normal.add(s);
                    r = s.getResource();
                } else {
                    forward = false;
                }
            }
            r = first;
            while (backwards) {
                s = lookBackwards(copy, r);
                if (s != null) {
                    normal.add(s);
                    r = s.getSubject();
                } else {
                    backwards = false;
                }
            }
            // REVERSE
            if (!copy.isEmpty()) {
                forward = true;
                backwards = true;
                s = copy.remove(0);
                first = s.getResource();
                reverse.add(s);
                r = s.getResource();
                while (forward) {
                    s = lookForward(copy, r);
                    if (s != null) {
                        reverse.add(s);
                        r = s.getResource();
                    } else {
                        forward = false;
                    }
                }
                r = first;
                while (backwards) {
                    s = lookBackwards(copy, r);
                    if (s != null) {
                        reverse.add(s);
                        r = s.getSubject();
                    } else {
                        backwards = false;
                    }
                }
            }
        }
        return normal.size() + reverse.size() == path.size();
    }

    private static Statement lookForward(List<Statement> list, Resource r){
        boolean done = false ;
        int i = 0 ;
        Statement found = null ;
        while(i<list.size() && !done){
            if(list.get(i).getSubject().equals(r)){
                found = list.remove(i) ;
                done = true ;
            }
            i = i+1 ;
        }
        return found ;
    }

    private static Statement lookBackwards(List<Statement> list, Resource r){
        boolean done = false ;
        int i = 0 ;
        Statement found = null ;
        while(i<list.size() && !done){
            if(list.get(i).getResource().equals(r)){
                found = list.remove(i) ;
                done = true ;
            }
            i = i+1 ;
        }
        return found ;
    }

    private void output(Model m) { // to output the disambiguated model
        OutputStream outputModelStream;

        try {
            outputModelStream = new FileOutputStream(outputModelFileName);
            m.write(outputModelStream, "TURTLE");

        } catch (FileNotFoundException e) {
            System.err.println("Could not create output stream: " + e.getLocalizedMessage());
            e.printStackTrace(System.err);
            return;
        }
    }

    public static void main(String[] args){
        DisambiguateTargets dt = new DisambiguateTargets(args[0],args[1], args[2]) ;
        dt.disambiguateTargets() ;
    }
}

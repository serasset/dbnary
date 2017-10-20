package org.getalp.dbnary.experiment;


import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.tdb.TDBFactory;
import org.getalp.dbnary.OntolexOnt;
import org.getalp.dbnary.VarTransOnt;
import org.jgrapht.DirectedGraph;
import org.jgrapht.Graph;
import org.jgrapht.alg.flow.*;
import org.jgrapht.alg.interfaces.MaximumFlowAlgorithm;
import org.jgrapht.ext.*;
import org.jgrapht.graph.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.plaf.nimbus.State;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.*;

public class ProcessGraph {
    private static Logger log = LoggerFactory.getLogger(ToolsGraph.class);
    private SimpleDirectedGraph<String,DefaultEdge> graph ;
    private SimpleGraph<String,DefaultEdge> graphUndirected ;

    public ProcessGraph(String fileTDB,List<String> sourceList, int depth){
        Dataset dataset = TDBFactory.createDataset(fileTDB) ;
        dataset.begin(ReadWrite.READ) ;
        Model g = dataset.getDefaultModel() ;

        List<Resource> resources = new ArrayList<>() ;
        for(String s : sourceList){
            resources.add(g.getResource(s)) ;
        }
        Model newGraph = getsubGraph(g,resources,depth) ;
        graph = getJGraph(newGraph) ;
        Set<String> vertices = graph.vertexSet() ;
        Set<DefaultEdge> edges = graph.edgeSet() ;
        log.debug("In the subgraph : "+vertices.size()+" vertices and "+edges.size()+" edges.") ;
        graphUndirected = getUJGraph(newGraph) ;
        Set<String> uvertices = graphUndirected.vertexSet() ;
        Set<DefaultEdge> uedges = graphUndirected.edgeSet() ;
        log.debug("In the undirected subgraph : "+uvertices.size()+" vertices and "+uedges.size()+" edges.") ;
    }

    public static void main(String args[]) throws FileNotFoundException {
        List<String> resourcesString = new ArrayList<>() ;
        resourcesString.add("http://kaiko.getalp.org/dbnary/eng/spring__Noun__1") ;
        ProcessGraph pg = new ProcessGraph(args[0],resourcesString,1) ;
        String source1 = "http://kaiko.getalp.org/dbnary/eng/__ws_26_spring__Noun__1" ;
        String sink1 = "http://kaiko.getalp.org/dbnary/eng/spring__Noun__1" ;
        String source2 = "http://kaiko.getalp.org/dbnary/fra/__ws_2_ressort__nom__1" ;
        String sink2 = "http://kaiko.getalp.org/dbnary/fra/ressort__nom__1" ;
        String source3 = "http://kaiko.getalp.org/dbnary/fra/__ws_1_source__nom__1" ;
        String sink3 = "http://kaiko.getalp.org/dbnary/fra/source__nom__1" ;
        double s1s2 = pg.nbEdgeDistinctPaths(source1,sink2) ;
        double s2s1 = pg.nbEdgeDistinctPaths(source2,sink1) ;
        double s1s3 = pg.nbEdgeDistinctPaths(source1,sink3) ;
        double s3s1 = pg.nbEdgeDistinctPaths(source3,sink1) ;
        System.out.println("Correct link : "+s1s2+" + "+s2s1) ;
        System.out.println("Wrong link : "+s1s3+" + "+s3s1) ;

        SimpleDirectedGraph<String,DefaultEdge> newGraph = pg.getExportableGraph() ;
        writeDot(newGraph,args[1],args[2]);
    }

    public static void writeDot(Graph<String,DefaultEdge> translationGraph, String targetDirectory, String file) throws FileNotFoundException {
        //try {
        if (translationGraph != null) {
            File dir = new File(targetDirectory) ;
            if(!dir.exists()){
                dir.mkdirs() ;
            }
            String path = targetDirectory+file ;
            Writer translationGraphWriter = new PrintWriter(path);
            VertexNameProvider vertIdProv = new StringNameProvider<>();
            ComponentNameProvider edgeProv = new StringEdgeNameProvider<>() ;
            DOTExporter dotExp = new DOTExporter(vertIdProv,null,edgeProv) ;
            dotExp.exportGraph(translationGraph,translationGraphWriter) ;
        } else {
            log.error("The graph is empty... Aborting.");
        }
        //}catch(FileNotFoundException e){
        //    System.out.println("FileNotFoundExcetion") ;
        //}
    }

    public double nbEdgeDistinctPaths(String source, String sink){
        if(isSense(source) && !isSense(sink)){
            MaximumFlowAlgorithm<String,DefaultEdge> mf = new EdmondsKarpMFImpl(this.graph) ;
            return mf.calculateMaximumFlow(source,sink) ;
        }else{
            return -1 ;
        }
    }

    public SimpleDirectedGraph<String,DefaultEdge> getExportableGraph(){
        SimpleDirectedGraph<String,DefaultEdge> res = new SimpleDirectedGraph<String, DefaultEdge>(DefaultEdge.class) ;
        Map<String,String> mapNames = new HashMap<>() ;
        int nextle = 0 ;
        int nextws = 0 ;
        for(DefaultEdge e : this.graph.edgeSet()){
            String source = this.graph.getEdgeSource(e) ;
            String target = this.graph.getEdgeTarget(e) ;

            String newSource = mapNames.get(source) ;
            if(newSource==null) {
                if (isSense(source)) {
                    newSource = "S" + nextws;
                    nextws = nextws + 1;
                    mapNames.put(source, newSource);
                }else{
                    newSource = "L" + nextle;
                    nextle = nextle + 1;
                    mapNames.put(source, newSource);
                }
            }
            res.addVertex(newSource) ;

            String newTarget = mapNames.get(target) ;
            if(newTarget==null){
                if (isSense(target)) {
                    newTarget = "S" + nextws;
                    nextws = nextws + 1;
                    mapNames.put(target, newTarget);
                }else{
                    newTarget = "L" + nextle;
                    nextle = nextle + 1;
                    mapNames.put(target, newTarget);
                }
            }
            res.addVertex(newTarget) ;

            res.addEdge(newSource,newTarget) ;
        }

        String out = "" ;
        for(String s : mapNames.keySet()){
            out = out+s+" -> "+mapNames.get(s)+"\n" ;
        }
        log.debug(out); ;
        return res ;
    }


    private static boolean isSense(String v){
        return v.charAt(35)=='_' ;
    }

    public static Model getsubGraph(Model m, List<Resource> sourceList, int depth){
        Model res = ModelFactory.createDefaultModel() ;
        Set<Resource> vertices = new HashSet<>() ;
        for(Resource source : sourceList) {
            vertices.addAll(getSubGraphVertices(m, source, depth));
        }
        Set<Resource> tmp = new HashSet<>() ;
        for(Resource r : vertices){
            StmtIterator stmt = m.listStatements(r, OntolexOnt.sense, (RDFNode) null);
            while(stmt.hasNext()){
                Statement next = stmt.next() ;
                Resource sense = next.getResource() ;
                tmp.add(sense) ;
            }
        }
        vertices.addAll(tmp) ;
        for(Resource r1 : vertices){
            for(Resource r2 : vertices){
                StmtIterator stmtIter = m.listStatements(r1, VarTransOnt.translatableAs,r2) ;
                while(stmtIter.hasNext()){
                    Statement stm = stmtIter.next() ;
                    res.add(stm) ;
                }
                stmtIter = m.listStatements(r1,OntolexOnt.sense,r2) ;
                while(stmtIter.hasNext()){
                    Statement stm = stmtIter.next() ;
                    res.add(stm) ;
                }
            }
        }
        return res ;
    }

    private static Set<Resource> getSubGraphVertices(Model m, Resource source, int depth){
        Set<Resource> res = new HashSet<>() ;
        res.add(source) ;
        if(depth==0){
            return res ;
        }else{
            StmtIterator stmtIter = m.listStatements(source, VarTransOnt.translatableAs, (RDFNode) null) ;
            while(stmtIter.hasNext()){
                Statement stm = stmtIter.next() ;
                Resource newSource = stm.getResource() ; // get the new source
                res.addAll(getSubGraphVertices(m,newSource,depth-1)) ;
            }
            stmtIter = m.listStatements(source, OntolexOnt.sense, (RDFNode) null) ;
            while(stmtIter.hasNext()){
                Statement stm = stmtIter.next() ;
                Resource newSource = stm.getResource() ; // get the new source
                res.addAll(getSubGraphVertices(m,newSource,depth-1)) ;
            }

            return res ;
        }
    }

    public static SimpleDirectedGraph<String,DefaultEdge> getJGraph(Model m){
        SimpleDirectedGraph<String,DefaultEdge> res = new SimpleDirectedGraph<String, DefaultEdge>(DefaultEdge.class) ;
        StmtIterator stmtIter = m.listStatements() ;
        while(stmtIter.hasNext()){
            Statement stm = stmtIter.next() ;
            Resource subject =  stm.getSubject() ;
            String source = subject.toString() ;
            Resource resource = stm.getResource() ;
            String target = resource.toString() ;
            res.addVertex(source) ;
            res.addVertex(target) ;
            res.addEdge(source, target) ;
        }
        return res ;
    }

    public static SimpleGraph<String,DefaultEdge> getUJGraph(Model m){
        SimpleGraph<String,DefaultEdge> res = new SimpleGraph<String, DefaultEdge>(DefaultEdge.class) ;
        StmtIterator stmtIter = m.listStatements() ;
        while(stmtIter.hasNext()){
            Statement stm = stmtIter.next() ;
            Resource subject =  stm.getSubject() ;
            String source = subject.toString() ;
            Resource resource = stm.getResource() ;
            String target = resource.toString() ;
            res.addVertex(source) ;
            res.addVertex(target) ;
            res.addEdge(source, target) ;
        }
        return res ;
    }
}

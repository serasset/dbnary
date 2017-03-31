package org.getalp.dbnary.experiment;


import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.tdb.TDBFactory;
import org.getalp.dbnary.LemonOnt;
import org.getalp.dbnary.VarTransOnt;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.graph.SimpleGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.plaf.nimbus.State;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProcessGraph {
    private Logger log = LoggerFactory.getLogger(ToolsGraph.class);
    private SimpleDirectedGraph<String,DefaultEdge> graph ;

    public ProcessGraph(String fileTDB,List<Resource> sourceList, int depth){
        Dataset dataset = TDBFactory.createDataset(fileTDB) ;
        dataset.begin(ReadWrite.READ) ;
        Model g = dataset.getDefaultModel() ;

        Model newGraph = getsubGraph(g,sourceList,depth) ;
        graph = getJGraph(newGraph) ;
        Set<String> vertices = graph.vertexSet() ;
        Set<DefaultEdge> edges = graph.edgeSet() ;
        log.debug("In the subgraph : "+vertices.size()+" vertices and "+edges.size()+" edges.") ;
    }

    // TODO max flow : push relabel ?

    public static Model getsubGraph(Model m, List<Resource> sourceList, int depth){
        Model res = ModelFactory.createDefaultModel() ;
        Set<Resource> vertices = new HashSet<>() ;
        for(Resource source : sourceList) {
            vertices.addAll(getSubGraphVertices(m, source, depth));
        }
        for(Resource r : vertices){
            StmtIterator stmt = m.listStatements(r,LemonOnt.sense, (RDFNode) null);
            while(stmt.hasNext()){
                Statement next = stmt.next() ;
                Resource sense = next.getResource() ;
                vertices.add(sense) ;
            }
        }
        for(Resource r1 : vertices){
            for(Resource r2 : vertices){
                StmtIterator stmtIter = m.listStatements(r1, VarTransOnt.translatableAs,r2) ;
                while(stmtIter.hasNext()){
                    Statement stm = stmtIter.next() ;
                    res.add(stm) ;
                }
                stmtIter = m.listStatements(r1,LemonOnt.sense,r2) ;
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
            stmtIter = m.listStatements(source, LemonOnt.sense, (RDFNode) null) ;
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
}

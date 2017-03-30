package org.getalp.dbnary.experiment;


import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.tdb.TDBFactory;
import org.jgrapht.Graph;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.ext.*;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

public class Visualization {

    private static Logger log = LoggerFactory.getLogger(ToolsGraph.class);

    public static void main(String args[])throws FileNotFoundException,IOException {
        /*Dataset dataset = TDBFactory.createDataset(args[0]) ;
        dataset.begin(ReadWrite.READ) ;
        Model graph = dataset.getDefaultModel() ;

        List<String> resourcesString = new ArrayList<>() ;
        resourcesString.add("http://kaiko.getalp.org/dbnary/eng/spring__Noun__1") ;
        List<Resource> resources = new ArrayList<>() ;
        for(String s : resourcesString){
            resources.add(graph.getResource(s)) ;
        }
        log.debug("Creating subgraph...") ;
        Model newGraph = ToolsGraph.getsubGraph(graph,resources,2) ;
        Graph<String,DefaultWeightedEdge> g = ToolsGraph.getGraph(newGraph) ;
        Set<String> vertices = g.vertexSet() ;
        Set<DefaultWeightedEdge> edges = g.edgeSet() ;
        log.debug("In the subgraph : "+vertices.size()+" vertices and "+edges.size()+" edges.") ;
        log.debug("Getting probas...") ;
        Map<String,Map<String,Double>> probas = ToolsGraph.getProbas(g) ;
        System.out.println(ToolsGraph.seeProbas(probas)) ;
        log.debug("Getting weighted graph...") ;
        SimpleWeightedGraph<String,DefaultWeightedEdge> wg = ToolsGraph.getWeightedGraph(probas) ;
        System.out.println(ToolsGraph.seeWeightedGraph(wg)) ;*/


        log.debug("Getting weighted graph from file...") ;
        SimpleWeightedGraph<String,DefaultWeightedEdge> wg = ToolsGraph.getWeightedGraph() ;
        log.debug(wg.vertexSet().size()+" vertices in the graph") ;
        log.debug(ToolsGraph.seeWeightedGraph(wg)) ;

        log.debug("Getting connected components...");
        ConnectivityInspector<String,DefaultWeightedEdge> ci = new ConnectivityInspector<String, DefaultWeightedEdge>(wg) ;
        List<Set<String>> components = ci.connectedSets() ;
        String connectComp = "" ;
        for(Set<String> comp : components){
            connectComp = connectComp+"{ " ;
            for(String node : comp){
                connectComp = connectComp+node+" " ;
            }
            connectComp = connectComp+"}\n" ;
        }
        log.debug(components.size()+" connected component(s)\n"+connectComp);

        wg = getExportableGraph(wg) ;
        log.debug(ToolsGraph.seeWeightedGraph(wg)) ;
        log.debug(wg.vertexSet().size()+" vertices in the renamed graph") ;

        Visualization.writeDot(wg,args[1],args[2]) ;

        log.debug("Getting clusters...") ;
        Set<Set<String>> minCut = ToolsGraph.minCutClustering(wg,3) ; //minCutClustering(wg,20) ;
        String clusters = "" ;
        for(Set<String> cluster : minCut){
            clusters = clusters+"{ " ;
            for(String v : cluster){
                clusters = clusters+v+" " ;
            }
            clusters = clusters+"}\n" ;
        }
        log.debug("Clusters : "+clusters);
        //writeDot(g,args[1],args[2]);

        //testMinCutClustering(1000,0.3,6);
    }

    public static SimpleWeightedGraph<String,DefaultWeightedEdge> getExportableGraph(SimpleWeightedGraph<String,DefaultWeightedEdge> g){
        SimpleWeightedGraph<String,DefaultWeightedEdge> res = new SimpleWeightedGraph<String, DefaultWeightedEdge>(DefaultWeightedEdge.class) ;
        Map<String,String> mapNames = new HashMap<>() ;
        int nextName = 0 ;
        for(DefaultWeightedEdge e : g.edgeSet()){
            String source = g.getEdgeSource(e) ;
            String target = g.getEdgeTarget(e) ;
            double d = g.getEdgeWeight(e) ;

            String newSource = mapNames.get(source) ;
            if(newSource==null){
                newSource = ""+nextName ;
                nextName = nextName+1 ;
                mapNames.put(source,newSource) ;
            }
            res.addVertex(newSource) ;

            String newTarget = mapNames.get(target) ;
            if(newTarget==null){
                newTarget = ""+nextName ;
                nextName = nextName+1 ;
                mapNames.put(target,newTarget) ;
            }
            res.addVertex(newTarget) ;

            DefaultWeightedEdge newEdge = res.addEdge(newSource,newTarget) ;
            res.setEdgeWeight(newEdge,d);
        }

        String out = "" ;
        for(String s : mapNames.keySet()){
            out = out+s+" -> "+mapNames.get(s)+"\n" ;
        }
        log.debug(out); ;
        return res ;
    }

    public static void exportGraph(Graph<String,DefaultWeightedEdge> translationGraph, String targetDirectory, String file) throws FileNotFoundException {
        //try {
        if (translationGraph != null) {
            File dir = new File(targetDirectory) ;
            if(!dir.exists()){
                dir.mkdirs() ;
            }
            String path = targetDirectory+file ;
            Writer translationGraphWriter = new PrintWriter(path);
            VertexNameProvider vertIdProv = new StringNameProvider<>();
            VisioExporter visioExp = new VisioExporter(vertIdProv) ;
            visioExp.exportGraph(translationGraph,translationGraphWriter);
        } else {
            log.error("The graph is empty... Aborting.");
        }
        //}catch(FileNotFoundException e){
        //    System.out.println("FileNotFoundExcetion") ;
        //}
    }

    public static void writeDot(Graph<String,DefaultWeightedEdge> translationGraph, String targetDirectory, String file) throws FileNotFoundException {
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
}

package org.getalp.dbnary.experiment;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.tdb.TDBFactory;
import org.getalp.dbnary.VarTransOnt;
import org.jgrapht.Graph;
import org.jgrapht.alg.BronKerboschCliqueFinder;
import org.jgrapht.ext.*;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

public class ToolsGraph {
    // create jgrapht graph object from a rdf graph file

    private static Logger log = LoggerFactory.getLogger(ToolsGraph.class);

    public static void main(String args[])throws FileNotFoundException,IOException{
        Dataset dataset = TDBFactory.createDataset(args[0]) ;
        dataset.begin(ReadWrite.READ) ;
        Model graph = dataset.getDefaultModel() ;
        /*Graph<String,DefaultWeightedEdge> g1 = getGraph(graph) ;
        Set<String> vertices1 = g1.vertexSet() ;
        Set<DefaultWeightedEdge> edges1 = g1.edgeSet() ;
        log.debug("In the translation graph : "+vertices1.size()+" vertices and "+edges1.size()+" edges.") ;*/

        List<String> resourcesString = new ArrayList<>() ;
        resourcesString.add("http://kaiko.getalp.org/dbnary/eng/spring__Noun__1") ;
        List<Resource> resources = new ArrayList<>() ;
        for(String s : resourcesString){
            resources.add(graph.getResource(s)) ;
        }
        Model newGraph = getsubGraph(graph,resources,3) ;
        Graph<String,DefaultWeightedEdge> g = getGraph(newGraph) ;
        Set<String> vertices = g.vertexSet() ;
        Set<DefaultWeightedEdge> edges = g.edgeSet() ;
        log.debug("In the subgraph : "+vertices.size()+" vertices and "+edges.size()+" edges.") ;
        doGraphAlgoInfo(g) ;
        //writeDot(g,args[1],args[2]);
    }

    public static Graph<String,DefaultWeightedEdge> getGraph(Model m){ // m is a translation graph with only translatableAs statements
        Graph<String,DefaultWeightedEdge> res = new SimpleWeightedGraph<String, DefaultWeightedEdge>(DefaultWeightedEdge.class) ;
        StmtIterator stmtIter = m.listStatements() ;
        while(stmtIter.hasNext()){
            Statement stm = stmtIter.next() ;
            String source = stm.getSubject().toString() ;
            //String[] splitSource = source.split("/") ;
            //source = splitSource[splitSource.length-2]+"_"+splitSource[splitSource.length-1] ;
            res.addVertex(source) ;
            String target = stm.getResource().toString() ;
            //String[] splitTarget = target.split("/") ;
            //target = splitTarget[splitTarget.length-2]+"_"+splitTarget[splitTarget.length-1] ;
            res.addVertex(target) ;
            if(!source.equals(target)) {
                res.addEdge(source, target);
            }
        }
        return res ;
    }

    public static Model getsubGraph(Model m, List<Resource> sourceList, int depth){
        Model res = ModelFactory.createDefaultModel() ;
        Set<Resource> vertices = new HashSet<>() ;
        for(Resource source : sourceList) {
            vertices.addAll(getSubGraphVertices(m, source, depth));
        }
        for(Resource r1 : vertices){
            for(Resource r2 : vertices){
                StmtIterator stmtIter = m.listStatements(r1,VarTransOnt.translatableAs,r2) ;
                while(stmtIter.hasNext()){
                    Statement stm = stmtIter.next() ;
                    res.add(stm) ;
                }
            }
        }
        log.debug(vertices.size()+" vertices");
        log.debug(res.size()+" arcs") ;
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
            return res ;
        }
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



    // methods for senseUniformPath use
    public static Graph<String, DefaultWeightedEdge> getGraphFromDot(String fileName, int nbVertices, int nbEdges) throws IOException {
        Graph<String,DefaultWeightedEdge> g = new SimpleWeightedGraph<String, DefaultWeightedEdge>(DefaultWeightedEdge.class);
        BufferedReader buf = new BufferedReader(new FileReader(fileName));
        String s = buf.readLine();
        s = buf.readLine(); // s = premiere ligne de la liste de sommets
        for (int i = 0; i < nbVertices; i++) {
            g.addVertex(getNameFromLine(s));
            s = buf.readLine();
        }
        for (int i = 0; i < nbEdges; i++) {
            addEdgeFromLine(g,s);
            s = buf.readLine();
        }

        return g ;
    }

    private static String getNameFromLine(String line) {
        String res = "";
        int i = 0;
        while (line.charAt(i) != '"') {
            i = i + 1;
        }
        i = i + 1;
        while (line.charAt(i) != '"') {
            res = res + line.charAt(i);
            i = i + 1;
        }
        return res;
    }

    private static void addEdgeFromLine(Graph<String,DefaultWeightedEdge> g, String line) {
        String source = "";
        String target = "";
        int i = 0;
        while (line.charAt(i) != '"') {
            i = i + 1;
        }
        i = i + 1;
        while (line.charAt(i) != '"') {
            source = source + line.charAt(i);
            i = i + 1;
        }
        i = i + 1;
        while (line.charAt(i) != '"') {
            i = i + 1;
        }
        i = i + 1;
        while (line.charAt(i) != '"') {
            target = target + line.charAt(i);
            i = i + 1;
        }
        g.addEdge(source, target);
    }

    public static void doGraphAlgoInfo(Graph<String,DefaultWeightedEdge> g)throws IOException{
        //Graph<String,DefaultWeightedEdge> g  = getGraphFromDot(fileName, nbVertices, nbEdges);
        Collection<Set<String>> cliques = getCliques(g);
        Collection<Set<String>> ambigSets = getAmbiguitySets(cliques);
        System.out.println("\nAmbiguity Sets : "+seeSets(ambigSets)+"\n");
        System.out.println(getAmbiguityInfos(ambigSets));
        int ng = 2000; // TODO ng a definir
        int nr = 2000; // TODO  nr a definir
        double pe = 0.9; // TODO pe a definir
        int maxCircuitLength = 7; // TODO maxCircuitLength a definir
        for(String v : g.vertexSet()){
            Map<String, Double> prob = senseUniformPaths(g,v,ambigSets,ng,nr,pe,maxCircuitLength);
            for(String v2 : prob.keySet()){
                Double d = prob.get(v2);
                if(d>0.0){
                    System.out.println(v+"-"+v2+":"+d+"\t("+existingLink(g,v,v2)+")") ;
                }else{
                    if(existingLink(g,v,v2)){
                        System.out.println("LINK:"+v+"-"+v2+":"+d) ;
                    }
                }
            }
            System.out.println() ;
        }
    }

    private static boolean existingLink(Graph<String,DefaultWeightedEdge> g, String v1, String v2){
        return g.containsEdge(v1,v2)||g.containsEdge(v2,v1) ;
    }

    public static Collection<Set<String>> getCliques(Graph<String,DefaultWeightedEdge> translationGraph){
        BronKerboschCliqueFinder bk = new BronKerboschCliqueFinder(translationGraph) ;
        Collection<Set<String>> cliques = bk.getAllMaximalCliques() ;
        return cliques ;
    }

    public static Collection<Set<String>> getAmbiguitySets(Collection<Set<String>> cliques){
        Collection<Set<String>> ambigSets = new ArrayList<Set<String>>() ;
        ArrayList<Set<String>> cliquesList = (ArrayList<Set<String>>) cliques ;
        for(int i = 0 ; i<cliquesList.size() ; i++){
            for(int j = i+1 ; j<cliquesList.size() ; j++){
                Set<String> set = getAmbiguitySet(cliquesList.get(i),cliquesList.get(j)) ;
                if(set.size()>0 && !ambigSets.contains(set)){
                    ambigSets.add(set) ;
                }
            }
        }
        return ambigSets ;
    }

    public static String getAmbiguityInfos(Collection<Set<String>> ambiguitySets){
        // le nombre d'ambiguity set
        // le nombre de mots ambigus
        int nbSet = ambiguitySets.size() ;
        int nbAmbigEntries = getNbAmbigEntry(ambiguitySets) ;
        return "There are "+nbSet+" ambiguity sets and "+nbAmbigEntries+" ambiguous entries.\n" ;
    }

    private static int getNbAmbigEntry(Collection<Set<String>> ambiguitySets){
        Set<String> ambigEntries = new HashSet<String>() ;
        for(Set<String> ambigSet : ambiguitySets){
            ambigEntries.addAll(ambigSet) ;
        }
        return ambigEntries.size() ;
    }

    public static Set<String> getAmbigEntries(Collection<Set<String>> ambiguitySets){
        Set<String> ambigEntries = new HashSet<String>() ;
        for(Set<String> ambigSet : ambiguitySets){
            ambigEntries.addAll(ambigSet) ;
        }
        return ambigEntries ;
    }

    private static Set<String> getAmbiguitySet(Set<String> clique1, Set<String> clique2){
        Set<String> ambigSet = new HashSet<String>() ;
        for(String le : clique1){
            if(clique2.contains(le)){
                ambigSet.add(le);
            }
        }
        return ambigSet ;
    }

    private static String seeSet(Set<String> set){
        String setString = "" ;
        for(String le : set){
            setString = setString+le+" , " ;
        }
        setString = setString.substring(0,setString.length()-3) ;
        setString = "("+setString+")";
        return setString ;
    }

    public static String seeSets(Collection<Set<String>> sets){
        String setsString = "" ;
        for(Set<String> sle : sets){
            setsString = setsString+seeSet(sle)+"\n" ;
        }
        setsString = "{\n"+setsString+"}";
        return setsString ;
    }

    /*public static Map<String,Double> fixedSenseUniformPaths(Graph<String,DefaultWeightedEdge> g, Collection<Set<String>> ambiguitySets, int ng, int nr, double pe, int maxCircuitLength, int index){
        Set<DefaultWeightedEdge> deSet = g.edgeSet() ;
        Object[] edgesTable = deSet.toArray();
        DefaultWeightedEdge de = (DefaultWeightedEdge)edgesTable[index] ;
        String v1 = g.getEdgeSource(de) ;
        String v2 = g.getEdgeTarget(de) ;
        System.out.println("Translation probabilities of "+v1+" and "+v2) ;
        return senseUniformPaths(g,v1,v2,ambiguitySets,ng,nr,pe,maxCircuitLength) ;
    }*/

    /*public static Map<String,Double> randomSenseUniformPaths(Graph<String,DefaultWeightedEdge> g,Collection<Set<String>> ambiguitySets, int ng, int nr, double pe, int maxCircuitLength){
        Set<DefaultWeightedEdge> deSet = g.edgeSet() ;
        int nbEdges = deSet.size() ;
        Object[] edgesTable = deSet.toArray();
        int r = (new Random()).nextInt(nbEdges) ;
        DefaultWeightedEdge de = (DefaultWeightedEdge)edgesTable[r] ;
        String v1 = g.getEdgeSource(de) ;
        String v2 = g.getEdgeTarget(de) ;
        System.out.println("Translation probabilities of "+v1+" and "+v2) ;
        return senseUniformPaths(g,v1,v2,ambiguitySets,ng,nr,pe,maxCircuitLength) ;
    }*/

    //public

    /**
     * Algorithm SenseUniformPaths from "Compiling a Massive, Multilingual Dictionary via Probabilistic inference"
     */
    public static Map<String,Double> senseUniformPaths(Graph<String,DefaultWeightedEdge> g, String v1, Collection<Set<String>> ambiguitySets, int ng, int nr, double pe, int maxCircuitLength){
    /*int ng = 2000 ; // TODO ng a definir
    int nr = 2000 ; // TODO  nr a definir
    double pe = 0.6 ; // TODO pe a definir
    int maxCircuitLength = 7 ; // TODO maxCircuitLength a definir
    */

        Map<String,int[]> rp = new HashMap<>() ;

        Set<String> allVertices = g.vertexSet() ;
        for(String v : allVertices) {
            // rp[v][i] = 0
            int[] prob = new int[ng] ;
            for(int i = 0 ; i<ng ; i++){
                prob[i] = 0 ;
            }
            rp.put(v,prob) ;
        }

        for(int i = 0 ; i<ng ; i++){
            // creation d'un "sample graph" (ng fois)
            Graph<String,DefaultWeightedEdge> sampleGraph = sampleGraph(g,pe) ;

            Set<String> translationCircuits = new HashSet<String>() ;
            for(int j = 0 ; j<nr ; j++){
                translationCircuits.addAll(randomWalk(sampleGraph,v1,ambiguitySets,maxCircuitLength)) ;
            }

            Set<String> allVertex = sampleGraph.vertexSet() ;
            for(String v : allVertex){
                if(translationCircuits.contains(v)){
                    // rp[v][i] = 1
                    int[] tab = rp.get(v) ;
                    tab[i] = 1 ;
                    rp.put(v,tab) ;
                }
            }
        }
        // probabilite que v soit une traduction de v1 et v2 : somme(rp[v][i])/ng
        Map<String,Double> translationProbability = new HashMap<String,Double>() ;
        for(String v : allVertices) {
            //System.out.print(v+" : ") ;
            int[] tab = rp.get(v) ;
            int sum = 0 ;
            for(int i = 0 ; i<ng ; i++){
                //System.out.print(tab[i]) ;
                sum = sum+tab[i] ;
            }
            //System.out.println() ;
            double prob = ((double)sum)/((double)ng) ;
            translationProbability.put(v,prob) ;
        }
        return translationProbability ;
    }

    private static Graph<String,DefaultWeightedEdge> sampleGraph(Graph<String,DefaultWeightedEdge> g, double pe){
        Graph<String,DefaultWeightedEdge> sample = new SimpleWeightedGraph<String, DefaultWeightedEdge>(DefaultWeightedEdge.class) ;
        Set<String> vertices = g.vertexSet() ;
        for(String le : vertices) {
            sample.addVertex(le);
        }
        Set<DefaultWeightedEdge> edges = g.edgeSet() ;
        for(DefaultWeightedEdge de : edges){
            double d = Math.random() ;
            if(d<pe){
                sample.addEdge(g.getEdgeSource(de), g.getEdgeTarget(de));
            }
        }
        return sample ;
    }

    /*
        g has an edge connecting v1 to v2
     */
    /*private static Set<String> randomWalk(Graph<String,DefaultWeightedEdge> g, String v1, String v2, Collection<Set<String>> ambiguitySets, int maxCircuitLength){
        int ambiguousVertices = 0 ;
        Set<String> translationCircuit = new HashSet<String>() ;
        translationCircuit.add(v1) ;
        translationCircuit.add(v2) ;
        String neighbor = getRandomNeighborNotPicked(g, v1,translationCircuit);
        if(neighbor==null){
            return new HashSet<String>() ;
        }
        if(isAmbiguous(neighbor,ambiguitySets)){
            ambiguousVertices = ambiguousVertices+1 ;
        }
        translationCircuit.add(neighbor) ;
        int i = 0 ;
        while(ambiguousVertices<2 && i<maxCircuitLength && !g.containsEdge(neighbor,v2) && !g.containsEdge(v2,neighbor)) {
            neighbor = getRandomNeighborNotPicked(g,neighbor,translationCircuit) ;
            if(neighbor==null){
                return new HashSet<String>() ;
            }
            if(isAmbiguous(neighbor,ambiguitySets)){
                ambiguousVertices = ambiguousVertices+1 ;
            }
            translationCircuit.add(neighbor) ;
            i = i+1 ;
        }
        if(i==maxCircuitLength || ambiguousVertices>=2){
            return new HashSet<String>() ;
        }else{
            return translationCircuit ;
        }
    }*/


    private static Set<String> randomWalk(Graph<String,DefaultWeightedEdge> g, String v1, Collection<Set<String>> ambiguitySets, int maxCircuitLength){
        int ambiguousVertices = 0 ;
        Set<String> translationCircuit = new HashSet<String>() ;
        translationCircuit.add(v1) ;
        String neighbor = getRandomNeighborNotPicked(g, v1,translationCircuit);
        if(neighbor==null){
            return new HashSet<String>() ;
        }
        if(isAmbiguous(neighbor,ambiguitySets)){
            ambiguousVertices = ambiguousVertices+1 ;
        }
        translationCircuit.add(neighbor) ;

        neighbor = getRandomNeighborNotPicked(g,neighbor,translationCircuit) ;
        if(neighbor==null){
            return new HashSet<String>() ;
        }
        if(isAmbiguous(neighbor,ambiguitySets)){
            ambiguousVertices = ambiguousVertices+1 ;
        }
        translationCircuit.add(neighbor) ;

        int i = 2 ;
        while((ambiguousVertices<2 || allinSameAmbigSet(translationCircuit,ambiguitySets)) && i<maxCircuitLength && !g.containsEdge(neighbor,v1) && !g.containsEdge(v1,neighbor)) {
            neighbor = getRandomNeighborNotPicked(g,neighbor,translationCircuit) ;
            if(neighbor==null){
                return new HashSet<String>() ;
            }
            if(isAmbiguous(neighbor,ambiguitySets)){
                ambiguousVertices = ambiguousVertices+1 ;
            }
            translationCircuit.add(neighbor) ;
            i = i+1 ;
        }
        if(i==maxCircuitLength || ambiguousVertices>=2){
            return new HashSet<String>() ;
        }else{
            return translationCircuit ;
        }
    }

    private static boolean allinSameAmbigSet(Set<String> vertices, Collection<Set<String>> ambiguitySets){
        for(Set<String> ambiguitySet : ambiguitySets){
            boolean res = true ;
            for(String v : vertices){
                res = res&&ambiguitySet.contains(v) ;
            }
            if (res){return res;}
        }
        return false ;
    }

    private static String getRandomNeighbor(Graph<String,DefaultWeightedEdge> g,String v){
        Set<DefaultWeightedEdge> deSet = g.edgesOf(v) ;
        int i = 0 ;
        int size = deSet.size() ;
        if(size>0) {
            int r = (new Random()).nextInt(size);
            for (DefaultWeightedEdge de : deSet) {
                if (i == r) {
                    if (g.getEdgeSource(de).equals(v)) {
                        return g.getEdgeTarget(de);
                    } else {
                        return g.getEdgeSource(de);
                    }
                }
                i = i + 1;
            }
        }
        return null ;
    }

    private static String getRandomNeighborNotPicked(Graph<String,DefaultWeightedEdge> g,String v, Set<String> picked){
        String neighbor = getRandomNeighbor(g,v);
        if(neighbor == null){
            return null ;
        }
        if(picked.containsAll(getNeighbors(g,v))){
            return null ;
        }else{
            while(picked.contains(neighbor)){
                neighbor = getRandomNeighbor(g,v) ;
            }
            return neighbor ;
        }
    }

    private static Set<String> getNeighbors(Graph<String,DefaultWeightedEdge> g,String v){
        Set<String> neighbors = new HashSet<String>() ;
        Set<DefaultWeightedEdge> deSet = g.edgesOf(v) ;
        for(DefaultWeightedEdge de : deSet){
            if(g.getEdgeSource(de).equals(v)){
                neighbors.add(g.getEdgeTarget(de)) ;
            }else{
                neighbors.add(g.getEdgeSource(de)) ;
            }
        }
        return neighbors ;
    }

    private static boolean isAmbiguous(String v, Collection<Set<String>> ambiguitySets){
        for(Set<String> ambiguitySet : ambiguitySets){
            if(ambiguitySet.contains(v)){
                return true ;
            }
        }
        return false ;
    }

}

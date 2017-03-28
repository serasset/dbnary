package org.getalp.dbnary.experiment;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.tdb.TDBFactory;
import org.getalp.dbnary.VarTransOnt;
import org.jgrapht.Graph;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.WeightedGraph;
import org.jgrapht.alg.BronKerboschCliqueFinder;
import org.jgrapht.alg.StoerWagnerMinimumCut;
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

        List<String> resourcesString = new ArrayList<>() ;
        resourcesString.add("http://kaiko.getalp.org/dbnary/eng/spring__Noun__1") ;
        List<Resource> resources = new ArrayList<>() ;
        for(String s : resourcesString){
            resources.add(graph.getResource(s)) ;
        }
        log.debug("Creating subgraph...") ;
        Model newGraph = getsubGraph(graph,resources,1) ;
        Graph<String,DefaultWeightedEdge> g = getGraph(newGraph) ;
        Set<String> vertices = g.vertexSet() ;
        Set<DefaultWeightedEdge> edges = g.edgeSet() ;
        log.debug("In the subgraph : "+vertices.size()+" vertices and "+edges.size()+" edges.") ;
        log.debug("Getting probas...") ;
        Map<String,Map<String,Double>> probas = getProbas(g) ;
        System.out.println(seeProbas(probas)) ;
        log.debug("Getting weighted graph...") ;
        SimpleWeightedGraph<String,DefaultWeightedEdge> wg = getWeightedGraph(probas) ;
        System.out.println(seeWeightedGraph(wg)) ;

        log.debug("Getting clusters...") ;
        Set<Set<String>> minCut = minCutClustering(wg,2) ;
        for(Set<String> cluster : minCut){
            System.out.print("{ ") ;
            for(String v : cluster){
                System.out.print(v+" ") ;
            }
            System.out.println("}") ;
        }
        //writeDot(g,args[1],args[2]);

        //testMinCutClustering(1000,0.3,6);
    }

    public static String seeWeightedGraph(SimpleWeightedGraph<String,DefaultWeightedEdge> g){
        String res = "" ;
        for(DefaultWeightedEdge e : g.edgeSet()){
            res = res+g.getEdgeSource(e)+" - "+g.getEdgeTarget(e)+" : "+g.getEdgeWeight(e)+"\n" ;
        }
        return res ;
    }

    public static void testMinCutClustering(int size,double probGetEdge, int depth){
        //int size = 1000 ;
        //double probGetEdge = 0.3 ;

        Random r = new Random() ;

        SimpleWeightedGraph<String,DefaultWeightedEdge> g = new SimpleWeightedGraph<String, DefaultWeightedEdge>(DefaultWeightedEdge.class) ;
        for(int i = 0 ; i<size ; i++){
            g.addVertex("V"+i) ;
        }
        for(int i = 0 ; i<size ; i++){
            for(int j = i+1 ; j<size ; j++){
                double d = Math.random() ;
                if(d<=probGetEdge){
                    DefaultWeightedEdge e = g.addEdge("V"+i,"V"+j) ;
                    double p = (r.nextGaussian()/2)+0.5 ;
                    while(p<0.0 || p>1.0){
                        p = (r.nextGaussian()/2)+0.5 ;
                    }
                    p = roundProba(p) ;
                    g.setEdgeWeight(e,p);
                }
            }
        }

        /*g.addVertex("v1") ;
        g.addVertex("v2") ;
        g.addVertex("v3") ;
        g.addVertex("v4") ;
        g.addVertex("v5") ;
        g.addVertex("v6") ;
        g.addVertex("v7") ;
        DefaultWeightedEdge ew = g.addEdge("v1","v2");
        g.setEdgeWeight(ew,0.1);
        ew = g.addEdge("v1","v3");
        g.setEdgeWeight(ew,0.1);
        ew = g.addEdge("v1","v6");
        g.setEdgeWeight(ew,0.1);
        ew = g.addEdge("v1","v7");
        g.setEdgeWeight(ew,0.7);
        ew = g.addEdge("v2","v3");
        g.setEdgeWeight(ew,0.3);
        ew = g.addEdge("v2","v4");
        g.setEdgeWeight(ew,0.2);
        ew = g.addEdge("v3","v4");
        g.setEdgeWeight(ew,0.2);
        ew = g.addEdge("v3","v6");
        g.setEdgeWeight(ew,0.1);
        ew = g.addEdge("v4","v5");
        g.setEdgeWeight(ew,0.1);
        ew = g.addEdge("v5","v6");
        g.setEdgeWeight(ew,0.7);
        ew = g.addEdge("v6","v7");
        g.setEdgeWeight(ew,0.3);*/


        //int depth = 5 ;

        for(DefaultWeightedEdge e : g.edgeSet()){
            System.out.println(g.getEdgeSource(e)+" - "+g.getEdgeTarget(e)+" : "+g.getEdgeWeight(e)) ;
        }
        System.out.println() ;

        Set<Set<String>> minCut = minCutClustering(g,depth) ;
        for(Set<String> cluster : minCut){
            System.out.print("{ ") ;
            for(String v : cluster){
                System.out.print(v+" ") ;
            }
            System.out.println("}") ;
        }
    }

    private static double roundProba(double p){
        if(p<0.1){
            return 0.0 ;
        }else if(p<0.2){
            return 0.1 ;
        }else if(p<0.3){
            return 0.2 ;
        }else if(p<0.4){
            return 0.3 ;
        }else if(p<0.5){
            return 0.4 ;
        }else if(p<0.6){
            return 0.5 ;
        }else if(p<0.7){
            return 0.6 ;
        }else if(p<0.8){
            return 0.7 ;
        }else if(p<0.9){
            return 0.8 ;
        }else{
            return 0.9 ;
        }
    }

    public static Set<Set<String>> minCutClustering(SimpleWeightedGraph<String,DefaultWeightedEdge> g, int depth){
        Set<Set<String>> res = new HashSet<>() ;
        if(depth==0 || g.vertexSet().size()<=1){
            res.add(g.vertexSet()) ;
            return res ;
        }
        StoerWagnerMinimumCut mc = new StoerWagnerMinimumCut(g) ;
        Set<String> oneSideVertices = mc.minCut() ;
        SimpleWeightedGraph<String,DefaultWeightedEdge> g1 = getCutGraph(g,oneSideVertices) ;
        SimpleWeightedGraph<String,DefaultWeightedEdge> g2 = getSecondCutGraph(g,oneSideVertices) ;
        res.addAll(minCutClustering(g1,depth-1)) ;
        res.addAll(minCutClustering(g2,depth-1)) ;
        return res ;
    }

    private static SimpleWeightedGraph<String,DefaultWeightedEdge> getCutGraph(SimpleWeightedGraph<String,DefaultWeightedEdge> g, Set<String> oneSideVertices){
        // TODO simplify so we do not compute twice the edges
        SimpleWeightedGraph<String,DefaultWeightedEdge> subGraph = new SimpleWeightedGraph<String, DefaultWeightedEdge>(DefaultWeightedEdge.class) ;
        for(String v1 : oneSideVertices){
            subGraph.addVertex(v1) ;
            for(String v2 : oneSideVertices){
                subGraph.addVertex(v2) ;
                if(g.containsEdge(v1,v2) && !subGraph.containsEdge(v1,v2)){
                    Double d = g.getEdgeWeight(g.getEdge(v1,v2)) ;
                    DefaultWeightedEdge e = subGraph.addEdge(v1,v2) ;
                    subGraph.setEdgeWeight(e, d);
                }
            }
        }
        return subGraph ;
    }

    private static SimpleWeightedGraph<String,DefaultWeightedEdge> getSecondCutGraph(SimpleWeightedGraph<String,DefaultWeightedEdge> g, Set<String> oneSideVertices){
        Set<String> otherSideVertices = new HashSet<>() ;
        for(String v : g.vertexSet()){
            if(!oneSideVertices.contains(v)){
                otherSideVertices.add(v) ;
            }
        }
        return getCutGraph(g,otherSideVertices) ;
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

    public static Map<String,Map<String,Double>> getProbas(Graph<String,DefaultWeightedEdge> g)throws IOException{
        //Graph<String,DefaultWeightedEdge> g  = getGraphFromDot(fileName, nbVertices, nbEdges);
        Map<String,Map<String,Double>> resProba = new HashMap<>() ;
        Collection<Set<String>> cliques = getCliques(g);
        Collection<Set<String>> ambigSets = getAmbiguitySets(cliques);
        //System.out.println("\nAmbiguity Sets : "+seeSets(ambigSets)+"\n");
        //System.out.println(getAmbiguityInfos(ambigSets));
        int ng = 2000; // TODO ng a definir
        int nr = 2000; // TODO  nr a definir
        double pe = 0.9; // TODO pe a definir
        int maxCircuitLength = 7; // TODO maxCircuitLength a definir
        int nbdone = 0 ;
        for(String v : g.vertexSet()){
            Map<String, Double> prob = senseUniformPaths(g,v,ambigSets,ng,nr,pe,maxCircuitLength);
            Map<String,Double> subRes = new HashMap<>() ;
            for(String v2 : prob.keySet()){
                Double d = prob.get(v2);
                if(d>0.0){
                    subRes.put(v2,d) ;
                    //System.out.println(v+"-"+v2+":"+d+"\t("+existingLink(g,v,v2)+")") ;
                }else{
                    if(existingLink(g,v,v2)){
                        subRes.put(v2,d) ;
                        //System.out.println("LINK:"+v+"-"+v2+":"+d) ;
                    }
                }
            }
            resProba.put(v,subRes) ;
            //System.out.println() ;
            nbdone = nbdone+1 ;
            log.debug(nbdone+" vertices done") ;
        }
        return resProba ;
    }

    public static SimpleWeightedGraph<String,DefaultWeightedEdge> getWeightedGraph(Map<String,Map<String,Double>> probas){
        SimpleWeightedGraph<String,DefaultWeightedEdge> weightedGraph = new SimpleWeightedGraph<String, DefaultWeightedEdge>(DefaultWeightedEdge.class) ;
        for(String source : probas.keySet()){
            weightedGraph.addVertex(source) ;
            for(String target : probas.get(source).keySet()){
                weightedGraph.addVertex(target) ;
                DefaultWeightedEdge e = weightedGraph.addEdge(source,target) ;
                if(e!=null){
                    double d1 = probas.get(source).get(target) ;
                    double d2 = probas.get(target).get(source) ;
                    double d = (d1+d2)/2 ;
                    weightedGraph.setEdgeWeight(e,d) ;
                }

            }
        }
        return weightedGraph ;
    }

    public static String seeProbas(Map<String,Map<String,Double>> probas){
        String disp = "" ;
        for(String source : probas.keySet()){
            for(String target : probas.get(source).keySet()){
                disp=disp+source+" - "+target+" : "+probas.get(source).get(target)+"\n" ;
            }
        }
        return disp ;
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

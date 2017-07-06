package org.getalp.dbnary.experiment;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.tdb.*;
import com.hp.hpl.jena.query.*;
import org.getalp.dbnary.DBnaryOnt;
import org.getalp.dbnary.VarTransOnt;

import java.io.File;
import java.util.*;

public class DetectHomonym {
    Model model ;
    Map<String,Model> initialModels ;

    private class CountHomonym{
        private Map<String,Integer> nbOccur ;

        public CountHomonym(){
            nbOccur = new HashMap<String,Integer>() ;
        }

        public void add(String key){
            Integer i = nbOccur.get(key) ;
            if(i != null){
                int j = i.intValue() ;
                j = j+1 ;
                nbOccur.put(key,new Integer(j));
            }else{
                nbOccur.put(key,new Integer(1));
            }
        }

        public int get(String key){
            return nbOccur.get(key).intValue() ;
        }

        public Set<String> keySet(){
            return nbOccur.keySet() ;
        }
    }

    private class MapVocableEntries{
        private Map<String,List<Resource>> map ;

        public MapVocableEntries(){
            map = new HashMap<String,List<Resource>>() ;
        }

        public void add(Resource entry){
            Resource vocable = getVocable(entry) ;
            List<Resource> l = map.get(vocable.toString()) ;
            if(l != null){
                l.add(entry) ;
                map.put(vocable.toString(),l) ;
            }else{
                List<Resource> list = new ArrayList<Resource>() ;
                list.add(entry) ;
                map.put(vocable.toString(),list) ;
            }
        }

        public List<Resource> get(String vocable){
            return map.get(vocable) ;
        }

        public Set<String> keySet(){
            return map.keySet() ;
        }
    }

    public DetectHomonym(String directory, String directory2){
        Dataset dataset = TDBFactory.createDataset(directory);
        dataset.begin(ReadWrite.READ) ;
        model = dataset.getDefaultModel() ;

        initialModels = new HashMap<String,Model>() ;

        String[] directories = new File(directory2).list();
        for(int i = 0 ; i<directories.length ; i++){
            if(directories[i].length()==3) {
                Dataset dat = TDBFactory.createDataset(directory2 + "/" + directories[i]);
                dat.begin(ReadWrite.READ);
                Model m = dat.getDefaultModel();
                initialModels.put(directories[i], m);
            }
        }
    }

    public boolean areHomonyms(Resource LexEntry1, Resource LexEntry2){
        String[] split1 = LexEntry1.toString().split("__");
        String str1 = split1[0]+"__"+split1[1] ;
        String[] split2 = LexEntry2.toString().split("__");
        String str2 = split2[0]+"__"+split2[1] ;
        return str1.equals(str2) ;
    }

    public Resource getVocable(Resource le){
        String lang = getLanguage(le) ;
        Model m = initialModels.get(lang) ;
        StmtIterator stmIter = m.listStatements(null,DBnaryOnt.describes,le);
        while(stmIter.hasNext()){
            Statement stm = stmIter.next() ;
            Resource voc = stm.getSubject() ;
            if(voc.hasProperty(RDF.type,DBnaryOnt.Page)){
                return voc ;
            }
        }
        return null ;
    }

    public String getVocable(String le){
        String lang = getLanguage(le) ;
        Model m = initialModels.get(lang) ;
        StmtIterator stmIter = m.listStatements(null,DBnaryOnt.describes,le);
        while(stmIter.hasNext()){
            Statement stm = stmIter.next() ;
            Resource voc = stm.getSubject() ;
            if(voc.hasProperty(RDF.type,DBnaryOnt.Page)){
                return voc.toString() ;
            }
        }
        return null ;
    }

    private String getLanguage(Resource r){
        String[] split = r.toString().split("/") ;
        return split[4] ;
    }

    private String getLanguage(String s){
        String[] split = s.split("/") ;
        return split[4] ;
    }

    private boolean isLexicalEntry(Resource r){
        //System.out.println(r.toString()) ;
        //System.out.println(r.toString().charAt(35)) ;
        return r.toString().charAt(35) != '_' ;
    }

    public void iterateAndCount(){
        int[] nbEntity = new int[126406] ;
        int sum = 0 ;
        int i = 0 ;
        int nbHomonym = 0 ;
        int nbTotal = 0 ;
        ResIterator resIter = model.listSubjects() ;
        while(resIter.hasNext()){
            CountHomonym ch = new CountHomonym();
            Resource source = resIter.nextResource() ;
            StmtIterator stm = source.listProperties(VarTransOnt.translatableAs) ;
            while(stm.hasNext()) {
                Statement s = stm.next();
                Resource r = s.getResource();
                String str = r.toString();
                String[] split = str.split("__");
                String target = split[0] + "__" + split[1];
                System.out.println(str+"\t->\t"+target) ;
                ch.add(target);
            }
            Set<String> set = ch.keySet();
            int nh = 0 ;
            int nt = 0 ;
            int nbH = 0 ;
            for(String targetEntry : set){
                if(ch.get(targetEntry)>1){
                    nh = nh+1 ;
                    nbH = ch.get(targetEntry);
                    nbEntity[i] = nbH;
                    sum = sum + nbH;
                    i = i + 1;
                    nbHomonym = nbHomonym+1 ;
                }
                nt = nt+1 ;
                nbTotal = nbTotal + 1 ;
            }
            System.out.println(source.toString()+"\t"+nt+"\tdont\t"+nh+"\thomonyms") ;
        }
        System.out.println(nbTotal+"\tlinks to words\t"+nbHomonym+"\thomonyms") ;
        for(int j =0 ; j<nbEntity.length ; j++){
            System.out.print(nbEntity[j]+" ") ;
        }
        System.out.println();
        System.out.println(sum+"/"+nbHomonym) ;
    }

    public void processHomonyms(){
        ResIterator resIter = model.listSubjects() ;
        while(resIter.hasNext()){
            MapVocableEntries mve = new MapVocableEntries();
            Resource source = resIter.nextResource() ;
            if(isLexicalEntry(source)) {
                StmtIterator stm = source.listProperties(VarTransOnt.translatableAs);
                while (stm.hasNext()) {
                    Statement s = stm.next();
                    Resource target = s.getResource();
                    mve.add(target);
                }

                Set<String> set = mve.keySet();
                for (String targetVocable : set) {
                    List<Resource> targets = mve.get(targetVocable);
                    if (targets.size() > 1) {
                        boolean gotCorrectLink = false ;
                        int nbCorrectLinks = 0 ;
                        for (int i = 0; i < targets.size(); i++) {
                            Resource target = targets.get(i);
                            if (target.hasProperty(VarTransOnt.translatableAs, source)) {
                                System.out.println("Correct link : " + source.getLocalName() + "\t->\t" + target.getLocalName());
                                gotCorrectLink = true ;
                                nbCorrectLinks = nbCorrectLinks+1 ;
                            } else {
                                if (target.hasProperty(VarTransOnt.translatableAs)) {
                                    System.out.println("Probably Incorrect link : " + source.getLocalName() + "\t->\t" + target.getLocalName());
                                } else {
                                    System.out.println("Questionable link : " + source.getLocalName() + "\t->\t" + target.getLocalName());
                                }
                            }
                        }
                        System.out.println("From "+source.getLocalName()+" to vocable "+targetVocable+" got a correct link :\t"+gotCorrectLink+" ("+nbCorrectLinks+")") ;
                    }else {
                        System.out.println("No homonym from " + source.getLocalName() + " to vocable " + targetVocable);
                    }
                }
            }
        }
    }

    public int countLinks(){
        int n = 0 ;
        StmtIterator stmiter = model.listStatements();
        while(stmiter.hasNext()){
            Statement stm = stmiter.next() ;
            n = n+1 ;
        }
        return n ;
    }

    public int countLinksFromLE(){
        int nle = 0 ;
        StmtIterator stmiter = model.listStatements();
        while(stmiter.hasNext()){
            Statement stm = stmiter.next() ;
            if(isLexicalEntry(stm.getSubject())){
                nle = nle+1 ;
            }
        }
        return nle ;
    }

    public int countDoubleLinks(){
        int n = 0 ;
        StmtIterator stmtIter = model.listStatements();
        while(stmtIter.hasNext()){
            Statement stm = stmtIter.next() ;
            if(stm.getResource().hasProperty(VarTransOnt.translatableAs,stm.getSubject())){
                n = n+1 ;
            }
        }
        return  n ;
    }

    public int countNodes(){
        int n = 0 ;
        ResIterator resIter = model.listSubjects() ;
        while(resIter.hasNext()) {
            Resource source = resIter.nextResource();
            n = n+1 ;
        }
        return n ;
    }

    public int countNodesLE(){
        int n = 0 ;
        ResIterator resIter = model.listSubjects() ;
        while(resIter.hasNext()) {
            Resource source = resIter.nextResource();
            if(isLexicalEntry(source)){
                n = n+1 ;
            }
        }
        return n ;
    }

    public static void main(String[] args){
        DetectHomonym dh = new DetectHomonym(args[0],args[1]);
        dh.processHomonyms();
        //dh.iterateAndCount();
        /*System.out.println("Number of links : "+dh.countLinks()) ;
        System.out.println("Number of links from a Lexical Entry: "+dh.countLinksFromLE()) ;
        System.out.println("Number of nodes : "+dh.countNodes()) ;
        System.out.println("Number of nodes LE : "+dh.countNodesLE()) ;
        System.out.println("Number of \"double\" links "+dh.countDoubleLinks()) ;
        */
    }
}

package org.getalp.dbnary.experiment;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.tdb.*;
import com.hp.hpl.jena.query.*;
import org.getalp.dbnary.DBnaryOnt;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DetectHomonym {
    Model model ;

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

    public DetectHomonym(String directory){
        Dataset dataset = TDBFactory.createDataset(directory);
        dataset.begin(ReadWrite.READ) ;
        model = dataset.getDefaultModel() ;
    }

    public void iterateAndCount(){
        int nbHomonym = 0 ;
        int nbTotal = 0 ;
        ResIterator resIter = model.listSubjects() ;
        while(resIter.hasNext()){
            CountHomonym ch = new CountHomonym();
            Resource source = resIter.nextResource() ;
            StmtIterator stm = source.listProperties(DBnaryOnt.isTranslationOf) ;
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
            for(String targetEntry : set){
                if(ch.get(targetEntry)>1){
                    nh = nh+1 ;
                    nbHomonym = nbHomonym+1 ;
                }
                nt = nt+1 ;
                nbTotal = nbTotal + 1 ;
            }
            System.out.println(source.toString()+"\t"+nt+"\tdont\t"+nh+"\thomonyms") ;
        }
        System.out.println(nbTotal+"\tlinks to words\t"+nbHomonym+"\thomonyms") ;
    }

    public static void main(String[] args){
        DetectHomonym dh = new DetectHomonym(args[0]);
        dh.iterateAndCount();
    }
}

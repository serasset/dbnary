/**
 *
 */
package org.getalp.dbnary.eng;

import org.getalp.dbnary.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author pantaleo
 */

/**
 * An ArrayList of POE-s, Parts of Etymology
 */
public class ArrayListPOE extends ArrayList<POE> {
    static Logger log = LoggerFactory.getLogger(ArrayListPOE.class);

    /**
     * This function is used to replace "COMPOUND_OF LEMMA AND LEMMA" and equivalents
     * with a single "LEMMA"
     * Given a pair of indexes it replaces whatever subarray of ArrayListPOE
     * with a "LEMMA" POE of type compound|lang|word1|word2
     *
     * @param positions an ArrayList&lt;Pair&gt; specifying the start and end indexes of a subarray of ArrayListPOE
     */
    public void replaceMatch(ArrayList<Pair> positions) {
	if (positions == null || positions.size() == 0){
	    return;
	}
        StringBuilder string = new StringBuilder();
        //iterate over all matches to a compound pattern
        //starting from the last
        for (int i = positions.size() - 1; i >= 0; i--) {
            int start = positions.get(i).start, end = positions.get(i).end;
            int counter = 0;
            string.setLength(0);//clear StringBuilder

            for (int k = start; k < end + 1; k++) {
                POE poe = this.get(k);
                for (int j = 0; j < poe.part.size(); j++) {
                    if (poe.part.get(j).equals("LEMMA")) {
                        if (counter == 0) {//set template as compound
                            string.append("compound|");
                            if (poe.args.get("lang") != null) {
                                string.append(poe.args.get("lang"));
                            } else {
                                log.debug("no language in {}", poe.args);
                            }
                            string.append("|");
                            string.append(poe.args.get("word1"));
                            string.append("|");
                            counter = 1;
                            break;
                        } else {//push words back in compound template
                            if (poe.args != null) {
                                string.append(poe.args.get("word1"));
                            }
                            POE p = new POE(string.toString());
                            this.set(start, p);
                            if (end > start) {
                                this.subList(start + 1, end + 1).clear();
                            }
			    k = end + 1;
                        }
                    }
                }
            }
        }
        /*int j;         
        for (int i=0; i<matches.size(); i++){                   
            j = matches.get(matches.size()-i-2);                                   
            System.out.format("%s\n", a.get(j).args.get("1"));        
            if (j == matches.get(matches.size()-i-1)){       
                System.out.format("the match has length 1, so cannot be compressed anymore; leave everything as it is only replace with LEMMA??");                   
            } else if (a.get(j).args.get("1").equals("compound")){   
                System.out.format("use COMPOUND template and compress to LEMMA");     
            } else if (a.get(j).args.get("1").equals("etycomp")){//All parameters except word1= can be omitted.                                                         
                System.out.format("use ETYCOMP template and compress to LEMMA");//{{etycomp|lang1=de|inf1=|case1=|word1=dumm|trans1=dumb|lang2=|inf2=|case2=|word2=Kopf|trans2=head}}                   
          } else if (a.get(j).args.get("1").equals("calque")){        
                System.out.format("use CALQUE template and compress to LEMMA");
            } else if (a.get(j).args.get("1").equals("blend")){                                        
                System.out.format("use BLEND template and compress to LEMMA");                                                                   
            } else {                                                                                                                 
                System.out.format("replace with a brand new COMPOUND template and compress to LEMMA");  
            }                                                                                                  
            System.out.format("\n");                                                                           
            i++;
	    }*/
    }

    public ArrayList<Pair> match(Pattern p) {
	ArrayList<Pair> toreturn = new ArrayList<Pair>();
        if (this == null || this.size() == 0){
	    return toreturn;
	}
        ArrayList<Integer> arrayListInteger = new ArrayList<Integer>();
        ArrayList<Integer> arrayListPosition = new ArrayList<Integer>();
        int c = 0;

        arrayListPosition.add(c);
        for (int i = 0; i < this.size(); i++) {
            POE poe = this.get(i);
            for (int j = 0; j < poe.part.size(); j++) {
                arrayListInteger.add(i);
                c += poe.part.get(j).length() + 1;
                arrayListPosition.add(c);
            }
        }
        Matcher m = p.matcher(this.toString());

        while (m.find()) {
            int start = -1, end = -1;
            for (int i = 0; i < arrayListPosition.size() - 1; i++) {
                if (arrayListPosition.get(i) == m.start()) {
                    start = arrayListInteger.get(i);
                } else if (arrayListPosition.get(i + 1) == m.end()) {
                    end = arrayListInteger.get(i);
                }
            }
            if (start < 0 || end < 0) {
                System.out.format("Error: start or end of match are not available\n");
            }
            toreturn.add(new Pair(start, end));
        }
        return toreturn;
    }

    @Override
    /**
     * @return a String like "FROM LEMMA OR LEMMA" concatenating the property "part" of each element of ArrayListPOE
     */
    public String toString() {
        StringBuilder s = new StringBuilder();

        for (int i = 0; i < this.size(); i++) {
            for (int j = 0; j < this.get(i).part.size(); j++) {
                s.append(this.get(i).part.get(j));
                if (!(this.get(i).part.equals("ERROR"))) {
                    s.append(" ");
                }
            }
        }
        return s.toString();
    }

}

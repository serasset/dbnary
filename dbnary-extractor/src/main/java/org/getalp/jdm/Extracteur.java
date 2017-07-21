package org.getalp.jdm;

import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Extracteur {

	public static void main(String[] args){
		//DECLARATIONS
		Pattern pattern;
		Matcher matcher;
		HashMap<String,Noeud> hmn = new HashMap<String, Noeud>();
		HashMap<String,Property> hmp = new HashMap<String, Property>();
		//CREATION MODEL
		Model model = ModelFactory.createDefaultModel();
		
		model.setNsPrefix("jdm_mot", "http://kaiko.getalp.org/jdm/");
		model.setNsPrefix("jdm_property", "http://kaiko.getalp.org/jdm#");
		
		Property poids = model.createProperty("http://kaiko.getalp.org/jdm#poids");
		Property chaine = model.createProperty("http://kaiko.getalp.org/jdm#chaine");
		Property nf = model.createProperty("http://kaiko.getalp.org/jdm#nf");
		Property type = model.createProperty("http://kaiko.getalp.org/jdm#type");
		
		
		try {
			Scanner scanner = new Scanner(new FileReader(args [0]));

			int nbr = 0;
			int nbn = 0;
			while(scanner.hasNextLine()){
				String line = scanner.nextLine();
				//FILTRAGE DE LA LECTURE
				pattern = Pattern.compile ("^(eid)=([0-9]+).n=\"([^0-9><]+)(?:>[0-9]+)?\".*t=([0-9]+).w=([0-9]+)(?:.*>([^0-9]*)\")?|^(rid)=([0-9]+).n1=([0-9]+).n2=([0-9]+).*t=([0-9]+).w=([0-9]+)|^(rtid)=([0-9]*).name=\"(.*)\".nom_etendu=\"(.*)\".info=\"(.*)\"");
				matcher = pattern.matcher(line);
				if(matcher.find()){
					//if(matcher.group(3) != null) System.out.println("mot : " + matcher.group(3));
					if(matcher.group(5) != null ) {
						//CREATION D'UN NOEUD
						hmn.put(matcher.group(2),new Noeud(matcher.group(2),matcher.group(3),Integer.parseInt(matcher.group(5)),matcher.group(6),Integer.parseInt(matcher.group(4))));
					    nbn = nbn + 1;
					    System.out.print("Node : " + nbn + "\r");
					} else if(matcher.group(7) != null) {
                        //LECTURE D'UNE RELATION
                        if (hmn.containsKey(matcher.group(9)) && hmn.containsKey(matcher.group(10))) {

                            //CREATION RESSOURCE 1
                            Resource n1 = model.createResource("http://kaiko.getalp.org/jdm/" + matcher.group(9));
                            model.addLiteral(n1, poids, model.createLiteral("" + hmn.get(matcher.group(9)).getpoids()));
                            model.addLiteral(n1, chaine, model.createLiteral(hmn.get(matcher.group(9)).getchaine(), "fr"));
                            model.addLiteral(n1, type, model.createLiteral("" + hmn.get(matcher.group(9)).gettype()));
                            if (hmn.get(matcher.group(9)).getnf() != null)
                                model.add(n1, nf, model.createLiteral(hmn.get(matcher.group(9)).getnf()));

                            //CREATION RESSOURCE 2
                            Resource n2 = model.createResource("http://kaiko.getalp.org/jdm/" + matcher.group(10));
                            model.addLiteral(n2, poids, model.createLiteral("" + hmn.get(matcher.group(10)).getpoids()));
                            model.addLiteral(n2, chaine, model.createLiteral("" + hmn.get(matcher.group(10)).getchaine(), "fr"));
                            model.addLiteral(n2, type, model.createLiteral("" + hmn.get(matcher.group(10)).gettype()));
                            if (hmn.get(matcher.group(10)).getnf() != null)
                                model.addLiteral(n2, nf, model.createLiteral("" + hmn.get(matcher.group(10)).getnf()));

                            //CREATION D'UNE RELATION
                            Statement r = model.createStatement(n1, hmp.get(matcher.group(11)), n2);
                            model.add(r);
                            ReifiedStatement rs = r.createReifiedStatement("jdm_mot:r_" + matcher.group(8));
                            rs.addLiteral(poids, Integer.parseInt(matcher.group(12)));
                            rs.addLiteral(type, Integer.parseInt(matcher.group(11)));

                            nbr = nbr + 1;
                            System.out.print("Relation : " + nbr + "\r");


                        } else {
                            //MOT(S) INEXISTANT(S)
                        }
                    } else { //INITIALISATION DES TYPES DE RELATIONS
							String nom=matcher.group(15).replace('>','_');
							Property p = model.createProperty("http://kaiko.getalp.org/jdm#",nom);
							hmp.put(matcher.group(14), p);
					}
				}
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} 
		//AFFICHAGE FORMAT TURTLE
		
		try {
			RDFDataMgr.write(new FileOutputStream(new File("finaljdm.ttl"), true), model, RDFFormat.TURTLE_BLOCKS) ;
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}
package org.getalp.dbnary.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;

import org.getalp.blexisma.api.ISO639_3;
import org.getalp.blexisma.api.ISO639_3.Lang;
import org.getalp.dbnary.SuomiLangToCode;

public class miseEnFormlang {

	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub

		  afficherLang();
	}

	public static String [] afficherLang(){
		String lang ;
		
		int n= 300;
		String []t = new String [n];
		Iterator<Lang> it = ISO639_3.sharedInstance.knownLanguagesIterator();
		int i=0;
		while (it.hasNext()) {
			Lang l = it.next();
			lang = l.getId();
			String l1 = l.getPart1();
			String l2b = l.getPart2b();
			if(!l2b.equals(null) && !l2b.equals("") && (l1.equals(null)||l1.equals(""))) {
				
				System.out.println("{{"+l2b+"}}" );
				System.out.println();
				t[i]=l2b;
				i=i+1;
				
			}
			//String l2b = l.getPart2b();
   
		}
		return t ;
	}

/*	public  static void affiche() throws IOException {
		String ligne = "";
		String fichier = "";

		fichier = "C:\\Users\\Mariam\\Desktop\\Stage\\dumps\\pour_voir.txt";
		BufferedReader ficTexte;
		try {
			ficTexte = new BufferedReader(new FileReader(new File(fichier)));
			if (ficTexte == null) {
				throw new FileNotFoundException("Fichier non trouvé: "
						+ fichier);
			}
			do {
				ligne = ficTexte.readLine();
				if (ligne != null) {
					System.out.println(ligne);
				}
			} while (ficTexte != null);
			ficTexte.close();
			System.out.println("\n");
		} catch (FileNotFoundException e) {
			System.out.println(e.getMessage());
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}

	}*/

}


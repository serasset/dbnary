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

		  String[] t = afficherLang();
		  int i=0;
		  while(i!=t.length&& t[i]!=null){
			
			  
			  System.out.println(t[i]);
			  
			  i++;
		  }
	}

	public static String [] afficherLang(){

		
		int n= 570;
		String []t = new String [n];
		Iterator<Lang> it = ISO639_3.sharedInstance.knownLanguagesIterator();
		int i=0;
		while (it.hasNext()) {
			Lang l = it.next();
			
			String l1 = l.getPart1();
			String l2b = l.getPart2b();
			if(!l1.equals(null) && !l1.equals("")) {
				
				System.out.println("{{"+l1+"}}" );
				System.out.println();
				t[i]=l1;
				i=i+1;
				
			}else if(!l2b.equals(null) && !l2b.equals("") && (l1.equals(null)||l1.equals(""))){
				
				System.out.println("{{"+l2b+"}}" );
				System.out.println();
				t[i]=l2b;
				i=i+1;
			}
			//String l2b = l.getPart2b();
   
		}
		return t ;
	}

}

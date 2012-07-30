package org.getalp.dbnary.tools;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;


/**
 * 
 */

/**
 * @author Mariam
 *
 */
public class DiffFichier {
	
	
	
	public static String diffFichier(String nomfichier1,String nomfichier2){
		String txt="";
		
		try{
			FileReader fr1 =new FileReader(nomfichier1);
			BufferedReader br1 =new BufferedReader(fr1);
			String ligne1 = br1.readLine();
			ligne1=ligne1.trim();
			while(ligne1!=null){
				FileReader fr2=new FileReader(nomfichier2);
				BufferedReader br2=new BufferedReader(fr2);
				String ligne2 = br2.readLine();
				ligne2=ligne2.trim();
				while(ligne2!=null && !ligne1.equalsIgnoreCase(ligne2)){
					ligne2= br2.readLine();
				}
				if(ligne2==null){
					txt=txt+"\n"+ligne1;
				}
					br2.close();
				
			ligne1=br1.readLine();
			}
			br1.close();
			txt=txt.trim();
			return txt;
		}
		catch(FileNotFoundException e ){
			System.out.println(" le fichier "+ nomfichier1 +" ou "+ nomfichier2+" non trouv� ");
			return null;
		}
		catch(IOException e){
			System.out.println("Probl�me � la lecture du fichier " +nomfichier1 +" et de "+ nomfichier2);
			return null;
		}
			
		
	}

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args){
		// TODO Auto-generated method stub
		String f1="C:\\Users\\Mariam\\Desktop\\Stage\\dumps\\lang_qui_au_debut_portuguais.txt";
		String f2="C:\\Users\\Mariam\\Desktop\\Stage\\dumps\\lang_qui_reste_portuguais.txt";
		
		
		String txt= diffFichier(f1, f2);
		if(!txt.equals(null)||!txt.equals(""))
		System.out.print(txt);
	    }

}

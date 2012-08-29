/**
 * 
 */
package org.getalp.dbnary;

import java.util.HashMap;

import org.getalp.blexisma.api.ISO639_3;

/**
 * @author Mariam
 *
 */
public class KoreanLangtoCode {

	static HashMap<String,String> h = new HashMap<String,String>();
	
	static {

		// TODO: The korean wiktionary do not use templates to generate the name of 
		// languages in translation section, but it puts the korean name + the ISO 639-1 code
		// Hence, I should extract the list from the set of all korean translations.
		
	}

	public static String triletterCode(String s){ 
		if(s!=null && s!="") {
			s= s.trim();
			s=s.toLowerCase();
			String resultat;
			if (ISO639_3.sharedInstance.getIdCode(s) != null) {
				resultat =ISO639_3.sharedInstance.getIdCode(s);
			}else{
				if (h.containsKey(s)) {
					s = h.get(s);
					resultat =ISO639_3.sharedInstance.getIdCode(s);
				} else {
					resultat=null;
				}
			}
			return resultat;
		} else {
			return s;
		}
	}

}

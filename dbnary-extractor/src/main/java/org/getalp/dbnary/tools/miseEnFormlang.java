package org.getalp.dbnary.tools;

import org.getalp.iso639.ISO639_3;
import org.getalp.iso639.ISO639_3.Lang;

import java.io.IOException;
import java.util.Iterator;

public class miseEnFormlang {


    /**
     * @param args nothing
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

        String[] t = afficherLang();
        int i = 0;
        while (i != t.length && t[i] != null) {


            System.out.println("{{" + t[i] + "}}");
            System.out.println("---");

            i++;
        }
    }

    public static String[] afficherLang() {


        int n = 570;
        String[] t = new String[n];
        Iterator<Lang> it = ISO639_3.sharedInstance.knownLanguagesIterator();
        int i = 0;
        while (it.hasNext()) {
            Lang l = it.next();

            String l1 = l.getPart1();
            String l2b = l.getPart2b();
            if (!l1.equals(null) && !l1.equals("")) {

                // System.out.println("{{"+l1+"}}" );
                // System.out.println();
                t[i] = l1;
                i = i + 1;

            } else if (!l2b.equals(null) && !l2b.equals("") && (l1.equals(null) || l1.equals(""))) {

                // System.out.println("{{"+l2b+"}}" );
                // System.out.println();
                t[i] = l2b;
                i = i + 1;
            }
            //String l2b = l.getPart2b();

        }
        return t;
    }

}

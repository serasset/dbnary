package org.getalp.jdm;

import org.apache.jena.rdf.model.Resource;

public class Noeud {

  // COMPOSANTS

  private String Id;
  private String chaine;
  private int poids;
  private String nf;
  private int type;
  private Resource ressource;

  // CONSTRUCTEUR

  public Noeud(String Idp, String chainep, int poidsp, String nfp, int typep) {
    Id = Idp;
    chaine = chainep;
    poids = poidsp;
    nf = nfp;
    type = typep;
  }

  // SET inutile ?
  public void setRessource(Resource ressourcep) {
    ressource = ressourcep;
  }

  public void setId(String id) {
    Id = id;
  }

  public void setchaine(String nom) {
    chaine = nom;
  }

  public void setpoids(int w) {
    poids = w;
  }

  public void setnf(String qualificatif) {
    nf = qualificatif;
  }

  public void settype(int t) {
    type = t;
  }

  // GET
  public Resource getressource() {
    return ressource;
  }

  public String getId() {
    return Id;
  }

  public String getchaine() {
    return chaine;
  }

  public int getpoids() {
    return poids;
  }

  public String getnf() {
    return nf;
  }

  public int gettype() {
    return type;
  }
}

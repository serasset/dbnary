package org.getalp.dbnary.hbs;


import org.getalp.dbnary.OliaOnt;
import org.getalp.dbnary.PropertyObjectPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;

public class SerboCroatianInflectionData {
    private Logger log = LoggerFactory.getLogger(SerboCroatianInflectionData.class);

    public enum Genre {MASCULIN,FEMININE,NEUTER,NOTHING};
    public enum Cas {NOMINATIF,GENITIF,DATIF,ACCUSATIF,VOCATIF,INSTRUMENTAL,LOCATIVE,NOTHING};
    public enum GNumber {SINGULAR,PLURAL,NOTHING};

    public Genre genre = Genre.NOTHING;
    public Cas cas = Cas.NOTHING;
    public GNumber number = GNumber.NOTHING;

    public HashSet<PropertyObjectPair> toPropertyObjectMap() {
        HashSet<PropertyObjectPair> inflections = new HashSet<>();

        switch(this.cas){
            case NOMINATIF:
                inflections.add(PropertyObjectPair.get(OliaOnt.hasCase, OliaOnt.Nominative));
                break;
            case GENITIF:
                inflections.add(PropertyObjectPair.get(OliaOnt.hasCase, OliaOnt.GenitiveCase));
                break;
            case ACCUSATIF:
                inflections.add(PropertyObjectPair.get(OliaOnt.hasCase, OliaOnt.Accusative));
                break;
            case DATIF:
                inflections.add(PropertyObjectPair.get(OliaOnt.hasCase, OliaOnt.DativeCase));
            case VOCATIF:
                inflections.add(PropertyObjectPair.get(OliaOnt.hasCase, OliaOnt.VocativeCase));
            case INSTRUMENTAL:
                inflections.add(PropertyObjectPair.get(OliaOnt.hasCase, OliaOnt.InstrumentalCase));
            case LOCATIVE:
                inflections.add(PropertyObjectPair.get(OliaOnt.hasCase, OliaOnt.LocativeCase));
            case NOTHING:
                break;
            default:
                log.debug("Unexpected value {} for case", this.cas);
                break;
        }
        switch(this.genre){
            case MASCULIN:
                inflections.add(PropertyObjectPair.get(OliaOnt.hasGender, OliaOnt.Masculine));
                break;
            case FEMININE:
                inflections.add(PropertyObjectPair.get(OliaOnt.hasGender, OliaOnt.Feminine));
                break;
            case NEUTER:
                inflections.add(PropertyObjectPair.get(OliaOnt.hasGender, OliaOnt.Neuter));
                break;
            case NOTHING:
                break;
            default :
                log.debug("Unexpected value {} for genre", this.genre);
                break;
        }
        switch(this.number){
            case SINGULAR:
                inflections.add(PropertyObjectPair.get(OliaOnt.hasNumber, OliaOnt.Singular));
                break;
            case PLURAL:
                inflections.add(PropertyObjectPair.get(OliaOnt.hasNumber, OliaOnt.Plural));
                break;
            case NOTHING:
                break;
            default:
                log.debug("Unexpected value {} for number", this.number);
                break;
        }
        return inflections;
    }
}

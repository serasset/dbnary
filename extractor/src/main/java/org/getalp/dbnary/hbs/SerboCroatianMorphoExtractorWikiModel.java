package org.getalp.dbnary.hbs;

import org.getalp.dbnary.IWiktionaryDataHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class SerboCroatianMorphoExtractorWikiModel {

    private Logger log = LoggerFactory.getLogger(SerboCroatianMorphoExtractorWikiModel.class);

    IWiktionaryDataHandler wdh;

    public SerboCroatianMorphoExtractorWikiModel(IWiktionaryDataHandler wdh){
        this.wdh = wdh;
    }

    public void extractImenicaDeklinacija(ArrayList<String> parameter){

    }

    public void extractTemplate(String templateName, ArrayList<String> parameter){
        switch(templateName){
            case "sh-imenica-deklinacija":
                extractImenicaDeklinacija(parameter);
                break;
            case "sh-imenica-deklinacija2":

                break;
            default:
                log.debug("Unkown templateName : {} in {}", templateName, wdh.currentLexEntry());
        }
    }
}

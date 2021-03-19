package org.getalp.lexinfo.model;

import java.util.Set;
import java.util.function.Consumer;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.morphology.MorphoSyntacticFeature;

public abstract class Case extends MorphoSyntacticFeature {
  public Property property() {
    return LexinfoOnt.case_;
  }

  public static Case ABESSIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.abessiveCase;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> abessiveCase = s -> s.add(ABESSIVE);

  public static Case ABLATIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.ablativeCase;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> ablative = s -> s.add(ABLATIVE);

  public static Case ABSOLUTIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.absolutiveCase;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> absolutive = s -> s.add(ABSOLUTIVE);

  public static Case ACCUSATIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.accusativeCase;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> accusative = s -> s.add(ACCUSATIVE);

  public static Case ADESSIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.adessiveCase;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> adessive = s -> s.add(ADESSIVE);

  public static Case ADITIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.aditiveCase;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> aditive = s -> s.add(ADITIVE);

  public static Case ALLATIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.allativeCase;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> allative = s -> s.add(ALLATIVE);

  public static Case BENEFACTIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.benefactiveCase;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> benefactive = s -> s.add(BENEFACTIVE);

  public static Case CAUSATIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.causativeCase;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> causative = s -> s.add(CAUSATIVE);

  public static Case COMITATIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.comitativeCase;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> comitative = s -> s.add(COMITATIVE);

  public static Case DATIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.dativeCase;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> dative = s -> s.add(DATIVE);

  public static Case DELATIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.delativeCase;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> delative = s -> s.add(DELATIVE);

  public static Case ELATIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.elativeCase;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> elative = s -> s.add(ELATIVE);

  public static Case EQUATIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.equativeCase;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> equative = s -> s.add(EQUATIVE);

  public static Case ERGATIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.ergativeCase;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> ergative = s -> s.add(ERGATIVE);

  public static Case ESSIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.essiveCase;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> essive = s -> s.add(ESSIVE);

  public static Case GENITIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.genitiveCase;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> genitive = s -> s.add(GENITIVE);

  public static Case ILLATIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.illativeCase;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> illative = s -> s.add(ILLATIVE);

  public static Case INESSIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.inessiveCase;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> inessive = s -> s.add(INESSIVE);

  public static Case INSTRUMENTAL = new Case() {
    public RDFNode value() {
      return LexinfoOnt.instrumentalCase;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> instrumental = s -> s.add(INSTRUMENTAL);

  public static Case LATIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.lativeCase;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> lative = s -> s.add(LATIVE);

  public static Case LOCATIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.locativeCase;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> locative = s -> s.add(LOCATIVE);

  public static Case NOMINATIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.nominativeCase;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> nominative = s -> s.add(NOMINATIVE);

  public static Case OBLIQUE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.obliqueCase;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> oblique = s -> s.add(OBLIQUE);

  public static Case PARTITIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.partitiveCase;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> partitive = s -> s.add(PARTITIVE);

  public static Case PROLATIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.prolativeCase;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> prolative = s -> s.add(PROLATIVE);

  public static Case SOCIATIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.sociativeCase;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> sociative = s -> s.add(SOCIATIVE);

  public static Case SUBLATIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.sublativeCase;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> sublative = s -> s.add(SUBLATIVE);

  public static Case SUPERESSIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.superessiveCase;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> superessive = s -> s.add(SUPERESSIVE);

  public static Case TERMINATIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.terminativeCase;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> terminative = s -> s.add(TERMINATIVE);

  public static Case TRANSLATIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.translativeCase;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> translative = s -> s.add(TRANSLATIVE);

  public static Case VOCATIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.vocativeCase;
    }
  };

  public static Consumer<Set<MorphoSyntacticFeature>> vocative = s -> s.add(VOCATIVE);

}



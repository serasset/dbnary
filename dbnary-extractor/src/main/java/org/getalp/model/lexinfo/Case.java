package org.getalp.model.lexinfo;

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

  public static final Case ABESSIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.abessiveCase;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> abessiveCase = s -> s.add(ABESSIVE);

  public static final Case ABLATIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.ablativeCase;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> ablative = s -> s.add(ABLATIVE);

  public static final Case ABSOLUTIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.absolutiveCase;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> absolutive = s -> s.add(ABSOLUTIVE);

  public static final Case ACCUSATIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.accusativeCase;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> accusative = s -> s.add(ACCUSATIVE);

  public static final Case ADESSIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.adessiveCase;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> adessive = s -> s.add(ADESSIVE);

  public static final Case ADITIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.aditiveCase;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> aditive = s -> s.add(ADITIVE);

  public static final Case ALLATIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.allativeCase;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> allative = s -> s.add(ALLATIVE);

  public static final Case BENEFACTIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.benefactiveCase;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> benefactive = s -> s.add(BENEFACTIVE);

  public static final Case CAUSATIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.causativeCase;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> causative = s -> s.add(CAUSATIVE);

  public static final Case COMITATIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.comitativeCase;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> comitative = s -> s.add(COMITATIVE);

  public static final Case DATIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.dativeCase;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> dative = s -> s.add(DATIVE);

  public static final Case DELATIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.delativeCase;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> delative = s -> s.add(DELATIVE);

  public static final Case ELATIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.elativeCase;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> elative = s -> s.add(ELATIVE);

  public static final Case EQUATIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.equativeCase;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> equative = s -> s.add(EQUATIVE);

  public static final Case ERGATIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.ergativeCase;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> ergative = s -> s.add(ERGATIVE);

  public static final Case ESSIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.essiveCase;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> essive = s -> s.add(ESSIVE);

  public static final Case GENITIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.genitiveCase;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> genitive = s -> s.add(GENITIVE);

  public static final Case ILLATIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.illativeCase;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> illative = s -> s.add(ILLATIVE);

  public static final Case INESSIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.inessiveCase;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> inessive = s -> s.add(INESSIVE);

  public static final Case INSTRUMENTAL = new Case() {
    public RDFNode value() {
      return LexinfoOnt.instrumentalCase;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> instrumental = s -> s.add(INSTRUMENTAL);

  public static final Case LATIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.lativeCase;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> lative = s -> s.add(LATIVE);

  public static final Case LOCATIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.locativeCase;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> locative = s -> s.add(LOCATIVE);

  public static final Case NOMINATIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.nominativeCase;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> nominative = s -> s.add(NOMINATIVE);

  public static final Case OBLIQUE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.obliqueCase;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> oblique = s -> s.add(OBLIQUE);

  public static final Case PARTITIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.partitiveCase;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> partitive = s -> s.add(PARTITIVE);

  public static final Case PROLATIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.prolativeCase;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> prolative = s -> s.add(PROLATIVE);

  public static final Case SOCIATIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.sociativeCase;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> sociative = s -> s.add(SOCIATIVE);

  public static final Case SUBLATIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.sublativeCase;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> sublative = s -> s.add(SUBLATIVE);

  public static final Case SUPERESSIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.superessiveCase;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> superessive = s -> s.add(SUPERESSIVE);

  public static final Case TERMINATIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.terminativeCase;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> terminative = s -> s.add(TERMINATIVE);

  public static final Case TRANSLATIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.translativeCase;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> translative = s -> s.add(TRANSLATIVE);

  public static final Case VOCATIVE = new Case() {
    public RDFNode value() {
      return LexinfoOnt.vocativeCase;
    }
  };

  public static final Consumer<Set<MorphoSyntacticFeature>> vocative = s -> s.add(VOCATIVE);

}



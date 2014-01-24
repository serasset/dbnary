package org.getalp.dbnary.experiment.fuzzystring;


import com.wcohen.ss.WinklerRescorer;

public class JaroWinklerUnicode extends WinklerRescorer {
    public JaroWinklerUnicode() {
        super(new JaroUnicode());
    }
}

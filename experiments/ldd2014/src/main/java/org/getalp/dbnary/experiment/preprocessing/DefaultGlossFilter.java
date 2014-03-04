package org.getalp.dbnary.experiment.preprocessing;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by tchechem on 04/03/14.
 */
public class DefaultGlossFilter extends GlossFilter{
    @Override
    public StructuredGloss extractGlossStructure(String rawGloss) {
        return new StructuredGloss(null,rawGloss);
    }
}

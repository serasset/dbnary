package org.getalp.dbnary.cli;

import org.getalp.dbnary.IWiktionaryDataHandler;
import org.getalp.dbnary.IWiktionaryDataHandler.Feature;
import org.getalp.dbnary.WiktionaryIndexerException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class GetExtractedSemnet extends DbnaryCommandLine {


    public static void main(String[] args) throws WiktionaryIndexerException, IOException {
        GetExtractedSemnet cliProg = new GetExtractedSemnet();
        cliProg.loadArgs(args);
        cliProg.doit();
    }

    @Override
    protected String getHelpText() {
        return "dumpFile must be a Wiktionary dump file in UTF-16 encoding. dumpFile directory must be writable to store the index." +
                System.getProperty("line.separator", "\n") +
                "Displays the extracted semnet of the wiktionary page(s) named \"entryname\", ...";
    }

    protected void doit() throws IOException {

        for (int i = 1; i < remainingArgs.length; i++) {
            String pageContent = wi.getTextOfPage(remainingArgs[i]);
            we.extractData(remainingArgs[i], pageContent);
        }

        dumpBox(Feature.MAIN);
        if (null != morphoOutputFile) {
            System.out.println("----------- MORPHOLOGY ----------");
            dumpBox(Feature.MORPHOLOGY);
        }
        if (null != etymologyOutputFile) {
            System.out.println("----------- ETYMOLOGY ----------");
            dumpBox(Feature.ETYMOLOGY);
        }
    }

    public void dumpBox(IWiktionaryDataHandler.Feature f) throws IOException {
        OutputStream ostream = System.out;
        try {
            wdh.dump(f, new PrintStream(ostream, false, "UTF-8"), outputFormat);
        } catch (IOException e) {
            System.err.println("Caught IOException while printing extracted data: \n" + e.getLocalizedMessage());
            e.printStackTrace(System.err);
            throw e;
        } finally {
            if (null != ostream) {
                ostream.flush();
            }
        }
    }

}

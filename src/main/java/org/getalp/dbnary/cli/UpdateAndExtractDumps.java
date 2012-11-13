package org.getalp.dbnary.cli;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.getalp.blexisma.api.ISO639_3;
import org.getalp.dbnary.WiktionaryIndexerException;

public class UpdateAndExtractDumps {

	private static Options options = null; // Command line options

	private static final String SERVER_URL_OPTION = "s";
	private static final String DEFAULT_SERVER_URL = "ftp://ftp.fi.muni.cz/pub/wikimedia/";

	private static final String FORCE_OPTION = "f";
	private static final boolean DEFAULT_FORCE = false;

	private static final String PREFIX_DIR_OPTION = "d";
	private static final String DEFAULT_PREFIX_DIR = ".";

	private static final String MODEL_OPTION = "m";
	private static final String DEFAULT_MODEL = "LMF";

	private static final String HISTORY_SIZE_OPTION = "k";
	private static final String DEFAULT_HISTORY_SIZE = "5";

	private CommandLine cmd = null; // Command Line arguments

	private String outputDir;
	private String extractDir;
	private int historySize;
	private boolean force = DEFAULT_FORCE;
	private String server = DEFAULT_SERVER_URL;
	private String model = DEFAULT_MODEL;

	String[] remainingArgs;


	static{
		options = new Options();
		options.addOption("h", false, "Prints usage and exits. ");	
		options.addOption(SERVER_URL_OPTION, true, "give the URL pointing to a wikimedia mirror. " + DEFAULT_SERVER_URL + " by default.");	
		options.addOption(FORCE_OPTION, false, 
				"force the updating even if a file with the same name already exists in the output directory. " + DEFAULT_FORCE + " by default.");
		options.addOption(HISTORY_SIZE_OPTION, true, "number of dumps to be kept in output directory. " + DEFAULT_HISTORY_SIZE + " by default ");	
		options.addOption(PREFIX_DIR_OPTION, true, "directory containing the wiktionary dumps and extracts. " + DEFAULT_PREFIX_DIR + " by default ");	
		options.addOption(MODEL_OPTION, true, "model of the extracts (LMF or LEMON) extracts. " + DEFAULT_MODEL + " by default ");	
	}

	/**
	 * @param args
	 * @throws IOException 
	 * @throws WiktionaryIndexerException 
	 */
	public static void main(String[] args) throws WiktionaryIndexerException, IOException {
		UpdateAndExtractDumps cliProg = new UpdateAndExtractDumps();
		cliProg.loadArgs(args);
		cliProg.updateAndExtract();
	}


	private String dumpFileName(String lang, String date) {

		return lang + "wiktionary-"+date+"-pages-articles.xml.bz2";
	}

	/**
	 * Validate and set command line arguments.
	 * Exit after printing usage if anything is astray
	 * @param args String[] args as featured in public static void main()
	 */
	private void loadArgs(String[] args){
		CommandLineParser parser = new PosixParser();
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			System.err.println("Error parsing arguments: " + e.getLocalizedMessage());
			printUsage();
			System.exit(1);
		}

		// Check for args
		if (cmd.hasOption("h")){
			printUsage();
			System.exit(0);
		}

		String h = cmd.getOptionValue(HISTORY_SIZE_OPTION, DEFAULT_HISTORY_SIZE);
		historySize = Integer.parseInt(h);
		
		if (cmd.hasOption(SERVER_URL_OPTION)) {
			server = cmd.getOptionValue(SERVER_URL_OPTION);
		}

		force = cmd.hasOption(FORCE_OPTION);

		if (cmd.hasOption(MODEL_OPTION)) {
			model = cmd.getOptionValue(MODEL_OPTION);
		}
		
		String prefixDir = DEFAULT_PREFIX_DIR;
		if (cmd.hasOption(PREFIX_DIR_OPTION)) {
			prefixDir = cmd.getOptionValue(PREFIX_DIR_OPTION);
		}
		outputDir = prefixDir + "/dumps";
		extractDir = prefixDir + "/extracts";
		
		remainingArgs = cmd.getArgs();
		if (remainingArgs.length == 0) {
			printUsage();
			System.exit(1);
		}

	}

	public void updateAndExtract() throws WiktionaryIndexerException, IOException {
		String [] dirs = updateDumpFiles(remainingArgs);
		uncompressDumpFiles(remainingArgs, dirs);
		extractDumpFiles(remainingArgs, dirs);
		cleanUpDumpFiles(remainingArgs, dirs);
		cleanUpExtractFiles(remainingArgs, dirs);
		linkToLatestExtractFiles(remainingArgs, dirs);
	}

	private void linkToLatestExtractFiles(String[] langs, String[] dirs) {
		// link to the extracted file
		System.err.println("==> Linking to latest versions.");
		for (int i = 0; i < langs.length; i++) {
			linkToLatestExtractFile(langs[i], dirs[i]);
		}
	}


	private void linkToLatestExtractFile(String lang, String dir) {
		if (null == dir || dir.equals("")) return;
		
		String latestdir = extractDir + "/" + model.toLowerCase() + "/latest";
		String odir = extractDir + "/" + model.toLowerCase() + "/" + lang;
		File d = new File(latestdir);
		d.mkdirs();

		String extractFile = odir + "/" + lang +"_dbnary_" + model.toLowerCase() + "_" + dir + ".ttl";
		File file = new File(extractFile);
		if (! file.exists()) {
			System.err.println("Extracted wiktionary file " + extractFile + " does not exists. I will not link to this version.");
			return;
		}
		
		String latestFile = latestdir + "/" + lang +"_dbnary_" + model.toLowerCase() + ".ttl";
		File lf = new File(latestFile);
		if (lf.exists()) {
			// System.err.println("Deleting old link: " + latestFile );
			lf.delete();
		}
		try {
			String[] args = {"ln", "-s", "../" + lang + "/" + lang +"_dbnary_" + model.toLowerCase() + "_" + dir + ".ttl", lang + "_dbnary_" + model.toLowerCase() + ".ttl"};
			Runtime.getRuntime().exec(args, null, d);
		} catch (IOException e) {
			System.err.println("Eror while trying to link to latest extract: " + latestFile + "->" + extractFile);
			e.printStackTrace(System.err);
		}
	}


	private void cleanUpExtractFiles(String[] langs, String[] dirs) {
		// Keep all for now...
	}


	private void cleanUpDumpFiles(String[] langs, String[] dirs) {
		// keep at most "historySize" number of compressed dumps and only 1 uncompressed dump
		for (int i = 0; i < langs.length; i++) {
			cleanUpDumps(langs[i], dirs[i]);
		}
	}


	private void cleanUpDumps(String lang, String lastDir) {
		
		// Do not cleanup if there has been any problems before...
		if (lastDir == null || lastDir.equals("")) return;
		
		String langDir = outputDir + "/" + lang;
		File[] dirs = new File(langDir).listFiles();
		
		if (null == dirs || dirs.length == 0) return;
		
		SortedSet<String> versions = new TreeSet<String>();
		for (File dir : dirs) {
	        if (dir.isDirectory()) {
	            versions.add(dir.getName());
	        } else {
	            System.err.println("Ignoring unexpected file: " + dir.getName());
	        }
	    }
		
		int vsize = versions.size();
		
		for (String v : versions) {
			if (vsize > historySize) deleteDump(lang, v);
			if (vsize > 1) deleteUncompressedDump(lang, v);
			if (vsize > historySize) deleteDumpDir(lang, v);
			vsize--;
		}
	}


	private void deleteDumpDir(String lang, String dir) {
		String dumpdir = outputDir + "/" + lang + "/" + dir;
		File f = new File(dumpdir);
		
		if (f.listFiles().length == 0) {
			System.err.println("Deleting dump directory: " + f.getName());
			f.delete();
		} else {
			System.err.println("Could not delete non empty dir: " + f.getName());
		}
	}

	private void deleteUncompressedDump(String lang, String dir) {
		String filename = uncompressDumpFileName(lang, dir);
		
		File f = new File(filename);
		
		if (f.exists()) {
			System.err.println("Deleting uncompressed dump: " + f.getName());
			f.delete();
		}
		
		File fidx = new File(filename+".idx");
		if (fidx.exists()) {
			System.err.println("Deleting index file: " + fidx.getName());
			fidx.delete();
		}
		
	}


	private void deleteDump(String lang, String dir) {
		String dumpdir = outputDir + "/" + lang + "/" + dir;
		String filename = dumpdir + "/" + dumpFileName(lang,dir);
		
		File f = new File(filename);
		
		if (f.exists()) {
			System.err.println("Deleting compressed dump: " + f.getName());
			f.delete();
		}
	}


	private String updateDumpFile(String lang) {
		FTPClient client = new FTPClient();

		try {
			System.err.println("Updating " + lang);
			URL url = new URL(server);

			if (url.getPort() != -1) {
				client.connect(url.getHost(), url.getPort());
			} else {
				client.connect(url.getHost());
			}
			client.login( "anonymous", "" );
			// System.err.println("Logged in...");
			client.enterLocalPassiveMode();
			client.changeWorkingDirectory(url.getPath());

			client.changeWorkingDirectory(lang+"wiktionary");

			SortedSet<String> dirs = new TreeSet<String>();
			// System.err.println("Retrieving directory list.");
			FTPFile[] ftpFiles = client.listFiles();
			// System.err.println("Retrieved: " + ftpFiles);
			for (FTPFile ftpFile : ftpFiles) {
				if (ftpFile.getType() == FTPFile.DIRECTORY_TYPE && ! ftpFile.getName().startsWith(".")) {
					dirs.add(ftpFile.getName());
				}
			}
			String lastDir = getLastVersionDir(dirs);
			// System.err.println("Last version of dump is " + lastDir);

			client.changeWorkingDirectory(lastDir);

			try {
				String dumpdir = outputDir + "/" + lang + "/" + lastDir;
				String filename = dumpdir + "/" + dumpFileName(lang,lastDir);
				File file = new File(filename);
				if (file.exists() && !force) {
					// System.err.println("Dump file " + filename + " already retrieved.");
					return lastDir;
				}
				File dumpFile = new File(dumpdir);
				dumpFile.mkdirs();
				client.setFileType(FTP.BINARY_FILE_TYPE);
				FileOutputStream dfile = new FileOutputStream(file);
				System.err.println("====>  Retrieving new dump for " + lang + ": " + lastDir);
				long s = System.currentTimeMillis();
				client.retrieveFile(dumpFileName(lang,lastDir),dfile);
				System.err.println("Retreived " + filename + "[" + (System.currentTimeMillis() - s) + " ms]");


			} catch(IOException e) {
				System.out.println(e);
			}
			client.logout();
			return lastDir;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			try {
				client.disconnect();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static String versionPattern = "\\d{8}";
	private static Pattern vpat = Pattern.compile(versionPattern);
	
	private String getLastVersionDir(SortedSet<String> dirs) {
		String res = null;
		Matcher m = vpat.matcher("");
		for (String d: dirs) {
			m.reset(d);
			if (m.matches()) {
				res = d;
			}
		}
		return res;
	}


	private void uncompressDumpFiles(String[] langs, String[] dirs) {
		boolean status;
		for (int i = 0; i < langs.length; i++) {
			status = uncompressDumpFile(langs[i], dirs[i]);
			if (!status) dirs[i] = null;
		}
	}

	private boolean uncompressDumpFile(String lang, String dir) {
		boolean status = true;
		if (null == dir || dir.equals("")) return false;
		
		Reader r = null;
		Writer w = null;
		try {
			String compressedDumpFile = outputDir + "/" + lang + "/" + dir + "/" + dumpFileName(lang, dir);
			String uncompressedDumpFile = uncompressDumpFileName(lang, dir);
			// System.err.println("Uncompressing " + compressedDumpFile);

			File file = new File(uncompressedDumpFile);
			if (file.exists() && !force) {
				// System.err.println("Uncompressed dump file " + uncompressedDumpFile + " already exists.");
				return true;
			}
			
			System.err.println("uncompressing file : " + compressedDumpFile + " to " + uncompressedDumpFile);

			BZip2CompressorInputStream bzIn = new BZip2CompressorInputStream(new FileInputStream(compressedDumpFile));
			r = new BufferedReader(new InputStreamReader(bzIn, "UTF-8"));

			FileOutputStream out = new FileOutputStream(uncompressedDumpFile);
			w = new BufferedWriter(new OutputStreamWriter(out, "UTF-16"));

			final char[] buffer = new char[4096];
			int len;
			while ((len = r.read(buffer)) != -1) 
				w.write(buffer, 0, len); 
			r.close(); 
			w.close(); 
		} catch (IOException e) {
			System.err.println("Caught an IOException while uncompressing dump: " + dumpFileName(lang, dir));
			System.err.println(e.getLocalizedMessage());
			e.printStackTrace();
			status = false;
		} finally {
			if (null != r) {
				try {
					r.close();
				} catch (IOException e) {
					// nop
				}
			}
			if (null != w) {
				try {
					w.close();
				} catch (IOException e) {
					// nop
				}
			}
		}
		return status;
	}

	private String uncompressDumpFileName(String lang, String dir) {
		return outputDir + "/" + lang + "/" + dir + "/" + lang + "wkt-" + dir + ".xml";
	}


	private String[] updateDumpFiles(String[] langs) {
		String[] res = new String[langs.length];
		int i = 0;
		for (String prefix : remainingArgs) {
			res[i] = updateDumpFile(prefix);
			i++;
		}
		return res;
	}

	private void extractDumpFiles(String[] langs, String[] dirs) {
		for (int i = 0; i < langs.length; i++) {
			extractDumpFile(langs[i], dirs[i]);
		}
	}

	private boolean extractDumpFile(String lang, String dir) {
		boolean status = true;
		if (null == dir || dir.equals("")) return false;
		
		String odir = extractDir + "/" + model.toLowerCase() + "/" + lang;
		File d = new File(odir);
		d.mkdirs();
	
		String extractFile = odir + "/" + lang +"_dbnary_" + model.toLowerCase() + "_" + dir + ".ttl";
		File file = new File(extractFile);
		if (file.exists() && !force) {
			// System.err.println("Extracted wiktionary file " + extractFile + " already exists.");
			return true;
		}
		System.err.println("========= EXTRACTING file " + extractFile + " ===========");
		
		String[] args = new String[] {"-f", "turtle", 
				"-l", lang, 
				"-o", extractFile,
				"-m", model,
				uncompressDumpFileName(lang, dir)
				};
		
		try {
			ExtractWiktionary.main(args);
		} catch (WiktionaryIndexerException e) {
			System.err.println("Caught IndexerException while extracting dump file: " + uncompressDumpFileName(lang, dir));
			System.err.println(e.getLocalizedMessage());
			e.printStackTrace();
			status = false;
		} catch (IOException e) {
			System.err.println("Caught IOExcetion while extracting dump file: " + uncompressDumpFileName(lang, dir));
			System.err.println(e.getLocalizedMessage());
			e.printStackTrace();
			status = false;
		}
		return status;
	}


	public static void printUsage() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("java -cp /path/to/wiktionary.jar org.getalp.dbnary.cli.ExtractWiktionary [OPTIONS] languageCode...", 
				"With OPTIONS in:", options, 
				"languageCode is the wiktionary code for a language (usualy a 2 letter code).", false);
	}

}

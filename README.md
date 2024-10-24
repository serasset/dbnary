<!-- [![SonarCloud](https://sonarcloud.io/images/project_badges/sonarcloud-white.svg)](https://sonarcloud.io/dashboard?id=serasset_dbnary)-->
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/ac93a96d001346a1818b5ebeafe80213)](https://www.codacy.com/gl/gilles.serasset/dbnary/dashboard?utm_source=gitlab.com&amp;utm_medium=referral&amp;utm_content=gilles.serasset/dbnary&amp;utm_campaign=Badge_Grade)

# DBnary extractor #

DBnary is an attempt to extract as many lexical data as possible from as many Wiktionary Language Editions as possible, in a structured (RDF) way, using standard lexicon ontology vocabulary (ontolex).

The extracted data is kept in sync with Wiktionary each time a new dump is generated and is available from http://kaiko.getalp.org/about-dbnary (more info is contained there).

The current repository contains the extraction programs, currently handling 26 language editions.

## Using the extracted data ##

Extracted data is available in RDF. You will have to load it in an RDF database or using an RDF API (Jena in Java or others in other languages...). You may download the data from the above web page.

You may also query the data from the above web page, using SPARQL.

This repository hosts the programs that extracted the data from Wiktionary. It does not contain tools to use it.

## Installing the extractor (without compiling) ##

The DBnary extractor utilities are now packaged as a homebrew java application. Just install it with : 
```bash
brew install serasset/tap/dbnary
```

This will install the DBnary commands along with all dependencies in the homebrew directory.

## Compiling the extractor ##

First, you do not have to compile this extractor if your only purpose is to use the extracted data. As stated in the previous section, the extracted data is made available in sync with wiktionary dumps.

However, you are free (and encouraged) to compile and enhance the extractors.

* DBnary extractor uses maven and is written in Java (with small parts in scala)
* Dependencies should be taken care of by maven
* There is no database to configure, the extractor directly uses the dump files


## Using the extractor? ##

Easiest way is to use the Command Line Interfaces packaged as a java app. 

You should either install DBnary extractor using homebrew or, if you are debugging the extractor, 
make sure the `dbnary` shell script defined at `YOUR_SOURCE_DIRECTORY/dbnary/dbnary-commands/target/appassembler/bin/dbnary` is selected first by your `PATH`. Note, this file only exists after a full `mvn package` run from the dbnary source code root folder.

```bash
$ dbnary --version
3.0.9
$ dbnary help
Usage: dbnary [-hvV] [--dir=<dbnaryDir>] [--debug=<debug>[,<debug>...]]...
              [--trace=<trace>]... [@<filename>...] [COMMAND]
DBnary is a set of tools used to extract lexical data from several editions of
wiktionaries. All extracted data is made available as Linked Open Data, using
ontolex, lexinfo, olia and several other specialized vocabularies.
      [@<filename>...]    One or more argument files containing options.
      --debug=<debug>[,<debug>...]

      --dir=<dbnaryDir>
  -h, --help              Show this help message and exit.
      --trace=<trace>
  -v                      Print extra information.
  -V, --version           Print version information and exit.

Commands:

The dbnary commands are:
  check    check the mediawiki syntax of all pages of a dump.
  extract  extract all pages from a dump and write resulting RDF files.
  help     Displays help information about the specified command
  update   Update dumps for all specified languages, then extract them.
  sample   extract the specified pages from a dump and write resulting RDF
             files to stdout.
  tree     Parse the specified entries wikitext and display the parse tree to
             stdout.
  source   get the wikitext source of the specified pages.
  compare  fetch and compare extracts from different dates.
  grep     grep a given pattern in all pages of a dump.
```

All subcommands are also documented using the help subcommand. E.g.
```bash
$ dbnary help grep
grep a given pattern in all pages of a dump.
Usage: dbnary grep [-hlvV] [--all-matches] [--[no-]compress] [--[no-]tdb]
                   [--plain] [--dir=<dbnaryDir>] [-F=NUMBER] [-T=NUMBER]
                   [--debug=<debug>[,<debug>...]]... [--trace=<trace>]...
                   <dumpFile> <pattern>
This command looks for a given pattern in all pages of a dump and output the
matching pages.
      <dumpFile>          The dump file of the wiki to be extracted.
      <pattern>           The pattern to be searched for.
      --all-matches       show all matches.
      --debug=<debug>[,<debug>...]

      --dir=<dbnaryDir>
  -F, --frompage=NUMBER   Begin the extraction at the specified page number.
  -h, --help              Show this help message and exit.
  -l, --pagename          only show the name of the page.
      --[no-]compress     Compress the resulting extracted files using BZip2.
                            set by default.
      --[no-]tdb          Use TDB2 (temporary file storage for extracted
                            models, usefull/necessary for big dumps. set by
                            default.
      --plain             match is displayed without specific formatting.
  -T, --topage=NUMBER     Stop the extraction at the specified page number.
      --trace=<trace>
  -v                      Print extra information.
  -V, --version           Print version information and exit.
```

## Performing releases ##

The DBnary project uses the [git flow](https://nvie.com/posts/a-successful-git-branching-model/) 
branching model. To successfully release the code using maven, we use the 
[git flow plugin](https://github.com/aleksandr-m/gitflow-maven-plugin).
 
```bash
mvn gitflow:release-start
# edit all scripts in kaiko/ to use the correct (non SNAPSHOT) version.
mvn deploy site:site site:deploy
mvn gitflow:release-finish 
```

## Using CI/CD to validate changes in the extractors ##

As DBnary now extracts 22 different languages editions which use very diverse microstructure for their 
entry descriptions, it is very likely that a change (especially one at the DataHandler level) breaks 
the extraction of another language.

Hence, it is essential to be able to evaluate the impact of a set of changes to the extraction of all 
languages. In oder to evaluate this, a CI/CD setup has been created that will launch the extraction
of a SAMPLE of 10000 pages from each languages and compute the diffs between the new and previous 
versions.

This CI/CD pipeline is triggered when a Merge Request is created on the gitlab platform.

As we are using the gitflow strategy, here are the different steps to be performed :

  * Features
    * ```mvn gitflow:feature-start -DpushRemote=true```
    * Develop the feature on its branch (don't forget to push the feature branch)
    * Create a Pull Request to develop branch on gitlab (this will trigger CI/CD evaluation of the pull request, the pipeline extracts a sample of pages from latest wiktionary dumps and compares these. The ttl files are available as an artefact in the pipeline, available for 14 days after evaluation, please keep in mind that evaluation can take a very long time (several hours))
    * When the PR has been evaluated, checked and approved, then finnish it using gitflow plugin
    * ```mvn gitflow:feature-finnish```
    * OR, merge it using the MR on gitlab (and delete the feature branch).
  * Releases
    * TDB

### Controlling CI/CD extractors validation ###

In order to avoid all languages to be re-evaluated when it is not necessary, it is possible to control the validation process in 2 different manners :

 1. Globally setting VALIDATION_LANGUAGES variable on the repository (see repository variables on gitlab)
 2. Specifying the languages in the COMMIT MESSAGE
     * The commit message THAT TRIGGERS THE EVALUATION (the last message of the PR), should contain the string : `VALIDATION_LANGUAGES="la es fr"` (note that the quotes are mandatory)
   

## Contribution guidelines ##

  * Writing tests
  * Code review
  * Other guidelines

## Contacts ##

  * Contact `Gilles Sérasset <Gilles.Serasset@imag.fr>`

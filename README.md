<!-- [![SonarCloud](https://sonarcloud.io/images/project_badges/sonarcloud-white.svg)](https://sonarcloud.io/dashboard?id=serasset_dbnary)-->
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/ac93a96d001346a1818b5ebeafe80213)](https://www.codacy.com/gl/gilles.serasset/dbnary/dashboard?utm_source=gitlab.com&amp;utm_medium=referral&amp;utm_content=gilles.serasset/dbnary&amp;utm_campaign=Badge_Grade)
[![Known Vulnerabilities](https://snyk.io/test/github/serasset/dbnay/badge.svg)](https://snyk.io/test/gitlab/gilles.serasset/dbnary)
# DBnary extractor #

DBnary is an attempt to extract as many lexical data as possible from Wiktionary language Edition as possible, in a structured (RDF) way, using standard lexicon ontology vocabulary (ontolex).

The extracted data is kept in sync with Wiktionary each time a new dump is generated and is available from http://kaiko.getalp.org/about-dbnary (more info is contained there).

The current repository contains the extraction programs, currently handling 21 language editions.

## Using the extracted data ##

Extracted data is available in RDF. You will have to load it in an RDF database or using an RDF API (Jena in Java or others in other languages...). You may download the data from the above web page.

You may also query the data from the above web page, using SPARQL.

This repository hosts the programs that extracted the data from Wiktionary. It does not contain tools to use it.

## Compiling the extractor ##

First, you do not have to compile this extractor if your only purpose is to use the extracted data. As stated in the previous section, the extracted data is made available in sync with wiktionary dumps.

However, you are free (and encouraged) to compile and enhance the extractors.

* DBnary extractor uses maven and is written in Java (with small parts in scala)
* Dependencies should be taken care of by maven
* There is no database to configure, the extractor directly uses the dump files


## Using the extractor? ##

Easiest way is to use the Command Line Interfaces found in the org.getalp.dbnary.cli package.

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

This CI/CD pipeline is triggered when a Pull Request is created on the bitbucket platform.

As we are using the gitflow strategy, here are the different steps to be performed :

  * Features
    * ```mvn gitflow:feature-start -DpushRemote=true```
    * Develop the feature on its branch (don't forget to push the feature branch)
    * Create a Pull Request to develop branch on bitbucket (this will trigger CI/CD evaluation of the pull request, the pipeline extracts a sample of pages from latest wiktionary dumps and compares these. The ttl files are available as an artefact in the pipeline, available for 14 days after evaluation, please keep in mind that evaluation can take a very long time (several hours))
    * When the PR has been evaluated, checked and approved, then finnish it using gitflow plugin
    * ```mvn gitflow:feature-finnish```
    * OR, merge it using the PR on bitbucket (and delete the feature branch).
  * Releases
    * TDB

### Controlling CI/CD extractors validation ###

In order to avoid all languages to be re-evaluated when it is not necessary, it is possible to contraol the validation process in 2 different manners :

 1. Globally setting VALIDATION_LANGUAGES variable on the repository (see repository variables on bitbucket)
 2. Specifying the languages in the COMMIT MESSAGE
     * The commit message THAT TRIGGERS THE EVALUATION (the last message of the PR), should contain the string : `VALIDATION_LANGUAGES="la es fr"` (note that the quotes are mandatory)
   

## Contribution guidelines ##

  * Writing tests
  * Code review
  * Other guidelines

## Contacts ##

  * Contact `Gilles Sérasset <Gilles.Serasset@imag.fr>`

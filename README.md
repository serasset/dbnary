# DBnary extractor #

DBnary is an attempt to extract as many lexical data as possible from Wiktionary language Edition as possible, in a structured (RDF) way, using standard lexicon ontology vocabulary (ontolex).

The extracted data is kept in sync with Wiktionary each time a new dump is generated and is available from http://kaiko.getalp.org/about-dbnary (more info is contained there).

The current repository contains the extraction programs, for more than 13 language editions.

### How can I use the extracted data? ###

Extracted data is available in RDF. You will have to load it in an RDF database or using an RDF API (Jena in Java or others in other languages...). You may download the data from the above web page.

You may also query the data from the above web page, using SPARQL.

This repository hosts the programs that extracted the data from Wiktionary. It does not contain tools to use it.

### How do I compile the extractor? ###

* DBnary extractor uses maven and is written in Java (with small parts in scala)
* Dependencies should be taken care of by maven
* There is no database to configure, the extractor directly uses the dump files

### How do I use the extractor? ###

Easiest way is to use the Command Line Interfaces found in the org.getalp.dbnary.cli package.

### Contribution guidelines ###


* Writing tests
* Code review
* Other guidelines

### Who do I talk to? ###

* Repo owner or admin
* Other community or team contact
Deploying a new version of the ontology
========

The script should be run from it's containing directory.

When a new version of the dbnary data model is created (with a new version number), then the data 
model and its documentation should be made available on the web server and served
when requested, using content negociation to serve either the doc or the ontology file.

### Layout of the datamodel folder

The datamodel folder is used to provide the ontology and its documentation in all its versions.

The datamodel folder is made available in the root of the static directory for the kaiko website.

```
datamodel/
  2.0.0/
    index-en.html (and other documentation files)
    ontology.xml (and other formats)
    ...
  2.1.0/
  current -> 2.1.0
```

The script will create a new version folder, use widoco to generate the docs for this version and 
link current to this new version.
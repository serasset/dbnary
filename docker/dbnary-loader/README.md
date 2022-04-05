## DBnary loader docker image

This docker image is used to prepare a docker volume for being served using a 
serasset/dbnary-server container.

It will create configuration files (assemblies) and load DBnary data in a TDB2 database.

### Limitation 
Due to a bug in Jena 4.4.0, all the files will be loaded to the default graph.

## Usage

### Loading everything from DBnary dataset

```bash
docker run -it --rm --disable-content-trust -v fuseki-db:/data/fuseki serasset/dbnary-loader
```

### Loading a set of languages

```bash
docker run -it --rm --disable-content-trust -v fuseki-db:/data/fuseki -e LANGUAGES="ku" serasset/dbnary-loader
```

### Loading only part of the DBnary features

This command will only load the ontolex part of the DBnary dataset for the specified languages.

```bash
docker run -it --rm --disable-content-trust -v fuseki-db:/data/fuseki -e FEATURES="ontolex" serasset/dbnary-loader
```

Known features are ontolex, morphology, etymology, statistics, lime and exolex_ontolex

Of course, LANGUAGES and FEATURES can be specified together.

## Notes and caveats

As the time of writing (docker version 4.5.0), mounting a local directory on a docker container
on Mac OS X has a very heavy impact on IO performance. As tdb loading is a IO heavy process this 
leads to a 10x performance degradation of the loading process.

So for now I would advocate using a docker volume and not a host folder to keep the DB.

Note that not mounting a volume or folder will be useless as the data will then be loaded inside
the container and there will be not persistence of the loaded data.

Also note that loading the full DBnary dataset will have a huge impact on disk size and will likely 
hit the disk size limit (64G by default) on a mac os x docker.

For now I would advocate using this loader only on a linux host machine.
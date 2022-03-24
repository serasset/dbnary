# Docker Container for Jena-Fuseki tailored to serve DBnary data

This is an Apache Jena Fuseki Docker image inspired by the following sources:

* https://github.com/stain/jena-docker/tree/master/jena-fuseki
* https://github.com/blankdots/docker-SemanticWebApps/tree/master/apache-fuseki
* https://github.com/charlesvardeman/fuseki-geosparql-docker
* https://github.com/maximelefrancois86/fuseki-docker
* https://github.com/SemanticComputing/fuseki-docker
* https://gitlab.com/calincs/infrastructure/fuseki-docker/-/tree/master
* https://github.com/apache/jena/blob/main/jena-fuseki2/jena-fuseki-docker/

This docker image has been stripped out of data loading and configuration and will simply 
serve the data located at /data/fuseki according to configuration stored at 
/data/fuseki/config/config.ttl

## Run DBnary Fuseki locally

### Prepare the data volume

For this step to be useful, the /data/fuseki volume should bound to a folder on the container 
or mounted from another docker image.

`here the dbnary command should be used to create the expected layout`

### Launch the fuseki server using the prepared data

Assuming the data has been prepared locally in folder ./data

```bash
docker run -p 3030:3030 -v ./data:/data/fuseki serasset/fuseki
```

* You can modify the server port by modifying the `-p 3030:3030` option value.
* You can specify the admin password by adding `-e ADMIN_PASSWORD=pass` option to the run command. 
If no password is provided, a new one will be generated and visible in the container log

Open the admin UI at [http://localhost:3030](http://localhost:3030)

## Notes

I initially wanted to produce a server exposing the dbnary data produced in hdt format, 
but hdt does not seem to support fuseki2 (yet) and there are issues/enhancement (still) open
that will reduce the availability of the data.

Hence, we currently (as of March 2022), will go through the route of downloading and 
loading all requested languages dumps in turtle format. 
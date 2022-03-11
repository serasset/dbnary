# Inspired by many docker/fuseki configuration and mainly by fuseki's own docker file.
## This Dockefile builds a reduced footprint container.
ARG DBNARY_BASE=/dbnary

FROM serasset/dbnary-server

ARG DBNARY_BASE

LABEL maintainer="gilles.serasset@gmail.com"
LABEL name="serasset/fuseki-dbnary"
LABEL org.opencontainers.image.source=https://gitlab.com/serasset/dbnary
LABEL org.opencontainers.image.title="Loader of DBnary data for Apache Jena Fuseki (tailored to serve DBnary data)"
LABEL org.opencontainers.image.description="Fuseki is a SPARQL 1.1 server with a web interface, backed by the Apache Jena TDB RDF triple store."
LABEL org.opencontainers.image.version=${JENA_VERSION}
LABEL org.opencontainers.image.licenses="(Apache-2.0 AND (GPL-2.0 WITH Classpath-exception-2.0) AND GPL-3.0)"
LABEL org.opencontainers.image.authors="Apache Jena Fuseki by https://jena.apache.org/; this image by serasset for dbnary project;"

ENV DBNARY_TEMPLATES=${DBNARY_BASE}/templates
ENV DBNARY_CACHE=${DBNARY_BASE}/ontolex
ENV DBNARY_TDB=${FUSEKI_BASE}/databases/dbnary

# Bootstrapping the FUSEKI_BASE folder
RUN mkdir -p ${DBNARY_TEMPLATES} ${DBNARY_CACHE} ${DBNARY_TDB} ${DBNARY_TEMPLATES}/configuration

COPY config.ttl ${DBNARY_TEMPLATES}/config.ttl
COPY configuration/ ${DBNARY_TEMPLATES}/configuration/

COPY tdb2.tdbloader $FUSEKI_ROOT
COPY load-dbnary.sh $FUSEKI_ROOT
#COPY load.sh $FUSEKI_ROOT

RUN chmod 755 $FUSEKI_ROOT/tdb2.tdbloader $FUSEKI_ROOT/load-dbnary.sh /docker-entrypoint.sh

ENV GOSU_USER fuseki:fuseki
# setting directories to be owned by the non-root user
ENV GOSU_CHOWN $FUSEKI_BASE ${DBNARY_BASE}

# Tools for loading data, index, and stats
ENV ASSEMBLER $FUSEKI_BASE/configuration/dbnary.ttl
ENV CONFIG $FUSEKI_BASE/config.ttl
ENV TDB2TDBLOADER $JAVA_CMD tdb2.tdbloader --desc=$ASSEMBLER
ENV TEXTINDEXER $JAVA_CMD jena.textindexer --desc=$ASSEMBLER
ENV TDB2TDBSTATS $JAVA_CMD tdb2.tdbstats --desc=$ASSEMBLER

WORKDIR ${FUSEKI_BASE}

ENTRYPOINT ["/docker-entrypoint.sh"]
CMD $FUSEKI_ROOT/load-dbnary.sh
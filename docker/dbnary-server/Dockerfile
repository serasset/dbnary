# Inspired by many docker/fuseki configuration and mainly by fuseki's own docker file.
## This Dockefile builds a reduced footprint container.

# Use an open-jdk that support arm64v8 (apple M1 chips) AND amd64
FROM serasset/dbnary-jena-base

ARG JENA_VERSION
# Config and data volume
ARG FUSEKI_BASE=/data/fuseki

LABEL maintainer="gilles.serasset@gmail.com"
LABEL name="serasset/fuseki-dbnary"
LABEL org.opencontainers.image.source=https://gitlab.com/serasset/dbnary
LABEL org.opencontainers.image.title="Apache Jena Fuseki tailored to serve DBnary data"
LABEL org.opencontainers.image.description="Fuseki is a SPARQL 1.1 server with a web interface, backed by the Apache Jena TDB RDF triple store."
LABEL org.opencontainers.image.version=${JENA_VERSION}
LABEL org.opencontainers.image.licenses="(Apache-2.0 AND (GPL-2.0 WITH Classpath-exception-2.0) AND GPL-3.0)"
LABEL org.opencontainers.image.authors="Apache Jena Fuseki by https://jena.apache.org/; this image by serasset for dbnary project;"

ENV FUSEKI_BASE=${FUSEKI_BASE}

# Enable basic-auth with a preset admin password
COPY shiro.ini.tmpl $FUSEKI_ROOT/shiro.ini.tmpl
COPY docker-entrypoint.sh /

# Customising config. Will be copied over to FUSEKI_BASE by start-fuseki.sh
RUN mkdir -p $FUSEKI_ROOT/extra
COPY extra/* $FUSEKI_ROOT/extra

COPY start-fuseki.sh $FUSEKI_ROOT
RUN chmod 755 $FUSEKI_ROOT/start-fuseki.sh /docker-entrypoint.sh

ENV GOSU_USER fuseki:fuseki
# setting directories to be owned by the non-root user
ENV GOSU_CHOWN $FUSEKI_ROOT $FUSEKI_BASE

# Tools for loading data, index, and stats
ENV JAVA_CMD java -cp "$FUSEKI_ROOT/fuseki-server.jar:$FUSEKI_ROOT/extra/*"

WORKDIR $FUSEKI_ROOT
EXPOSE 3030
ENTRYPOINT ["/docker-entrypoint.sh"]
CMD $FUSEKI_ROOT/start-fuseki.sh
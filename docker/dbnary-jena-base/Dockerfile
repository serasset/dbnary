# Inspired by many docker/fuseki configuration and mainly by fuseki's own docker file.
## This Dockefile builds a reduced footprint container.

ARG OPENJDK_VERSION=17
ARG JENA_VERSION="4.4.0"

# Internal, passed between stages.
# Installation folder
ARG FUSEKI_ROOT=/fuseki
ARG FUSEKI_BIN=apache-jena-fuseki-${JENA_VERSION}

# Use an open-jdk that support arm64v8 (apple M1 chips) AND amd64
FROM openjdk:${OPENJDK_VERSION}-slim-bullseye

ARG JENA_VERSION
ARG FUSEKI_ROOT
ARG FUSEKI_BIN

LABEL maintainer="gilles.serasset@gmail.com"
LABEL name="serasset/fuseki-dbnary"
LABEL org.opencontainers.image.source=https://gitlab.com/serasset/dbnary
LABEL org.opencontainers.image.title="Base image with Apache Jena Fuseki to be used in other DBnary provided images"
LABEL org.opencontainers.image.version=${JENA_VERSION}
LABEL org.opencontainers.image.licenses="(Apache-2.0 AND (GPL-2.0 WITH Classpath-exception-2.0) AND GPL-3.0)"
LABEL org.opencontainers.image.authors="Apache Jena Fuseki by https://jena.apache.org/; this image by serasset for dbnary project;"

ARG REPO=https://dlcdn.apache.org/jena/binaries
ARG BIN_URL=${REPO}/${FUSEKI_BIN}.tar.gz

RUN apt-get update; \
    apt-get install -y --no-install-recommends \
       bash curl bzip2 ca-certificates findutils coreutils pwgen procps wget nano less

ENV FUSEKI_ROOT=${FUSEKI_ROOT}
ENV FUSEKI_HOME=${FUSEKI_ROOT}
ENV JENA_VERSION=${JENA_VERSION}
RUN mkdir ${FUSEKI_ROOT}

WORKDIR /tmp

# Download/unpack/move in one go (to reduce image size)
COPY download.sh .
RUN chmod a+x download.sh

RUN ./download.sh --chksum sha512 "$BIN_URL" \
    && tar zxvf ${FUSEKI_BIN}.tar.gz \
    && mv ${FUSEKI_BIN}/* $FUSEKI_ROOT \
    && rm ${FUSEKI_BIN}.tar.gz ${FUSEKI_BIN}.tar.gz.sha512 download.sh \
    && rmdir ${FUSEKI_BIN} \
    && cd $FUSEKI_ROOT && rm -rf fuseki.war \
    && chmod 755 $FUSEKI_ROOT/fuseki-server

WORKDIR ${FUSEKI_ROOT}

ENV GOSU_VERSION 1.12
RUN apt-get install -y gosu; \
    gosu nobody true

# Adding user without root privileges alongside a user group
RUN adduser --system --disabled-password --no-create-home --group fuseki

CMD bash
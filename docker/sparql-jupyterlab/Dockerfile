# Copyright (c) Gilles Sérasset - DBnary - Univ Grenoble Alpes.
# Distributed under the terms of the Modified BSD License.

# A Docker configuration where we can interract with sparql either using rdflib,
# or using sparql kernel
ARG BASE_CONTAINER=serasset/scipy-notebook:arm64
FROM $BASE_CONTAINER


LABEL maintainer="Jupyter Project <jupyter@googlegroups.com>"

USER root

# ffmpeg for matplotlib anim & dvipng+cm-super for latex labels
RUN apt-get update && \
    apt-get install -y --no-install-recommends graphviz && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

RUN pip install jupyter_contrib_nbextensions && jupyter contrib nbextension install

USER $NB_UID

RUN pip install --upgrade pip

# Install Python 3 packages
RUN pip install \
    'rdflib>=5.0.0' \
    'rdflib-jsonld==0.5.*' \
    'sparqlkernel==1.3.*' \
    'sparqlwrapper==1.8.*' && \
    fix-permissions "${CONDA_DIR}" && \
    fix-permissions "/home/${NB_USER}"

USER root

RUN jupyter sparqlkernel install

# Bug workaround for https://github.com/jamesdbrock/ihaskell-notebook/issues/9
RUN mkdir -p /home/jovyan/.local/share/jupyter/runtime \
    && fix-permissions /home/jovyan/.local \
    && fix-permissions /home/jovyan/.local/share \
    && fix-permissions /home/jovyan/.local/share/jupyter \
    && fix-permissions /home/jovyan/.local/share/jupyter/runtime

## Make Jupyter Lab the default as notebook will soon be deprecated.
ENV JUPYTER_ENABLE_LAB=yes

USER $NB_UID

WORKDIR $HOME

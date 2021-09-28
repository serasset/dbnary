# Copyright (c) Jupyter Development Team.
# Distributed under the terms of the Modified BSD License.
.PHONY: docs help test

# Use bash for inline if-statements in arch_patch target
SHELL:=bash
ARCH:=$(shell uname -m)
OWNER?=serasset

ALL_IMAGES:=sparql-jupyterlab

help:
# http://marmelab.com/blog/2016/02/29/auto-documented-makefile.html
	@echo "dbnary/docker"
	@echo "====================="
	@echo "Replace % with a docker configuration directory name (e.g., make build/sparql-jupyterlab)"
	@echo
	@grep -E '^[a-zA-Z0-9_%/-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

build/%: TAG?=arm64
build/%: PLATFORM?=arm64
build/%: DARGS?=
build/%: ## build the latest image for a stack
	docker buildx build $(DARGS) --platform "linux/$(PLATFORM)" --output type=docker --rm --force-rm \
	  --tag $(OWNER)/$(notdir $@):$(TAG) ./$(notdir $@)
	@echo -n "Built image size: "
	@docker images $(OWNER)/$(notdir $@):$(TAG) --format "{{.Size}}"

build-all: $(foreach I,$(ALL_IMAGES),arch_patch/$(I) build/$(I) ) ## build all stacks

pull/%: DARGS?=
pull/%: ## pull a jupyter image
	docker pull $(DARGS) $(OWNER)/$(notdir $@)

push/%: DARGS?=
push/%: ## push all tags for a jupyter image
	docker push --all-tags $(DARGS) $(OWNER)/$(notdir $@)

push-all: $(foreach I,$(ALL_IMAGES),push/$(I) ) ## push all tagged images

run/%: DARGS?=
run/%: ## run a bash in interactive mode in a stack
	docker run -it --rm $(DARGS) $(OWNER)/$(notdir $@) $(SHELL)

run-sudo/%: DARGS?=
run-sudo/%: ## run a bash in interactive mode as root in a stack
	docker run -it --rm -u root $(DARGS) $(OWNER)/$(notdir $@) $(SHELL)
# Using docker to use DBnary data

# Building jupyter notebook images for arm64

## Easy but maybe obsolete

* Use serasset/scipy-notbook or other images :
```
docker run --rm -p 8888:8888 -e JUPYTER_ENABLE_LAB=yes -v `pwd`/notebooks:/home/jovyan/work serasset/scipy-notebook:arm64
```

## Building the jupyter docker-stacks for arm :

* clone the github docker-stacks
* add the following inside Makefile : 
```Makefile 
# Determine this makefile's path.
# Be sure to place this BEFORE `include` directives, if any.
THIS_FILE:=$(lastword $(MAKEFILE_LIST))

build-arch/%: PLATFORM?=
build-arch/%: DARGS?=
build-arch/%: ## build a arch image for a stack
	@echo "Building $(OWNER)/$(notdir $@) for platform $(PLATFORM) ..."
	docker buildx build $(DARGS) --platform "linux/$(PLATFORM)"  \
		--output type=docker --rm --force-rm \
		--tag $(OWNER)/$(notdir $@):$(PLATFORM) ./$(notdir $@)
	@docker images $(OWNER)/$(notdir $@):$(PLATFORM) --format "{{.Size}}"

build-arm64: ## build arm64 images for a stack  
	@echo "Building arm64v8 images ..." 
	@$(MAKE) -f $(THIS_FILE) build-arch/base-notebook PLATFORM=arm64 \
	 	DARGS="--build-arg miniforge_arch=aarch64 --build-arg miniforge_checksum=7332318ef8c7de0ff29f146949972895412610449fd8a339463e09d70304c36e --build-arg BASE_CONTAINER=ubuntu:focal-20210401"
	@$(MAKE) -f $(THIS_FILE) build-arch/minimal-notebook PLATFORM=arm64 \
	 	DARGS="--build-arg BASE_CONTAINER=$(OWNER)/base-notebook:arm64"
	@$(MAKE) -f $(THIS_FILE) build-arch/scipy-notebook PLATFORM=arm64 \
	 	DARGS="--build-arg BASE_CONTAINER=$(OWNER)/minimal-notebook:arm64"
```
* launch make build-arm64 and wait with a good coffee.

### Evaluating performance against amd64 build

I created a small notbook that aproximates Pi. This notebook was evaluated on a MacBook pro M1 (ARM) 
with Docker for mac RC3. When executing the notebook in a jupyter/scipy-notebook container, it 
took >36s to run. On the serasset/scipy-notebook:arm64, it took 3.6 seconds (almost 10x less).

## Tensorflow for ARM M1 should be installed locally (no docker support)

Before installing;, make sure you install apple command line tools from XCode :

```shell
xcode-select --install
```

First install Miniconda (arm m1 mac os version):

```shell
wget https://github.com/conda-forge/miniforge/releases/latest/download/Miniforge3-MacOSX-arm64.sh
sh Miniforge3-MacOSX-arm64.sh
rm Miniforge3-MacOSX-arm64.sh
```
As I want to install tensorflow in a conda environment, I used the instructions available here at
https://github.com/apple/tensorflow_macos/issues/153 (these may be obsolete now, check with the
tensorflow web site).

```shell
# Should show 11.2 or higher
sw_vers -productVersion
# should show /usr/bin/xcrun (this means you have the developer tools correctly installed)
which xcrun
# get tensorflow environment to install correct dependencies
wget https://raw.githubusercontent.com/mwidjaja1/DSOnMacARM/main/environment.yml
# create the conda environment with all necessary dependencies
conda env create --file=environment.yml --name=tf_macos
conda activate tf_macos
pip install --upgrade --force --no-dependencies https://github.com/apple/tensorflow_macos/releases/download/v0.1alpha3/tensorflow_macos-0.1a3-cp38-cp38-macosx_11_0_arm64.whl https://github.com/apple/tensorflow_macos/releases/download/v0.1alpha3/tensorflow_addons_macos-0.1a3-cp38-cp38-macosx_11_0_arm64.whl
```

Then you can launch `python` and try `import tensoflow`. You can now install additional packages.

```shell
pip install jupyter
conda install scipy pandas matplotlib
```

## Pytorch does not run on mac os x arm64

At the time of writing, installing pytorch requires using a x86-64 python. Solution is to install another miniforge for 
intel and install pytorch under an environment created with intel miniconda and using an 
intel python. Have to check if it is still the case though.

Well I tried `conda install pytorch` using ma arm64 miniconda and it seems to work now (April 2021).

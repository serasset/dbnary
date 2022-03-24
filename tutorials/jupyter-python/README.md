# Setup the environment for tutorials

The tutorials use a docker image that allows to run jupyter notebooks. To prepare the environment, you should install docker on you machine.

## Viewing the notebook

* install docker
* launch docker-compose
  - ```docker-compose up```
  - copy the link that appears in the terminal (including the token) and paste it in your browser. 

## Shuting down the container
* type control-C twice
* alternatively if you lost the container terminal, use ```docker-control ps``` and ```docker-control down```
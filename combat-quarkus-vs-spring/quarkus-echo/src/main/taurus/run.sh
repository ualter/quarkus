#!/bin/bash

echo "Copy the Script"
rm -f /tmp/taurus_artifacts_quarkus/*.*
cp script_taurus_quarkus_echo.yaml /tmp/taurus_scripts/


OS=$(cat  /etc/os-release | grep ^NAME= | sed s/NAME=// | sed s/\"//g)
echo "OS --> $OS"

echo " <<<---- Running the Taurus ---->>>"
if [ "$OS" = "Ubuntu" ]; then
    export HOST_DOCKER_IP=`ifconfig enp0s3 | grep inet | awk 'FNR==1 {print $2}'`

    if [ -z "${HOST_DOCKER_IP}" ]; then
	     echo "HOST_DOCKER_IP not properly set!"
	     exit
	fi

	echo "Running in LINUX $OS @ ${HOST_DOCKER_IP}" 
    docker run --rm -v $PWD:/bzt-configs -v $PWD/artifacts:/tmp/artifacts -it blazemeter/taurus script_taurus_quarkus_echo.yaml -o settings.env.HOST_DOCKER_IP="${HOST_DOCKER_IP}"

else
    export HOST_DOCKER_IP=`ifconfig en0 | grep inet | awk 'FNR==2 {print $2}'`

    if [ -z "${HOST_DOCKER_IP}" ]; then
	     echo "HOST_DOCKER_IP not properly set!"
	     exit
	fi
	
	echo "Running in MACOS $OS @ ${HOST_DOCKER_IP}" 
    docker run --rm -v /tmp/taurus_scripts:/bzt-configs -v /tmp/taurus_artifacts_quarkus:/tmp/artifacts -it blazemeter/taurus script_taurus_quarkus_echo.yaml -o settings.env.HOST_DOCKER_IP="$HOST_DOCKER_IP"
fi

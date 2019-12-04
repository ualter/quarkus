#!/bin/sh

echo "Copy the Script"
rm -f /tmp/taurus_artifacts_quarkus/*.*
cp script_taurus_quarkus-echo.yaml /tmp/taurus_scripts/

export HOST_DOCKER_IP=`ifconfig en0 | grep inet | awk 'FNR==2 {print $2}'`

if [ "$HOST_DOCKER_IP" = "" ]; then
	echo "HOST_DOCKER_IP not properly set!"
	exit
fi

echo "Running the Taurus"
docker run --rm -v /tmp/taurus_scripts:/bzt-configs -v /tmp/taurus_artifacts_quarkus:/tmp/artifacts -it blazemeter/taurus script_taurus_quarkus-echo.yaml -o settings.env.HOST_DOCKER_IP="$HOST_DOCKER_IP"
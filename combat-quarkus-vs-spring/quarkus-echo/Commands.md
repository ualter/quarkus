#### Use the Maven Plugin to create a Quarkus squeleton app
```bash
mvn io.quarkus:quarkus-maven-plugin:1.0.0.CR2:create \
    -DprojectGroupId=ujr.combat.quarkus \
    -DprojectArtifactId=quarkus-echo \
    -DclassName="ujr.combat.quarkus.EchoResource" \
    -Dpath="/echo"
```

####
```bash
mvn compile quarkus:dev

curl -sw "\n\n" http://localhost:8081/echo/ualter | jq .
```

### Download and decompress the GraalVM
```bash
https://github.com/graalvm/graalvm-ce-builds/releases
For Quarkus 1.0 - GraalVM Version must be 19.2.1

```

#### We need the GraalVM environment variable set
```bash
export GRAALVM_HOME=/Users/ualter/Developer/quarkus/graalvm-ce-19.2.1/Contents/Home/
```

#### Instaling the native-image for GraalVM (Once)
```bash
# If problems with Developer not recognize on MacOs run..
sudo xattr -r -d com.apple.quarantine /Users/ualter/Developer/quarkus/graalvm-ce-19.2.1

/Users/ualter/Developer/quarkus/graalvm-ce-19.1.0/Contents/Home/bin/gu install native-image
```

## Running Quarkus Application - Native Executable 
#### Create a Native App of current Platform (MacOS) 
```bash
mvn package -Pnative
```
#### Create a Native App on a specific Plataform (Linux) - With this command we have a Native Linux executable of the this MicroServices (Everything in once file - JVM, App and Dependencies)
```bash
mvn package -Pnative -Dquarkus.native.container-build=true
#mvn package -Pnative -Dnative-image.docker-build=true 
```


### Generate the Docker Image to our App
```bash
docker build -t ujr/quarkus-echo -f src/main/docker/Dockerfile.native .
```

### Create a Container from our generated Docker Image
```bash
docker run -i --name quarkus-echo --rm -p 8081:8081 ujr/quarkus-echo 
```

### Docker Image for SpringBootBenchmarkApp
```bash
mvn install dockerfile:build  
docker run -i --rm -p 8082:8082 ualter/springboot-app

# Start Both Docker Containers
docker run --name springboot --rm -p 8082:8082 ujr/springboot-echo
docker run --name quarkus --rm -p 8081:8081 ujr/quarkus-echo
```


### Taurus (https://gettaurus.org/)
```bash
$ docker pull blazemeter/taurus

# Get the Host Docker IP (Only for Windows Env)
# Using the Busybox image, stored at environment variable:
$ export HOST_DOCKER_IP=`docker run busybox ping -c 1 host.docker.internal | awk 'FNR==2 {print $4}' | sed s'/.$//'`
# Manually (two steps)
## Use any running container image
$ docker exec -it IMAGE_ID
## Run the command below inside the container to get the IP  
$ nslookup host.docker.internal

## For macOS or Linux
$ export HOST_DOCKER_IP=`ifconfig en0 | grep inet | awk 'FNR==2 {print $2}'`

# Spring Boot
$ docker run --rm -v /tmp/taurus_scripts:/bzt-configs -v /tmp/taurus_artifacts_springboot:/tmp/artifacts -it blazemeter/taurus script_taurus_springboot-echo.yaml -o settings.env.HOST_DOCKER_IP=$HOST_DOCKER_IP

# Quarkus
$ docker run --rm -v /tmp/taurus_scripts:/bzt-configs -v /tmp/taurus_artifacts_quarkus:/tmp/artifacts -it blazemeter/taurus script_taurus_quarkus-echo.yaml -o settings.env.HOST_DOCKER_IP=$HOST_DOCKER_IP

# Extra commands
$ docker run -it blazemeter/taurus http://blazedemo.com
```

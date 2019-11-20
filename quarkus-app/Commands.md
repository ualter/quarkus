#### Use the Maven Plugin to create a Quarkus squeleton app
```bash
mvn io.quarkus:quarkus-maven-plugin:0.13.3:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=getting-started \
    -DclassName="org.acme.quickstart.GreetingResource" \
    -Dpath="/hello"
```

####
```bash
mvn compile quarkus:dev
```

#### Download and uncompress the GraalVM

#### We need the GraalVM environment variable set
```bash
export GRAALVM_HOME=/Users/ualter/Developer/quarkus/graalvm-ce-19.1.0/Contents/Home 
```

#### Instaling the native-image for GraalVM (Once)
```bash
/Users/ualter/Developer/quarkus/graalvm-ce-19.1.0/Contents/Home/bin/gu install native-image
```

### Create a Native Executable from the Running Quarkus Application
#### Create a Native App of current Platform (MacOS) 
```bash
mvn package -Pnative
```
#### Create a Native App on a specific Plataform (Linux) - With this command we have a Native Linux executable of the this MicroServices (Everything in once file - JVM, App and Dependencies)
```bash
mvn package -Pnative -Dnative-image.docker-build=true 
```


### Generate the Docker Image to our App
```bash
docker build -t ualter/quarkus-app -f src/main/docker/Dockerfile.native .
```

### Create a Container from our generated Docker Image
```bash
docker run -i --rm -p 8080:8080 ualter/quarkus-app 
```

### Docker Image for SpringBootBenchmarkApp
```bash
mvn install dockerfile:build  
docker run -i --rm -p 8087:8087 ualter/springboot-app
```

### Taurus (https://gettaurus.org/)
```bash
$ docker pull blazemeter/taurus

# Get the Host Docker IP
# Using the Busybox image, stored at environment variable:
$ HOST_DOCKER_IP=`docker run busybox ping -c 1 host.docker.internal | awk 'FNR==2 {print $4}' | sed s'/.$//'`
# Manually (two steps)
## Use any running container image
$ docker exec -it IMAGE_ID
## Run the command below inside the container to get the IP  
$ nslookup host.docker.internal


$ docker run --rm -v /tmp/taurus_scripts:/bzt-configs -v /tmp/taurus_artifacts:/tmp/artifacts -it blazemeter/taurus script_test_taurus.yaml -o settings.env.HOST_DOCKER_IP=$HOST_DOCKER_IP

$ docker run --rm -v /tmp/taurus_scripts:/bzt-configs -v /tmp/taurus_artifacts:/tmp/artifacts -it blazemeter/taurus script_test_taurus.yaml -o settings.env.HOST_DOCKER_IP=192.168.65.2 

# Extra commands
$ docker run -it blazemeter/taurus http://blazedemo.com
```

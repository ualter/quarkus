
### Docker Image
##### Login to Docker Hub
$ docker login
##### Push Image to Docker Hub
$ docker push ualter/quarkus-app

### Deploy at AWS Fargate
##### Create a ECS Cluster
$ aws ecs create-cluster  --cluster-name "quarkus" --tags key=project,value=quarkus
##### Check it the clusters
$ aws ecs list-clusters
##### Task-Definition Registration
$ aws ecs register-task-definition --cli-input-json file://task-definition.json
##### Service Creation



### CleanUp
#### Clean Service
#### Clean TaskDefinition
#### Clean Cluster

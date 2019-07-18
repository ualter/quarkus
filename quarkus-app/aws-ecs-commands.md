
### Docker Image
##### Login to Docker Hub
```bash
$ docker login
```
##### Push Image to Docker Hub
```bash
$ docker push ualter/quarkus-app
```

### Deploy at AWS Fargate
##### Create a ECS Cluster
```bash
$ aws ecs create-cluster  --cluster-name "quarkus" --tags key=project,value=quarkus
```
##### Check it the clusters
```bash
$ aws ecs list-clusters
```
##### Task-Definition Registration
```bash
$ aws ecs register-task-definition --cli-input-json file://task-definition.json
```

### Create ELB for the Container
```bash
$ aws elbv2 create-load-balancer \
                 --name elb-quarkus  \
                 --subnets subnet-df7c3f94 subnet-7996b000 subnet-144e7e4e \
                 --security-groups sg-0507cb12a7638f8b4
```
####### List the load balancer created 
```bash
$ aws elbv2 describe-load-balancers \
 | jq '.LoadBalancers[] | select( .LoadBalancerName | contains("quarkus")) | [.LoadBalancerName,.VpcId,.LoadBalancerArn,.AvailabilityZones]'
 ```
####### Create the Target Group for the ELB (same VPC ID)
```bash
$ aws elbv2 create-target-group --name target-groups-quarkus --protocol HTTP --port 8080 --vpc-id vpc-58ac7820 --target-type ip
```
####### List the Target Group created 
```bash
$ aws elbv2 describe-target-groups \
 | jq '.TargetGroups[] | select( .TargetGroupName | contains("quarkus")) | [.TargetGroupName,.VpcId,.TargetGroupArn]'
 ```
####### Create a Listener for ELB - Associate the Target Group to ELB
```bash
$ ELB=$(aws elbv2 describe-load-balancers  | jq -r '.LoadBalancers[] | select( .LoadBalancerName | contains("quarkus")) | .LoadBalancerArn')

$ TARGET_GROUP=$(aws elbv2 describe-target-groups \
 | jq -r '.TargetGroups[] | select( .TargetGroupName | contains("quarkus")) | .TargetGroupArn')

$ aws elbv2 create-listener --load-balancer-arn $ELB \
--protocol HTTP --port 8080  \
--default-actions Type=forward,TargetGroupArn=$TARGET_GROUP 
```

!!! BEFORE CHECK AWS CLI Create Role arn:aws:iam::933272457605:role/quarkus-ecs-role

##### Service Creation
```bash
$ aws ecs create-service --cli-input-json file://service-definition.json
```
####### Check Service Info
```bash
$ aws ecs describe-services --services "service-quarkus" --cluster "quarkus"

$ aws ecs describe-services --services "service-quarkus" --cluster "quarkus" | jq '[ .services[] | ("Service: " + .serviceName + "     Status: " + .status + "     Containers: " + (.runningCount|tostring)) ]'
```


##### Executing the Service
###### Set LoadBalancer DNSName Variable
```bash
$ ELB=$(aws elbv2 describe-load-balancers | jq -r '.LoadBalancers[] | select( .LoadBalancerName | contains("quarkus")) | .DNSName')

$ curl -vw "\n\n" http://$ELB:8080/hello/greeting/ualter
```

##### Service Update
####### Change the Task Number (Containers numbers)
```bash
$ aws ecs update-service --service service-quarkus --desired-count 3 --cluster quarkus
```


### CleanUp
##### Clean Service
```bash
$ aws ecs update-service --service service-quarkus --desired-count 0 --cluster quarkus

$ aws ecs delete-service --cluster "quarkus" --service "service-quarkus"
```
##### Clean TaskDefinition
```bash
$ aws ecs deregister-task-definition --task-definition "quarkus:1"
# or (all revisions, if more than 1)
$ for i in {1..10}; do aws ecs deregister-task-definition --task-definition quarkus:$i ; done
```
##### Clean Cluster
```bash
$ aws ecs delete-cluster --cluster quarkus
```
##### Clean ELB
```bash
$ ELB=$(aws elbv2 describe-load-balancers  | jq -r '.LoadBalancers[] | select( .LoadBalancerName | contains("quarkus")) | .LoadBalancerArn')

$ aws elbv2 delete-load-balancer --load-balancer-arn $ELB
```
##### Clean TargetGroup
```bash
$ TARGET_GROUP=$(aws elbv2 describe-target-groups \
 | jq -r '.TargetGroups[] | select( .TargetGroupName | contains("quarkus")) | .TargetGroupArn')
$ aws elbv2 delete-target-group --target-group-arn $TARGET_GROUP
```



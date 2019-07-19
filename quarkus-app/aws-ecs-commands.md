
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
$ aws ecs create-cluster  --cluster-name "quarkus"
```
##### Check it the clusters
```bash
$ aws ecs list-clusters
```
##### Task-Definition Registration
```bash
$ aws ecs register-task-definition --cli-input-json file://task-definition.json
```

##### Create ELB for the Container
```bash
# Choose one of your VPCs
 $ aws ec2 describe-vpcs | jq '.Vpcs[] | (" --------> VPC.....: " + .VpcId,.Tags,.CidrBlock)'
 
# Save its Vpc-ID
 $ VPC_ID=vpc-58ac7820
 
# Check all its Subnets
 $ aws ec2 describe-subnets --filters Name=vpc-id,Values=$VPC_ID
 
# List only the Subnets-IDs
 $ aws ec2 describe-subnets --filters Name=vpc-id,Values=$VPC_ID | jq '.Subnets[].SubnetId'
 
# Let's use them all
 $ SUBNETS_IDS=$(aws ec2 describe-subnets --filters Name=vpc-id,Values=$VPC_ID | jq -r '.Subnets[].SubnetId' | tr '\n' ' ')


# Create the Security Group (Ports 80, 8080)
 $ SG_ID=$(aws ec2 create-security-group  --description "quarkus-DMZ" --group-name "quarkus-DMZ" --vpc-id $VPC_ID | jq -r .GroupId) 
 $ aws ec2 authorize-security-group-ingress --group-id $SG_ID --ip-permissions IpProtocol=tcp,FromPort=8080,ToPort=8080,IpRanges=[{CidrIp=0.0.0.0/0}] IpProtocol=tcp,FromPort=80,ToPort=80,IpRanges=[{CidrIp=0.0.0.0/0}] IpProtocol=tcp,FromPort=8080,ToPort=8080,Ipv6Ranges=[{CidrIpv6=::/0}] IpProtocol=tcp,FromPort=80,ToPort=80,Ipv6Ranges=[{CidrIpv6=::/0}]

 

# Create the Load Balancer
 $ aws elbv2 create-load-balancer \
                 --name elb-quarkus  \
                 --subnets $SUBNETS_IDS \
                 --security-groups $SG_ID

# List the load balancer created 
 $ aws elbv2 describe-load-balancers \
 | jq '.LoadBalancers[] | select( .LoadBalancerName | contains("quarkus")) | [.LoadBalancerName,.VpcId,.LoadBalancerArn,.AvailabilityZones]'
 
# Create the Target Group for the ELB (same VPC ID)
 $ aws elbv2 create-target-group --name target-groups-quarkus --protocol HTTP --port 8080 --vpc-id $VPC_ID --target-type ip

# List the Target Group created 
 $ aws elbv2 describe-target-groups \
 | jq '.TargetGroups[] | select( .TargetGroupName | contains("quarkus")) | [.TargetGroupName,.VpcId,.TargetGroupArn]'

# Create a Listener for ELB - Associate this Target Group to ELB
 $ ELB=$(aws elbv2 describe-load-balancers  | jq -r '.LoadBalancers[] | select( .LoadBalancerName | contains("quarkus")) | .LoadBalancerArn')

 $ TARGET_GROUP=$(aws elbv2 describe-target-groups \
 | jq -r '.TargetGroups[] | select( .TargetGroupName | contains("quarkus")) | .TargetGroupArn')

 $ aws elbv2 create-listener --load-balancer-arn $ELB \
--protocol HTTP --port 8080  \
--default-actions Type=forward,TargetGroupArn=$TARGET_GROUP 
```

##### Service Creation
```bash
$ SUBNETS_SERVICE=$(aws ec2 describe-subnets --filters Name=vpc-id,Values=$VPC_ID | jq '.Subnets[].SubnetId' | tr '\n' ',' | sed 's/.$//')

$ cat service-definition.json  | sed 's/$TARGET_GROUP/$TARGET_GROUP/' | sed 's/ENABLED/mierda/'

$ aws ecs create-service --cli-input-json file://service-definition.json
```
##### Check Service Info
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



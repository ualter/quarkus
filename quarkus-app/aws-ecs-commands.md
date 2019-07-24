
## Docker Image Preparation
##### Login to Docker Hub
```bash
$ docker login
```
##### Push Image to Docker Hub
```bash
$ docker push ualter/quarkus-app
```

## Deploying at AWS Fargate
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
 # If this group already exist only put it its variable at the session
$ SG_ID=$(aws ec2 describe-security-groups | jq -r '.SecurityGroups[] | select( .Description | contains("quarkus")) | .GroupId')

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
 ## Save the ELB ARN to a env variable 
 $ ELB=$(aws elbv2 describe-load-balancers  | jq -r '.LoadBalancers[] | select( .LoadBalancerName | contains("quarkus")) | .LoadBalancerArn')

 ## Save the Target Group ELB to a env variable
 $ TARGET_GROUP=$(aws elbv2 describe-target-groups \
 | jq -r '.TargetGroups[] | select( .TargetGroupName | contains("quarkus")) | .TargetGroupArn')

 $ aws elbv2 create-listener --load-balancer-arn $ELB \
--protocol HTTP --port 8080  \
--default-actions Type=forward,TargetGroupArn=$TARGET_GROUP 
```

##### Service Creation
```bash
$ SUBNETS_SERVICE=$(aws ec2 describe-subnets --filters Name=vpc-id,Values=$VPC_ID | jq '.Subnets[].SubnetId' | tr '\n' ',' | sed 's/.$//')

# Replace the variables on the Service Creation with our Created Services ID
$ cat service-definition.json | sed 's~$TARGET_GROUP~'"$TARGET_GROUP"'~' | sed 's~$SUBNETS_SERVICE~'"$SUBNETS_SERVICE"'~' | sed 's~$SG_ID~'"$SG_ID"'~' > service-definition-ready.json
$ 

$ aws ecs create-service --cli-input-json file://service-definition-ready.json
```
##### Check Service Info
```bash
$ aws ecs describe-services --services "service-quarkus" --cluster "quarkus"

$ aws ecs describe-services --services "service-quarkus" --cluster "quarkus" | jq '[ .services[] | ("Service: " + .serviceName + "     Status: " + .status + "     Containers: " + (.runningCount|tostring)  + "  DesiredCount: " + (.desiredCount|tostring)) ]'
```


##### Executing the Service
###### Set LoadBalancer DNSName Variable
```bash
$ ELB_URL=$(aws elbv2 describe-load-balancers | jq -r '.LoadBalancers[] | select( .LoadBalancerName | contains("quarkus")) | .DNSName')

# $ curl -vw "\n\n" http://$ELB_URL:8080/hello/greeting/ualter
```

##### Service Update (Containers - Scale Out / Scale In)
####### Change the Task Number (Containers)
```bash
$ aws ecs update-service --service service-quarkus --desired-count 3 --cluster quarkus

# In case it's needed the command to save the Target Group Arn to a env variable (It should already exist this variable)
$ aws elbv2 describe-target-groups | jq -r '.TargetGroups[] | select(.TargetGroupName | contains("quarkus")) | .TargetGroupArn'

# List the Registered Targets (Ips and State) - Check the Scale out / Scale In
$ aws elbv2 describe-target-health --target-group-arn $TARGET_GROUP | jq '.TargetHealthDescriptions[]'
```

### More...
#### Apache Benchmark, Gnuplot

```bash
# Apache Benchmark
$ ab -n 50 -c 5 -g data.tsv "http://$ELB_URL:8080/hello/greeting/ualter/"

# gnuplot
$ brew install gnuplot #If not installed (Mac OS)

$ gnuplot apache-benchmark.p
$ gnuplot -e "TITLE='Requests'" -e "LINE='Response Time'" -e "IMAGE='benchmark.png'" apache-benchmark.p
$ open benchmark.png

$ gnuplot -e "TITLE='Requests'" -e "LINE='Response Time'" -e "PLOT=1" apache-benchmark.p
$ open benchmark-1.png

$ gnuplot -e "TITLE='Requests'" -e "LINE='Response Time'" -e "PLOT=4" apache-benchmark.p
$ open benchmark-4.png


```


## CleanUp
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



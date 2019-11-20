## ECS with AWS EC2 (AutoScaling)
##### Create a ECS Cluster
```bash
# Cluster
$ aws ecs create-cluster  --cluster-name "quarkus-ec2"

# Register Task Definition (Host NetWorkMode for EC2)
$ aws ecs register-task-definition --cli-input-json file://task-definition-host.json
```

##### Create LoadBalancer Configuration
```bash
## Create Target Group for Load Balancer

# Choose a VPC
$ aws ec2 describe-vpcs | jq '.Vpcs[] | (" --------> VPC.....: " + .VpcId,.Tags,.CidrBlock)'

# Save its ID
$ VPC_ID=vpc-58ac7820

# Create the Target Group
$ aws elbv2 create-target-group --name "ECSQuarkusTargetGroup" --protocol HTTP --port 8080 --vpc-id $VPC_ID 
// --target-type ip
// --target-type instance

# List Target Groups Created
$ aws elbv2 describe-target-groups  | jq '.TargetGroups[] | select( .TargetGroupName | contains("ECSQuarkus")) | [.TargetGroupName,.VpcId,.TargetGroupArn]'

# Create the Security Group (Ports 80, 8080) - If do not exist!
$ SG_ID=$(aws ec2 create-security-group  --description "quarkus-DMZ" --group-name "quarkus-DMZ" --vpc-id $VPC_ID | jq -r .GroupId) 
# Tag the Security Group
$ aws ec2 create-tags --resources $SG_ID --tags 'Key=Name,Value=Quarkus-EC2-Instance'
$ aws ec2 authorize-security-group-ingress --group-id $SG_ID --ip-permissions IpProtocol=tcp,FromPort=8080,ToPort=8080,IpRanges=[{CidrIp=0.0.0.0/0}] IpProtocol=tcp,FromPort=80,ToPort=80,IpRanges=[{CidrIp=0.0.0.0/0}] IpProtocol=tcp,FromPort=8080,ToPort=8080,Ipv6Ranges=[{CidrIpv6=::/0}] IpProtocol=tcp,FromPort=80,ToPort=80,Ipv6Ranges=[{CidrIpv6=::/0}]
# If this group already exist only put it its variable at the session
$ SG_ID=$(aws ec2 describe-security-groups | jq -r '.SecurityGroups[] | select( .Description | contains("quarkus")) | .GroupId')

# Create Load Balancer
# Save all the Subnets Ids for the Load Balancer
$ SUBNETS_IDS=$(aws ec2 describe-subnets --filters Name=vpc-id,Values=$VPC_ID | jq -r '.Subnets[].SubnetId' | tr '\n' ' ')
$ aws elbv2 create-load-balancer --name "ECSQuarkusLoadBalancer" --subnets $SUBNETS_IDS --security-groups $SG_ID 

# List Load Balancer
$ aws elbv2 describe-load-balancers  | jq '.LoadBalancers[] | select( .LoadBalancerName | contains("ECS")) | [.LoadBalancerName,.VpcId,.LoadBalancerArn,.AvailabilityZones]'

## Create a Listener for ELB - Associate this Target Group to ELB
# Save the ELB ARN to a env variable 
$ ELB=$(aws elbv2 describe-load-balancers  | jq -r '.LoadBalancers[] | select( .LoadBalancerName | contains("ECSQuarkus")) | .LoadBalancerArn')

# Save the Target Group ELB to a env variable
$ TARGET_GROUP=$(aws elbv2 describe-target-groups \
 | jq -r '.TargetGroups[] | select( .TargetGroupName | contains("ECSQuarkus")) | .TargetGroupArn')

# Create Listener for the Load Balancer with the created Target Group
$ aws elbv2 create-listener --load-balancer-arn $ELB \
--protocol HTTP --port 8080  \
--default-actions Type=forward,TargetGroupArn=$TARGET_GROUP
```

##### Create EC2 AutoScaling Configuration (Scale the EC2)
```bash
# Launch Configuration (EC2 AMI) - used for AutoScaling Configuration
$ aws autoscaling create-launch-configuration --launch-configuration-name "ECSQuarkusMachines" --image-id ami-077368b501184adb9 --security-groups $SG_ID --user-data file://userData.txt --instance-type t2.micro --iam-instance-profile ecsInstanceRole --key-name ec2-ualter

# Auto Scaling Group
$ SUBNETS_IDS_AUTOSCALING=$(aws ec2 describe-subnets --filters Name=vpc-id,Values=$VPC_ID | jq -r '.Subnets[].SubnetId' | tr '\n' ',')
$ aws autoscaling create-auto-scaling-group --auto-scaling-group-name "ECSQuarkusAutoScalingGroup" --launch-configuration-name "ECSQuarkusMachines" --min-size 1 --max-size 3 --desired-capacity 1 --vpc-zone-identifier $SUBNETS_IDS_AUTOSCALING --target-group-arns $TARGET_GROUP --health-check-type "EC2"  --health-check-grace-period 30 --default-cooldown 32 --tags Key=Name,Value=Quarkus-EC2-Instance,PropagateAtLaunch=true

# List instance created by the Auto Scaling (if min-size > 1)
$ aws ec2 describe-instances --filter "Name=tag-value,Values=ECSQuarkusAutoScalingGroup,Name=tag-key,Values=aws:autoscaling:groupName"

# Check Instances in the Cluster Quarkus-EC2
$ aws ecs list-container-instances --cluster quarkus-ec2
```

##### Create Service
```bash

# Preparing the JSON Template with the Values - Replace the variables
$ cat service-definition-ec2-template.json | sed 's~$TARGET_GROUP~'"$TARGET_GROUP"'~' > service-definition-ec2.json

$ aws ecs create-service --cli-input-json file://service-definition-ec2.json
```

##### Create Service Application AutoScaling Configuration (Scale the Containers)
```bash
$ aws application-autoscaling register-scalable-target --max-capacity "3" --min-capacity "1" --resource-id "service/quarkus-ec2/service-quarkus-ec2" --service-namespace "ecs" --scalable-dimension "ecs:service:DesiredCount"

$ aws application-autoscaling put-scaling-policy --resource-id "service/quarkus-ec2/service-quarkus-ec2" --policy-type "TargetTrackingScaling" --policy-name "AverageMemoryUtilization" --service-namespace "ecs" --scalable-dimension "ecs:service:DesiredCount" --target-tracking-scaling-policy-configuration '{"TargetValue":1,"ScaleOutCooldown":30,"ScaleInCooldown":30,"PredefinedMetricSpecification":{"PredefinedMetricType":"ECSServiceAverageMemoryUtilization"}}'
```

##### Test Service
```bash
# ELB URL
$ ELB_URL=$(aws elbv2 describe-load-balancers  | jq -r '.LoadBalancers[] | select( .LoadBalancerName | contains("ECSQuarkus")) | .DNSName')

$ curl -w "\n" $ELB_URL:8080/hello/greeting/ualter

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

# Clean Up
```bash
# Update AutoScaling Group MinSize to Zeros
$ aws autoscaling update-auto-scaling-group  --auto-scaling-group-name "ECSQuarkusAutoScalingGroup" --min-size 0 --desired-capacity 0 --max-size 0

# Delete AutoScaling Group
$ aws autoscaling delete-auto-scaling-group --auto-scaling-group-name "ECSQuarkusAutoScalingGroup" --force-delete

# Delete Launch Configuration
$ aws autoscaling delete-launch-configuration --launch-configuration-name "ECSQuarkusMachines"

# Delete Load Balancer / Target Group
$ aws elbv2 delete-load-balancer --load-balancer-arn $ELB
$ aws elbv2 delete-target-group --target-group-arn $TARGET_GROUP

# Delete Service
$ aws ecs update-service --service service-quarkus-ec2 --desired-count 0 --cluster quarkus-ec2
$ aws ecs delete-service --cluster "quarkus-ec2" --service "service-quarkus-ec2"
$ aws ecs delete-cluster --cluster quarkus-ec2

# Delete Task Definition
$ aws ecs deregister-task-definition --task-definition "quarkus:20"
# or (all revisions, if more than 1)
$ for i in {1..10}; do aws ecs deregister-task-definition --task-definition quarkus:$i ; done
```

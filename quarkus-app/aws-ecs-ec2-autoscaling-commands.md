
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
# Create Target Group for Load Balancer
$ aws elbv2 create-target-group --name "ECSQuarkusTargetGroup" --protocol HTTP --port 8080 --vpc-id $VPC_ID --target-type ip
// --target-type instance

# List Target Groups Created
$ aws elbv2 describe-target-groups  | jq '.TargetGroups[] | select( .TargetGroupName | contains("ECSQuarkus")) | [.TargetGroupName,.VpcId,.TargetGroupArn]'

# Create Load Balancer
$ aws elbv2 create-load-balancer --name "ECSQuarkusLoadBalancer" --subnets $SUBNETS_IDS --security-groups $SG_ID 

# List Load Balancer
$ aws elbv2 describe-load-balancers  | jq '.LoadBalancers[] | select( .LoadBalancerName | contains("ECS")) | [.LoadBalancerName,.VpcId,.LoadBalancerArn,.AvailabilityZones]'

# Create a Listener for ELB - Associate this Target Group to ELB
 ## Save the ELB ARN to a env variable 
 $ ELB=$(aws elbv2 describe-load-balancers  | jq -r '.LoadBalancers[] | select( .LoadBalancerName | contains("ECSQuarkus")) | .LoadBalancerArn')

 ## Save the Target Group ELB to a env variable
 $ TARGET_GROUP=$(aws elbv2 describe-target-groups \
 | jq -r '.TargetGroups[] | select( .TargetGroupName | contains("ECSQuarkus")) | .TargetGroupArn')

 $ aws elbv2 create-listener --load-balancer-arn $ELB \
--protocol HTTP --port 8080  \
--default-actions Type=forward,TargetGroupArn=$TARGET_GROUP

```

##### Create AutoScaling Configuration
```bash
# Launch Configuration (EC2 AMI)
$ aws autoscaling create-launch-configuration --launch-configuration-name "ECSQuarkusMachines" --image-id ami-077368b501184adb9 --security-groups $SG_ID --user-data file://userData.txt --instance-type t2.micro --iam-instance-profile ecsInstanceRole --key-name ec2-ualter

# Auto Scaling Group
SUBNETS_IDS_AUTOSCALING=subnet-df7c3f94,subnet-0c467624,subnet-144e7e4e,subnet-7996b000

$ SUBNETS_IDS_AUTOSCALING=$(aws ec2 describe-subnets --filters Name=vpc-id,Values=$VPC_ID | jq -r '.Subnets[].SubnetId' | tr '\n' ',')
$ aws autoscaling create-auto-scaling-group --auto-scaling-group-name "ECSQuarkusAutoScalingGroup" --launch-configuration-name "ECSQuarkusMachines" --min-size 1 --max-size 1 --desired-capacity 1 --vpc-zone-identifier $SUBNETS_IDS_AUTOSCALING --target-group-arns $TARGET_GROUP --health-check-type "ELB"  --health-check-grace-period 30

# List instance created by the Auto Scaling (if min-size > 1)
$ aws ec2 describe-instances --filter "Name=tag-value,Values=ECSQuarkusAutoScalingGroup,Name=tag-key,Values=aws:autoscaling:groupName"

# Check Instances in the Cluster Quarkus-EC2
$ aws ecs list-container-instances --cluster quarkus-ec2
```


##### Create Service
```bash
$ aws ecs create-service --cli-input-json file://service-definition-ec2.json
```

##### Test Service
```bash
# EC2 Instance IP
$ EC2_URL=$(aws ec2 describe-instances --filter "Name=tag-value,Values=Quarkus-EC2-Instance" | jq -r '.Reservations[] | .Instances[] | .PublicDnsName')

$ curl -w "\n" $EC2_URL:8080/hello/greeting/ualter
```

# Clean Up
```bash

$ aws ecs update-service --service service-quarkus-ec2 --desired-count 0 --cluster quarkus-ec2

# Update AutoScaling Group MinSize to Zeros
$ aws autoscaling update-auto-scaling-group  --auto-scaling-group-name "ECSQuarkusAutoScalingGroup" --min-size 0 --desired-capacity 0 --max-size 0

# Get the Intances Ids created for this AutoScaling Group 
 $ INSTANCES_ID=$(aws ec2 describe-instances --filter "Name=tag-value,Values=ECSQuarkusAutoScalingGroup,Name=tag-key,Values=aws:autoscaling:groupName" | jq -r '.Reservations[].Instances[].InstanceId' | tr '\n' ' ')

# Detach Instances from this AutoScaling Group 
$ aws autoscaling detach-instances --instance-ids $INSTANCES_IDS --auto-scaling-group-name "ECSQuarkusAutoScalingGroup"  --should-decrement-desired-capacity

# Shutdown Instances created
$ aws ec2 terminate-instances --instance-ids $INSTANCES_ID

# Delete AutoScaling Group
$  aws autoscaling delete-auto-scaling-group --auto-scaling-group-name "ECSQuarkusAutoScalingGroup"

# Delete Launch Configuration
$ aws autoscaling delete-launch-configuration --launch-configuration-name "ECSQuarkusMachines"

# Delete Load Balancer / Target Group
$ aws elbv2 delete-load-balancer --load-balancer-arn $ELB
$ aws elbv2 delete-target-group --target-group-arn $TARGET_GROUP

$ aws ecs delete-service --cluster "quarkus-ec2" --service "service-quarkus-ec2"

$ aws ecs delete-cluster --cluster quarkus-ec2
```
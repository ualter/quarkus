## ECS with AWS EC2 (Without LoadBalancer/TargetGroups)
##### Create a ECS Cluster
```bash
# Cluster
$ aws ecs create-cluster  --cluster-name "quarkus-ec2"

# Register Task Definition (Host NetWorkMode for EC2)
$ aws ecs register-task-definition --cli-input-json file://task-definition-host.json
```

##### Create EC2 Instance for the Cluster
```bash

# (Deprecated - With the name is enough)  Get the ecsInstanceRole ARN 
$ ECS_ROLE=$(aws iam list-instance-profiles | jq -r '.InstanceProfiles[] | .Roles[] | select(.RoleName|contains("ecsInstanceRole")) | .Arn')

# Create Instances EC2 for the ECS Cluster
$ aws ec2 run-instances --image-id ami-077368b501184adb9 --count 1 --instance-type t2.micro --key-name ec2-ualter --security-group-ids $SG_ID --subnet-id $(aws ec2 describe-subnets --filters Name=vpc-id,Values=$VPC_ID | jq -r '.Subnets[0].SubnetId') --iam-instance-profile Name=ecsInstanceRole --user-data file://userData.txt --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=Quarkus-EC2-Instance}]'

# List instance created
$ aws ec2 describe-instances --filter "Name=tag-value,Values=Quarkus-EC2-Instance"

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

$ aws ecs delete-service --cluster "quarkus-ec2" --service "service-quarkus-ec2"

$ aws ec2 terminate-instances --instance-ids $(aws ec2 describe-instances --filter "Name=tag-value,Values=Quarkus-EC2-Instance" | jq -r '.Reservations[] | .Instances[] | .InstanceId')

$ aws ecs delete-cluster --cluster quarkus-ec2
```
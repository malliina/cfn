# cfn

This repo contains [AWS CloudFormation](https://aws.amazon.com/cloudformation/) templates.

Based on [startup-kit-templates](https://github.com/aws-samples/startup-kit-templates).

## Infrastructure

- [ECR](https://aws.amazon.com/ecr/) repository
- [VPC](https://aws.amazon.com/documentation/vpc/)
- [Bastion host](https://docs.aws.amazon.com/quickstart/latest/linux-bastion/architecture.html)
- [Aurora database](https://aws.amazon.com/rds/aurora/)
- Single-container [Elastic Beanstalk](https://aws.amazon.com/elasticbeanstalk/) with [Docker](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/docker-basics.html)
- [CodePipeline](https://aws.amazon.com/codepipeline/) with [CodeBuild](https://aws.amazon.com/codebuild/)

The ECR repository contains build images used by CodeBuild to build the application deployed to Elastic Beanstalk.

## Usage

Create everything in one nested stack, or create the resources separately.

### Nested

Create

1. vpc-bastion-aurora-eb-ci

or without a database

1. vpc-bastion-eb-ci

### Separately

Create

1. vpc
1. bastion
1. aurora
1. elastic-docker-aurora
1. codepipeline-eb

or without a database

1. vpc
1. bastion
1. elastic-docker-nodb
1. codepipeline-eb

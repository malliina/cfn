# cfn

This repo contains [AWS CloudFormation](https://aws.amazon.com/cloudformation/) templates.

Based on [startup-kit-templates](https://github.com/aws-samples/startup-kit-templates). I used those templates as a 
starting point and modified them to my needs.

## Infrastructure

The templates create the following stack:

- [VPC](https://aws.amazon.com/documentation/vpc/) for networking
- [Bastion host](https://docs.aws.amazon.com/quickstart/latest/linux-bastion/architecture.html)
- [ECR](https://aws.amazon.com/ecr/) repository for Docker images
- [Aurora database](https://aws.amazon.com/rds/aurora/) for persistence
- Single-container [Elastic Beanstalk](https://aws.amazon.com/elasticbeanstalk/) with [Docker](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/docker-basics.html) for applications
- [CodePipeline](https://aws.amazon.com/codepipeline/) with [CodeBuild](https://aws.amazon.com/codebuild/) for CI
- [CloudFront](https://aws.amazon.com/cloudfront/) CDN for HTTP requests and assets
- [Route 53](https://aws.amazon.com/route53/) for DNS

The ECR repository contains build images used by CodeBuild to build applications. CodePipeline fetches application code 
from [GitHub](https://github.com/), then builds and deploys the app to Elastic Beanstalk. Route 53 routes incoming 
requests to the Beanstalk app via CloudFront.

## Creation

Create everything in one nested stack, or create the resources separately. Add `.cfn.yml` to the below names to find the
relevant files.

### Nested

Create

1. vpc-bastion-aurora-eb-ci-cf-r53

or without a database

1. vpc-bastion-eb-ci-cf-r53

### Standalone

Create

1. vpc
1. bastion
1. aurora
1. elastic-docker-aurora
1. codepipeline-eb
1. cloudfront
1. route53

or without a database

1. vpc
1. bastion
1. elastic-docker-nodb
1. codepipeline-eb
1. cloudfront
1. route53

You may want to create the resources sequentially due to dependencies. However, you can omit e.g. Route 53 if you don't 
use it.

# cfn

This repo contains [AWS CloudFormation](https://aws.amazon.com/cloudformation/) templates. They help you get started
with application development on AWS.

I used [startup-kit-templates](https://github.com/aws-samples/startup-kit-templates) as a starting point and modified 
them to my needs.

## Infrastructure

The templates define the following components:

- [VPC](https://aws.amazon.com/documentation/vpc/) for networking
- [Bastion host](https://docs.aws.amazon.com/quickstart/latest/linux-bastion/architecture.html) for SSH access
- [ECR](https://aws.amazon.com/ecr/) repository for Docker images
- [Aurora database](https://aws.amazon.com/rds/aurora/) for persistence
- Single-container [Elastic Beanstalk](https://aws.amazon.com/elasticbeanstalk/) with [Docker](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/docker-basics.html) for applications
- [CodePipeline](https://aws.amazon.com/codepipeline/) with [CodeBuild](https://aws.amazon.com/codebuild/) for CI
- [CloudFront](https://aws.amazon.com/cloudfront/) CDN for HTTP requests and assets
- [Route 53](https://aws.amazon.com/route53/) for DNS
- [AWS Lambda](https://aws.amazon.com/lambda/) for serverless computing

The ECR repository stores build images used by CodeBuild to build applications. CodePipeline fetches application code 
from [GitHub](https://github.com/), then builds and deploys the app to Elastic Beanstalk. Route 53 routes incoming 
requests to the Beanstalk app via CloudFront.

This [small web app](https://github.com/malliina/play-docka) is used as the default application to build and deploy 
unless otherwise specified.

## Creation

Create an ECR repository and CodeBuild project for build images using [ecr](build-images/ecr.cfn.yml). Push an image
to the repo by running the created CodeBuild project, then proceed with creating more CloudFormation stacks below.

Create multiple resources from one nested stack, or create the resources separately. Add `.cfn.yml` to the below names 
to find the relevant files.

### Nested

Create

1. [vpc-bastion-aurora-eb-ci-cf-r53](cfn-demo-templates/vpc-bastion-aurora-eb-ci.cfn.yml)

or without any Aurora database

1. [vpc-bastion-eb-ci-cf-r53](cfn-demo-templates/vpc-bastion-eb-ci-cf-r53.cfn.yml)

The nested templates must be available in an S3 bucket, for example `cfn-demo-templates`.

### Standalone

Create

1. [vpc](cfn-demo-templates/vpc.cfn.yml)
1. [bastion](cfn-demo-templates/bastion.cfn.yml)
1. [aurora](cfn-demo-templates/aurora.cfn.yml) (optionally)
1. [beanstalk-aurora](cfn-demo-templates/beanstalk-aurora.cfn.yml) or without a database [beanstalk-nodb](cfn-demo-templates/beanstalk-nodb.cfn.yml)
1. [codepipeline](cfn-demo-templates/codepipeline.cfn.yml)
1. [cloudfront](cfn-demo-templates/cloudfront.cfn.yml)
1. [route53](cfn-demo-templates/route53.cfn.yml)
1. [redis](cfn-demo-templates/redis.cfn.yml)

You may want to create the resources sequentially due to dependencies. However, you can omit e.g. Route 53 if you don't 
use it.

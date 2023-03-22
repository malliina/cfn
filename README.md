# cfn

CDK and bicep sample code.

## CDK

### Bastion

To connect to RDS, craft a tunnel via the bastion host:

    aws ec2-instance-connect send-ssh-public-key \
        --instance-id <bastion-instance-id> \
        --instance-os-user ec2-user \
        --ssh-public-key file:///path/to/.ssh/id_rsa.pub
    ssh -f -N -L 3307:<rds-hostname>.eu-west-1.rds.amazonaws.com:3306 ec2-user@<bastion-dns-name> -v

Then connect to:

    jdbc:mysql://localhost:3307

### Amplify

Deploy the infra with

    cdk deploy amplify

Check [amplify.yml](amplify.yml) for details.

Verify the custom domain after the initial deployment in the AWS Console under Amplify.

The infra generates a CodeCommit repository. Add a git origin that points to the CodeCommit repo:

    git remote add amplify codecommit::eu-west-1://amplify

Push to the origin to trigger a deployment:

    git push amplify

### OpenSearch

The initial setup is not fully automated. First deploy with

    cdk deploy opensearch

1. OpenSearch creates a Lambda that streams CloudWatch logs to the domain. Assign the `StreamingRole` to the Lambda.
2. To access the dashboard, create a user in the Opensearch Cognito user pool and log in.
3. Follow the role mapping guidance in https://aws.amazon.com/premiumsupport/knowledge-center/opensearch-troubleshoot-cloudwatch-logs/ to add the `StreamingRole` as a backend role in OpenSearch Dashboards.
4. Create an index pattern in OpenSearch Dashboards. The pattern is most likely `cwl-*` for CloudWatch Logs.

## Azure with bicep

[Install](https://learn.microsoft.com/en-us/cli/azure/install-azure-cli) Azure CLI.

Create a resource group (unless you already have one):

    az group create --name exampleRG --location northeurope

Deploy the resources to the resource group:

    az deployment group create --resource-group exampleRG --template-file infra.bicep

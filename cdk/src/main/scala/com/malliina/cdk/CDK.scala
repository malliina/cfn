package com.malliina.cdk

import com.malliina.cdk.S3Redirect.RedirectConf
import com.malliina.cdk.S3WebsiteStack.WebsiteConf
import com.malliina.cdk.opensearch.OpenSearch
import software.amazon.awscdk.services.ec2.{Vpc, VpcLookupOptions}
import software.amazon.awscdk.{Environment, StackProps, App as AWSApp}

object CDK:
  val stackProps: StackProps =
    StackProps
      .builder()
      .env(
        Environment
          .builder()
          .account("297686094835")
          .region("eu-west-1")
          .build()
      )
      .build()

  def main(args: Array[String]): Unit =
    val app = new AWSApp()

    val vpc = VPCStack(app, "cdkvpc")

    val websiteConf =
      WebsiteConf(
        "cdk.malliina.site",
        "/global/route53/zone",
        "/global/certificates/arn"
      )
    val website = S3WebsiteStack(websiteConf, app, "s3-website")
    val redirect = S3Redirect(
      RedirectConf(
        "old.malliina.site",
        websiteConf.domain,
        websiteConf.hostedZoneParamName,
        websiteConf.certificateParamName
      ),
      app,
      "cdk-redirect"
    )
    val amplifyApp =
      AmplifyStack(AmplifyConf(domainName = None), app, "amplify")
    val search = OpenSearch.stack(app, "opensearch")
    val database = AuroraServerless.stack(
      Env.Qa,
      "ref",
      vpc.vpc,
      vpc.bastionSecurityGroups.map(_.getSecurityGroupId),
      app
    )

    val assembly = app.synth()

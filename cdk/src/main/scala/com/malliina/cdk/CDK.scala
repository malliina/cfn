package com.malliina.cdk

import com.malliina.cdk.AmplifyStack.AmplifyConf
import com.malliina.cdk.S3Redirect.RedirectConf
import com.malliina.cdk.S3WebsiteStack.WebsiteConf
import com.malliina.cdk.opensearch.Opensearch
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
    val website = S3WebsiteStack(websiteConf, app, "cdk-website")
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
    val amplifyApp = AmplifyStack(AmplifyConf("malliina.site"), app, "amplify")
    val search = Opensearch.stack(app, "opensearch")
    val database = AuroraServerless.stack(
      Env.Qa,
      "ref",
      vpc.vpc,
      vpc.bastionSecurityGroup.getSecurityGroupId,
      app
    )

    val assembly = app.synth()

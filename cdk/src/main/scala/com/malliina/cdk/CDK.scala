package com.malliina.cdk

import com.malliina.cdk.AmplifyStack.AmplifyConf
import com.malliina.cdk.S3Redirect.RedirectConf
import com.malliina.cdk.S3WebsiteStack.WebsiteConf
import software.amazon.awscdk.core.{Environment, StackProps, App => AWSApp}

object CDK {
  val stackProps =
    StackProps
      .builder()
      .env(Environment.builder().account("297686094835").region("eu-west-1").build())
      .build()

  def main(args: Array[String]): Unit = {
    val app = new AWSApp()

    val websiteConf =
      WebsiteConf("cdk.malliina.site", "/global/route53/zone", "/global/certificates/arn")
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
    val amplifyApp = AmplifyStack(AmplifyConf(42), app, "amplify")

    val assembly = app.synth()
  }
}

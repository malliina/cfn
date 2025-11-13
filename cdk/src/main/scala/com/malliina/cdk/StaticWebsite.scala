package com.malliina.cdk

import com.malliina.cdk.StaticWebsite.StaticConf
import software.amazon.awscdk.services.cloudfront.{CfnDistribution, S3OriginAccessControl, Signing}
import software.amazon.awscdk.services.cloudfront.CfnDistribution.*
import software.amazon.awscdk.services.iam.{Effect, PolicyDocument, PolicyStatement}
import software.amazon.awscdk.services.s3.{Bucket, BucketPolicy}
import software.amazon.awscdk.{RemovalPolicy, Stack}
import software.constructs.Construct

object StaticWebsite:
  case class StaticConf(
    domain: String,
    certificateParamName: String
  )

class StaticWebsite(conf: StaticConf, scope: Construct, stackName: String)
  extends Stack(scope, stackName, CDK.stackProps)
  with CDKSyntax:
  val stack = this
  override val construct: Construct = stack

  val indexDocument = "index.html"

  val bucket: Bucket = Bucket.Builder
    .create(stack, "bucket")
    .make: b =>
      b.removalPolicy(RemovalPolicy.DESTROY)
        .autoDeleteObjects(true)
  val viewerProtocolPolicy = "redirect-to-https"
  val bucketOrigin = "bucket"
  val oac = S3OriginAccessControl.Builder.create(stack, "oac").signing(Signing.SIGV4_ALWAYS).build()
  val cloudFront: CfnDistribution = CfnDistribution.Builder
    .create(stack, "cloudfront")
    .make: b =>
      b.distributionConfig(
        DistributionConfigProperty
          .builder()
          .make: b =>
            b
              .aliases(list("s3.malliina.com"))
              .enabled(true)
              .defaultRootObject(indexDocument)
              .customErrorResponses(
                list(
                  CustomErrorResponseProperty
                    .builder()
                    .make: rp =>
                      rp.errorCode(404).responseCode(404).responsePagePath("/404.html"),
                  CustomErrorResponseProperty
                    .builder()
                    .make: rp =>
                      rp.errorCode(403).responseCode(403).responsePagePath("/403.html")
                )
              )
              .defaultCacheBehavior(
                DefaultCacheBehaviorProperty
                  .builder()
                  .make: b =>
                    b.allowedMethods(list("HEAD", "GET"))
                      .targetOriginId(bucketOrigin)
                      .forwardedValues(
                        ForwardedValuesProperty
                          .builder()
                          .make: b =>
                            b.queryString(true)
                              .cookies(CookiesProperty.builder().make(_.forward("all")))
                      )
                      .viewerProtocolPolicy(viewerProtocolPolicy)
              )
              .origins(
                list(
                  OriginProperty
                    .builder()
                    .make: b =>
                      b.domainName(bucket.getBucketDomainName)
                        .id(bucketOrigin)
                        .originAccessControlId(oac.getOriginAccessControlId)
                        .s3OriginConfig(S3OriginConfigProperty.builder().build())
                )
              )
              .viewerCertificate(
                ViewerCertificateProperty
                  .builder()
                  .make: b =>
                    b.acmCertificateArn(stringParameter(conf.certificateParamName))
                      .sslSupportMethod("sni-only")
              )
      )

  val cloudFrontArn =
    s"arn:aws:cloudfront::$getAccount:distribution/${cloudFront.getDistributionRef.getDistributionId}"
  val bp = BucketPolicy.Builder
    .create(stack, "bucketpolicy")
    .make: b =>
      b.bucket(bucket)
        .document(
          PolicyDocument.Builder
            .create()
            .make: pd =>
              pd.statements(
                list(
                  PolicyStatement.Builder
                    .create()
                    .make: s =>
                      s.actions(list("s3:GetObject"))
                        .principals(list(principal("cloudfront.amazonaws.com")))
                        .effect(Effect.ALLOW)
                        .resources(list(s"${bucket.getBucketArn}/*"))
                        .conditions(map("StringEquals" -> map("AWS:SourceArn" -> cloudFrontArn)))
                )
              )
        )
  val outs = outputs(stack)(
    "BucketDomain" -> bucket.getBucketDomainName,
    "WebsiteURL" -> bucket.getBucketWebsiteUrl,
    "CloudFrontDomainName" -> cloudFront.getAttrDomainName
  )

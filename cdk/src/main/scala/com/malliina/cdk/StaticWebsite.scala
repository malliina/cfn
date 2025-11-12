package com.malliina.cdk

import com.malliina.cdk.StaticWebsite.StaticConf
import software.amazon.awscdk.services.cloudfront.CfnDistribution
import software.amazon.awscdk.services.cloudfront.CfnDistribution.*
import software.amazon.awscdk.services.iam.{AnyPrincipal, PolicyStatement}
import software.amazon.awscdk.services.s3.{BlockPublicAccess, Bucket}
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

  val headerName = "Referer"
  val secretHeader = "secret"

  val bucket = Bucket.Builder
    .create(stack, "bucket")
    .make: b =>
      b.websiteIndexDocument(indexDocument)
        .websiteErrorDocument("error.html")
        .removalPolicy(RemovalPolicy.RETAIN)
        .blockPublicAccess(BlockPublicAccess.Builder.create().make(_.blockPublicPolicy(false)))
  bucket.addToResourcePolicy(
    PolicyStatement.Builder
      .create()
      .make: b =>
        b.principals(list(new AnyPrincipal()))
          .actions(list("s3:GetObject"))
          .resources(list(s"${bucket.getBucketArn}/*"))
          .conditions(
            map("StringEquals" -> map(s"aws:$headerName" -> list(secretHeader)))
          )
  )
  val viewerProtocolPolicy = "redirect-to-https"
  val bucketOrigin = "bucket"
  val cloudFront = CfnDistribution.Builder
    .create(stack, "cloudfront")
    .make: b =>
      b.distributionConfig(
        DistributionConfigProperty
          .builder()
          .make: b =>
            b.comment(s"Static website at ${conf.domain}")
              .enabled(true)
              .defaultRootObject(indexDocument)
              .aliases(list(conf.domain))
              .cacheBehaviors(
                list(
                  CacheBehaviorProperty
                    .builder()
                    .make: b =>
                      b.allowedMethods(
                        list("HEAD", "GET", "POST", "PUT", "PATCH", "OPTIONS", "DELETE")
                      ).pathPattern("assets/*")
                        .targetOriginId(bucketOrigin)
                        .forwardedValues(
                          ForwardedValuesProperty
                            .builder()
                            .make: b =>
                              b.queryString(true)
                                .cookies(CookiesProperty.builder().make(_.forward("none")))
                        )
                        .viewerProtocolPolicy(viewerProtocolPolicy)
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
                              .headers(list("Authorization"))
                              .cookies(CookiesProperty.builder().make(_.forward("all")))
                      )
                      .viewerProtocolPolicy(viewerProtocolPolicy)
              )
              .origins(
                list(
                  OriginProperty
                    .builder()
                    .make: b =>
                      b.domainName(bucket.getBucketWebsiteDomainName)
                        .id(bucketOrigin)
                        .customOriginConfig(
                          CustomOriginConfigProperty
                            .builder()
                            .make: b =>
                              b.originProtocolPolicy("http-only")
                        )
                        .originCustomHeaders(
                          list(
                            OriginCustomHeaderProperty
                              .builder()
                              .make: b =>
                                b.headerName(headerName)
                                  .headerValue(secretHeader)
                          )
                        )
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

  val outs = outputs(stack)(
    "WebsiteURL" -> bucket.getBucketWebsiteUrl,
    "CloudFrontDomainName" -> cloudFront.getAttrDomainName
  )

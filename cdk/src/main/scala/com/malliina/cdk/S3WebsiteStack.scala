package com.malliina.cdk

import com.malliina.cdk.S3WebsiteStack.WebsiteConf
import software.amazon.awscdk.services.cloudfront.CfnDistribution
import software.amazon.awscdk.services.cloudfront.CfnDistribution.*
import software.amazon.awscdk.services.iam.{AnyPrincipal, PolicyStatement}
import software.amazon.awscdk.services.route53.CfnRecordSet
import software.amazon.awscdk.services.route53.CfnRecordSet.AliasTargetProperty
import software.amazon.awscdk.services.route53.targets.CloudFrontTarget
import software.amazon.awscdk.services.s3.Bucket
import software.amazon.awscdk.{RemovalPolicy, Stack}
import software.constructs.Construct

object S3WebsiteStack:
  case class WebsiteConf(
    domain: String,
    hostedZoneParamName: Option[String],
    certificateParamName: String
  )

class S3WebsiteStack(conf: WebsiteConf, scope: Construct, stackName: String)
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
            b.comment(s"Website hosting for ${conf.domain}")
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
  conf.hostedZoneParamName.foreach: hostedZoneParam =>
    val dns = CfnRecordSet.Builder
      .create(stack, "dns")
      .make: b =>
        b.name(conf.domain)
          .hostedZoneId(stringParameter(hostedZoneParam))
          .`type`("A")
          .aliasTarget(
            AliasTargetProperty
              .builder()
              .make: b =>
                b.dnsName(cloudFront.getAttrDomainName)
                  .hostedZoneId(CloudFrontTarget.CLOUDFRONT_ZONE_ID)
          )
    val dnsOut = outputs(stack)(
      "DomainName" -> dns.getRef
    )

  val outs = outputs(stack)(
    "WebsiteURL" -> bucket.getBucketWebsiteUrl,
    "CloudFrontDomainName" -> cloudFront.getAttrDomainName
  )

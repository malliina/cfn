package com.malliina.cdk

import com.malliina.cdk.VPCStack.CIDRs
import software.amazon.awscdk.core.{Construct, Stack}
import software.amazon.awscdk.services.ec2._

import scala.jdk.CollectionConverters.ListHasAsScala
object VPCStack {
  def apply(scope: Construct, stackName: String, cidrs: CIDRs = CIDRs.default) =
    new VPCStack(scope, stackName, cidrs)

  case class CIDRs(
    vpc: String,
    public1: String,
    public2: String,
    private1: String,
    private2: String
  )
  object CIDRs {
    val default =
      CIDRs("10.50.0.0/16", "10.50.0.0/24", "10.50.1.0/24", "10.50.64.0/19", "10.50.96.0/19")
    val default2 =
      CIDRs(
        "172.16.0.0/16",
        "172.16.64.0/24",
        "172.16.128.0/24",
        "172.16.64.0/19",
        "172.16.96.0/19"
      )
  }
}

class VPCStack(scope: Construct, stackName: String, cidrs: CIDRs)
  extends Stack(scope, stackName, CDK.stackProps)
  with CDKSyntax {
  val stack = this

  val vpc = Vpc.Builder
    .create(stack, "VPC")
    .cidr(cidrs.vpc)
    .enableDnsSupport(true)
    .enableDnsHostnames(true)
    .maxAzs(2)
    .build()
  val azs = vpc.getAvailabilityZones.asScala
  val az1 = azs(0)
  val az2 = azs(1)
  val public1 = subnet("PublicSubnet1", cidrs.public1, az1)
//  val public2 = subnet("PublicSubnet2", cidrs.public2, az2)
//  val private1 = subnet("PrivateSubnet1", cidrs.private1, az1)
//  val private2 = subnet("PrivateSubnet2", cidrs.private2, az2)
//  val igw = CfnInternetGateway.Builder
//    .create(stack, "InternetGateway")
//    .tags(tagList("Name" -> s"$stackName-igw"))
//    .build()
//  val gatewayAttachment = CfnVPCGatewayAttachment.Builder
//    .create(stack, "GatewayAttachment")
//    .vpcId(vpc.getVpcId)
//    .internetGatewayId(igw.getRef)
//    .build()
//  val routeTable = CfnRouteTable.Builder
//    .create(stack, "RouteTable")
//    .vpcId(vpc.getVpcId)
//    .tags(tagList("Name" -> s"$stackName-public-igw"))
//    .build()
//  val publicRoute = CfnRoute.Builder
//    .create(stack, "PublicRoute")
//    .routeTableId(routeTable.getRef)
//    .destinationCidrBlock("0.0.0.0/0")
//    .gatewayId(igw.getRef)
//    .build()
//  val psrta1 = routeAssociation("PSRTA1", public1, routeTable)
//  val psrta2 = routeAssociation("PSRTA2", public2, routeTable)
//  val snaa1 = aclAssociation("SNAA1", public1)
//  val snaa2 = aclAssociation("SNAA2", public2)

  private def aclAssociation(id: String, subnet: ISubnet) =
    SubnetNetworkAclAssociation.Builder
      .create(stack, id)
      .subnet(subnet)
      .networkAcl(NetworkAcl.fromNetworkAclId(stack, s"ACL-$id", vpc.getVpcDefaultNetworkAcl))
      .build()
  private def routeAssociation(id: String, subnet: Subnet, routeTable: CfnRouteTable) =
    CfnSubnetRouteTableAssociation.Builder
      .create(stack, id)
      .subnetId(subnet.getSubnetId)
      .routeTableId(routeTable.getRef)
      .build()

  private def subnet(id: String, cidr: String, az: String) =
    Subnet.Builder
      .create(stack, id)
      .vpcId(vpc.getVpcId)
      .cidrBlock(cidr)
      .availabilityZone(az)
      .build()
}

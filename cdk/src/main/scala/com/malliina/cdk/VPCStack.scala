package com.malliina.cdk

import com.malliina.cdk.VPCStack.CIDRs
import software.amazon.awscdk.Stack
import software.amazon.awscdk.services.ec2.*
import software.constructs.Construct

import scala.jdk.CollectionConverters.ListHasAsScala

object VPCStack:
  def apply(
    scope: Construct,
    stackName: String,
    cidrs: CIDRs = CIDRs.default2
  ) =
    new VPCStack(scope, stackName, cidrs)

  case class CIDRs(
    vpc: String,
    public1: String,
    public2: String,
    private1: String,
    private2: String
  )
  object CIDRs:
    val default =
      CIDRs(
        "10.50.0.0/16",
        "10.50.0.0/24",
        "10.50.1.0/24",
        "10.50.64.0/19",
        "10.50.96.0/19"
      )
    val default2 =
      CIDRs(
        "172.16.0.0/16",
        "172.16.64.0/24",
        "172.16.128.0/24",
        "172.16.64.0/19",
        "172.16.96.0/19"
      )

class VPCStack(scope: Construct, stackName: String, cidrs: CIDRs)
  extends Stack(scope, stackName, CDK.stackProps)
  with CDKSyntax:
  val stack = this

  /** This creates a VPC along with private, public subnets, etc networking
    * resources.
    */
  val vpc = Vpc.Builder
    .create(stack, "VPC")
    .ipAddresses(IpAddresses.cidr(cidrs.vpc))
    .enableDnsSupport(true)
    .enableDnsHostnames(true)
    .maxAzs(2)
    .build()
  val azs = vpc.getAvailabilityZones.asScala
  val az1 = azs(0)
  val az2 = azs(1)

  val bastion = BastionHostLinux.Builder
    .create(stack, "BastionHost")
    .vpc(vpc)
    .subnetSelection(
      SubnetSelection.builder().subnetType(SubnetType.PUBLIC).build()
    )
    .build()
  bastion.allowSshAccessFrom(Peer.anyIpv4())
  val bastionSecurityGroups =
    bastion.getInstance().getConnections.getSecurityGroups.asScala.toList
  outputs(stack)(
    "BastionPublicDnsName" -> bastion.getInstancePublicDnsName,
    "BastionPublicIp" -> bastion.getInstancePublicIp,
    "VPCId" -> vpc.getVpcId,
    "VPCArn" -> vpc.getVpcArn,
    "PrivateSubnetCIDRs" -> vpc.getPrivateSubnets.asScala
      .map(_.getIpv4CidrBlock)
      .mkString(", "),
    "PublicSubnetCIDRs" -> vpc.getPublicSubnets.asScala
      .map(_.getIpv4CidrBlock)
      .mkString(", ")
  )

trait VPCSyntax:
  def stack: Stack
  def vpc: Vpc

  private def aclAssociation(id: String, subnet: ISubnet) =
    SubnetNetworkAclAssociation.Builder
      .create(stack, id)
      .subnet(subnet)
      .networkAcl(
        NetworkAcl
          .fromNetworkAclId(stack, s"ACL-$id", vpc.getVpcDefaultNetworkAcl)
      )
      .build()
  private def routeAssociation(
    id: String,
    subnet: Subnet,
    routeTable: CfnRouteTable
  ) =
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

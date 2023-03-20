param location string = resourceGroup().location
param uniqueId string = uniqueString(resourceGroup().id)

resource vnet 'Microsoft.Network/virtualNetworks@2022-09-01' = {
  name: 'demo-vnet-${uniqueId}'
  location: location
  properties: {
    addressSpace: {
      addressPrefixes: [
        '10.0.0.0/16'
      ]
    }
  }

  resource databaseSubnet 'subnets' = {
    name: 'database-subnet-${uniqueId}'
    properties: {
      addressPrefix: '10.0.0.0/24'
      delegations: [
        {
          name: 'delegation-mysql-${uniqueId}'
          properties: {
            serviceName: 'Microsoft.DBforMySQL/flexibleServers'
          }
        }
      ]
    }
  }

  resource vmSubnet 'subnets' = {
    name: 'vm-subnet-${uniqueId}'
    properties: {
      addressPrefix: '10.0.1.0/24'
      privateEndpointNetworkPolicies: 'Enabled'
      privateLinkServiceNetworkPolicies: 'Enabled'
    }
  }
}

output databaseSubnetId string = vnet::databaseSubnet.id
output vmSubnetId string = vnet::vmSubnet.id
output vnetName string = vnet.name

param location string = resourceGroup().location
param uniqueId string = uniqueString(resourceGroup().id)

@secure()
param dbPass string
param subnetId string

resource database 'Microsoft.DBforMySQL/flexibleServers@2021-05-01' = {
  name: 'demo-db-${uniqueId}'
  location: location
  sku: {
    name: 'Standard_B2s'
    tier: 'Burstable'
  }
  properties: {
    version: '8.0.21'
    administratorLogin: 'malliina'
    administratorLoginPassword: dbPass
    storage: {
      autoGrow: 'Enabled'
      storageSizeGB: 64
      iops: 720
    }
    backup: {
      geoRedundantBackup: 'Disabled'
      backupRetentionDays: 7
    }
    network: {
      delegatedSubnetResourceId: subnetId
    }
  }

  resource fw 'firewallRules' = {
    name: 'demo-fw-${uniqueId}'
    properties: {
      endIpAddress: '0.0.0.0'
      startIpAddress: '255.255.255.255'
    }
  }
}

output databaseName string = database.name

param location string = resourceGroup().location
param uniqueId string = uniqueString(resourceGroup().id)

resource keyVault 'Microsoft.KeyVault/vaults@2022-07-01' existing = {
  name: 'vault-${uniqueId}'
}

module networking 'networking.bicep' = {
  name: 'demo-networking-${uniqueId}'
  params: {
    location: location
  }
}


module database 'database.bicep' = {
  name: 'demo-infra-${uniqueId}'
  params: {
    location: location
    dbPass: keyVault.getSecret('DEMO-ADMIN-DB-PASS')
    subnetId: networking.outputs.subnetId
  }
}

module logs 'app.bicep' = {
  name: 'demo-app-${uniqueId}'
  params: {
    location: location
    dbPass: keyVault.getSecret('DEMO-DB-PASS')
    logstreamsPass: keyVault.getSecret('DEMO-LOGSTREAMS-PASS')
  }
}

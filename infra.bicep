param location string = resourceGroup().location
param uniqueId string = uniqueString(resourceGroup().id)

resource keyVault 'Microsoft.KeyVault/vaults@2022-07-01' existing = {
  name: 'vault-${uniqueId}'
}

module logs 'app.bicep' = {
  name: 'demo-${uniqueId}'
  params: {
    location: location
    dbPass: keyVault.getSecret('DEMO-DB-PASS')
    logstreamsPass: keyVault.getSecret('DEMO-LOGSTREAMS-PASS')
  }
}
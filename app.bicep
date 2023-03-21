// Java web app with staging (+ production) slot
param location string = resourceGroup().location
param uniqueId string = uniqueString(resourceGroup().id)

param vnetSubnetId string
@secure()
param dbPass string
@secure()
param logstreamsPass string

resource appServicePlan 'Microsoft.Web/serverfarms@2021-03-01' existing = {
  name: 'plan-win-${uniqueId}'
}

resource site 'Microsoft.Web/sites@2021-03-01' = {
  name: 'demo-${uniqueId}'
  location: location
  properties: {
    siteConfig: {
      healthCheckPath: '/health'
      javaContainer: 'JAVA'
      javaContainerVersion: 'SE'
      javaVersion: '11'
      alwaysOn: true
      webSocketsEnabled: true
      vnetRouteAllEnabled: true
    }
    httpsOnly: true
    serverFarmId: appServicePlan.id
    virtualNetworkSubnetId: vnetSubnetId
  }
  identity: {
    type: 'SystemAssigned'
  }

  resource settings 'config' = {
    name: 'appsettings'
    properties: {
      WEBSITES_ENABLE_APP_SERVICE_STORAGE: 'false'
      DB_PASS: dbPass
      LOGSTREAMS_USER: 'demo'
      LOGSTREAMS_PASS: logstreamsPass
      LOGSTREAMS_ENABLED: 'true'
      JAVA_OPTS: '-Xmx512m'
    }
  }

  resource logSettings 'config' = {
    name: 'logs'
    properties: {
       applicationLogs: {
          fileSystem: {
            level: 'Information'
          }
       }
       httpLogs: {
          fileSystem: {
             enabled: true
          }
       }
       detailedErrorMessages: {
          enabled: true
       }
    }
  }

  resource slotConfig 'config' = {
    name: 'slotConfigNames'
    properties: {
      appSettingNames: [
        'LOGSTREAMS_USER'
      ]
    }
  }

  resource slots 'slots' = {
    name: 'staging'
    location: location
    properties: {
      siteConfig: {
        healthCheckPath: '/health'
        autoSwapSlotName: 'production'
        javaContainer: 'JAVA'
        javaContainerVersion: 'SE'
        javaVersion: '11'
        alwaysOn: true
        webSocketsEnabled: true
        vnetRouteAllEnabled: true
      }
      httpsOnly: true
      serverFarmId: appServicePlan.id
      virtualNetworkSubnetId: vnetSubnetId
    }

    resource settings 'config' = {
      name: 'appsettings'
      properties: {
        WEBSITES_ENABLE_APP_SERVICE_STORAGE: 'false'
        DB_PASS: dbPass
        LOGSTREAMS_USER: 'demo-staging'
        LOGSTREAMS_PASS: logstreamsPass
        LOGSTREAMS_ENABLED: 'true'
        JAVA_OPTS: '-Xmx256m'
      }
    }

    resource logSettings 'config' = {
      name: 'logs'
      properties: {
         applicationLogs: {
            fileSystem: {
              level: 'Information'
            }
         }
         httpLogs: {
            fileSystem: {
               enabled: true
            }
         }
         detailedErrorMessages: {
            enabled: true
         }
      }
    }
  }
}

resource analyticsWorkspace 'Microsoft.OperationalInsights/workspaces@2021-06-01' existing = {
  name: 'workspace-${uniqueId}'
}

resource diagnosticSettings 'Microsoft.Insights/diagnosticSettings@2021-05-01-preview' = {
  name: 'demo-diagnostics-${uniqueId}'
  scope: site
  properties: {
    workspaceId: analyticsWorkspace.id
    logs: [
      {
        category: 'AppServiceAppLogs'
        enabled: true
      }
      {
        category: 'AppServiceConsoleLogs'
        enabled: true
      }

      {
        category: 'AppServiceHTTPLogs'
        enabled: true
      }
      {
        category: 'AppServicePlatformLogs'
        enabled: true
      }
    ]
    metrics: [
      {
        category: 'AllMetrics'
        enabled: true
      }
    ]
  }
}

output txtDomainVerification string = site.properties.customDomainVerificationId
output sitePrincipalId string = site.identity.principalId

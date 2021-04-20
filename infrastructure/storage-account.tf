provider "azurerm" {
  alias                      = "mgmt"
  subscription_id            = var.mgmt_subscription_id
  skip_provider_registration = true
  features {}
}

locals {
  container_names = [
    "new",
    "backup",
    "processed",
    "zipped",
    "encrypted"
  ]

  valid_subnets = [
    data.azurerm_subnet.jenkins_subnet.id,
    data.azurerm_subnet.jenkins_aks_00.id,
    data.azurerm_subnet.jenkins_aks_01.id,
    data.azurerm_subnet.app_aks_00_subnet.id,
    data.azurerm_subnet.app_aks_01_subnet.id
  ]

  short_component = replace(var.component, "-service", "")
}

module "storage_account" {
  source                   = "git@github.com:hmcts/cnp-module-storage-account?ref=master"
  env                      = var.env
  storage_account_name     = replace("${var.product}${local.short_component}${var.env}", "-", "")
  resource_group_name      = azurerm_resource_group.rg.name
  location                 = azurerm_resource_group.rg.location
  account_kind             = "StorageV2"
  account_tier             = "Standard"
  account_replication_type = "LRS"
  sa_subnets               = local.valid_subnets
  managed_identity_object_id = var.managed_identity_object_id
  role_assignments           = [
    "Storage Blob Delegator",
    "Storage Blob Data Contributor"
  ]

  common_tags                = local.tags
}

resource "azurerm_storage_container" "service_containers" {
  name                 = local.container_names[count.index]
  storage_account_name = module.storage_account.storageaccount_name
  count                = length(local.container_names)
}

resource "azurerm_key_vault_secret" "storage_account_name" {
  name         = "storage-account-name"
  value        = module.storage_account.storageaccount_name
  key_vault_id = module.send-letter-key-vault.key_vault_id
}

resource "azurerm_key_vault_secret" "storage_account_primary_key" {
  name         = "storage-account-primary-key"
  value        = module.storage_account.storageaccount_primary_access_key
  key_vault_id = module.send-letter-key-vault.key_vault_id
}

# this secret is used by blob-router-service for uploading blobs
resource "azurerm_key_vault_secret" "storage_account_connection_string" {
  name         = "storage-account-connection-string"
  value        = module.storage_account.storageaccount_primary_connection_string
  key_vault_id = module.send-letter-key-vault.key_vault_id
}

output "storage_account_name" {
  value = module.storage_account.storageaccount_name
}

output "storage_account_primary_key" {
  sensitive = true
  value     = module.storage_account.storageaccount_primary_access_key
}

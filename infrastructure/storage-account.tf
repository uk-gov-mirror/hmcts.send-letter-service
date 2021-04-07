provider "azurerm" {
  alias = "mgmt"
  subscription_id = var.mgmt_subscription_id
  skip_provider_registration = true
  features {}
}

locals {
  account_name = replace("${var.product}${var.env}", "-", "")
  container_names = [
    "new",
    "backup",
    "processed",
    "zipped",
    "encrypted"]
  resourcegroup_name = azurerm_resource_group.rg.name

  valid_subnets = [
    data.azurerm_subnet.jenkins_subnet.id,
    data.azurerm_subnet.jenkins_aks_00.id,
    data.azurerm_subnet.jenkins_aks_01.id,
    data.azurerm_subnet.app_aks_00_subnet.id,
    data.azurerm_subnet.app_aks_01_subnet.id
  ]

}

resource "azurerm_storage_account" "storage_account" {
  name = local.account_name
  resource_group_name = azurerm_resource_group.rg.name
  location = azurerm_resource_group.rg.location
  account_tier = "Standard"
  account_replication_type = "LRS"
  account_kind = "StorageV2"

  network_rules {
    virtual_network_subnet_ids = local.valid_subnets
    bypass = [
      "Logging",
      "Metrics",
      "AzureServices"]
    default_action = "Deny"
  }

  tags = local.tags
}

resource "azurerm_storage_container" "service_containers" {
  name = local.container_names[count.index]
  storage_account_name = azurerm_storage_account.storage_account.name
  count = length(local.container_names)
}

resource "azurerm_key_vault_secret" "storage_account_name" {
  name = "storage-account-name"
  value = azurerm_storage_account.storage_account.name
  key_vault_id = module.send-letter-key-vault.key_vault_id
}

resource "azurerm_key_vault_secret" "storage_account_primary_key" {
  name = "storage-account-primary-key"
  value = azurerm_storage_account.storage_account.primary_access_key
  key_vault_id = module.send-letter-key-vault.key_vault_id
}

# this secret is used by blob-router-service for uploading blobs
resource "azurerm_key_vault_secret" "storage_account_connection_string" {
  name = "storage-account-connection-string"
  value = azurerm_storage_account.storage_account.primary_connection_string
  key_vault_id = module.send-letter-key-vault.key_vault_id
}

output "storage_account_name" {
  value = azurerm_storage_account.storage_account.name
}

output "storage_account_primary_key" {
  sensitive = true
  value = azurerm_storage_account.storage_account.primary_access_key
}

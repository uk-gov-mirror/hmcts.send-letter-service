# copy S2S secret from S2S's vault to app's vault, so that it can be passed to tests by Jenkins
data "azurerm_key_vault_secret" "source_test_s2s_secret" {
  name      = "microservicekey-send-letter-tests"
  vault_uri = "${local.s2s_vault_url}"
}

resource "azurerm_key_vault_secret" "test_s2s_secret" {
  name      = "test-s2s-secret"
  value     = "${data.azurerm_key_vault_secret.source_test_s2s_secret.value}"
  vault_uri = "${module.send-letter-key-vault.key_vault_uri}"
}

# Secrets for tests are stored in permanent (long-lived) Azure Key Vault instances.
# With the exception of (s)preview all Vault instances are long-lived. For preview, however,
# test secrets (not created during deployment) need to be copied over from a permanent vault -
# that's what the code below does.
data "azurerm_key_vault_secret" "source_test_ftp_user" {
  name      = "test-ftp-user"
  vault_uri = "${local.permanent_vault_uri}"
}

resource "azurerm_key_vault_secret" "test_ftp_user" {
  name      = "test-ftp-user"
  value     = "${data.azurerm_key_vault_secret.source_test_ftp_user.value}"
  vault_uri = "${module.send-letter-key-vault.key_vault_uri}"
}

data "azurerm_key_vault_secret" "source_test_ftp_private_key" {
  name      = "test-ftp-private-key"
  vault_uri = "${local.permanent_vault_uri}"
}

resource "azurerm_key_vault_secret" "test_ftp_private_key" {
  name      = "test-ftp-private-key"
  value     = "${data.azurerm_key_vault_secret.source_test_ftp_private_key.value}"
  vault_uri = "${module.send-letter-key-vault.key_vault_uri}"
}

data "azurerm_key_vault_secret" "source_test_ftp_public_key" {
  name      = "test-ftp-public-key"
  vault_uri = "${local.permanent_vault_uri}"
}

resource "azurerm_key_vault_secret" "test_ftp_public_key" {
  name      = "test-ftp-public-key"
  value     = "${data.azurerm_key_vault_secret.source_test_ftp_public_key.value}"
  vault_uri = "${module.send-letter-key-vault.key_vault_uri}"
}

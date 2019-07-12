# copy S2S secret from S2S's vault to app's vault, so that it can be passed to tests by Jenkins
data "azurerm_key_vault_secret" "source_test_s2s_secret" {
  key_vault_id = "${data.azurerm_key_vault.s2s_key_vault.id}"
  name         = "microservicekey-send-letter-tests"
}

resource "azurerm_key_vault_secret" "test_s2s_secret" {
  name         = "test-s2s-secret"
  value        = "${data.azurerm_key_vault_secret.source_test_s2s_secret.value}"
  key_vault_id = "${module.send-letter-key-vault.key_vault_id}"
}

data "azurerm_key_vault_secret" "source_test_ftp_user" {
  name         = "test-ftp-user"
  key_vault_id = "${module.send-letter-key-vault.key_vault_id}"
}

data "azurerm_key_vault_secret" "source_test_ftp_private_key" {
  name         = "test-ftp-private-key"
  key_vault_id = "${module.send-letter-key-vault.key_vault_id}"
}

data "azurerm_key_vault_secret" "source_test_ftp_public_key" {
  name         = "test-ftp-public-key"
  key_vault_id = "${module.send-letter-key-vault.key_vault_id}"
}

data "azurerm_key_vault_secret" "source_sl_email_secret" {
  name      = "send-letter-failure-email"
  vault_uri = "${local.permanent_vault_uri}"
}

resource "azurerm_key_vault_secret" "sl_email_secret" {
  name      = "send-letter-failure-email"
  value     = "${data.azurerm_key_vault_secret.source_sl_email_secret.value}"
  vault_uri = "${module.send-letter-key-vault.key_vault_uri}"
}

module "is-down-action-group" {
  source   = "git@github.com:hmcts/cnp-module-action-group"
  location = "global"
  env      = "${var.env}"

  resourcegroup_name     = "${azurerm_resource_group.rg.name}"
  action_group_name      = "Send Letter is DOWN Alert - ${var.env}"
  short_name             = "SL_is_down"
  email_receiver_name    = "Send Letter Alerts"
  email_receiver_address = "${data.azurerm_key_vault_secret.source_sl_email_secret.value}"
}

output "action_group_name" {
  value = "${module.is-down-action-group.action_group_name}"
}

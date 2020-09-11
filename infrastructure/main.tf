provider "azurerm" {
  version = "=1.42.0"
}

# Make sure the resource group exists
resource "azurerm_resource_group" "rg" {
  name     = "${var.product}-${var.component}-${var.env}"
  location = "${var.location_app}"
}

locals {
  ase_name = "core-compute-${var.env}"

  ftp_private_key = "${data.azurerm_key_vault_secret.ftp_private_key.value}"
  ftp_public_key  = "${data.azurerm_key_vault_secret.ftp_public_key.value}"
  ftp_user        = "${data.azurerm_key_vault_secret.ftp_user.value}"

  encryption_public_key = "${data.azurerm_key_vault_secret.encryption_public_key.value}"

  local_env = "${(var.env == "preview" || var.env == "spreview") ? (var.env == "preview" ) ? "aat" : "saat" : var.env}"
  local_ase = "${(var.env == "preview" || var.env == "spreview") ? (var.env == "preview" ) ? "core-compute-aat" : "core-compute-saat" : local.ase_name}"

  s2s_rg  = "rpe-service-auth-provider-${local.local_env}"
  s2s_url = "http://${local.s2s_rg}.service.core-compute-${local.local_env}.internal"

  previewVaultName    = "${var.product}-send-letter"
  nonPreviewVaultName = "${var.product}-send-letter-${var.env}"
  vaultName           = "${(var.env == "preview" || var.env == "spreview") ? local.previewVaultName : local.nonPreviewVaultName}"

  db_connection_options = "?sslmode=require"

  sku_size = "${var.env == "prod" || var.env == "sprod" || var.env == "aat" ? "I2" : "I1"}"
}

module "db" {
  source          = "git@github.com:hmcts/cnp-module-postgres?ref=master"
  product         = "${var.product}-${var.component}-db"
  location        = "${var.location_db}"
  env             = "${var.env}"
  database_name   = "send_letter"
  postgresql_user = "send_letter"
  sku_name        = "GP_Gen5_2"
  sku_tier        = "GeneralPurpose"
  common_tags     = "${var.common_tags}"
  subscription    = "${var.subscription}"
}

module "db-v11" {
  source             = "git@github.com:hmcts/cnp-module-postgres?ref=master"
  product            = "${var.component}-db"
  location           = "${var.location_db}"
  env                = "${var.env}"
  database_name      = "send_letter"
  postgresql_user    = "send_letter"
  postgresql_version = "11"
  sku_name           = "GP_Gen5_2"
  sku_tier           = "GeneralPurpose"
  common_tags        = "${var.common_tags}"
  subscription       = "${var.subscription}"
}

module "staging-db" {
  source             = "git@github.com:hmcts/cnp-module-postgres?ref=master"
  product            = "${var.component}-staging-db"
  location           = "${var.location_db}"
  env                = "${var.env}"
  database_name      = "send_letter"
  postgresql_user    = "send_letter"
  postgresql_version = "11"
  sku_name           = "GP_Gen5_2"
  sku_tier           = "GeneralPurpose"
  common_tags        = "${var.common_tags}"
  subscription       = "${var.subscription}"
}

module "send-letter-service" {
  source                 = "git@github.com:hmcts/cnp-module-webapp?ref=master"
  product                = "${var.product}-${var.component}"
  location               = "${var.location_app}"
  env                    = "${var.env}"
  ilbIp                  = "${var.ilbIp}"
  resource_group_name    = "${azurerm_resource_group.rg.name}"
  subscription           = "${var.subscription}"
  capacity               = "${var.capacity}"
  common_tags            = "${var.common_tags}"
  asp_name               = "${var.product}-${var.component}-${var.env}"
  asp_rg                 = "${var.product}-${var.component}-${var.env}"
  instance_size          = "${local.sku_size}"
  java_container_version = "9.0"
  enable_ase             = "${var.enable_ase}"

  app_settings = {
    S2S_URL                         = "${local.s2s_url}"
    LETTER_TRACKING_DB_HOST         = "${module.db.host_name}"
    LETTER_TRACKING_DB_PORT         = "${module.db.postgresql_listen_port}"
    LETTER_TRACKING_DB_USER_NAME    = "${module.db.user_name}"
    LETTER_TRACKING_DB_PASSWORD     = "${module.db.postgresql_password}"
    LETTER_TRACKING_DB_NAME         = "${module.db.postgresql_database}"
    LETTER_TRACKING_DB_CONN_OPTIONS = "${local.db_connection_options}"
    FLYWAY_URL                      = "jdbc:postgresql://${module.db.host_name}:${module.db.postgresql_listen_port}/${module.db.postgresql_database}${local.db_connection_options}"
    FLYWAY_USER                     = "${module.db.user_name}"
    FLYWAY_PASSWORD                 = "${module.db.postgresql_password}"
    ENCRYPTION_ENABLED              = "${var.encyption_enabled}"
    SCHEDULING_ENABLED              = "${var.scheduling_enabled}"
    SCHEDULING_LOCK_AT_MOST_FOR     = "${var.scheduling_lock_at_most_for}"
    FILE_CLEANUP_ENABLED            = "${var.file_cleanup_enabled}"
    FILE_CLEANUP_CRON               = "${var.file_cleanup_cron}"

    // ftp
    FTP_HOSTNAME                 = "${var.ftp_hostname}"
    FTP_PORT                     = "${var.ftp_port}"
    FTP_FINGERPRINT              = "${var.ftp_fingerprint}"
    FTP_TARGET_FOLDER            = "${var.ftp_target_folder}"
    FTP_SMOKE_TEST_TARGET_FOLDER = "${var.ftp_smoke_test_target_folder}"
    FTP_REPORTS_FOLDER           = "${var.ftp_reports_folder}"
    FTP_REPORTS_CRON             = "${var.ftp_reports_cron}"
    FTP_USER                     = "${local.ftp_user}"
    FTP_PRIVATE_KEY              = "${local.ftp_private_key}"
    FTP_PUBLIC_KEY               = "${local.ftp_public_key}"
    ENCRYPTION_PUBLIC_KEY        = "${local.encryption_public_key}"

    // smtp
    SMTP_HOST     = "${var.smtp_host}"
    SMTP_USERNAME = "${data.azurerm_key_vault_secret.smtp_username.value}"
    SMTP_PASSWORD = "${data.azurerm_key_vault_secret.smtp_password.value}"

    // reports. depends on smtp
    UPLOAD_SUMMARY_REPORT_CRON       = "${var.upload_summary_report_cron}"
    UPLOAD_SUMMARY_REPORT_ENABLED    = "${var.upload_summary_report_enabled}"
    UPLOAD_SUMMARY_REPORT_RECIPIENTS = "${data.azurerm_key_vault_secret.upload_summary_recipients.value}"
  }
}

# region save DB details to Azure Key Vault
module "send-letter-key-vault" {
  source              = "git@github.com:hmcts/cnp-module-key-vault?ref=master"
  name                = "${local.vaultName}"
  product             = "${var.product}"
  env                 = "${var.env}"
  tenant_id           = "${var.tenant_id}"
  object_id           = "${var.jenkins_AAD_objectId}"
  resource_group_name = "${azurerm_resource_group.rg.name}"

  # dcd_cc-dev group object ID
  product_group_object_id = "38f9dea6-e861-4a50-9e73-21e64f563537"
  common_tags             = "${var.common_tags}"

  managed_identity_object_id = "${var.managed_identity_object_id}"
}

data "azurerm_key_vault" "s2s_key_vault" {
  name                = "s2s-${local.local_env}"
  resource_group_name = "${local.s2s_rg}"
}

resource "azurerm_key_vault_secret" "POSTGRES-USER" {
  key_vault_id = "${module.send-letter-key-vault.key_vault_id}"
  name         = "${var.component}-POSTGRES-USER"
  value        = "${module.db-v11.user_name}"
}

resource "azurerm_key_vault_secret" "POSTGRES-PASS" {
  key_vault_id = "${module.send-letter-key-vault.key_vault_id}"
  name         = "${var.component}-POSTGRES-PASS"
  value        = "${module.db-v11.postgresql_password}"
}

resource "azurerm_key_vault_secret" "POSTGRES_HOST" {
  key_vault_id = "${module.send-letter-key-vault.key_vault_id}"
  name         = "${var.component}-POSTGRES-HOST"
  value        = "${module.db-v11.host_name}"
}

resource "azurerm_key_vault_secret" "POSTGRES_PORT" {
  key_vault_id = "${module.send-letter-key-vault.key_vault_id}"
  name         = "${var.component}-POSTGRES-PORT"
  value        = "${module.db-v11.postgresql_listen_port}"
}

resource "azurerm_key_vault_secret" "POSTGRES_DATABASE" {
  key_vault_id = "${module.send-letter-key-vault.key_vault_id}"
  name         = "${var.component}-POSTGRES-DATABASE"
  value        = "${module.db-v11.postgresql_database}"
}

resource "azurerm_key_vault_secret" "APP-INSTRUMENTATION-KEY" {
  key_vault_id = "${module.send-letter-key-vault.key_vault_id}"
  name         = "app-insights-instrumentation-key"
  value        = "${azurerm_application_insights.appinsights.instrumentation_key}"
}

# endregion

data "azurerm_key_vault_secret" "smtp_username" {
  name         = "reports-email-username"
  key_vault_id = "${module.send-letter-key-vault.key_vault_id}"
}

data "azurerm_key_vault_secret" "smtp_password" {
  name         = "reports-email-password"
  key_vault_id = "${module.send-letter-key-vault.key_vault_id}"
}

data "azurerm_key_vault_secret" "upload_summary_recipients" {
  name         = "upload-summary-report-recipients"
  key_vault_id = "${module.send-letter-key-vault.key_vault_id}"
}

data "azurerm_key_vault_secret" "ftp_user" {
  name         = "ftp-user"
  key_vault_id = "${module.send-letter-key-vault.key_vault_id}"
}

data "azurerm_key_vault_secret" "ftp_private_key" {
  name         = "ftp-private-key"
  key_vault_id = "${module.send-letter-key-vault.key_vault_id}"
}

data "azurerm_key_vault_secret" "ftp_public_key" {
  name         = "ftp-public-key"
  key_vault_id = "${module.send-letter-key-vault.key_vault_id}"
}

data "azurerm_key_vault_secret" "encryption_public_key" {
  name         = "encryption-public-key"
  key_vault_id = "${module.send-letter-key-vault.key_vault_id}"
}

# region staging DB secrets
resource "azurerm_key_vault_secret" "staging_db_user" {
  key_vault_id = "${module.send-letter-key-vault.key_vault_id}"
  name         = "${var.component}-staging-db-user"
  value        = "${module.staging-db.user_name}"
}

resource "azurerm_key_vault_secret" "staging_db_password" {
  key_vault_id = "${module.send-letter-key-vault.key_vault_id}"
  name         = "${var.component}-staging-db-password"
  value        = "${module.staging-db.postgresql_password}"
}

resource "azurerm_key_vault_secret" "staging_db_host" {
  key_vault_id = "${module.send-letter-key-vault.key_vault_id}"
  name         = "${var.component}-staging-db-host"
  value        = "${module.staging-db.host_name}"
}

resource "azurerm_key_vault_secret" "staging_db_port" {
  key_vault_id = "${module.send-letter-key-vault.key_vault_id}"
  name         = "${var.component}-staging-db-port"
  value        = "${module.staging-db.postgresql_listen_port}"
}

resource "azurerm_key_vault_secret" "staging_db_name" {
  key_vault_id = "${module.send-letter-key-vault.key_vault_id}"
  name         = "${var.component}-staging-db-name"
  value        = "${module.staging-db.postgresql_database}"
}
# endregion

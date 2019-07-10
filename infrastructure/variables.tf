variable "product" {
  type    = "string"
}

variable "raw_product" {
  default = "rpe" // jenkins-library overrides product for PRs and adds e.g. pr-118-rpe-...
}

variable "component" {
  type = "string"
}

variable "location_app" {
  type    = "string"
  default = "UK South"
}

variable "location_db" {
  type    = "string"
  default = "UK South"
}

variable "appinsights_location" {
  type        = "string"
  default     = "West Europe"
  description = "Location for Application Insights"
}

variable "application_type" {
  type        = "string"
  default     = "Web"
  description = "Type of Application Insights (Web/Other)"
}

variable "env" {
  type = "string"
}

variable "capacity" {
  default = "1"
}

variable "vault_section" {
  type = "string"
  description = "Name of the environment-specific section in Vault key path, i.e. secret/{vault_section}/..."
  default = "test"
}

variable "ilbIp" {}

variable "tenant_id" {}

variable "client_id" {
  description = "(Required) The object ID of a user, service principal or security group in the Azure Active Directory tenant for the vault. The object ID must be unique for the list of access policies. This is usually sourced from environment variables and not normally required to be specified."
}

variable "subscription" {}

variable "jenkins_AAD_objectId" {
  type        = "string"
  description = "(Required) The Azure AD object ID of a user, service principal or security group in the Azure Active Directory tenant for the vault. The object ID must be unique for the list of access policies."
}

variable encyption_enabled {
  default = "false"
}

variable scheduling_enabled {
  default = "false"
}

variable "file_cleanup_enabled" {
  default = "false"
  description = "Determines whether old files should be deleted from the FTP server. Should only be enabled on AAT."
}

variable "file_cleanup_cron" {
  default     = "0 15 * * * *"
  description = "Crontab value for task to be run"
}

variable scheduling_lock_at_most_for {
  default = "PT10M"
  description = "For how long to keep the lock of the specific task"
}

variable "ftp_hostname" {
  default = "cmseft.services.xerox.com"
}

variable "ftp_port" {
  default = "22"
}

variable "ftp_fingerprint" {
  default = "SHA256:3tX3DIkqd1Loz2alHfnt+qjHocfxk0YUOZHlnf9Zgdk"
}

variable "ftp_target_folder" {
  default = "TO_XEROX"
}

variable "ftp_smoke_test_target_folder" {
  default = "SMOKE_TEST"
}

variable "ftp_reports_folder" {
  default = "FROM_XEROX"
}

variable "ftp_reports_cron" {
  default = "0 30 * * * *"
}

variable "smtp_host" {
  type        = "string"
  default     = "false"
  description = "SMTP host for sending out reports via JavaMailSender"
}

variable upload_letters_key_fingerprint {
  default = ""
}

# region reports

variable "upload_summary_report_cron" {
  default     = "0 0 19 * * *"
  description = "Cron signature for job to send daily summary report of uploaded letters"
}

variable "upload_summary_report_enabled" {
  default = "false"
}

# endregion

variable "deployment_namespace" {
  default = ""
}

variable "common_tags" {
  type = "map"
}

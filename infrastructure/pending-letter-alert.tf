module "send-letter-service-pending-letter-alert" {
  source            = "git@github.com:hmcts/cnp-module-metric-alert"
  location          = azurerm_application_insights.appinsights.location
  app_insights_name = azurerm_application_insights.appinsights.name

  enabled    = "${var.env == "prod"}"
  alert_name = "Send_Letter_Service_Pending_Letter_-_BSP"
  alert_desc = "Triggers when send letter service records a pending letter message within a 15 minutes window timeframe."

  app_insights_query = <<EOF
customEvents
| where name == "PendingLetter"
| extend dimensions = parse_json(customDimensions)
| project timestamp,
          letterId = dimensions.letterId,
          service = dimensions.service,
          type = dimensions.type,
          createdAt = dimensions.createdAt,
          createdDayOfWeek = dimensions.createdDayOfWeek
| summarize by format_datetime(timestamp, 'MM-dd-yyyy'),
          tostring(letterId),
          tostring(service),
          tostring(type),
          tostring(createdAt),
          tostring(createdDayOfWeek)
EOF

  frequency_in_minutes       = 15
  time_window_in_minutes     = 15
  severity_level             = "2"
  action_group_name          = module.alert-action-group.action_group_name
  custom_email_subject       = "Send Letter Service pending letter"
  trigger_threshold_operator = "GreaterThan"
  trigger_threshold          = 0
  resourcegroup_name         = azurerm_resource_group.rg.name
}

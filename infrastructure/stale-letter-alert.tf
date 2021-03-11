module "send-letter-service-stale-letter-alert" {
  source            = "git@github.com:hmcts/cnp-module-metric-alert"
  location          = azurerm_application_insights.appinsights.location
  app_insights_name = azurerm_application_insights.appinsights.name

  enabled    = "${var.env == "prod"}"
  alert_name = "Send_Letter_Service_Stale_Letter_-_BSP"
  alert_desc = "Triggers when send letter service records a stale letter message within a 15 minutes window timeframe."

  app_insights_query = <<EOF
customEvents
| where name == "LetterNotPrinted"
| extend dimensions = parse_json(customDimensions)
| project timestamp,
          letterId = dimensions.letterId,
          checksum = dimensions.checksum,
          service = dimensions.service,
          type = dimensions.type,
          sentToPrintDayOfWeek = dimensions.sentToPrintDayOfWeek,
          sentToPrintAt = dimensions.sentToPrintAt
| summarize by format_datetime(timestamp, 'MM-dd-yyyy'),
          tostring(letterId),
          tostring(checksum),
          tostring(service),
          tostring(type),
          tostring(sentToPrintDayOfWeek),
          tostring(sentToPrintAt)
EOF

  frequency_in_minutes       = 15
  time_window_in_minutes     = 15
  severity_level             = "2"
  action_group_name          = module.alert-action-group.action_group_name
  custom_email_subject       = "Send Letter Service stale letter"
  trigger_threshold_operator = "GreaterThan"
  trigger_threshold          = 0
  resourcegroup_name         = azurerm_resource_group.rg.name
}

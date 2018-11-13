module "send-letter-up-alert" {
  source            = "git@github.com:hmcts/cnp-module-metric-alert"
  location          = "${azurerm_application_insights.appinsights.location}"
  app_insights_name = "${azurerm_application_insights.appinsights.name}"

  alert_name = "Send Letter is DOWN - RPE"
  alert_desc = "Triggers when send letter service looks like being down within a 15 minutes timeframe."

  app_insights_query = <<EOF
union
  (dependencies
    | where timestamp > ago(3h)
    | where data == 'FtpFileUploaded' and success =~ 'true'
    | summarize ['name'] = 'uploaded', ['counter'] = sum(itemCount) by ['timecut'] = bin(timestamp, 30m)
  ),
  (requests
    | where timestamp > ago(3h)
    | where name == 'POST SendLetterController/sendLetter' and success =~ 'true'
    | where tostring(operation_SyntheticSource) == ''
    | summarize ['name'] = 'received', ['counter'] = sum(itemCount) by ['timecut'] = bin(timestamp, 30m)
  ),
  (traces
    | where timestamp > ago(3h)
    | extend logger = customDimensions.LoggerName
    | where logger == 'uk.gov.hmcts.reform.sendletter.tasks.UploadLettersTask' and message has 'FTP downtime'
    | summarize ['name'] = 'downtime', ['counter'] = sum(itemCount) by ['timecut'] = bin(timestamp, 30m)
  )
| summarize ['received'] = sum(iif(name == 'received', counter, 0)),
            ['uploaded'] = sum(iif(name == 'uploaded', counter, 0)),
            ['downtime'] = sum(iif(name == 'downtime', counter, 0)) by bin(timecut, 30m)
| project timecut, ['is_operational'] = iif(received > 0 and downtime > 0, true, iif(received > 0, uploaded > 0, true))
| where is_operational == false
EOF

  frequency_in_minutes       = 10
  // window no longer matters as it is defined in the query. but it is a requirement for module
  time_window_in_minutes     = 30
  severity_level             = "2"
  action_group_name          = "${module.is-down-action-group.action_group_name}"
  custom_email_subject       = "Send Letter is DOWN"
  trigger_threshold_operator = "GreaterThan"
  trigger_threshold          = 3
  resourcegroup_name         = "${azurerm_resource_group.rg.name}"
}

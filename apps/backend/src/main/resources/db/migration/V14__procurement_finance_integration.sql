INSERT INTO iam_permission (id, capability, description)
VALUES
    ('39100000-0000-4000-8000-000000000001', 'procurement.finance.post',
     'Publish company-scoped Procurement accounting events'),
    ('39100000-0000-4000-8000-000000000002', 'procurement.finance.retry',
     'Retry company-scoped Procurement Finance postings')
ON CONFLICT (capability) DO NOTHING;

INSERT INTO platform_feature_flag
    (flag_key, enabled, description, updated_at, updated_by)
VALUES
    ('procurement.finance.purchase-order-approved', false,
     'Enables optional PurchaseOrderApproved accounting-event publication',
     now(), 'migration')
ON CONFLICT (flag_key) DO NOTHING;

INSERT INTO platform_domain_event_catalog (event_type, owner_context, description)
VALUES
    ('ProcurementAccountingEventPublished', 'procurement',
     'A Procurement accounting fact was accepted by the Finance Posting Engine'),
    ('ProcurementFinancePostingRetried', 'procurement',
     'A failed Procurement Finance posting was retried')
ON CONFLICT (event_type) DO NOTHING;

-- =====================================================================================
-- Demo data so the dashboard has something to show.
-- Due dates are relative to CURRENT_DATE so "overdue" numbers stay meaningful
-- no matter when you first start the database.
-- =====================================================================================

INSERT INTO projects (name, description) VALUES
    ('Customer Portal',   'Self-service portal for customers (Angular + Spring Boot)'),
    ('Payments Service',  'Payment gateway integration and reconciliation'),
    ('Mobile App',        'Companion mobile application'),
    ('Data Platform',     'Reporting warehouse and nightly ETL jobs');

INSERT INTO tasks (title, status, priority, due_date, project_id)
SELECT t.title, t.status, t.priority, CURRENT_DATE + t.due_offset, p.id
FROM (VALUES
    -- title                              status         priority  due (days from today)  project
    ('Design login page',                 'DONE',        'HIGH',   -20, 'Customer Portal'),
    ('Implement JWT authentication',      'DONE',        'HIGH',   -12, 'Customer Portal'),
    ('Profile settings screen',           'IN_PROGRESS', 'MEDIUM',   3, 'Customer Portal'),
    ('Order history table',               'TODO',        'MEDIUM',  10, 'Customer Portal'),
    ('Accessibility audit',               'TODO',        'LOW',     -2, 'Customer Portal'),
    ('Integrate payment provider SDK',    'DONE',        'HIGH',   -15, 'Payments Service'),
    ('Webhook signature verification',    'IN_PROGRESS', 'HIGH',    -1, 'Payments Service'),
    ('Daily reconciliation job',          'TODO',        'HIGH',     5, 'Payments Service'),
    ('Refund API',                        'TODO',        'MEDIUM',  14, 'Payments Service'),
    ('Push notification setup',           'DONE',        'MEDIUM',  -8, 'Mobile App'),
    ('Offline mode',                      'IN_PROGRESS', 'LOW',      7, 'Mobile App'),
    ('App store screenshots',             'TODO',        'LOW',     -4, 'Mobile App'),
    ('Crash reporting',                   'DONE',        'MEDIUM', -30, 'Mobile App'),
    ('Warehouse schema design',           'DONE',        'HIGH',   -25, 'Data Platform'),
    ('Nightly ETL pipeline',              'IN_PROGRESS', 'HIGH',     2, 'Data Platform'),
    ('Sales dashboard',                   'TODO',        'MEDIUM',  12, 'Data Platform'),
    ('Data retention policy',             'TODO',        'LOW',     20, 'Data Platform'),
    ('GDPR export endpoint',              'IN_PROGRESS', 'MEDIUM',  -3, 'Data Platform')
) AS t(title, status, priority, due_offset, project_name)
JOIN projects p ON p.name = t.project_name;

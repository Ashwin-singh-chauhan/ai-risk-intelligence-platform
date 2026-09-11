-- Reference catalog of compliance controls (conceptual mappings, not authoritative legal text)
INSERT INTO compliance_controls (framework, control_id, control_name, control_description, category) VALUES
('NIST_CSF','ID.AM-1','Physical devices and systems inventoried','Physical devices and systems within the organization are inventoried.','Identify'),
('NIST_CSF','ID.AM-2','Software platforms and applications inventoried','Software platforms and applications within the organization are inventoried.','Identify'),
('NIST_CSF','ID.RA-1','Asset vulnerabilities identified','Asset vulnerabilities are identified and documented.','Identify'),
('NIST_CSF','PR.AC-1','Identities and credentials managed','Identities and credentials are issued, managed, verified, revoked, and audited.','Protect'),
('NIST_CSF','PR.DS-1','Data-at-rest protection','Data-at-rest is protected.','Protect'),
('NIST_CSF','PR.IP-12','Vulnerability management plan','A vulnerability management plan is developed and implemented.','Protect'),
('NIST_CSF','DE.CM-1','Network monitoring','The network is monitored to detect potential cybersecurity events.','Detect'),
('NIST_CSF','DE.AE-1','Anomaly baseline','A baseline of network operations and expected data flows is established and managed.','Detect'),
('NIST_CSF','RS.RP-1','Response plan execution','Response plan is executed during or after an incident.','Respond'),
('NIST_CSF','RC.RP-1','Recovery plan execution','Recovery plan is executed during or after a cybersecurity incident.','Recover'),

('ISO_27001','A.5.9','Inventory of information and other associated assets','An inventory of information and other associated assets is maintained.','Asset Management'),
('ISO_27001','A.5.12','Classification of information','Information is classified per confidentiality, integrity, availability and criticality.','Asset Management'),
('ISO_27001','A.8.8','Management of technical vulnerabilities','Information about technical vulnerabilities is obtained and evaluated, and appropriate measures taken.','Vulnerability Management'),
('ISO_27001','A.8.16','Monitoring activities','Networks, systems and applications are monitored for anomalous behavior.','Operations Security'),
('ISO_27001','A.5.24','Incident management planning','Incident management is planned and prepared.','Incident Management'),
('ISO_27001','A.5.30','ICT readiness for business continuity','ICT readiness is planned, implemented, maintained and tested.','Business Continuity'),
('ISO_27001','A.8.24','Use of cryptography','Rules for the effective use of cryptography are defined and implemented.','Cryptography'),
('ISO_27001','A.5.15','Access control','Rules to control physical and logical access are established.','Access Control'),

('SOC2','CC6.1','Logical access controls','The entity implements logical access security software, infrastructure, and architectures.','Common Criteria'),
('SOC2','CC6.6','Boundary protection','The entity implements boundary protection controls against external threats.','Common Criteria'),
('SOC2','CC7.1','Detection of security events','The entity uses detection and monitoring procedures to identify anomalies.','Common Criteria'),
('SOC2','CC7.2','Security incident response','The entity monitors and responds to identified security incidents.','Common Criteria'),
('SOC2','CC7.3','Incident evaluation','The entity evaluates security events to determine response.','Common Criteria'),
('SOC2','CC9.1','Risk mitigation','The entity identifies and mitigates risk from vendors and business partners.','Common Criteria'),
('SOC2','A1.2','Availability - recovery','Environmental protections, backups and recovery infrastructure are in place.','Availability');

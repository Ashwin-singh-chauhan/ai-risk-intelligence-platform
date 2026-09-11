#!/usr/bin/env python3
"""Generates realistic synthetic data for the Enterprise Risk Intelligence Platform demo:
100+ assets, 300+ vulnerabilities, 10,000+ security events, and 50+ incidents, plus a
compliance assessment baseline. Connects directly to Postgres (bypassing the API/Kafka
path) so a full demo dataset can be loaded in seconds rather than minutes.

Usage:
    pip install -r scripts/requirements.txt
    python scripts/generate_synthetic_data.py --database-url postgresql://erip_app:erip_dev_password@localhost:5432/erip

Real-time event ingestion through Kafka -> ML anomaly scoring is exercised separately by
the running application (see EventController#ingest); this script seeds the historical
baseline the dashboards and risk engine need to look "lived-in" on first run.
"""
import argparse
import random
import uuid
from datetime import datetime, timedelta, timezone

import psycopg2
import psycopg2.extras

random.seed(20240115)

BUSINESS_UNITS = [
    "Retail Banking", "Corporate IT", "Payments Platform",
    "Cloud Platform Engineering", "Human Resources", "Customer Data Analytics",
]

ASSET_TYPES = ["SERVER", "DATABASE", "APPLICATION", "NETWORK_DEVICE", "CLOUD_RESOURCE", "ENDPOINT", "CONTAINER"]
CRITICALITIES = ["LOW", "MEDIUM", "HIGH", "CRITICAL"]
CRITICALITY_WEIGHTS = [0.25, 0.35, 0.25, 0.15]
EXPOSURES = ["INTERNAL", "DMZ", "PUBLIC"]
EXPOSURE_WEIGHTS = [0.55, 0.25, 0.20]
ENVIRONMENTS = ["PRODUCTION", "STAGING", "DEVELOPMENT", "TEST"]
ENVIRONMENT_WEIGHTS = [0.5, 0.2, 0.2, 0.1]

ASSET_NAME_PARTS = [
    "payment-gateway", "core-banking-db", "customer-portal", "auth-service", "api-gateway",
    "data-warehouse", "kubernetes-node", "load-balancer", "vpn-concentrator", "hr-portal",
    "analytics-pipeline", "mobile-backend", "file-server", "email-gateway", "backup-server",
    "monitoring-stack", "ci-cd-runner", "identity-provider", "fraud-detection-svc", "reporting-service",
]

VULN_TITLES = [
    ("Remote Code Execution in Web Framework", 9.8), ("SQL Injection in Login Endpoint", 8.6),
    ("Cross-Site Scripting in Search Form", 6.1), ("Outdated TLS Configuration", 5.3),
    ("Privilege Escalation via Misconfigured Sudoers", 8.4), ("Unpatched OpenSSL Vulnerability", 9.1),
    ("Default Credentials on Admin Interface", 9.0), ("Insecure Direct Object Reference in API", 7.1),
    ("Missing Rate Limiting on Auth Endpoint", 5.9), ("Server-Side Request Forgery", 8.2),
    ("Sensitive Data Exposure in Logs", 6.5), ("Broken Access Control on File Upload", 7.8),
    ("Deserialization of Untrusted Data", 9.4), ("Weak Password Hashing Algorithm", 6.8),
    ("Unrestricted File Upload", 8.0), ("XML External Entity Injection", 7.5),
    ("Denial of Service via Resource Exhaustion", 6.2), ("Path Traversal in File Download", 7.3),
    ("Missing Security Headers", 3.1), ("Outdated Container Base Image with Known CVEs", 6.9),
]

EVENT_TYPES = [
    "LOGIN_SUCCESS", "LOGIN_FAILURE", "PORT_SCAN", "MALWARE_DETECTED", "DATA_EXFILTRATION_ATTEMPT",
    "PRIVILEGE_ESCALATION", "FIREWALL_BLOCK", "DNS_TUNNELING_SUSPECTED", "BRUTE_FORCE_ATTEMPT",
    "UNUSUAL_LOGIN_LOCATION", "FILE_INTEGRITY_CHANGE", "CONFIG_CHANGE", "NORMAL_TRAFFIC",
    "TLS_HANDSHAKE", "HEALTH_CHECK", "VPN_CONNECT", "OUTBOUND_CONNECTION_BLOCKED",
]
SUSPICIOUS_EVENT_TYPES = {
    "PORT_SCAN", "MALWARE_DETECTED", "DATA_EXFILTRATION_ATTEMPT", "PRIVILEGE_ESCALATION",
    "DNS_TUNNELING_SUSPECTED", "BRUTE_FORCE_ATTEMPT", "UNUSUAL_LOGIN_LOCATION",
}
PROTOCOLS = ["TCP", "UDP", "HTTPS", "HTTP", "SSH"]

INCIDENT_TITLES = [
    "Suspected brute-force attack against VPN gateway", "Malware detected on endpoint",
    "Unauthorized privilege escalation attempt", "Data exfiltration attempt blocked by DLP",
    "Phishing campaign led to credential compromise", "DDoS attempt against public API",
    "Misconfigured S3 bucket exposed customer data", "Ransomware activity detected on file server",
    "Insider threat - unusual data access pattern", "Third-party vendor breach affecting shared systems",
]


def now_minus_days(days: float) -> datetime:
    return datetime.now(timezone.utc) - timedelta(days=days)


def generate_assets(n: int):
    assets = []
    for i in range(n):
        base = random.choice(ASSET_NAME_PARTS)
        bu = random.choices(BUSINESS_UNITS)[0]
        asset_type = random.choice(ASSET_TYPES)
        criticality = random.choices(CRITICALITIES, weights=CRITICALITY_WEIGHTS)[0]
        exposure = random.choices(EXPOSURES, weights=EXPOSURE_WEIGHTS)[0]
        environment = random.choices(ENVIRONMENTS, weights=ENVIRONMENT_WEIGHTS)[0]
        asset_id = str(uuid.uuid4())
        assets.append({
            "id": asset_id,
            "asset_tag": f"AST-{i + 1:05d}",
            "name": f"{base}-{i + 1:03d}",
            "asset_type": asset_type,
            "business_unit": bu,
            "owner": f"{bu.split()[0].lower()}-ops@erip.com",
            "criticality": criticality,
            "exposure": exposure,
            "environment": environment,
            "ip_address": f"10.{random.randint(0,255)}.{random.randint(0,255)}.{random.randint(1,254)}",
            "hostname": f"{base}-{i + 1:03d}.internal.erip.com",
            "tags": ["synthetic-data"],
        })
    return assets


def generate_vulnerabilities(assets, n: int):
    vulns = []
    for i in range(n):
        asset = random.choice(assets)
        title, base_cvss = random.choice(VULN_TITLES)
        cvss = round(min(10.0, max(0.1, base_cvss + random.uniform(-0.6, 0.6))), 1)
        severity = "CRITICAL" if cvss >= 9 else "HIGH" if cvss >= 7 else "MEDIUM" if cvss >= 4 else "LOW"
        status = random.choices(
            ["OPEN", "IN_PROGRESS", "MITIGATED", "RESOLVED", "ACCEPTED_RISK"],
            weights=[0.40, 0.20, 0.10, 0.25, 0.05],
        )[0]
        discovered_days_ago = random.uniform(1, 240)
        vulns.append({
            "id": str(uuid.uuid4()),
            "asset_id": asset["id"],
            "cve_id": f"CVE-2024-{10000 + i}",
            "title": title,
            "description": f"{title} identified during automated vulnerability scan of {asset['name']}.",
            "cvss_score": cvss,
            "severity": severity,
            "status": status,
            "exploit_available": random.random() < (0.35 if severity in ("CRITICAL", "HIGH") else 0.08),
            "patch_available": random.random() < 0.82,
            "discovered_at": now_minus_days(discovered_days_ago),
            "due_date": (now_minus_days(discovered_days_ago) + timedelta(days=random.choice([7, 14, 30, 60]))).date(),
            "resolved_at": now_minus_days(discovered_days_ago / 2) if status in ("RESOLVED", "MITIGATED") else None,
            "remediation_notes": None,
        })
    return vulns


def generate_events(assets, n: int):
    events = []
    for _ in range(n):
        asset = random.choice(assets) if random.random() < 0.85 else None
        suspicious = random.random() < 0.07
        event_type = random.choice(list(SUSPICIOUS_EVENT_TYPES)) if suspicious else random.choice(EVENT_TYPES)
        severity = random.choices(
            ["INFO", "LOW", "MEDIUM", "HIGH", "CRITICAL"],
            weights=[0.15, 0.35, 0.5, 0.0, 0.0] if not suspicious else [0.0, 0.05, 0.25, 0.45, 0.25],
        )[0]
        days_ago = random.uniform(0, 45)
        anomaly_score = round(random.uniform(0.75, 0.99), 5) if suspicious else round(random.uniform(0.01, 0.45), 5)
        events.append({
            "id": str(uuid.uuid4()),
            "asset_id": asset["id"] if asset else None,
            "event_type": event_type,
            "severity": severity,
            "source_ip": f"{random.randint(1,223)}.{random.randint(0,255)}.{random.randint(0,255)}.{random.randint(1,254)}",
            "destination_ip": asset["ip_address"] if asset else None,
            "source_port": random.randint(1024, 65535),
            "destination_port": random.choice([22, 80, 443, 3389, 8080, 8443, 3306, 5432]),
            "protocol": random.choice(PROTOCOLS),
            "description": f"{event_type.replace('_', ' ').title()} observed" + (f" on {asset['name']}" if asset else ""),
            "raw_payload": psycopg2.extras.Json({"synthetic": True, "eventType": event_type}),
            "event_time": now_minus_days(days_ago),
            "ingested_at": now_minus_days(max(0, days_ago - 0.001)),
            "anomaly_score": anomaly_score,
            "is_anomaly": suspicious,
            "scored_at": now_minus_days(max(0, days_ago - 0.001)),
        })
    return events


def generate_incidents(assets, vulns, n: int):
    incidents = []
    for i in range(n):
        asset = random.choice(assets)
        related_vuln = random.choice(vulns) if random.random() < 0.5 else None
        severity = random.choices(["LOW", "MEDIUM", "HIGH", "CRITICAL"], weights=[0.25, 0.35, 0.3, 0.1])[0]
        status = random.choices(
            ["OPEN", "INVESTIGATING", "CONTAINED", "RESOLVED", "CLOSED"],
            weights=[0.15, 0.15, 0.15, 0.25, 0.30],
        )[0]
        opened_days_ago = random.uniform(1, 200)
        opened_at = now_minus_days(opened_days_ago)
        contained_at = opened_at + timedelta(hours=random.uniform(1, 48)) if status not in ("OPEN", "INVESTIGATING") else None
        resolved_at = contained_at + timedelta(hours=random.uniform(1, 72)) if status in ("RESOLVED", "CLOSED") and contained_at else None
        closed_at = resolved_at + timedelta(days=random.uniform(0, 5)) if status == "CLOSED" and resolved_at else None

        incidents.append({
            "id": str(uuid.uuid4()),
            "incident_number": f"INC-{i + 1:06d}",
            "title": random.choice(INCIDENT_TITLES),
            "description": f"Security incident affecting {asset['name']} in {asset['business_unit']}.",
            "severity": severity,
            "status": status,
            "related_asset_id": asset["id"],
            "related_vulnerability_id": related_vuln["id"] if related_vuln else None,
            "related_event_id": None,
            "assigned_analyst_id": None,
            "root_cause": "Root cause analysis completed; details in incident ticketing system." if status != "OPEN" else None,
            "resolution_summary": "Contained and remediated per incident response runbook." if resolved_at else None,
            "opened_at": opened_at,
            "contained_at": contained_at,
            "resolved_at": resolved_at,
            "closed_at": closed_at,
        })
    return incidents


def generate_compliance_assessments(conn, business_units):
    with conn.cursor() as cur:
        cur.execute("SELECT id, framework FROM compliance_controls")
        controls = cur.fetchall()

    rows = []
    for control_id, _framework in controls:
        for bu in business_units:
            if random.random() < 0.75:  # ~75% of controls assessed per business unit
                status = random.choices(
                    ["COMPLIANT", "PARTIAL", "NON_COMPLIANT"], weights=[0.6, 0.25, 0.15]
                )[0]
                rows.append((
                    str(uuid.uuid4()), control_id, bu, status,
                    "Assessed during quarterly internal compliance review.",
                    now_minus_days(random.uniform(1, 120)),
                ))
    return rows


def bulk_insert(conn, table, columns, rows, page_size=1000):
    if not rows:
        return
    with conn.cursor() as cur:
        query = f"INSERT INTO {table} ({', '.join(columns)}) VALUES %s"
        values = [tuple(row[c] for c in columns) for row in rows]
        psycopg2.extras.execute_values(cur, query, values, page_size=page_size)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--database-url", default="postgresql://erip_app:erip_dev_password@localhost:5432/erip")
    parser.add_argument("--assets", type=int, default=120)
    parser.add_argument("--vulnerabilities", type=int, default=350)
    parser.add_argument("--events", type=int, default=12000)
    parser.add_argument("--incidents", type=int, default=60)
    args = parser.parse_args()

    conn = psycopg2.connect(args.database_url)
    try:
        print(f"Generating {args.assets} assets...")
        assets = generate_assets(args.assets)
        bulk_insert(conn, "assets", [
            "id", "asset_tag", "name", "asset_type", "business_unit", "owner", "criticality",
            "exposure", "environment", "ip_address", "hostname",
        ], [{**a, "tags": psycopg2.extras.Json(a["tags"])} for a in assets])
        conn.commit()

        print(f"Generating {args.vulnerabilities} vulnerabilities...")
        vulns = generate_vulnerabilities(assets, args.vulnerabilities)
        bulk_insert(conn, "vulnerabilities", [
            "id", "asset_id", "cve_id", "title", "description", "cvss_score", "severity", "status",
            "exploit_available", "patch_available", "discovered_at", "due_date", "resolved_at", "remediation_notes",
        ], vulns)
        conn.commit()

        print(f"Generating {args.events} security events...")
        events = generate_events(assets, args.events)
        bulk_insert(conn, "security_events", [
            "id", "asset_id", "event_type", "severity", "source_ip", "destination_ip", "source_port",
            "destination_port", "protocol", "description", "raw_payload", "event_time", "ingested_at",
            "anomaly_score", "is_anomaly", "scored_at",
        ], events, page_size=2000)
        conn.commit()

        print(f"Generating {args.incidents} incidents...")
        incidents = generate_incidents(assets, vulns, args.incidents)
        bulk_insert(conn, "incidents", [
            "id", "incident_number", "title", "description", "severity", "status", "related_asset_id",
            "related_vulnerability_id", "related_event_id", "assigned_analyst_id", "root_cause",
            "resolution_summary", "opened_at", "contained_at", "resolved_at", "closed_at",
        ], incidents)
        conn.commit()

        print("Generating compliance assessments...")
        assessment_rows = generate_compliance_assessments(conn, BUSINESS_UNITS)
        with conn.cursor() as cur:
            psycopg2.extras.execute_values(
                cur,
                "INSERT INTO compliance_assessments (id, control_id, business_unit, status, evidence_summary, assessed_at) VALUES %s",
                assessment_rows,
            )
        conn.commit()

        print("Synthetic data generation complete:")
        print(f"  Assets:                  {len(assets)}")
        print(f"  Vulnerabilities:         {len(vulns)}")
        print(f"  Security events:         {len(events)}")
        print(f"  Incidents:               {len(incidents)}")
        print(f"  Compliance assessments:  {len(assessment_rows)}")
        print("\nNext steps: log in and call POST /api/v1/risk-scores/assets/{id}/recompute for a few assets,")
        print("or wait for the scheduled risk-engine job (runs every 15 minutes) to populate risk scores,")
        print("then POST /api/v1/rag/reindex (as ADMIN) to build the RAG knowledge base from this data.")
    finally:
        conn.close()


if __name__ == "__main__":
    main()

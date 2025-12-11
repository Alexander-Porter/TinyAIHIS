#!/usr/bin/env python3
"""
Clear/cancel all registrations in the TinyHIS instance.
Usage (PowerShell):
    python .\clear_registrations.py --db-host localhost --db-port 3306 --db-user root --db-pass tinyhis123 --admin-user admin --admin-pass admin123 --base-url http://localhost/api

This script will:
    1. Connect to MySQL and list all registration IDs
    2. Login as admin to the backend to get token
    3. For each reg_id attempt to cancel via POST /api/registration/cancel/{regId}

Notes:
- This script requires pymysql and requests. Install with: pip install pymysql requests
- Only registration records with status < 3 can be cancelled. Others will be logged.
- This script cancels registrations (sets status to 5) and updates schedule counts; it does not delete rows.
"""

import argparse
import pymysql
import requests
import sys


def parse_args():
    parser = argparse.ArgumentParser(description='Clear registrations for TinyHIS (cancel them via admin API)')
    parser.add_argument('--db-host', default='localhost')
    parser.add_argument('--db-port', type=int, default=3306)
    parser.add_argument('--db-user', default='root')
    parser.add_argument('--db-pass', default='tinyhis123')
    parser.add_argument('--db-name', default='tinyhis')
    parser.add_argument('--admin-user', default='admin')
    parser.add_argument('--admin-pass', default='admin123')
    parser.add_argument('--base-url', default='http://localhost/api')
    parser.add_argument('--dry-run', action='store_true')
    parser.add_argument('--delete', action='store_true', help='Hard delete all registration and related data from DB')
    return parser.parse_args()


def get_all_registration_ids(conn):
    with conn.cursor() as cur:
        cur.execute("SELECT reg_id, status FROM registration;")
        return cur.fetchall()  # list of tuples (reg_id, status)


def hard_delete_all(conn):
    """
    Hard delete all data related to registrations to reset the system state.
    """
    print("WARNING: Performing hard delete of all registration data...")
    with conn.cursor() as cur:
        # 1. Delete prescriptions
        print("Deleting prescriptions...")
        cur.execute("DELETE FROM prescription")
        
        # 2. Delete lab orders
        print("Deleting lab orders...")
        cur.execute("DELETE FROM lab_order")
        
        # 3. Delete medical records
        print("Deleting medical records...")
        cur.execute("DELETE FROM medical_record")
        
        # 4. Delete registrations
        print("Deleting registrations...")
        cur.execute("DELETE FROM registration")
        
        # 5. Reset schedule counts
        print("Resetting schedule counts...")
        cur.execute("UPDATE schedule SET current_count = 0")
        
        conn.commit()
    print("Hard delete completed successfully.")


def admin_login(base_url, username, password):
    url = base_url.rstrip('/') + '/auth/staff/login'
    resp = requests.post(url, json={"username": username, "password": password})
    resp.raise_for_status()
    js = resp.json()
    if isinstance(js, dict) and js.get('code') == 200:
        data = js.get('data', {})
        return data.get('token')
    raise RuntimeError(f"Admin login failed: {js}")


def cancel_registration(base_url, token, reg_id):
    url = base_url.rstrip('/') + f'/registration/cancel/{reg_id}'
    headers = {'Authorization': f'Bearer {token}'}
    resp = requests.post(url, headers=headers)
    if resp.status_code != 200:
        return False, f'HTTP {resp.status_code} {resp.text[:256]}'
    js = resp.json()
    if js.get('code') == 200:
        return True, js.get('data')
    else:
        return False, js.get('message')


if __name__ == '__main__':
    args = parse_args()

    print(f"Connecting to DB {args.db_host}:{args.db_port} (DB: {args.db_name}) as {args.db_user}")
    conn = pymysql.connect(host=args.db_host, port=args.db_port, user=args.db_user, passwd=args.db_pass, db=args.db_name, cursorclass=pymysql.cursors.DictCursor)
    try:
        if args.delete:
            if args.dry_run:
                print("Dry run enabled. Skipping actual deletion.")
            else:
                hard_delete_all(conn)
            sys.exit(0)

        regs = get_all_registration_ids(conn)
        print(f"Found {len(regs)} registration rows")
        if not regs:
            sys.exit(0)

        # Admin login
        try:
            token = admin_login(args.base_url, args.admin_user, args.admin_pass)
            print(f"Admin token obtained (length {len(token)})")
        except Exception as e:
            print(f"Admin login failed: {e}")
            sys.exit(1)

        cancel_success = 0
        cancel_fail = 0
        cannot_cancel = 0
        fail_reasons = {}
        for row in regs:
            reg_id = row['reg_id']
            status = row['status']
            if status is None:
                status = 0
            # status >= 3 cannot be cancelled (already in consultation or completed)
            if status >= 3:
                print(f"Skipping reg {reg_id} with status {status} - cannot cancel")
                cannot_cancel += 1
                continue

            if args.dry_run:
                print(f"DRY RUN: Would cancel reg {reg_id} (status {status})")
                continue

            ok, info = cancel_registration(args.base_url, token, reg_id)
            if ok:
                print(f"Cancelled reg {reg_id}")
                cancel_success += 1
            else:
                print(f"Failed to cancel reg {reg_id}: {info}")
                cancel_fail += 1
                fail_reasons[str(info)] = fail_reasons.get(str(info), 0) + 1

        print('--- Summary ---')
        print(f"Total registrations: {len(regs)}")
        print(f"Cancelled: {cancel_success}")
        print(f"Failed to cancel: {cancel_fail}")
        print(f"Skipped (in consultation/completed): {cannot_cancel}")

        if fail_reasons:
            print('\nFailure reasons:')
            for k, v in fail_reasons.items():
                print(f"  {v}x -> {k}")

    finally:
        conn.close()

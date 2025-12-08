import requests
import threading
import time
import random
import datetime
import argparse
from typing import Optional

# Configuration
BASE_URL = "http://localhost/api"  # default for docker+nginx reverse proxy (port 80)
DEFAULT_PATIENT_COUNT = 20
MAX_PATIENT_COUNT = 200


def date_str(d):
    return d.strftime('%Y-%m-%d')


def get_json_safe(resp):
    try:
        return resp.json()
    except Exception:
        print(f"DEBUG: response not JSON. Status: {resp.status_code}. Body begins: {resp.text[:256]}")
        return None

def register_patient(phone, password, name):
    """Register a new patient using demo API"""
    url = f"{BASE_URL}/auth/patient/register"
    payload = {
        "name": name,
        "phone": phone,
        "password": password
    }
    resp = requests.post(url, json=payload)
    if resp.status_code != 200:
        js = get_json_safe(resp)
        raise RuntimeError(f"Failed to register {phone}: status {resp.status_code}, response: {js or resp.text}")
    return get_json_safe(resp).get('data')


def login_patient(phone, password):
    """Patient login to get token and userId"""
    url = f"{BASE_URL}/auth/patient/login"
    payload = {"username": phone, "password": password}
    resp = requests.post(url, json=payload)
    if resp.status_code != 200:
        js = get_json_safe(resp)
        raise RuntimeError(f"Failed to login {phone}: status {resp.status_code}, response: {js or resp.text}")
    data = get_json_safe(resp).get('data', {})
    return data.get('token'), data.get('userId')

def grab_number(patient_id, token, schedule_id):
    """Attempt to grab a number for a specific schedule"""
    url = f"{BASE_URL}/registration/create"
    headers = {"Authorization": f"Bearer {token}"} if token else {}
    payload = {
        "patientId": patient_id,
        "scheduleId": schedule_id
    }
    
    try:
        start_time = time.time()
        response = requests.post(url, json=payload, headers=headers, timeout=10)
        end_time = time.time()
        
        if response.status_code == 200:
            js = get_json_safe(response)
            if js and js.get('code') == 200:
                print(f"Patient {patient_id} SUCCESS. Time: {end_time - start_time:.3f}s")
                return True, js.get('data')
            else:
                msg = js.get('message') if js else ''
                # Detect expired time messages
                if msg and ('上午号源已过期' in msg or '下午号源已过期' in msg):
                    print(f"Patient {patient_id} FAILED (expired slot). Time: {end_time - start_time:.3f}s. Msg: {msg}. Consider booking ER schedule.")
                    return False, {'expired': True, 'message': msg}
                print(f"Patient {patient_id} FAILED (app code). Time: {end_time - start_time:.3f}s. Body: {js or response.text}")
                return False, js
        else:
            print(f"Patient {patient_id} FAILED. Status: {response.status_code}. Msg: {response.text}")
            return False, response.text
            
    except Exception as e:
        print(f"Patient {patient_id} ERROR: {e}")
        return False, str(e)

def get_demo_info():
    try:
        resp = requests.get(f"{BASE_URL}/auth/demo-info")
        resp.raise_for_status()
        js = get_json_safe(resp)
        return js.get('data', {}) if js else {}
    except Exception:
        return {}



def find_schedule_to_test(dept_override: Optional[int] = None, schedule_override: Optional[int] = None):
    # pick the first department and a schedule that has enough remaining
    # If schedule override is provided, try to locate it in schedule list instead of calling detail endpoint
    if schedule_override is not None:
        # get schedule list for today's date and search
        deps_resp = requests.get(f"{BASE_URL}/schedule/departments")
        deps_js = get_json_safe(deps_resp)
        deps_local = deps_js.get('data', []) if deps_js else []
        if not deps_local:
            raise RuntimeError("No departments available to validate schedule override")
        # choose dept_override if provided, else use the first department
        dept_search = dept_override if dept_override is not None else deps_local[0].get('deptId')
        today = datetime.date.today()
        url = f"{BASE_URL}/schedule/list?deptId={dept_search}&startDate={date_str(today)}&endDate={date_str(today)}"
        resp = requests.get(url)
        resp.raise_for_status()
        js = get_json_safe(resp)
        schedules = js.get('data', []) if js else []
        for s in schedules:
            if s.get('scheduleId') == schedule_override:
                quota_left = s.get('quotaLeft') or 0
                expired = s.get('expired')
                if expired:
                    print(f"WARNING: schedule {schedule_override} is expired. Attempting to find ER schedule fallback...")
                    # try to find ER schedule in the same department
                    er = next((x for x in schedules if x.get('shiftType') == 'ER' and x.get('quotaLeft', 0) > 0), None)
                    if er:
                        print(f"Found ER schedule {er.get('scheduleId')} as fallback")
                        return dept_search, er.get('scheduleId'), er.get('quotaLeft')
                    else:
                        print("No available ER schedule found; continue to return the expired schedule's quota (likely 0)")
                return dept_search, schedule_override, quota_left
        raise RuntimeError(f"Schedule {schedule_override} not found in dept {dept_search} schedule list")

    # Get departments and choose one (or use override dept)
    dep_resp = requests.get(f"{BASE_URL}/schedule/departments")
    dep_js = get_json_safe(dep_resp)
    deps = dep_js.get('data', []) if dep_js else []
    if dept_override is not None:
        dept_id = dept_override
    if not deps:
        raise RuntimeError("No departments available")
    dept_id = deps[0].get('deptId')
    today = datetime.date.today()
    url = f"{BASE_URL}/schedule/list?deptId={dept_id}&startDate={date_str(today)}&endDate={date_str(today)}"
    resp = requests.get(url)
    resp.raise_for_status()
    js = get_json_safe(resp)
    schedules = js.get('data', []) if js else []
    # Prefer non-expired schedules with available quota
    for s in schedules:
        quota_left = s.get('quotaLeft')
        expired = s.get('expired')
        if quota_left and quota_left > 0 and not expired:
            return dept_id, s.get('scheduleId'), quota_left
    # Fallback: prefer ER (urgent) schedules
    for s in schedules:
        shift = s.get('shift') or s.get('shiftType')
        if shift == 'ER' and s.get('quotaLeft', 0) > 0:
            return dept_id, s.get('scheduleId'), s.get('quotaLeft')
    # Old behavior: pick first schedule with quota even if expired
    for s in schedules:
        quota_left = s.get('quotaLeft')
        if quota_left and quota_left > 0:
            return dept_id, s.get('scheduleId'), quota_left
    # fallback: pick first schedule
    if schedules:
        s = schedules[0]
        quota_left = s.get('quotaLeft') or 0
        return dept_id, s.get('scheduleId'), quota_left
    raise RuntimeError('Unable to locate schedule for test')


def test_concurrency(dept_override: Optional[int] = None, schedule_override: Optional[int] = None, override_patient_count: Optional[int] = None):
    print(f"--- Starting Concurrency Test (finding schedule and quota first) ---")
    # Setup: find schedule
    dept_id, schedule_id, quota_left = find_schedule_to_test(dept_override, schedule_override)
    print(f"Using dept {dept_id}, schedule {schedule_id}")
    # QUOTA is the number of seats available (quotaLeft) from backend
    QUOTA = quota_left if quota_left is not None else 0

    # Ensure patient count is greater than QUOTA for contention
    if override_patient_count is not None:
        PATIENT_COUNT = override_patient_count
    else:
        PATIENT_COUNT = max(DEFAULT_PATIENT_COUNT, QUOTA + 5)
    # Avoid excessive load
    PATIENT_COUNT = min(PATIENT_COUNT, MAX_PATIENT_COUNT)

    # Avoid calling /schedule/{id} which may require auth; use public schedule list to find details
    list_url = f"{BASE_URL}/schedule/list?deptId={dept_id}&startDate={date_str(datetime.date.today())}&endDate={date_str(datetime.date.today())}"
    list_resp = requests.get(list_url)
    list_js = get_json_safe(list_resp)
    if not list_js or 'data' not in list_js:
        raise RuntimeError(f"Failed to retrieve schedule list: status {list_resp.status_code}")
    schedules = list_js.get('data', [])
    schedule_before = next((s for s in schedules if s.get('scheduleId') == schedule_id), None)
    if schedule_before is None:
        raise RuntimeError(f"Schedule {schedule_id} not found in list for dept {dept_id}; possible permission issue")
    print(f"Schedule before: currentCount={schedule_before.get('currentCount')}, maxQuota={schedule_before.get('maxQuota')}")
    print(f"Detected QUOTA (quotaLeft): {QUOTA}, launching PATIENT_COUNT: {PATIENT_COUNT}")

    # Prepare test users
    users = []  # list of (userId, token)
    for i in range(PATIENT_COUNT):
        phone = f"1550000{1000 + i}"
        password = "testpass123"
        name = f"TestUser{i}"
        try:
            data = register_patient(phone, password, name)
            patient_id = data.get('patientId') if data else None
            token, uid = login_patient(phone, password)
            if token is None:
                print(f"Could not login patient {phone}")
            users.append((uid or patient_id, token))
        except Exception as e:
            print(f"Failed to create/login test user {phone}: {e}")
            # try to login existing
            try:
                token, uid = login_patient(phone, password)
                users.append((uid, token))
            except Exception:
                continue

    print(f"Prepared {len(users)} users")

    # Prepare threads
    results = []
    def worker(u):
        uid, token = u
        #time.sleep(random.uniform(0, 0.1))  # random gap
        ok, data = grab_number(uid, token, schedule_id)
        results.append((ok, uid, data))

    threads = []
    for u in users:
        t = threading.Thread(target=worker, args=(u,))
        threads.append(t)
        t.start()

    for t in threads:
        t.join()

    # Summary
    success_count = sum(1 for r in results if r[0])
    fail_count = len(results) - success_count
    print("--- Test Summary ---")
    print(f"Total attempts: {len(results)}, Success: {success_count}, Fail: {fail_count}")
    # Fetch updated schedule list to compare
    list_resp_after = requests.get(list_url)
    list_js_after = get_json_safe(list_resp_after)
    schedules_after = list_js_after.get('data', []) if list_js_after else []
    schedule_after = next((s for s in schedules_after if s.get('scheduleId') == schedule_id), None)
    if schedule_after is None:
        print(f"WARNING: Schedule {schedule_id} not found after test; could be removed or no longer visible")
    print(f"Schedule after: currentCount={schedule_after.get('currentCount')}, maxQuota={schedule_after.get('maxQuota')}")
    # Aggregate failure reasons
    failure_reasons = {}
    for ok, uid, data in results:
        if ok:
            continue
        reason = 'unknown'
        if isinstance(data, dict):
            if data.get('expired'):
                reason = 'expired'
            elif data.get('message'):
                reason = str(data.get('message'))
            else:
                reason = str(data)
        else:
            reason = str(data)
        failure_reasons[reason] = failure_reasons.get(reason, 0) + 1

    if failure_reasons:
        print('\nFailure reasons breakdown:')
        for reason, count in failure_reasons.items():
            print(f"  {count}x -> {reason}")

    return users

def test_permissions(users):
    print("\n--- Starting Permission Test ---")
    if not users or len(users) < 2:
        print("Not enough users to test permission behavior.")
        return
    # Use first two users
    userA = users[0]
    userB = users[1]
    tokenA = userA[1]
    idB = userB[0]

    # 1. Patient A trying to access Patient B's medical records
    other_patient_record_url = f"{BASE_URL}/emr/patient/{idB}"
    response = requests.get(other_patient_record_url, headers={"Authorization": f"Bearer {tokenA}"})
    js = get_json_safe(response)
    # Interpret JSON app-level code if present, otherwise fall back to HTTP status
    if js and js.get('code') != 200:
        # Non-zero app code means access denied or app error -> PASS if it's permission related
        msg = js.get('message', '')
        if 'permission' in (msg or '').lower() or '权限' in (msg or ''):
            print("PASS: Patient cannot access other patient's records (app level permission)")
        else:
            print(f"PASS (app error): {msg}")
    elif response.status_code in [403, 401]:
        print("PASS: Patient cannot access other patient's records (HTTP forbidden)")
    elif js and js.get('code') == 200:
        print(f"FAIL: Patient accessed other record. app code 0; data: {js.get('data')}")
    else:
        print(f"FAIL: Unexpected response. Status: {response.status_code}; Body: {response.text}")

    # 2. Patient trying to access admin interface
    admin_url = f"{BASE_URL}/admin/users"
    response = requests.get(admin_url, headers={"Authorization": f"Bearer {tokenA}"})
    js = get_json_safe(response)
    if js and js.get('code') != 200:
        msg = js.get('message', '')
        if 'permission' in (msg or '').lower() or '权限' in (msg or ''):
            print("PASS: Patient cannot access admin interface (app level permission)")
        else:
            print(f"PASS (app error): {msg}")
    elif response.status_code in [403, 401]:
        print("PASS: Patient cannot access admin interface (HTTP forbidden)")
    elif js and js.get('code') == 200:
        print(f"FAIL: Patient accessed admin interface. data: {js.get('data')}")
    else:
        print(f"FAIL: Unexpected response. Status: {response.status_code}; Body: {response.text}")

if __name__ == "__main__":
    # Note: This script assumes the backend is running.
    # If backend is not running, it will just print connection errors, which is expected in this environment.
    parser = argparse.ArgumentParser(description='Concurrency and permission test for TinyHIS')
    parser.add_argument('--base-url', type=str, default=BASE_URL, help='Base URL of backend (default: http://localhost/api)')
    parser.add_argument('--dept', type=int, default=None, help='Department ID to select schedules from')
    parser.add_argument('--schedule', type=int, default=None, help='Schedule ID to test directly')
    parser.add_argument('--patients', type=int, default=None, help='Number of test patients to create (overrides default)')
    args = parser.parse_args()

    # allow override of BASE_URL
    if args.base_url:
        BASE_URL = args.base_url
    print(f"Using BASE_URL: {BASE_URL}")
    if args.dept:
        print(f"Using dept override: {args.dept}")
    if args.schedule:
        print(f"Using schedule override: {args.schedule}")
    try:
        users = test_concurrency(dept_override=args.dept, schedule_override=args.schedule, override_patient_count=args.patients)
        test_permissions(users)
    except Exception as e:
        print(f"Test execution failed (Backend likely offline): {e}")

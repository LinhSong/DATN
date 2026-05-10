import streamlit as st
import subprocess
import json
import os
import tempfile
import pandas as pd

from datetime import datetime

# =========================================================
# CONFIG
# =========================================================

st.set_page_config(
    page_title="API Testing Tool",
    layout="wide"
)

st.title(" API Testing Framework UI")

# =========================================================
# SESSION STATE
# =========================================================

default_states = {
    "results": None,
    "run_dir": None,

    # TAB1
    "json_data_tab1": "",
    "file_name_tab1": "custom",

    # TAB2
    "json_data_tab2": "",
    "file_name_tab2": "custom",

    "save_tc_tab1": False,
    "save_tc_tab2": False,

    "url_history": [],

    "last_input_mode_old": ""
}

for key, value in default_states.items():
    if key not in st.session_state:
        st.session_state[key] = value

# =========================================================
# UTILS
# =========================================================

def extract_group_from_url(url):

    try:
        return (
            url
            .replace("https://", "")
            .replace("http://", "")
            .split("/")[0]
            .replace(".", "_")
        )

    except:
        return "default_group"


def validate_testcase_structure(data):

    required_fields = [
        "id",
        "method",
        "endpoint",
        "expected_status"
    ]

    if not isinstance(data, list):
        return False, "JSON phải là ARRAY"

    for index, tc in enumerate(data):

        for field in required_fields:

            if field not in tc:
                return False, f"Test case {index + 1} thiếu field: {field}"

    return True, "OK"

# =========================================================
# INPUT
# =========================================================

st.header("1. Nhập dữ liệu Test Case")

tab1, tab2 = st.tabs([
    "🆕 URL mới",
    "📂 URL đã chạy"
])


active_base_url = ""
selected_group = None

# =========================================================
# TAB 1
# =========================================================

with tab1:
    st.subheader("🌐 API mới")

    base_url_new = st.text_input(
        "Base URL",
        key="base_url_new",
        placeholder="https://api.example.com"
    )

    input_mode_new = st.radio(
        "Chọn cách nhập Test Case",
        ["📄 Dán JSON", "📁 Upload file"],
        key="input_mode_new"
    )

    json_data_new = None
    file_name_new = "custom"

    # ================= PASTE =================
    if input_mode_new == "📄 Dán JSON":

        json_data_new = st.text_area(
            "Paste JSON",
            height=300,
            key="paste_new"
        )

        if json_data_new:
            file_name_new = f"paste_{datetime.now().strftime('%H%M%S')}"

    # ================= UPLOAD =================
    elif input_mode_new == "📁 Upload file":

        uploaded = st.file_uploader(
            "Upload JSON",
            type=["json"],
            key="upload_new"
        )

        if uploaded:
            json_data_new = uploaded.read().decode("utf-8")
            file_name_new = uploaded.name.replace(".json", "")

    # =====================================================
    # SAVE OPTION
    # =====================================================
    save_tc_tab1 = st.checkbox(
        "💾 Lưu test case (tự tạo nhóm theo URL)",
        key="save_tc_tab1"
    )

    # =====================================================
    # PREVIEW (IMPORTANT FIX)
    # =====================================================
    if st.session_state.json_data_tab1:

        st.subheader("📄 Preview")

        try:
            parsed = json.loads(st.session_state.json_data_tab1)

            with st.container(height=350, border=True):
                st.json(parsed)

        except Exception as e:
            st.error(f"JSON lỗi: {e}")

# =========================================================
# UPDATE SESSION STATE (FIXED SAFE WAY)
# =========================================================
if json_data_new:
    st.session_state.json_data_tab1 = json_data_new
    st.session_state.file_name_tab1 = file_name_new
# =========================================================
# TAB 2
# =========================================================

with tab2:
    st.subheader("📂 API đã chạy")

    base_url_old = st.text_input(
        "Base URL",
        key="base_url_old",
        placeholder="https://api.example.com"
    )

    testcase_root = "testcases"

    # ================= GROUP =================

    selected_group = None

    if os.path.exists(testcase_root):

        groups = [
            d for d in os.listdir(testcase_root)
            if os.path.isdir(os.path.join(testcase_root, d))
        ]

        if groups:

            selected_group = st.selectbox(
                "Chọn nhóm Testcase",
                groups
            )

    # ================= INPUT MODE =================

    input_mode_old = st.radio(
        "Chọn cách nhập Test Case",
        [
            "📄 Dán JSON",
            "📁 Upload file",
            "📂 Chọn file có sẵn"
        ],
        key="input_mode_old"
    )

    # ================= RESET WHEN CHANGE MODE =================

    if st.session_state.last_input_mode_old != input_mode_old:
        st.session_state.json_data_tab2 = ""
        st.session_state.file_name_tab2 = "custom"

    st.session_state.last_input_mode_old = input_mode_old

    json_data_old = None
    file_name_old = "custom"

    # ================= SAVE OPTION =================

    save_tc_tab2 = False

    if input_mode_old in ["📄 Dán JSON", "📁 Upload file"]:

        save_tc_tab2 = st.checkbox(
            "💾 Lưu test case vào nhóm",
            key="save_tc_tab2"
        )

    # ================= PASTE =================

    if input_mode_old == "📄 Dán JSON":

        json_data_old = st.text_area(
            "Paste JSON",
            height=300,
            key="paste_old"
        )

        file_name_old = f"paste_{datetime.now().strftime('%H%M%S')}"

    # ================= UPLOAD =================

    elif input_mode_old == "📁 Upload file":

        uploaded_old = st.file_uploader(
            "Upload JSON",
            type=["json"],
            key="upload_old"
        )

        if uploaded_old:
            json_data_old = uploaded_old.read().decode("utf-8")
            file_name_old = uploaded_old.name.replace(".json", "")

    # ================= EXISTING FILE =================

    elif input_mode_old == "📂 Chọn file có sẵn":

        if selected_group:

            group_path = os.path.join(testcase_root, selected_group)

            files = [
                f for f in os.listdir(group_path)
                if f.endswith(".json")
            ]

            if files:

                selected_file = st.selectbox(
                    "Chọn file",
                    files,
                    key="selected_tc_file"
                )

                if selected_file:

                    full_path = os.path.join(group_path, selected_file)

                    with open(full_path, "r", encoding="utf-8") as f:
                        content = f.read()

                    # ✔ IMPORTANT: update session_state NGAY khi chọn file
                    st.session_state.json_data_tab2 = content
                    st.session_state.file_name_tab2 = selected_file.replace(".json", "")

    # ================= UPDATE SESSION (ONLY WHEN INPUT EXISTS) =================

    if json_data_old:
        st.session_state.json_data_tab2 = json_data_old
        st.session_state.file_name_tab2 = file_name_old

    # ================= PREVIEW =================

    if st.session_state.json_data_tab2:

        st.subheader("📄 Preview")

        try:
            parsed = json.loads(st.session_state.json_data_tab2)

            with st.container(height=350, border=True):
                st.json(parsed)

        except Exception as e:
            st.error(f"JSON lỗi: {e}")
# =========================================================
# RUN TEST
# =========================================================

st.header("3. Chạy Test")

run_source = st.radio(
    "Chạy dữ liệu từ",
    ["🆕 Tab URL mới", "📂 Tab URL đã chạy"],
    horizontal=True
)

if st.button("▶️ Run Test"):

    if run_source == "🆕 Tab URL mới":
        current_data = st.session_state.json_data_tab1
        current_file = st.session_state.file_name_tab1
        current_base_url = st.session_state.get("base_url_new", "")

    else:
        current_data = st.session_state.json_data_tab2
        current_file = st.session_state.file_name_tab2
        current_base_url = st.session_state.get("base_url_old", "")

    # =====================================================
    # VALIDATE INPUT
    # =====================================================

    if not current_data:

        st.error("❌ Chưa có dữ liệu test case")
        st.stop()

    elif not current_base_url:

        st.error("❌ Vui lòng nhập Base URL")
        st.stop()

    # =====================================================
    # VALIDATE JSON
    # =====================================================

    try:

        parsed_json = json.loads(current_data)

    except Exception as e:

        st.error(f"JSON lỗi: {e}")
        st.stop()

    valid, message = validate_testcase_structure(
        parsed_json
    )

    if not valid:

        st.error(message)
        st.stop()

    # =====================================================
    # RUN TEST
    # =====================================================

    with st.spinner("Running API Test..."):

        # =================================================
        # DELETE OLD RESULT
        # =================================================

        if os.path.exists("output/result.json"):

            os.remove("output/result.json")

        # =================================================
        # TEMP FILE
        # =================================================

        tmp = tempfile.NamedTemporaryFile(
            delete=False,
            suffix=".json"
        )

        tmp.write(
            current_data.encode("utf-8")
        )

        tmp.close()

        # =================================================
        # RUN DIR
        # =================================================

        time_str = datetime.now().strftime(
            "%Y%m%d_%H%M%S"
        )

        run_dir = os.path.abspath(
            f"allure-results/run-{time_str}-{current_file}"
        )

        os.makedirs(
            run_dir,
            exist_ok=True
        )

        # =================================================
        # ENV
        # =================================================

        env = os.environ.copy()

        env["ALLURE_RESULTS_DIRECTORY"] = run_dir

        # =================================================
        # MAVEN COMMAND
        # =================================================

        cmd = (

            f'mvn clean test '

            f'-q '

            f'-Dsurefire.printSummary=false '

            f'-Dfile="{tmp.name}" '

            f'-DbaseUrl="{current_base_url}" '

            f'-Dallure.results.directory="{run_dir}"'
        )

        process = subprocess.run(
            cmd,
            shell=True,
            capture_output=True,
            text=True,
            env=env
        )

        # =================================================
        # MAVEN OUTPUT
        # =================================================

        if (
            process.stderr
            and "SLF4J" not in process.stderr
        ):

            st.warning(process.stderr)

        # =================================================
        # CHECK RESULT
        # =================================================

        if not os.path.exists("output/result.json"):

            st.error("❌ Không tạo được result.json")

            st.code(process.stdout)
            st.code(process.stderr)

            st.stop()

        st.success(
            f"✅ Saved: allure-results\\{os.path.basename(run_dir)}"
        )

        # =================================================
        # LOAD RESULT
        # =================================================

        with open(
            "output/result.json",
            "r",
            encoding="utf-8"
        ) as f:

            results = json.load(f)

        st.session_state.results = results
        st.session_state.run_dir = run_dir

        # =================================================
        # SAVE TESTCASE TAB1
        # =================================================

        if (
            st.session_state.active_tab == "tab1"
            and save_tc_tab1
        ):

            group_name = extract_group_from_url(
                current_base_url
            )

            tc_dir = os.path.join(
                "testcases",
                group_name
            )

            os.makedirs(
                tc_dir,
                exist_ok=True
            )

            auto_name = (
                f"tc_{datetime.now().strftime('%H%M%S')}.json"
            )

            with open(
                os.path.join(tc_dir, auto_name),
                "w",
                encoding="utf-8"
            ) as f:

                f.write(current_data)

        # =================================================
        # SAVE TESTCASE TAB2
        # =================================================

        if (
            st.session_state.active_tab == "tab2"
            and st.session_state.save_tc_tab2
            and selected_group
        ):

            tc_dir = os.path.join(
                "testcases",
                selected_group
            )

            os.makedirs(
                tc_dir,
                exist_ok=True
            )

            auto_name = (
                f"tc_{datetime.now().strftime('%H%M%S')}.json"
            )

            with open(
                os.path.join(tc_dir, auto_name),
                "w",
                encoding="utf-8"
            ) as f:

                f.write(current_data)

# =========================================================
# RESULT
# =========================================================

if st.session_state.results:

    results = st.session_state.results

    st.header("4. Kết quả")

    total = len(results)

    passed = sum(
        1 for r in results
        if r.get("passed")
    )

    failed = total - passed

    pass_rate = (
        round((passed / total) * 100, 2)
        if total > 0
        else 0
    )

    # =====================================================
    # METRICS
    # =====================================================

    col1, col2, col3, col4 = st.columns(4)

    col1.metric("Total", total)
    col2.metric("Passed", passed)
    col3.metric("Failed", failed)
    col4.metric("Pass Rate", f"{pass_rate}%")

    # =====================================================
    # TABLE
    # =====================================================

    df = pd.DataFrame(results)

    st.dataframe(
        df,
        use_container_width=True
    )

    # =====================================================
    # ERROR ANALYSIS
    # =====================================================

    st.subheader("📉 Phân loại lỗi")

    error_types = {

        "assertion_error": 0,
        "invalid_test_data": 0,
        "runtime_error": 0,
        "invalid_endpoint": 0
    }

    for r in results:

        if not r.get("passed"):

            err = r.get("errorType", "")

            if err in error_types:
                error_types[err] += 1

    c1, c2, c3, c4 = st.columns(4)

    c1.metric(
        "Assertion",
        error_types["assertion_error"]
    )

    c2.metric(
        "Invalid Data",
        error_types["invalid_test_data"]
    )

    c3.metric(
        "Runtime",
        error_types["runtime_error"]
    )

    c4.metric(
        "Invalid Endpoint",
        error_types["invalid_endpoint"]
    )

    # =====================================================
    # METHOD COVERAGE
    # =====================================================

    st.subheader("📌 API Coverage")

    method_count = {}

    for r in results:

        method = r.get("method", "UNKNOWN")

        method_count[method] = (
            method_count.get(method, 0) + 1
        )

    cols = st.columns(len(method_count))

    for index, (method, count) in enumerate(method_count.items()):

        cols[index].metric(method, count)

# =========================================================
# ALLURE REPORT
# =========================================================

st.header("5. Allure Report")

# =========================================================
# CURRENT RUN
# =========================================================

if st.session_state.run_dir:

    st.subheader("🚀 Current Run")

    st.code(st.session_state.run_dir)

    if st.button("🌐 Open Current Allure Report"):

        abs_path = os.path.abspath(
            st.session_state.run_dir
        )

        subprocess.Popen(
            f'allure serve "{abs_path}"',
            shell=True,
            cwd=os.getcwd()
        )

# =========================================================
# HISTORY
# =========================================================

st.subheader("📂 History Reports")

allure_root = "allure-results"

if os.path.exists(allure_root):

    folders = sorted(

        [

            f for f in os.listdir(allure_root)

            if os.path.isdir(
                os.path.join(allure_root, f)
            )
        ],

        reverse=True
    )

    if folders:

        selected_history = st.selectbox(
            "Chọn report cũ",
            folders
        )

        selected_path = os.path.abspath(
            os.path.join(
                allure_root,
                selected_history
            )
        )

        st.code(
            f"allure-results\\{selected_history}"
        )

        if st.button("📂 Open Selected Report"):

            subprocess.Popen(
                f'allure serve "{selected_path}"',
                shell=True,
                cwd=os.getcwd()
            )

    else:

        st.info("Chưa có report nào")

else:

    st.info("Chưa có thư mục allure-results")
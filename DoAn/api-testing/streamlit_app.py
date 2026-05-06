import streamlit as st
import subprocess
import json
import os
import tempfile
from datetime import datetime

st.set_page_config(page_title="API Testing Tool", layout="wide")

st.title("🚀 API Testing Framework UI")

# ================= SESSION =================
if "results" not in st.session_state:
    st.session_state.results = None
if "run_dir" not in st.session_state:
    st.session_state.run_dir = None
if "json_data" not in st.session_state:
    st.session_state.json_data = ""
if "file_name" not in st.session_state:
    st.session_state.file_name = "custom"
if "last_input_mode" not in st.session_state:
    st.session_state.last_input_mode = None
if "last_selected_file" not in st.session_state:
    st.session_state.last_selected_file = None

# ================= INPUT =================
st.header("1. Nhập dữ liệu Test Case")

input_mode = st.radio(
    "Chọn cách nhập",
    ["📄 Dán JSON", "📁 Upload file", "📂 Chọn file có sẵn"],
    key="input_mode"
)

# ===== detect change input mode =====
if st.session_state.last_input_mode != input_mode:
    st.session_state.last_input_mode = input_mode

    # ✅ RESET KẾT QUẢ
    st.session_state.results = None
    st.session_state.run_dir = None

    st.rerun()

# ===== HANDLE INPUT =====
if input_mode == "📄 Dán JSON":
    st.session_state.json_data = st.text_area(
        "Paste JSON",
        value=st.session_state.json_data,
        height=300
    )
    st.session_state.file_name = "custom"

elif input_mode == "📁 Upload file":
    uploaded = st.file_uploader("Upload JSON", type=["json"])
    if uploaded:
        st.session_state.json_data = uploaded.read().decode("utf-8")
        st.session_state.file_name = uploaded.name.replace(".json", "")

elif input_mode == "📂 Chọn file có sẵn":
    if os.path.exists("testcases"):
        files = [f for f in os.listdir("testcases") if f.endswith(".json")]

        selected = st.selectbox(
            "Chọn file",
            files,
            key="selected_file"
        )

        # ===== detect change file =====
        if st.session_state.last_selected_file != selected:
            st.session_state.last_selected_file = selected

            if selected:
                with open(f"testcases/{selected}") as f:
                    st.session_state.json_data = f.read()
                st.session_state.file_name = selected.replace(".json", "")

            # ✅ RESET KẾT QUẢ
            st.session_state.results = None
            st.session_state.run_dir = None

            st.rerun()

# ================= PREVIEW =================
if st.session_state.json_data:
    try:
        parsed = json.loads(st.session_state.json_data)
        with st.container(height=300):
            st.json(parsed)
    except:
        st.error("JSON lỗi")

# ================= RUN =================
st.header("2. Chạy Test")

if st.button("▶️ Run Test"):

    if not st.session_state.json_data:
        st.error("Chưa có data")
    else:
        with st.spinner("Running..."):

            # ===== TEMP FILE =====
            tmp = tempfile.NamedTemporaryFile(delete=False, suffix=".json")
            tmp.write(st.session_state.json_data.encode())
            tmp.close()

            # ===== TẠO RUN DIR =====
            time_str = datetime.now().strftime("%Y%m%d_%H%M%S")
            run_dir = os.path.abspath(f"allure-results/run-{time_str}-{st.session_state.file_name}")
            os.makedirs(run_dir, exist_ok=True)

            # ===== ENV ALLURE =====
            env = os.environ.copy()
            env["ALLURE_RESULTS_DIRECTORY"] = run_dir

            cmd = f'mvn clean test -q -Dsurefire.printSummary=false -Dfile="{tmp.name}" -Dallure.results.directory="{run_dir}"'

            subprocess.run(
                cmd,
                shell=True,
                capture_output=True,
                text=True,
                env=env
            )

            st.success(f"📁 Saved: {run_dir}")

            # ===== LOAD RESULT =====
            if os.path.exists("output/result.json"):
                with open("output/result.json") as f:
                    results = json.load(f)

                st.session_state.results = results
                st.session_state.run_dir = run_dir

# ================= RESULT =================
if st.session_state.results:

    results = st.session_state.results

    st.subheader("📊 Kết quả")

    total = len(results)
    passed = sum(1 for r in results if r["passed"])
    failed = total - passed
    pass_rate = round((passed / total) * 100, 2) if total > 0 else 0

    col1, col2, col3, col4 = st.columns(4)
    col1.metric("Total", total)
    col2.metric("Passed", passed)
    col3.metric("Failed", failed)
    col4.metric("Pass %", f"{pass_rate}%")

    st.dataframe(results, width="stretch")

    # ===== ERROR ANALYSIS =====
    st.subheader("📉 Phân loại lỗi")

    error_types = {
        "assertion_error": 0,
        "invalid_test_data": 0,
        "runtime_error": 0
    }

    for r in results:
        if not r["passed"]:
            err = r.get("errorType", "unknown")
            if err in error_types:
                error_types[err] += 1

    col1, col2, col3 = st.columns(3)
    col1.metric("Assertion", error_types["assertion_error"])
    col2.metric("Invalid Data", error_types["invalid_test_data"])
    col3.metric("Runtime", error_types["runtime_error"])

# ================= ALLURE =================
st.subheader("📈 Allure Report")

if st.session_state.run_dir:
    if st.button("🌐 Open Allure Report"):

        abs_path = os.path.abspath(st.session_state.run_dir)

        subprocess.Popen(
            f'allure serve "{abs_path}"',
            shell=True,
            cwd=os.getcwd()
        )
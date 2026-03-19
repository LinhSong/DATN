import json
import os

def load_test_data(file_name):
    base_path = os.path.dirname(os.path.dirname(__file__))
    file_path = os.path.join(base_path, 'data', file_name)

    if not os.path.exists(file_path):
        raise FileNotFoundError(f"File not found: {file_path}")

    with open(file_path, 'r', encoding='utf-8') as file:
        return json.load(file)
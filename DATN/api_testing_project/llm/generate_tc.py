import os
import json
from dotenv import load_dotenv
from openai import OpenAI

# Load API key
load_dotenv()
client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

# Prompt 
PROMPT = """
You are a software tester.

Generate detailed test cases for the API GET /pet/{petId}

Requirements:
- Cover equivalence partitioning
- Include boundary value analysis
- Include negative testing

Return ONLY in format:
Test Case ID | Description | Input | Expected Result

Ensure no duplicate cases.
"""

def call_llm():
    response = client.chat.completions.create(
        model="gpt-4.1-mini",
        messages=[
            {"role": "system", "content": "You are a professional QA engineer."},
            {"role": "user", "content": PROMPT}
        ],
        temperature=0.2
    )

    return response.choices[0].message.content


def parse_output(output):
    test_cases = []

    lines = output.strip().split("\n")

    for line in lines:
        if "|" not in line:
            continue

        parts = [p.strip() for p in line.split("|")]

        if len(parts) != 4:
            continue

        tc_id, desc, input_val, expected = parts

        try:
            if input_val.startswith('"') and input_val.endswith('"'):
                input_val = input_val.strip('"')
            elif input_val.lower() == "true":
                input_val = "true"
            elif "." in input_val:
                input_val = float(input_val)
            else:
                input_val = int(input_val)
        except:
            pass

        try:
            expected = int(expected.split()[0])
        except:
            expected = expected

        test_cases.append({
            "id": tc_id,
            "description": desc,
            "input": input_val,
            "expected": expected
        })

    return test_cases


def save_json(data):
    os.makedirs("data", exist_ok=True)

    with open("data/pet_tc.json", "w", encoding="utf-8") as f:
        json.dump(data, f, indent=4)


if __name__ == "__main__":
    print("Calling LLM to generate test cases...\n")

    raw_output = call_llm()

    print(" LLM Output:\n")
    print(raw_output)

    parsed = parse_output(raw_output)

    save_json(parsed)

    print("\nTest cases saved to data/pet_tc.json")
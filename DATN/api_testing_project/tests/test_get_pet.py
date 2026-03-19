import pytest 
from api.pet_api import PetAPI
from utils.read_json import load_test_data

test_data = load_test_data("pet_tc.json")   
@pytest.mark.parametrize("tc", test_data)
def test_get_pet(tc):
    pet_id = tc["input"]
    expected = tc["expected"]

    print(f"\nRunning {tc['id']} - {tc['description']}")

    response = PetAPI.get_pet(pet_id)

    if isinstance(expected, list):
        assert response.status_code in expected
    else:
        assert response.status_code == expected
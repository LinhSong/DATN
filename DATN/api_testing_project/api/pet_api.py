import requests

BASE_URL = "https://petstore.swagger.io/v2"

class PetAPI:

    @staticmethod
    def get_pet(pet_id):
        url = f"{BASE_URL}/pet/{pet_id}"
        return requests.get(url)
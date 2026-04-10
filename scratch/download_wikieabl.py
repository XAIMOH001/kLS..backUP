import os
import requests

drinks = {
    "tusker": "https://upload.wikimedia.org/wikipedia/commons/e/e0/Tusker_Lager.jpg",
    "guinness": "https://upload.wikimedia.org/wikipedia/commons/4/4e/Guinness_Foreign_Extra.jpg",
    "coca_cola": "https://upload.wikimedia.org/wikipedia/commons/b/b2/Coca-Cola_bottle_2006.jpg",
    "fanta_orange": "https://upload.wikimedia.org/wikipedia/commons/c/ca/Fanta_Glass_Bottle.jpg",
    "sprite": "https://upload.wikimedia.org/wikipedia/commons/e/e4/Sprite_Bottle.jpg",
    "balozi": "https://www.eabl.com/sites/default/files/balozi-lager_0.png", # Trying simpler EABL link
    "pilsner": "https://www.eabl.com/sites/default/files/pilsner_0.png", # Trying simpler EABL link
    "stoney": "https://www.carrefour.ke/mafke/en/drinks/soft-drinks/stoney-tangawizi-500ml/p/1325" # This is a page, need to scrape or something
}

output_dir = "/var/home/xaimoh/Downloads/drinks-pos-v2/web/images/drinks"
os.makedirs(output_dir, exist_ok=True)

for name, url in drinks.items():
    if name == "stoney": continue # Special handling
    save_path = os.path.join(output_dir, f"{name}.jpg")
    print(f"Downloading {name} from {url}...")
    try:
        response = requests.get(url, timeout=15, headers={'User-Agent': 'Mozilla/5.0'})
        if response.status_code == 200:
            with open(save_path, 'wb') as f:
                f.write(response.content)
            print(f"Successfully saved to {save_path}")
        else:
            print(f"Failed to download {name}: Status {response.status_code}")
    except Exception as e:
        print(f"Error downloading {name}: {e}")

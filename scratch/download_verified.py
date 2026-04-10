import os
import requests

drinks = {
    "tusker": "https://www.eabl.com/sites/default/files/styles/product_full/public/tusker-lager_0.png",
    "guinness": "https://www.eabl.com/sites/default/files/styles/product_full/public/guinness-foreign-extra_1.png",
    "balozi": "https://www.eabl.com/sites/default/files/styles/product_full/public/balozi-lager_0.png",
    "pilsner": "https://www.eabl.com/sites/default/files/styles/product_full/public/pilsner_0.png",
    "coca_cola": "https://images.coke.com/is/image/Coke/coca-cola-original-20oz?wid=800&fmt=png-alpha",
    "fanta_orange": "https://images.coke.com/is/image/Coke/fanta-orange-20oz?wid=800&fmt=png-alpha",
    "sprite": "https://images.coke.com/is/image/Coke/sprite-20oz?wid=800&fmt=png-alpha",
    "stoney": "https://api.carrefourkenya.com/v1/images/products/1325/image",
    "white_cap": "https://thebar.com/media/catalog/product/w/h/white_cap_lager_500ml_bottle_1.jpg", # Alternative for white cap
    "water": "https://images.coke.com/is/image/Coke/dasani-purified-water-20oz?wid=800&fmt=png-alpha"
}

output_dir = "/var/home/xaimoh/Downloads/drinks-pos-v2/web/images/drinks"
os.makedirs(output_dir, exist_ok=True)

for name, url in drinks.items():
    save_path = os.path.join(output_dir, f"{name}.png" if "png" in url else f"{name}.jpg")
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

import os
import sqlite3
import requests
from duckduckgo_search import DDGS
import time
import random

def get_drink_names():
    db_path = "/var/home/xaimoh/Downloads/drinks-pos-v2/db/drinks.db"
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    cursor.execute("SELECT DISTINCT name, brand FROM drinks")
    drinks = cursor.fetchall()
    conn.close()
    return drinks

def download_image(query, save_path):
    print(f"Searching for: {query}")
    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
    }
    try:
        with DDGS() as ddgs:
            # Adding a bit of randomness to search parameters might help avoid simple detection
            results = ddgs.images(query, max_results=5)
            if results:
                # Try a few results if the first one fails to download
                for result in results:
                    img_url = result['image']
                    print(f"Attempting to download {img_url}")
                    try:
                        response = requests.get(img_url, timeout=10, headers=headers)
                        if response.status_code == 200:
                            # Basic check if it's an image
                            if 'image' in response.headers.get('Content-Type', ''):
                                with open(save_path, 'wb') as f:
                                    f.write(response.content)
                                return True
                    except Exception as e:
                        print(f"Failed to download from {img_url}: {e}")
                        continue
    except Exception as e:
        print(f"Error searching for {query}: {e}")
    return False

def main():
    drinks = get_drink_names()
    output_dir = "/var/home/xaimoh/Downloads/drinks-pos-v2/web/images/drinks"
    os.makedirs(output_dir, exist_ok=True)
    
    for name, brand in drinks:
        filename = name.lower().replace(" ", "_") + ".jpg"
        save_path = os.path.join(output_dir, filename)
        
        if os.path.exists(save_path) and os.path.getsize(save_path) > 0:
            print(f"Skipping {name}, already exists.")
            continue
            
        query = f"{name} {brand} beverage bottle"
        success = download_image(query, save_path)
        if success:
            print(f"Successfully downloaded {name}")
        else:
            print(f"Failed to download {name}")
        
        # Wait between 5 to 10 seconds to avoid rate limiting
        wait_time = random.uniform(5, 10)
        print(f"Waiting {wait_time:.2f} seconds...")
        time.sleep(wait_time)

if __name__ == "__main__":
    main()

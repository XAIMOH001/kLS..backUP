import os
import sqlite3
import requests
from duckduckgo_search import DDGS
import time
import random

def get_drinks():
    db_path = "/var/home/xaimoh/Downloads/drinks-pos-v2/db/drinks.db"
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    cursor.execute("SELECT DISTINCT name, brand FROM drinks")
    drinks = cursor.fetchall()
    conn.close()
    return drinks

def download_image(name, brand):
    output_dir = "/var/home/xaimoh/Downloads/drinks-pos-v2/web/images/drinks"
    os.makedirs(output_dir, exist_ok=True)
    
    filename = name.lower().replace(" ", "_") + ".jpg"
    save_path = os.path.join(output_dir, filename)
    
    if os.path.exists(save_path) and os.path.getsize(save_path) > 0:
        print(f"Skipping {name}, already exists.")
        return True
        
    query = f"{name} {brand} drink bottle product photo"
    print(f"Searching for: {query}")
    
    try:
        with DDGS() as ddgs:
            # We use text search first to find a page, or just use images if it works
            # Let's try text search to find a URL that looks like an image
            results = list(ddgs.images(query, max_results=3))
            if results:
                for res in results:
                    img_url = res['image']
                    print(f"Trying to download: {img_url}")
                    try:
                        resp = requests.get(img_url, timeout=10, headers={'User-Agent': 'Mozilla/5.0'})
                        if resp.status_code == 200:
                            with open(save_path, 'wb') as f:
                                f.write(resp.content)
                            print(f"Successfully downloaded {name}")
                            return True
                    except:
                        continue
    except Exception as e:
        print(f"Error searching for {name}: {e}")
    
    return False

def main():
    drinks = get_drinks()
    for name, brand in drinks:
        success = download_image(name, brand)
        if not success:
            print(f"Failed to download {name}")
        
        # Wait a long time to avoid rate limiting
        wait = random.uniform(15, 25)
        print(f"Waiting {wait:.1f}s...")
        time.sleep(wait)

if __name__ == "__main__":
    main()

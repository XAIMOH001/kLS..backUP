import os
import sqlite3
from bing_image_downloader import downloader

def get_drink_names():
    db_path = "/var/home/xaimoh/Downloads/drinks-pos-v2/db/drinks.db"
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    cursor.execute("SELECT DISTINCT name, brand FROM drinks")
    drinks = cursor.fetchall()
    conn.close()
    return [f"{name} {brand}" for name, brand in drinks]

def main():
    drinks = get_drink_names()
    output_dir = "/var/home/xaimoh/Downloads/drinks-pos-v2/web/images/drinks"
    os.makedirs(output_dir, exist_ok=True)
    
    for drink in drinks:
        clean_name = drink.split(' ')[0].lower()
        save_path = os.path.join(output_dir, f"{clean_name}.jpg")
        
        if os.path.exists(save_path) and os.path.getsize(save_path) > 0:
            print(f"Skipping {drink}, already exists.")
            continue
            
        print(f"Downloading image for: {drink}")
        try:
            downloader.download(f"{drink} bottle", limit=1, output_dir=output_dir, adult_filter_off=True, force_replace=False, timeout=60, verbose=False)
            # bing-image-downloader creates subdirectories, we need to move the file
            sub_dir = os.path.join(output_dir, f"{drink} bottle")
            if os.path.exists(sub_dir):
                files = os.listdir(sub_dir)
                if files:
                    os.rename(os.path.join(sub_dir, files[0]), save_path)
                # Cleanup sub_dir
                for f in os.listdir(sub_dir):
                    os.remove(os.path.join(sub_dir, f))
                os.rmdir(sub_dir)
        except Exception as e:
            print(f"Error downloading {drink}: {e}")

if __name__ == "__main__":
    main()

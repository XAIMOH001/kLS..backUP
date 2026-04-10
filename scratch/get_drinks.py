import sqlite3

def get_drink_names():
    db_path = "/var/home/xaimoh/Downloads/drinks-pos-v2/db/drinks.db"
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    
    # Get names and brands
    cursor.execute("SELECT DISTINCT name, brand FROM drinks")
    drinks = cursor.fetchall()
    
    conn.close()
    return drinks

if __name__ == "__main__":
    drinks = get_drink_names()
    for name, brand in drinks:
        print(f"{name} ({brand})")

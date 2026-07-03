import os
import firebase_admin
from firebase_admin import credentials, firestore, storage

# 1. Khởi tạo Firebase Admin SDK
# Bạn cần tải file serviceAccountKey.json từ Firebase Console:
# Project Settings -> Service accounts -> Generate new private key
cred = credentials.Certificate('serviceAccountKey.json')
firebase_admin.initialize_app(cred, {
    'storageBucket': 'YOUR_PROJECT_ID.appspot.com'  # Thay thế bằng Storage Bucket của bạn
})

db = firestore.client()
bucket = storage.bucket()

def upload_image_to_storage(local_file_path, storage_path):
    """
    Upload một file ảnh cục bộ lên Firebase Storage và trả về Download URL
    """
    if not os.path.exists(local_file_path):
        print(f"Không tìm thấy file: {local_file_path}")
        return None
        
    blob = bucket.blob(storage_path)
    blob.upload_from_filename(local_file_path)
    
    # Cấu hình để file có thể truy cập qua URL download công khai
    blob.make_public()
    return blob.public_url

def update_restaurant_image(restaurant_name, local_image_path):
    """
    Upload ảnh quán ăn lên Storage và cập nhật vào document tương ứng trong Firestore
    """
    storage_path = f"restaurant_images/{restaurant_name.replace(' ', '_').lower()}.jpg"
    download_url = upload_image_to_storage(local_image_path, storage_path)
    
    if not download_url:
        return
        
    # Tìm quán ăn theo tên trong collection 'food_places'
    places_ref = db.collection('food_places')
    query = places_ref.where('name', '==', restaurant_name).stream()
    
    found = False
    for doc in query:
        found = True
        doc.reference.update({
            'imageUrls': [download_url]  # Cập nhật danh sách URL ảnh
        })
        print(f"Đã cập nhật ảnh cho quán '{restaurant_name}': {download_url}")
        
    if not found:
        print(f"Không tìm thấy quán ăn có tên '{restaurant_name}' trong Firestore.")

def update_food_item_image(food_name, local_image_path):
    """
    Upload ảnh món ăn lên Storage và cập nhật vào document tương ứng trong Firestore
    """
    storage_path = f"food_images/{food_name.replace(' ', '_').lower()}.jpg"
    download_url = upload_image_to_storage(local_image_path, storage_path)
    
    if not download_url:
        return
        
    # Tìm món ăn theo tên trong collection 'food_items'
    items_ref = db.collection('food_items')
    query = items_ref.where('name', '==', food_name).stream()
    
    found = False
    for doc in query:
        found = True
        doc.reference.update({
            'imageUrls': [download_url]  # Cập nhật danh sách URL ảnh
        })
        print(f"Đã cập nhật ảnh cho món ăn '{food_name}': {download_url}")
        
    if not found:
        print(f"Không tìm thấy món ăn có tên '{food_name}' trong Firestore.")

# --- Ví dụ cách sử dụng ---
if __name__ == '__main__':
    # BƯỚC 1: Đảm bảo đã đặt file 'serviceAccountKey.json' vào cùng thư mục với script này.
    # BƯỚC 2: Điền đúng tên Storage Bucket ở trên.
    # BƯỚC 3: Uncomment các dòng dưới đây để chạy thử:
    
    # print("Bắt đầu upload...")
    # # Cập nhật ảnh cho quán ăn
    # update_restaurant_image("Quán Phở Gia Truyền", "path/to/local/pho_quan.jpg")
    
    # # Cập nhật ảnh cho món ăn
    # update_food_item_image("Phở Bò Đặc Biệt", "path/to/local/pho_bo.jpg")
    pass

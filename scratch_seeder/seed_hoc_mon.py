import os
import time
import firebase_admin
from firebase_admin import credentials, firestore

# serviceAccountKey.json must be in this folder or parent folder

if not os.path.exists('serviceAccountKey.json'):
    print("WARNING: serviceAccountKey.json not found.")
else:
    cred = credentials.Certificate('serviceAccountKey.json')
    firebase_admin.initialize_app(cred)

db = firestore.client()

def seed_hoc_mon_data():
    print("Starting clean seeding Hoc Mon and To Ky data to Firestore...")
    
    # List of places
    places = [
        {
            "name": "Lẩu Gà Hấp Hèm 34",
            "description": "Quán lẩu gà hấp hèm trứ danh Hóc Môn với hương vị chua ngọt đặc trưng, không gian sân vườn rộng rãi thoáng mát.",
            "address": "34/4G Nguyễn Ảnh Thủ, Bà Điểm, Hóc Môn, TP. HCM",
            "foodType": "Lẩu & Gà",
            "tags": ["lẩu gà", "hấp hèm", "đặc sản", "quán nhậu"],
            "latitude": 10.852431,
            "longitude": 106.612056,
            "openTime": "09:00",
            "closeTime": "22:00",
            "openLate": False,
            "priceRange": "MEDIUM",
            "averageRating": 4.6,
            "totalRating": 4.6,
            "reviewCount": 1,
            "favoriteCount": 0,
            "exploredCount": 0,
            "imageUrls": ["https://images.unsplash.com/photo-1547058886-f3b0942d67db?auto=format&fit=crop&w=800&q=80"],
            "createdBy": "system",
            "status": "APPROVED",
            "createdAt": int(time.time() * 1000),
            "updatedAt": int(time.time() * 1000),
            "dishes": [
                {
                    "name": "Lẩu Gà Hấp Hèm",
                    "description": "Lẩu gà hấp hèm đặc sản nước lẩu chua thanh ngon ngọt kèm rau rừng ngon miệng.",
                    "categoryName": "Món chính",
                    "foodType": "Lẩu & Gà",
                    "price": 250000.0,
                    "averageRating": 4.6,
                    "imageUrls": []
                },
                {
                    "name": "Gà Nướng Lu",
                    "description": "Gà ta thả vườn nướng lu da giòn rụm, thịt ngọt thơm dùng kèm xôi chiên phồng.",
                    "categoryName": "Món chính",
                    "foodType": "Lẩu & Gà",
                    "price": 180000.0,
                    "averageRating": 4.7,
                    "imageUrls": []
                },
                {
                    "name": "Gà Hấp Hành",
                    "description": "Gà ta hấp hành lá thơm phức, giữ nguyên vị ngọt đậm đà của thịt gà.",
                    "categoryName": "Món chính",
                    "foodType": "Lẩu & Gà",
                    "price": 160000.0,
                    "averageRating": 4.5,
                    "imageUrls": []
                }
            ]
        },
        {
            "name": "Bún Bò Huế Song Anh",
            "description": "Bún bò Huế chuẩn vị miền Trung, nước dùng đậm đà thơm mùi sả và mắm ruốc, thịt bò mềm và chả cua siêu chất lượng.",
            "address": "45/3 Song Hành, Hóc Môn, TP. HCM",
            "foodType": "Bún bò",
            "tags": ["bún bò", "bún bò huế", "ăn sáng", "bình dân"],
            "latitude": 10.870321,
            "longitude": 106.589123,
            "openTime": "06:00",
            "closeTime": "21:00",
            "openLate": False,
            "priceRange": "CHEAP",
            "averageRating": 4.5,
            "totalRating": 4.5,
            "reviewCount": 1,
            "favoriteCount": 0,
            "exploredCount": 0,
            "imageUrls": ["https://images.unsplash.com/photo-1625398407796-82650a8c135f?auto=format&fit=crop&w=800&q=80"],
            "createdBy": "system",
            "status": "APPROVED",
            "createdAt": int(time.time() * 1000),
            "updatedAt": int(time.time() * 1000),
            "dishes": [
                {
                    "name": "Bún Bò Đặc Biệt",
                    "description": "Tô đặc biệt đầy đủ nạm, chả cua, giò heo, gân bò và bò viên.",
                    "categoryName": "Món chính",
                    "foodType": "Bún bò",
                    "price": 55000.0,
                    "averageRating": 4.6,
                    "imageUrls": []
                },
                {
                    "name": "Bún Bò Tái Nạm",
                    "description": "Thịt bò tái tươi ngon và nạm bò chín mềm thơm ngọt.",
                    "categoryName": "Món chính",
                    "foodType": "Bún bò",
                    "price": 40000.0,
                    "averageRating": 4.4,
                    "imageUrls": []
                },
                {
                    "name": "Chả Cua Thêm",
                    "description": "Phần chả cua Huế dai giòn đậm vị cua tươi ngon.",
                    "categoryName": "Món thêm",
                    "foodType": "Bún bò",
                    "price": 15000.0,
                    "averageRating": 4.5,
                    "imageUrls": []
                }
            ]
        },
        {
            "name": "Bánh Mì Heo Quay Cô Chín",
            "description": "Bánh mì heo quay giòn bì siêu ngon với nước sốt đậm đà gia truyền của cô Chín, phục vụ nhanh chóng nhiệt tình.",
            "address": "12 Nguyễn Hữu Cầu, Hóc Môn, TP. HCM",
            "foodType": "Bánh mì",
            "tags": ["bánh mì", "heo quay", "ăn nhanh", "đường phố"],
            "latitude": 10.881452,
            "longitude": 106.595674,
            "openTime": "06:00",
            "closeTime": "20:00",
            "openLate": False,
            "priceRange": "CHEAP",
            "averageRating": 4.7,
            "totalRating": 4.7,
            "reviewCount": 1,
            "favoriteCount": 0,
            "exploredCount": 0,
            "imageUrls": ["https://images.unsplash.com/photo-1601050690597-df056fb4ce78?auto=format&fit=crop&w=800&q=80"],
            "createdBy": "system",
            "status": "APPROVED",
            "createdAt": int(time.time() * 1000),
            "updatedAt": int(time.time() * 1000),
            "dishes": [
                {
                    "name": "Bánh Mì Heo Quay Giòn Bì",
                    "description": "Bánh mì kẹp heo quay giòn bì nóng hổi, nước sốt cay nhẹ kèm dưa chua.",
                    "categoryName": "Món chính",
                    "foodType": "Bánh mì",
                    "price": 25000.0,
                    "averageRating": 4.8,
                    "imageUrls": []
                },
                {
                    "name": "Bánh Mì Xá Xíu",
                    "description": "Thịt xá xíu đậm đà, nước sốt ngon chuẩn vị nhà làm.",
                    "categoryName": "Món chính",
                    "foodType": "Bánh mì",
                    "price": 20000.0,
                    "averageRating": 4.6,
                    "imageUrls": []
                },
                {
                    "name": "Bánh Mì Chả Lụa",
                    "description": "Chả lụa thơm ngon kẹp bánh mì giòn tan, bơ và pate béo ngậy.",
                    "categoryName": "Món chính",
                    "foodType": "Bánh mì",
                    "price": 18000.0,
                    "averageRating": 4.5,
                    "imageUrls": []
                }
            ]
        },
        {
            "name": "Cơm Tấm Nguyễn Ảnh Thủ",
            "description": "Cơm tấm sườn nướng mật ong thơm phức, hạt cơm dẻo thơm ăn kèm bì chả tự làm cực ngon và nước mắm chua ngọt đặc sánh.",
            "address": "182 Nguyễn Ảnh Thủ, Hóc Môn, TP. HCM",
            "foodType": "Cơm tấm",
            "tags": ["cơm tấm", "sườn bì chả", "ăn trưa", "bình dân"],
            "latitude": 10.854125,
            "longitude": 106.610543,
            "openTime": "07:00",
            "closeTime": "21:00",
            "openLate": False,
            "priceRange": "CHEAP",
            "averageRating": 4.4,
            "totalRating": 4.4,
            "reviewCount": 1,
            "favoriteCount": 0,
            "exploredCount": 0,
            "imageUrls": ["https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=800&q=80"],
            "createdBy": "system",
            "status": "APPROVED",
            "createdAt": int(time.time() * 1000),
            "updatedAt": int(time.time() * 1000),
            "dishes": [
                {
                    "name": "Cơm Tấm Sườn Bì Chả",
                    "description": "Sườn nướng tẩm vị đậm đà kèm bì thơm, chả chưng trứng ngon ngậy.",
                    "categoryName": "Món chính",
                    "foodType": "Cơm tấm",
                    "price": 45000.0,
                    "averageRating": 4.5,
                    "imageUrls": []
                },
                {
                    "name": "Cơm Tấm Đùi Gà Nướng",
                    "description": "Đùi gà nướng mật ong vàng óng, da giòn thịt mọng nước.",
                    "categoryName": "Món chính",
                    "foodType": "Cơm tấm",
                    "price": 50000.0,
                    "averageRating": 4.3,
                    "imageUrls": []
                },
                {
                    "name": "Canh Khổ Qua Nhồi Thịt",
                    "description": "Canh khổ qua giải nhiệt mát lành nhồi thịt băm đậm vị.",
                    "categoryName": "Món phụ",
                    "foodType": "Cơm tấm",
                    "price": 15000.0,
                    "averageRating": 4.4,
                    "imageUrls": []
                }
            ]
        },
        {
            "name": "Bún Đậu Mắm Tôm Phố Cổ",
            "description": "Bún đậu mắm tôm chuẩn vị Hà Nội giữa lòng Hóc Môn. Đậu hũ chiên giòn, mắm tôm pha chế thơm ngon, nem chua rán nóng hổi.",
            "address": "78 Lê Thị Hà, Hóc Môn, TP. HCM",
            "foodType": "Bún đậu",
            "tags": ["bún đậu", "mắm tôm", "ăn tối", "đặc sản hà nội"],
            "latitude": 10.880153,
            "longitude": 106.594876,
            "openTime": "10:00",
            "closeTime": "22:00",
            "openLate": False,
            "priceRange": "CHEAP",
            "averageRating": 4.5,
            "totalRating": 4.5,
            "reviewCount": 1,
            "favoriteCount": 0,
            "exploredCount": 0,
            "imageUrls": ["https://images.unsplash.com/photo-1540189549336-e6e99c3679fe?auto=format&fit=crop&w=800&q=80"],
            "createdBy": "system",
            "status": "APPROVED",
            "createdAt": int(time.time() * 1000),
            "updatedAt": int(time.time() * 1000),
            "dishes": [
                {
                    "name": "Mẹt Bún Đậu Đầy Đủ",
                    "description": "Mẹt bún đậu đầy đủ bún lá, đậu rán giòn, thịt chân giò luộc, chả cốm, nem chua rán.",
                    "categoryName": "Món chính",
                    "foodType": "Bún đậu",
                    "price": 60000.0,
                    "averageRating": 4.6,
                    "imageUrls": []
                },
                {
                    "name": "Nem Chua Rán",
                    "description": "Đĩa nem chua chiên giòn rụm bên ngoài, dai mềm ngọt bên trong.",
                    "categoryName": "Ăn kèm",
                    "foodType": "Bún đậu",
                    "price": 35000.0,
                    "averageRating": 4.4,
                    "imageUrls": []
                },
                {
                    "name": "Chả Cốm Chiên",
                    "description": "Chả cốm dẻo thơm mùi nếp cốm non chiên vàng giòn.",
                    "categoryName": "Ăn kèm",
                    "foodType": "Bún đậu",
                    "price": 30000.0,
                    "averageRating": 4.5,
                    "imageUrls": []
                }
            ]
        },
        {
            "name": "Bún Cá Nha Trang Tô Ký",
            "description": "Bún cá Nha Trang chuẩn vị với nước lèo trong veo ngọt thanh, sứa tươi giòn sần sật, chả cá chiên và chả cá hấp dẻo dai ngon miệng.",
            "address": "142 Tô Ký, Thới Tam Thôn, Hóc Môn, TP. HCM",
            "foodType": "Bún cá",
            "tags": ["bún cá", "đặc sản nha trang", "ăn sáng", "bình dân"],
            "latitude": 10.871200,
            "longitude": 106.615400,
            "openTime": "06:00",
            "closeTime": "21:30",
            "openLate": False,
            "priceRange": "CHEAP",
            "averageRating": 4.5,
            "totalRating": 4.5,
            "reviewCount": 1,
            "favoriteCount": 0,
            "exploredCount": 0,
            "imageUrls": ["https://images.unsplash.com/photo-1569718212165-3a8278d5f624?auto=format&fit=crop&w=800&q=80"],
            "createdBy": "system",
            "status": "APPROVED",
            "createdAt": int(time.time() * 1000),
            "updatedAt": int(time.time() * 1000),
            "dishes": [
                {
                    "name": "Bún Cá Sứa Đặc Biệt",
                    "description": "Tô bún đầy đủ với sứa tươi giòn, chả cá hấp, chả cá chiên và cá dầm ngọt thịt.",
                    "categoryName": "Món chính",
                    "foodType": "Bún cá",
                    "price": 45000.0,
                    "averageRating": 4.6,
                    "imageUrls": []
                },
                {
                    "name": "Bún Chả Cá",
                    "description": "Bún chả cá chiên và hấp nóng hổi, nước dùng ngọt thanh thanh vị cá biển.",
                    "categoryName": "Món chính",
                    "foodType": "Bún cá",
                    "price": 35000.0,
                    "averageRating": 4.4,
                    "imageUrls": []
                },
                {
                    "name": "Sứa Thêm",
                    "description": "Phần sứa Nha Trang tươi giòn giòn ăn kèm cho đã thèm.",
                    "categoryName": "Món thêm",
                    "foodType": "Bún cá",
                    "price": 15000.0,
                    "averageRating": 4.5,
                    "imageUrls": []
                }
            ]
        },
        {
            "name": "Quán Lẩu Bò Tô Ký",
            "description": "Quán lẩu bò bình dân nổi tiếng khu Tô Ký với nước dùng đậm vị xương ống bò ninh nhừ, thịt bò tơ Củ Chi mềm ngọt và các món nướng hấp dẫn.",
            "address": "205 Tô Ký, Trung Mỹ Tây, Hóc Môn, TP. HCM",
            "foodType": "Lẩu bò",
            "tags": ["lẩu bò", "phá lấu", "quán nhậu", "lẩu đuôi bò"],
            "latitude": 10.865400,
            "longitude": 106.616200,
            "openTime": "11:00",
            "closeTime": "23:00",
            "openLate": True,
            "priceRange": "MEDIUM",
            "averageRating": 4.6,
            "totalRating": 4.6,
            "reviewCount": 1,
            "favoriteCount": 0,
            "exploredCount": 0,
            "imageUrls": ["https://images.unsplash.com/photo-1555126634-323283e090fa?auto=format&fit=crop&w=800&q=80"],
            "createdBy": "system",
            "status": "APPROVED",
            "createdAt": int(time.time() * 1000),
            "updatedAt": int(time.time() * 1000),
            "dishes": [
                {
                    "name": "Lẩu Bò Thập Cẩm",
                    "description": "Nồi lẩu bò thơm phức gồm nạm, gân, đuôi bò, lòng bò và các loại rau nấm ăn kèm.",
                    "categoryName": "Món chính",
                    "foodType": "Lẩu bò",
                    "price": 200000.0,
                    "averageRating": 4.7,
                    "imageUrls": []
                },
                {
                    "name": "Bò Tơ Nướng Y",
                    "description": "Thịt bò tơ cắt lát mỏng nướng trực tiếp tại bàn ăn kèm muối ớt xanh.",
                    "categoryName": "Món chính",
                    "foodType": "Lẩu bò",
                    "price": 150000.0,
                    "averageRating": 4.6,
                    "imageUrls": []
                },
                {
                    "name": "Phá Lấu Bò Kèm Bánh Mì",
                    "description": "Phá lấu bò béo ngậy nước cốt dừa kèm ổ bánh mì đặc ruột giòn rụm.",
                    "categoryName": "Món phụ",
                    "foodType": "Lẩu bò",
                    "price": 40000.0,
                    "averageRating": 4.5,
                    "imageUrls": []
                }
            ]
        },
        {
            "name": "Cơm Gà Xối Mỡ 142 Tô Ký",
            "description": "Cơm gà xối mỡ giòn rụm với hạt cơm chiên vàng giòn thơm dẻo, đùi gà góc tư siêu to chiên ráo dầu thịt mềm mọng nước dùng kèm kim chi chua ngọt.",
            "address": "185 Tô Ký, Thới Tam Thôn, Hóc Môn, TP. HCM",
            "foodType": "Cơm gà",
            "tags": ["cơm gà", "gà xối mỡ", "ăn trưa", "bình dân"],
            "latitude": 10.868200,
            "longitude": 106.615800,
            "openTime": "09:30",
            "closeTime": "21:30",
            "openLate": False,
            "priceRange": "CHEAP",
            "averageRating": 4.4,
            "totalRating": 4.4,
            "reviewCount": 1,
            "favoriteCount": 0,
            "exploredCount": 0,
            "imageUrls": ["https://images.unsplash.com/photo-1562967916-eb82221dfb92?auto=format&fit=crop&w=800&q=80"],
            "createdBy": "system",
            "status": "APPROVED",
            "createdAt": int(time.time() * 1000),
            "updatedAt": int(time.time() * 1000),
            "dishes": [
                {
                    "name": "Cơm Đùi Gà Xối Mỡ",
                    "description": "Phần cơm chiên giòn thơm kèm đùi gà góc tư da giòn thịt ngọt dưa leo cà chua.",
                    "categoryName": "Món chính",
                    "foodType": "Cơm gà",
                    "price": 42000.0,
                    "averageRating": 4.5,
                    "imageUrls": []
                },
                {
                    "name": "Cơm Cánh Gà Xối Mỡ",
                    "description": "Phần cơm kèm cánh gà chiên xối mỡ mắm tỏi thơm giòn đậm đà.",
                    "categoryName": "Món chính",
                    "foodType": "Cơm gà",
                    "price": 38000.0,
                    "averageRating": 4.3,
                    "imageUrls": []
                },
                {
                    "name": "Canh Rong Biển Thịt Bằm",
                    "description": "Canh rong biển nấu thịt băm thanh mát giải ngấy khi ăn đồ chiên.",
                    "categoryName": "Món thêm",
                    "foodType": "Cơm gà",
                    "price": 10000.0,
                    "averageRating": 4.4,
                    "imageUrls": []
                }
            ]
        },
        {
            "name": "Hủ Tiếu Nam Vang Tài Anh Tô Ký",
            "description": "Hủ tiếu Nam Vang chuẩn vị với sợi hủ tiếu dai ngon trộn sốt đậm đà, nước dùng hầm xương ngọt lịm, kèm tôm tươi, thịt băm, gan và trứng cút.",
            "address": "95 Tô Ký, Thới Tam Thôn, Hóc Môn, TP. HCM",
            "foodType": "Hủ tiếu",
            "tags": ["hủ tiếu nam vang", "hủ tiếu khô", "ăn sáng", "ăn tối"],
            "latitude": 10.873500,
            "longitude": 106.615100,
            "openTime": "06:00",
            "closeTime": "22:30",
            "openLate": False,
            "priceRange": "CHEAP",
            "averageRating": 4.5,
            "totalRating": 4.5,
            "reviewCount": 1,
            "favoriteCount": 0,
            "exploredCount": 0,
            "imageUrls": ["https://images.unsplash.com/photo-1569718212165-3a8278d5f624?auto=format&fit=crop&w=800&q=80"],
            "createdBy": "system",
            "status": "APPROVED",
            "createdAt": int(time.time() * 1000),
            "updatedAt": int(time.time() * 1000),
            "dishes": [
                {
                    "name": "Hủ Tiếu Khô Đặc Biệt",
                    "description": "Hủ tiếu trộn nước sốt sệt cay chua ngọt cùng tôm tươi lột vỏ, tim gan bong bóng và trứng cút.",
                    "categoryName": "Món chính",
                    "foodType": "Hủ tiếu",
                    "price": 50000.0,
                    "averageRating": 4.6,
                    "imageUrls": []
                },
                {
                    "name": "Hủ Tiếu Nước",
                    "description": "Tô hủ tiếu truyền thống ngập tràn nước lèo thanh ngọt rắc hành lá hẹ tươi tôm tươi xắt mỏng.",
                    "categoryName": "Món chính",
                    "foodType": "Hủ tiếu",
                    "price": 45000.0,
                    "averageRating": 4.4,
                    "imageUrls": []
                },
                {
                    "name": "Xương Ống Thêm",
                    "description": "Một chén xương ống hầm tủy ngọt béo đầy ắp nhiều thịt.",
                    "categoryName": "Món thêm",
                    "foodType": "Hủ tiếu",
                    "price": 20000.0,
                    "averageRating": 4.5,
                    "imageUrls": []
                }
            ]
        }
    ]

    for p in places:
        # Delete existing restaurant and its dishes to ensure fresh start
        existing = db.collection('food_places').where('name', '==', p['name']).get()
        for doc in existing:
            place_id = doc.id
            print(f"Cleaning existing restaurant {place_id} and its dishes...")
            # Delete corresponding dishes
            dishes_query = db.collection('food_items').where('placeId', '==', place_id).get()
            for dish_doc in dishes_query:
                dish_doc.reference.delete()
            # Delete restaurant
            doc.reference.delete()

        dishes_data = p.pop('dishes')
        
        # Add food place
        place_ref = db.collection('food_places').document()
        p['id'] = place_ref.id
        place_ref.set(p)
        print(f"Added restaurant ID: {place_ref.id}")
        
        # Add dishes
        for d in dishes_data:
            dish_ref = db.collection('food_items').document()
            d['id'] = dish_ref.id
            d['placeId'] = place_ref.id
            d['createdAt'] = p['createdAt']
            d['updatedAt'] = p['updatedAt']
            dish_ref.set(d)
            print(f"  -> Added dish ID: {dish_ref.id}")

    print("Success: Hoc Mon and To Ky data seeding complete!")

if __name__ == '__main__':
    if os.path.exists('serviceAccountKey.json'):
        seed_hoc_mon_data()
    else:
        print("ERROR: serviceAccountKey.json not found.")

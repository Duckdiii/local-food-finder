# US-10 Community Chat - Test Checklist

## Chuẩn bị

- Bật Firebase Authentication, Firestore Database và Firebase Storage.
- Đăng nhập trên hai thiết bị hoặc emulator bằng hai tài khoản khác nhau.
- Cấp quyền vị trí cho ứng dụng.

## Acceptance Criteria

### AC-01 - Tự động vào phòng theo GPS

1. Mở tab `Cộng đồng` và cấp quyền vị trí.
2. Xác nhận ứng dụng tự mở phòng quận/huyện, không cần nhấn nút.
3. Xác nhận tên quận/huyện hiện rõ trên header.
4. Quay lại, chọn quận khác và nhấn `Vào phòng chat`.
5. Tắt GPS rồi mở lại tab.

Kết quả: phòng GPS hoặc phòng gần nhất được mở; room chưa tồn tại được tạo tự động.

### AC-02 - Optimistic và realtime

1. Mở cùng phòng trên hai thiết bị.
2. Gửi tin nhắn từ thiết bị A.
3. Nhập thử hơn 500 ký tự.

Kết quả: A thấy tin ngay với biểu tượng `Đang gửi`; B nhận không cần refresh; input bị giới hạn 500 ký tự.

### AC-03 - Offline queue

1. Tắt Wi-Fi và mobile data trong phòng chat.
2. Gửi tin text.
3. Chờ hơn 3 giây rồi bật mạng lại.

Kết quả: banner `Đang kết nối lại...` xuất hiện; tin giữ trạng thái chờ gửi và tự đồng bộ khi có mạng.

### AC-04 - Share card quán

1. Mở một phòng chat để lưu phòng hiện tại.
2. Mở chi tiết một quán và nhấn nút share.

Kết quả: app mở phòng hiện tại và gửi card gồm ảnh hoặc placeholder, tên và rating.

### AC-05 - Mở đúng chi tiết quán

1. Nhấn card quán trong chat.

Kết quả: màn chi tiết tải đúng document `food_places/{restaurantId}` của card.

### AC-06 - Giới hạn ảnh

1. Chọn ảnh lớn hơn 5MB.
2. Chọn ảnh hợp lệ rồi ngắt mạng khi đang upload.
3. Chọn lại ảnh hợp lệ khi có mạng.

Kết quả: ảnh lớn bị từ chối; upload lỗi rollback preview và báo toast; ảnh hợp lệ xuất hiện ngay rồi được gửi.

### AC-07 - Report

1. Nhấn giữ tin nhắn và chọn lý do.
2. Report lại bằng cùng tài khoản.
3. Report bằng năm tài khoản khác nhau.

Kết quả: mỗi lần thao tác có toast; một tài khoản chỉ tạo một report; tin vẫn hiện trước ngưỡng và bị ẩn khi đủ năm report.

## Kiểm tra bổ sung

- Tạo hơn 50 tin rồi scroll lên để kiểm tra cursor pagination.
- Mở lại phòng từng truy cập khi offline để kiểm tra cache 50 tin.
- Chuyển giữa nhiều phòng và xác nhận tin nhắn không lẫn phòng.

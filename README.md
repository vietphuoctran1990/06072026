# 🐾 Thú Cưng Ảo — Game nuôi thú cho bé trên Android TV

Game nuôi thú ảo (phong cách Pokemon) dành cho trẻ tiểu học, chạy trên tivi Android TV
(thiết kế cho Xiaomi TV S Mini LED 55" — Android 14) và **điều khiển hoàn toàn bằng
điều khiển tivi** (D-pad: lên / xuống / trái / phải / OK / BACK).

## 🎮 Chơi như thế nào?

- **Chọn 1 trong 3 quả trứng bí ẩn** (🔥 Lửa / 💧 Nước / 🍀 Lá) rồi bấm OK để ấp —
  trứng nứt dần và nở ra bé thú con!
- **Chăm sóc thú**: cho ăn 🍎, tắm 🛁, cho ngủ 💤 — giữ 4 thanh chỉ số luôn xanh
- **Chơi 2 mini game để kiếm xu**:
  - 🍎🧺 **Hứng Trái Cây** — di chuyển trái/phải hứng trái cây rơi, né hòn đá
  - 🌈🧠 **Bé Nhớ Giỏi** — nhìn màu nhấp nháy rồi bấm lại đúng thứ tự bằng D-pad
- **Lên cấp và TIẾN HÓA**: trứng → thú con → cấp 5 mọc tai/đuôi → cấp 10 thành rồng
  có cánh, sừng, màu đậm hơn và to lớn hơn hẳn!
- Quà tặng mỗi ngày, thú nói chuyện đáng yêu, hiệu ứng ngày/đêm theo giờ thật
- Tự động lưu game — tắt tivi bật lại vẫn còn nguyên bé thú

## 📺 Cách cài lên tivi Xiaomi

1. Tải file **ThuCungAo.apk** ở mục [Releases](../../releases) của repo này.
2. Chép APK vào USB rồi cắm vào tivi (hoặc dùng app *Send files to TV*).
3. Trên tivi: **Cài đặt → Bảo mật → Cho phép cài ứng dụng không rõ nguồn gốc** cho trình quản lý file.
4. Mở trình quản lý file trên tivi, chọn file APK và cài đặt.
5. Mở app **Thú Cưng Ảo** từ danh sách ứng dụng và chơi thôi! 🎉

## 🔧 Kỹ thuật

- App Android TV thuần Java (không dependency ngoài) + game HTML5 chạy trong WebView
- `minSdk 21`, `targetSdk 34` — tương thích Android TV 5.0 → 14
- Có category `LEANBACK_LAUNCHER`, banner TV, không yêu cầu màn hình cảm ứng
- Build tự động bằng GitHub Actions (`.github/workflows/build-apk.yml`),
  APK ký sẵn được đăng lên Releases sau mỗi lần push

Build thủ công: `./gradlew assembleRelease` (cần Android SDK 34).

# 🎧 Sesli Intercom Android App

Motorcuların kullandığı intercom sistemlerine benzer şekilde çalışan Android uygulaması. Kullanıcılar kulaklıklarını intercom cihazları gibi kullanabilirler.

## 📱 Özellikler

### 🔐 Kimlik Doğrulama
- Firebase Authentication ile güvenli kayıt ve giriş
- Kullanıcı profili yönetimi
- Şifre sıfırlama

### 🆔 ID Sistemi
- Her kullanıcının benzersiz ID'si
- ID kopyalama ve paylaşma
- ID ile doğrudan bağlantı kurma

### 🔗 Bağlantı Sistemi
- Google Nearby Connections ile peer-to-peer bağlantı
- Bluetooth üzerinden düşük gecikmeli iletişim
- Otomatik cihaz keşfi

### 🎵 Ses Özellikleri
- Gerçek zamanlı ses iletişimi
- Müzik paylaşımı
- Ses seviyesi kontrolü
- Susturma özelliği

### 🎤 Sesli Komutlar
- "Bağlan" / "Kapat" komutları
- "Sustur" / "Susturma kaldır" komutları
- "Müzik başlat" / "Müziği durdur" komutları
- "Ses aç" / "Ses kıs" komutları

## 🚀 Kurulum

### Gereksinimler
- Android Studio Arctic Fox veya üzeri
- Android SDK 24+
- Java 17
- Node.js (signaling server için)

### Adımlar

1. **Repository'yi klonlayın:**
```bash
git clone https://github.com/deniz79/Sesli-interxxx.git
cd Sesli-interxxx
```

2. **Firebase projesini ayarlayın:**
   - [Firebase Console](https://console.firebase.google.com/)'a gidin
   - Yeni proje oluşturun
   - Android uygulaması ekleyin (package: `com.intercomapp`)
   - `google-services.json` dosyasını `app/` klasörüne indirin

3. **Uygulamayı build edin:**
```bash
./gradlew assembleDebug
```

4. **Signaling server'ı başlatın (opsiyonel):**
```bash
cd signaling-server
npm install
node server.js
```

## 📥 APK İndirme

Doğrudan APK dosyasını indirmek için:

### 🟢 **Önerilen: İmzalı APK (En İyi)**
- [Sesli-Intercom-App-Signed.apk](Sesli-Intercom-App-Signed.apk) dosyasını indirin
- Android cihazınıza yükleyin
- **Bu APK doğru şekilde imzalanmış ve optimize edilmiştir**

### 🟡 **Alternatif: Debug APK (İmzalı)**
- [Sesli-Intercom-App-Debug.apk](Sesli-Intercom-App-Debug.apk) dosyasını indirin
- Android cihazınıza yükleyin
- **Bu APK imzalanmıştır ve doğrudan yüklenebilir**

### ⚠️ **Eski: Release APK (İmzalanmamış)**
- [Sesli-Intercom-App.apk](Sesli-Intercom-App.apk) dosyasını indirin
- **Bu APK imzalanmamıştır, yükleme sorunları yaşayabilirsiniz**

## 🛠️ Teknik Detaylar

### Kullanılan Teknolojiler
- **Android:** Kotlin, Material Design
- **Backend:** Firebase (Auth, Firestore, Cloud Messaging)
- **Bağlantı:** Google Nearby Connections API
- **Ses:** WebRTC (geçici olarak devre dışı)
- **UI:** ViewBinding, Navigation Component

### Mimari
- **MVVM Pattern:** ViewModel ve LiveData kullanımı
- **Repository Pattern:** Veri erişimi için
- **Service Pattern:** Arka plan işlemleri için

### İzinler
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

## 📱 Kullanım

1. **Uygulamayı açın ve kayıt olun**
2. **Ana sayfada kendi ID'nizi görün**
3. **"Kopyala" butonuyla ID'nizi paylaşın**
4. **Arkadaşınızın ID'sini "ID ile Bağlan" alanına girin**
5. **"ID ile Bağlan" butonuna tıklayın**
6. **İki kişi birbiriyle iletişime geçebilir!**

## 🔧 Geliştirme

### Proje Yapısı
```
app/
├── src/main/
│   ├── java/com/intercomapp/
│   │   ├── communication/     # Bağlantı yönetimi
│   │   ├── data/             # Veri katmanı
│   │   ├── service/          # Arka plan servisleri
│   │   └── ui/               # Kullanıcı arayüzü
│   └── res/                  # Kaynaklar
└── build.gradle              # Build konfigürasyonu
```

### Önemli Dosyalar
- `HomeFragment.kt` - Ana ekran
- `ConnectionManager.kt` - Bağlantı yönetimi
- `IntercomService.kt` - Arka plan servisi
- `AuthActivity.kt` - Kimlik doğrulama

## 🐛 Bilinen Sorunlar

- WebRTC geçici olarak devre dışı (dependency sorunları)
- Hilt dependency injection geçici olarak devre dışı
- Google Speech API geçici olarak devre dışı

## 🤝 Katkıda Bulunma

1. Fork yapın
2. Feature branch oluşturun (`git checkout -b feature/amazing-feature`)
3. Commit yapın (`git commit -m 'Add amazing feature'`)
4. Push yapın (`git push origin feature/amazing-feature`)
5. Pull Request oluşturun

## 📄 Lisans

Bu proje MIT lisansı altında lisanslanmıştır.

## 📞 İletişim

- **GitHub:** [@deniz79](https://github.com/deniz79)
- **Repository:** [Sesli-interxxx](https://github.com/deniz79/Sesli-interxxx)

## 🙏 Teşekkürler

- Firebase ekibine
- Google Nearby Connections API ekibine
- Android geliştirici topluluğuna

---

**Not:** Bu uygulama eğitim amaçlı geliştirilmiştir. Ticari kullanım için ek güvenlik önlemleri alınması gerekebilir.

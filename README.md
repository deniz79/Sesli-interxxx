# Intercom App

Motorcular için geliştirilmiş intercom sistemi Android uygulaması. Bu uygulama, kullanıcıların kulaklıklarını intercom cihazları gibi kullanabilmelerini sağlar.

## Özellikler

### 🔗 Gerçek Zamanlı İletişim
- **WebRTC** tabanlı düşük gecikmeli ses iletişimi
- **Bluetooth** ve **internet** üzerinden bağlantı
- **Grup konuşmaları** ve **birebir görüşmeler**

### 🎵 Müzik Paylaşımı
- Kullanıcıların kendi müziklerini grup içinde paylaşabilmesi
- **ExoPlayer** ile yüksek kaliteli ses çalma

### 🎤 Sesli Komutlar
- **Google Speech-to-Text** API ile sesli komut tanıma
- Türkçe dil desteği
- Komutlar: "bağlan", "kapat", "müzik başlat", "sustur" vb.

### 📱 Kullanıcı Yönetimi
- **Firebase Authentication** ile güvenli giriş
- **Firestore** ile kullanıcı profilleri ve grup yönetimi
- **Firebase Cloud Messaging** ile bildirimler

### 🔧 Teknik Özellikler
- **MVVM** mimari pattern
- **Hilt** dependency injection
- **Kotlin Coroutines** ile asenkron işlemler
- **Material Design** UI
- **Foreground Service** ile arka plan çalışma

## Kurulum

### Gereksinimler
- Android Studio Arctic Fox veya üzeri
- Android SDK 24+
- Kotlin 1.8.0+
- Firebase projesi

### Adımlar

1. **Projeyi klonlayın**
```bash
git clone https://github.com/your-username/intercom-app.git
cd intercom-app
```

2. **Firebase projesi oluşturun**
   - [Firebase Console](https://console.firebase.google.com/)'a gidin
   - Yeni proje oluşturun
   - Android uygulaması ekleyin (package: com.intercomapp)
   - `google-services.json` dosyasını indirin ve `app/` klasörüne yerleştirin

3. **Firebase servislerini etkinleştirin**
   - Authentication (Email/Password)
   - Firestore Database
   - Cloud Messaging

4. **Projeyi derleyin**
```bash
./gradlew build
```

## Kullanım

### İlk Kurulum
1. Uygulamayı açın
2. Kayıt olun veya giriş yapın
3. Gerekli izinleri verin (mikrofon, bluetooth, konum)

### Temel Kullanım
1. **Ana Sayfa**: Bağlantı durumu ve temel kontroller
2. **Kişiler**: Birebir görüşme yapabileceğiniz kişiler
3. **Gruplar**: Grup konuşmaları ve müzik paylaşımı
4. **Profil**: Hesap ayarları ve çıkış

### Sesli Komutlar
- "Bağlan" - Intercom sistemine bağlan
- "Kapat" - Bağlantıyı kes
- "Müzik başlat" - Müzik paylaşımını başlat
- "Müziği durdur" - Müzik paylaşımını durdur
- "Sustur" - Mikrofonu kapat
- "Susturmayı kaldır" - Mikrofonu aç
- "Ses aç" - Sesi artır
- "Ses kıs" - Sesi azalt

## Mimari

```
app/
├── data/
│   ├── model/          # Veri modelleri
│   └── repository/     # Repository sınıfları
├── communication/      # WebRTC ve sesli komut yönetimi
├── service/           # Arka plan servisleri
├── ui/                # UI bileşenleri
│   ├── auth/          # Giriş/kayıt ekranları
│   ├── home/          # Ana sayfa
│   ├── contacts/      # Kişiler
│   ├── groups/        # Gruplar
│   └── profile/       # Profil
└── di/                # Dependency injection
```

## Teknolojiler

- **Android**: Kotlin, Material Design
- **Mimari**: MVVM, Repository Pattern
- **Dependency Injection**: Hilt
- **Asenkron İşlemler**: Kotlin Coroutines
- **Ağ İletişimi**: Retrofit, OkHttp
- **Gerçek Zamanlı İletişim**: WebRTC
- **Sesli Komutlar**: Google Speech-to-Text
- **Backend**: Firebase (Auth, Firestore, FCM)
- **Ses İşleme**: ExoPlayer
- **Bluetooth**: Google Play Services Nearby

## Güvenlik

- Firebase Authentication ile güvenli kullanıcı yönetimi
- WebRTC ile şifrelenmiş ses iletişimi
- HTTPS üzerinden güvenli veri transferi
- Kullanıcı verilerinin Firestore'da güvenli saklanması

## Katkıda Bulunma

1. Fork yapın
2. Feature branch oluşturun (`git checkout -b feature/amazing-feature`)
3. Commit yapın (`git commit -m 'Add amazing feature'`)
4. Push yapın (`git push origin feature/amazing-feature`)
5. Pull Request oluşturun

## Lisans

Bu proje MIT lisansı altında lisanslanmıştır. Detaylar için [LICENSE](LICENSE) dosyasına bakın.

## İletişim

- Proje Linki: [https://github.com/your-username/intercom-app](https://github.com/your-username/intercom-app)
- Sorunlar: [Issues](https://github.com/your-username/intercom-app/issues)

## Teşekkürler

- [WebRTC](https://webrtc.org/) - Gerçek zamanlı iletişim
- [Firebase](https://firebase.google.com/) - Backend servisleri
- [Material Design](https://material.io/) - UI tasarım sistemi

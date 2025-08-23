# Intercom App - Kurulum Talimatları

## 🚀 Hızlı Başlangıç

### 1. **Google Speech API Key'i Alın**
- Google Cloud Console'da **"Sonraki"** butonuna tıklayın
- API key oluşturulacak
- API key'i kopyalayın

### 2. **API Key'i Uygulamaya Ekleyin**
- `app/src/main/res/values/api_keys.xml` dosyasını açın
- `YOUR_GOOGLE_SPEECH_API_KEY_HERE` yerine gerçek API key'inizi yazın

### 3. **Signaling Server'ı Başlatın**

#### **Windows için:**
```bash
cd signaling-server
start.bat
```

#### **Linux/Mac için:**
```bash
cd signaling-server
chmod +x start.sh
./start.sh
```

#### **Manuel başlatma:**
```bash
cd signaling-server
npm install
npm start
```

### 4. **Android Uygulamasını Çalıştırın**
- Android Studio'da projeyi açın
- **Run** butonuna tıklayın

## 📱 Uygulama Özellikleri

### ✅ **Çalışan Özellikler:**
- Firebase Authentication (giriş/kayıt)
- WebRTC gerçek zamanlı iletişim
- Signaling server entegrasyonu
- Sesli komut sistemi (Google Speech API ile)
- Modern Material Design UI
- Arka plan servisi

### 🎯 **Test Edilebilir Özellikler:**
1. **Giriş/Kayıt**: Firebase ile kullanıcı yönetimi
2. **Ana Sayfa**: İntercom kontrolleri
3. **Sesli Komutlar**: "bağlan", "kapat", "sustur" vb.
4. **WebRTC**: Gerçek zamanlı ses iletişimi
5. **Gruplar**: Çoklu kullanıcı iletişimi

## 🔧 **Teknik Detaylar**

### **Signaling Server:**
- **Port**: 3000
- **URL**: ws://10.0.2.2:3000 (emulator)
- **Health Check**: http://localhost:3000/health

### **Firebase Servisleri:**
- Authentication: ✅ Etkin
- Firestore: ✅ Etkin
- Cloud Messaging: ✅ Etkin

### **API'ler:**
- Google Speech-to-Text: ✅ Entegre
- WebRTC: ✅ Entegre
- Firebase: ✅ Entegre

## 🐛 **Olası Sorunlar ve Çözümleri**

### **Signaling Server Bağlantı Hatası:**
- Server'ın çalıştığından emin olun
- Port 3000'in açık olduğunu kontrol edin
- Firewall ayarlarını kontrol edin

### **Google Speech API Hatası:**
- API key'in doğru olduğunu kontrol edin
- Google Cloud Console'da API'nin etkin olduğunu kontrol edin
- Faturalama hesabının aktif olduğunu kontrol edin

### **Firebase Bağlantı Hatası:**
- `google-services.json` dosyasının doğru konumda olduğunu kontrol edin
- Firebase Console'da servislerin etkin olduğunu kontrol edin

## 📊 **Test Senaryoları**

### **1. Temel Test:**
- Uygulamayı açın
- Kayıt olun veya giriş yapın
- Ana sayfaya geçin
- Bağlan butonuna tıklayın

### **2. Sesli Komut Testi:**
- "Bağlan" deyin
- "Sustur" deyin
- "Müzik başlat" deyin

### **3. WebRTC Testi:**
- İki cihazda uygulamayı açın
- Aynı odaya katılın
- Ses iletişimini test edin

## 🎉 **Başarı!**

Uygulamanız artık tam özellikli bir intercom sistemi olarak çalışıyor!

### **Desteklenen Özellikler:**
- ✅ Gerçek zamanlı ses iletişimi
- ✅ Sesli komutlar
- ✅ Grup konuşmaları
- ✅ Kullanıcı yönetimi
- ✅ Modern UI/UX

### **Sonraki Adımlar:**
- Bluetooth entegrasyonu ekleyin
- Müzik paylaşımı geliştirin
- Gelişmiş sesli komutlar ekleyin
- UI/UX iyileştirmeleri yapın

# Napxstream

Xtream Codes API kullanan, Android telefon/tablet ve Android TV box'lar için IPTV uygulaması.

## Özellikler
- **Canlı TV**: Kategori bazlı kanal listesi, "Şimdi oynayan" bilgisi, favoriler
- **Filmler (VOD)**: Kategori/grid görünüm, film detay sayfası (afiş, konu, puan), kaldığı yerden devam et
- **Diziler**: Sezon/bölüm listesi, bölüm detay ve oynatma, kaldığı yerden devam et
- **EPG (Program Rehberi)**: Bir kanalın program akışını gösteren ekran (kanala uzun basarak açılır)
- **Arama**: Canlı/film/dizi genelinde anlık arama
- **Favoriler**: Canlı/film/dizi için ayrı favori listeleri (Room ile yerelde saklanır)
- **Android TV desteği**: D-pad ile gezinme, TV'ye özel sol menü (`layout-television/activity_main.xml`)

## Mimari
- Kotlin + MVVM (ViewModel + LiveData)
- Retrofit + Gson: Xtream Codes `player_api.php` uç noktaları
- Media3 (ExoPlayer): HLS/TS canlı yayın + VOD/dizi oynatımı
- Room: favoriler ve izleme ilerlemesi (resume position)
- Glide: kanal logosu / film-dizi afişleri

## Xtream API Entegrasyonu
Tüm istekler `XtreamAccount` (host, port, kullanıcı adı, şifre) üzerinden üretilen tam URL'lerle
`player_api.php?username=..&password=..&action=..` formatında yapılır. Kullanılan action'lar:
`get_live_categories`, `get_live_streams`, `get_vod_categories`, `get_vod_streams`, `get_vod_info`,
`get_series_categories`, `get_series`, `get_series_info`, `get_short_epg`.

Stream URL'leri:
- Canlı: `/live/{user}/{pass}/{stream_id}.m3u8`
- Film: `/movie/{user}/{pass}/{stream_id}.{ext}`
- Dizi: `/series/{user}/{pass}/{episode_id}.{ext}`

## Derleme
1. Bu klasörü Android Studio'da (Hedgehog veya üzeri) açın, Gradle senkronizasyonunu bekleyin.
2. `app/build.gradle.kts` içindeki bağımlılıklar internet üzerinden (Google/Maven Central) inecektir.
3. `Run` ile bir telefon/tablet emülatöründe veya Android TV emülatöründe çalıştırabilirsiniz.
4. Giriş ekranında Xtream sunucu adresinizi (host), portu, kullanıcı adı ve şifreyi girin.

## Bilinen sınırlamalar / geliştirilebilecek noktalar
- EPG ekranı şu an tek kanalın program listesini gösteriyor (çoklu kanal zaman çizelgesi/"grid" görünümü değil). Tam TV-rehberi tarzı grid istenirse ayrı bir geliştirme olarak eklenebilir.
- Şifre `SharedPreferences`'ta düz metin saklanıyor; istenirse `androidx.security:security-crypto` ile şifrelenebilir.
- Çoklu profil / çoklu hesap desteği yok (tek hesap ile giriş).
- Uygulama ikonları basit placeholder vektörlerdir; marka ikonlarıyla değiştirilebilir.

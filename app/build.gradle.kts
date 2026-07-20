import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
}

// keystore.properties dosyası varsa release imzalama bilgilerini oradan okur.
// Bu dosya repoya eklenmez (.gitignore'da); keystore.properties.example'a bakın.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()

if (hasKeystoreProperties) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.napxstream"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.napxstream"
        // NOT: minSdk 23 (Android 6.0) olarak ayarlandı. androidx.security:security-crypto
        // kütüphanesi (şifreli hesap bilgisi depolama için kullanılıyor) kendi
        // AndroidManifest.xml'inde minSdkVersion=23 talep ediyor; minSdk bundan düşük
        // olursa "Manifest merger failed: uses-sdk:minSdkVersion cannot be smaller than
        // version declared in library [androidx.security:security-crypto]" hatası alınır.
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasKeystoreProperties) {
            create("release") {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasKeystoreProperties) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }

    packaging {
        resources {
            // Play Services (Cast), OkHttp/Retrofit, Tink (security-crypto) ve Room gibi
            // çok sayıda kütüphane bir arada kullanıldığında aynı META-INF dosya yolunu
            // birden fazla .aar/.jar içinde bulundurabiliyor. Bu durum AGP'de
            // "More than one file was found with OS independent path 'META-INF/...'"
            // derleme hatasına yol açar. Aşağıdaki kurallar bu çakışmaların en yaygın
            // görülen dosya adlarını kapsar.
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
                "/META-INF/LICENSE",
                "/META-INF/LICENSE.txt",
                "/META-INF/LICENSE.md",
                "/META-INF/LICENSE-notice.md",
                "/META-INF/NOTICE",
                "/META-INF/NOTICE.txt",
                "/META-INF/*.kotlin_module",
                "/META-INF/INDEX.LIST",
                "/META-INF/versions/9/previous-compilation-data.bin"
            )
            pickFirsts += setOf(
                "/META-INF/proguard/*.pro"
            )
        }
    }
}

dependencies {
    // Core / UI
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.fragment:fragment-ktx:1.8.1")
    implementation("androidx.slidingpanelayout:slidingpanelayout:1.2.0")
    implementation("androidx.leanback:leanback:1.0.0") // Android TV D-pad odak/bileşen desteği

    // Lifecycle / MVVM
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

    // Networking (Xtream Codes API)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Görsel yükleme (kanal logo, poster)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    ksp("com.github.bumptech.glide:ksp:4.16.0")

    // ExoPlayer / Media3 (Live TV, VOD, Series oynatma + altyazı + Chromecast)
    implementation("androidx.media3:media3-exoplayer:1.4.0")
    implementation("androidx.media3:media3-exoplayer-hls:1.4.0")
    implementation("androidx.media3:media3-exoplayer-dash:1.4.0")
    implementation("androidx.media3:media3-ui:1.4.0")
    implementation("androidx.media3:media3-ui-leanback:1.4.0")
    implementation("androidx.media3:media3-datasource-okhttp:1.4.0")
    implementation("androidx.media3:media3-cast:1.4.0")

    // Chromecast
    implementation("com.google.android.gms:play-services-cast-framework:21.5.0")
    implementation("androidx.mediarouter:mediarouter:1.7.0")

    // Şifreli hesap depolama
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Yerel depolama (favoriler, EPG cache, hesap bilgileri)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Test
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}

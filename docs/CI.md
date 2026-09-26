# Android CI ve otomatik sürümleme

Akış: [`.github/workflows/android.yml`](../.github/workflows/android.yml).

## Proje incelemesi

- UI ağırlıklı olarak **Jetpack Compose + Material 3**; ana sekmeler Dashboard,
  Log, Tools ve Settings. Dar ekranda alt navigasyon, geniş ekranda navigation rail
  kullanılıyor. `MainActivity.kt`, `compose/navigation/` ve `compose/theme/Theme.kt`
  bu yapının giriş noktalarıdır.
- Sistem açık/koyu teması ve Android 12+ dinamik renkler destekleniyor. Editör ve
  kamera gibi bazı bileşenler `AndroidView` ile entegre edilmiş.
- Ekran durumu ViewModel/StateFlow, profil saklama Room üzerinden yönetiliyor.
  UI, Go tabanlı `libbox` çekirdeğine bağlı; yalnızca Gradle çalıştırmak yeterli değil.
- `other` Android 7+ (API 24), `otherLegacy` Android 5+ (API 21) içindir.
  Legacy için ayrı Compose/AndroidX sürümleri, editör uyarlaması ve native AAR var.
  Google Play'e özel `play` varyantı bu GitHub dağıtım akışının kapsamı dışında.
- Bunlar kaynak kod incelemesinin bulgularıdır; görsel kalite veya cihaz üzerinde
  uyumluluk testi yapılmış olduğu anlamına gelmez. UI değiştirilmemiştir.

## Ne zaman ve nasıl çalışır?

- Her dala yapılan push, `v*` tag push'ları, pull request ve manuel çalıştırma.
- Aynı dal/PR için yeni çalışma başladığında eski çalışma iptal edilir. Farklı
  dallar birbirini iptal etmez. Her push tetikler fakat eski her push tamamlanmaz.
- Önce Gradle wrapper ve Python yardımcı testleri kontrol edilir, sürüm üretilir.
- `LIBBOX_REF` ile sabitlenen upstream çekirdek, `GO_VERSION`, Java 17 ve NDK
  `28.0.13004108` kullanılarak derlenir. Tek üretim adımı hem `libbox.aar` hem
  `libbox-legacy.aar` oluşturur. Makefile'ın sabitlediği gomobile sürümü kullanılır.
- Native cache anahtarı core SHA, Go, NDK, seçilen ABI'ler ve CI tarifini içerir.
  Cache hit olsa bile AAR ZIP bütünlüğü, minSdk ve native kütüphanelerin seçilen
  ABI listesiyle tam eşleşmesi doğrulanır.
- İki Android varyantı paralel çalışır: lint, unit-test task'ı, APK derlemesi,
  APK sürüm metadata kontrolü ve `apksigner verify`.
- Android derlemesi Java **21** kullanır; gomobile Java **17** ister. Bu ayrım,
  mevcut AGP 9.3.1'in Java 17 lint hatasından kaçınır; uygulamanın Java/Kotlin
  bytecode hedefi hâlâ 17'dir.
- API 37.1, vendored libxposed-api için API 36 ve Build Tools 36.0.0 kurulur.
  Gradle wrapper sürümü değiştirilmez, resmi dağıtım SHA-256 kontrolü eklenmiştir.
- Native ve Gradle cache'leri, zaman aşımı ve sınırlı Gradle/Go paralelliği kullanılır.
  İlk native derleme sonraki cache'li çalışmalardan belirgin şekilde uzun sürebilir.

## ABI seçimi

Native çekirdek, Gradle APK split'leri ve çıktı doğrulaması aynı
`gradle.properties` ayarlarını kullanır. Şimdiki yapılandırma:

```properties
buildAbis=armeabi-v7a
buildUniversalApk=false
```

Bu ayarla yalnızca ARMv7 çekirdeği (`android/arm`) derlenir. `other` ve
`otherLegacy` varyantlarının her biri tek `armeabi-v7a` APK üretir; universal,
ARM64 ve x86 APK üretilmez. APK, 32 bit ARM uygulama desteği gerektirir;
yalnızca 64 bit uygulama çalıştıran cihazlara kurulamaz.

İleride tüm mimarileri ve universal APK'yı açmak için aynı iki ayarı değiştirin:

```properties
buildAbis=armeabi-v7a,arm64-v8a,x86,x86_64
buildUniversalApk=true
```

Listenin herhangi bir alt kümesi de kullanılabilir. Geçersiz veya tekrarlanan ABI
adları derlemeden önce reddedilir. ABI değişince native cache anahtarı da değişir;
eski ARMv7 AAR yanlışlıkla çoklu ABI derlemesinde kullanılmaz.

## Sürüm politikası

`version.properties` kaynak sürümünü tanımlar. CI yalnızca runner'ın çalışma
kopyasında `VERSION_CODE` ve `VERSION_NAME` değerlerini günceller; Gradle bunları
APK'ya ve `BuildConfig` alanlarına aktarır. **Depoya bot commit'i, tag veya Release
oluşturulmaz.** Böylece recursive CI, push yetkisi ve branch-protection istisnası
 gerekmez.

| Girdi | Çalışma no. | APK versionName | APK versionCode |
| --- | --- | --- | --- |
| `1.15.0-alpha.8` dal push'u | 42 | `1.15.0-alpha.8.ci.42` | `1000042` |
| `1.15.0` dal push'u | 43 | `1.15.1-ci.43` | `1000043` |
| Kaynakla eşleşen `v1.15.0` tag'i | 44 | `1.15.0` | `1000044` |

- Kod: `CI_VERSION_CODE_BASE + GITHUB_RUN_NUMBER`. Sabit başlangıç ofseti **1000000**.
  Kaynak `VERSION_CODE` değerinden büyük olmalı ve Android sınırı **2100000000**
  aşılmamalıdır. Hatalı girişlerde APK derlemesinden önce işlem durur.
- Artış bu workflow'un çalışma sayacına bağlıdır; PR'lar, iptal edilen ve başarısız
  çalışmalar da sayacı tüketebilir. Numaraların ardışık olması gerekmez.
- Aynı run yeniden çalıştırılırsa sürüm aynı kalır. Yeni push veya manuel yeni run
  yeni kod üretir. Eski run'ı sonradan yeniden çalıştırmak onu yeni sürüm yapmaz.
- Ofseti dağıtım başladıktan sonra düşürmeyin; workflow'u başka bir workflow/depo
  olarak yeniden oluşturup sayacı sıfırlamayın. Taşıma gerekiyorsa yeni ofseti en
  yüksek dağıtılmış kodun üstüne çıkarın. Dalların kaynak SemVer değerini geriye
  almak da sürüm adı sıralamasını bozabilir.
- SemVer sıralamasını değiştirmeyen `+sha` yerine `.ci.N` / `-ci.N` kullanılır.
  Kararlı bir sürümden sonraki geliştirme derlemesi sonraki patch'in ön sürümüdür.
- Tag tam olarak kaynak `VERSION_NAME` ile eşleşmelidir. Tag sürüm adı geliştirme
  adından düşük olabilir; CI ve tag kanallarını aynı otomatik güncelleme akışı gibi
  değerlendirmeyin. Bu workflow zaten otomatik Release yayımlamaz.
- `LIBBOX_REF` tam commit SHA'sıdır ve uygulama sürümünden bağımsızdır. Native core
  güncellemesi için uyumlu core SHA'sını ve gerekirse `GO_VERSION` değerini açıkça
  güncelleyin. CI adını upstream tag olarak kullanmak yanlış olur.

## İmzalama kurulumu

Secret tanımlamak zorunlu değildir: varsayılan çıktı kurulabilir **Debug** APK'dır.
**Ardışık APK'ları uygulama verilerini silmeden güncellemek için sabit release
anahtarını tanımlayın.** Temiz runner'lardaki otomatik debug anahtarı kalıcı değildir;
çeşitli run/flavor debug APK'ları birbirinin üzerine kurulamayabilir. Debug APK'lar
üretim dağıtımı için değildir.

GitHub → **Settings → Secrets and variables → Actions → New repository secret**:

| Secret | İçerik |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | Mevcut release keystore dosyasının Base64 içeriği |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore parolası |
| `ANDROID_KEY_ALIAS` | İmzalama anahtarının alias'ı |
| `ANDROID_KEY_PASSWORD` | Anahtar parolası |

Linux üzerinde, keystore'un bulunduğu dizinde Base64 hazırlama örneği:

```sh
base64 -w 0 release.keystore > release.keystore.base64
```

Base64 dosyasının içeriğini secret alanına aktarın; **bu dosyayı veya keystore'u
commit etmeyin**. Keystore, alias ve parolaların güvenli yedeğini saklayın. Her push
veya her sürüm için yeni anahtar üretmeyin. Mevcut upstream uygulaması farklı
sertifikayla imzalıysa kendi derlemeniz doğrudan onun üzerine kurulamaz.

- Dört secret'ın tamamı varsa **yalnızca repository'nin varsayılan dalındaki**
  push/manuel run imzalı, R8 ile küçültülmüş **Release** APK üretir. `dev` varsayılan
  dalsa ayrıca `main` dalı oluşturmanız gerekmez.
- Diğer dallar, PR'lar ve tag run'ları secretsız Debug üretir; `pull_request_target`
  kullanılmaz. Tag'i push etmek otomatik olarak release imzalama yetkisi vermez.
- Bir kısmı eksikse varsayılan dal sessizce debug'a düşmez: açık hata ile durur.
- Secrets yalnızca imzalama hazırlama adımına verilir. Geçici dosyalar gitignore
  kapsamındadır ve job sonunda temizlenir; artifact'e eklenmez.
- Varsayılan dalı ve workflow değişikliklerini koruyun; bu daldaki Gradle/script
  kodu imzalama dosyalarına erişebileceğinden güvenilmeyen değişiklikleri incelemeden
  birleştirmeyin.

## APK indirme

**Actions → Android CI → ilgili çalışma → Artifacts** bölümünden `SFA-...` adlı
artifact'i indirin. Her varyantın arşivinde:

- şu an yalnızca bir `armeabi-v7a` APK;
- `SHA256SUMS`;
- sürüm, app/core commit'leri, run bağlantısı ve APK hash'leri içeren
  `build-metadata.json` bulunur.

Android 5/6 için `otherLegacy`, Android 7+ için `other` artifact'ini kullanın.
Her ikisi de şu an 32 bit ARM desteği gerektirir. Modern ve legacy APK aynı
application ID'yi kullanır; aynı anda ayrı uygulamalar olarak kurulmazlar.

APK'lar 30 gün, lint/test raporları ve release R8 mapping'leri 14 gün, ara native
AAR artifact'i 1 gün saklanır. Kalıcı dağıtım ve crash çözümlemesi için release APK
ve mapping dosyalarını ayrıca arşivleyin. Repository/org saklama limitleri daha
kısa süre uygulayabilir. Çok eski başarısız job'ları tek başına tekrar çalıştırırken
ara AAR süresi dolmuşsa tüm workflow'u yeniden çalıştırın.

## Kapsam ve doğrulama sınırları

- Actions referansları tam commit SHA'larına sabitlidir; Dependabot haftalık Actions
  güncellemeleri açar. Token yalnızca `contents: read` yetkisine sahiptir.
- Projede mevcut uygulama unit/instrumentation test kaynakları bulunamadı. Gradle
  unit-test task'ı test yoksa `NO-SOURCE` olabilir; bu, UI test kapsamı var demek
  değildir. Eklenen Python testleri sürümleme, imzalama hazırlığı, native kontrol ve
  APK paketleme yardımcılarını sınar.
- Emülatör, screenshot ve fiziksel cihaz testleri bu akışta yoktur. Özellikle API
  21/23, profil içe aktarma, açık/koyu tema ve geniş ekran navigasyonu ayrıca
  doğrulanmalıdır. Lint hataları görmezden gelinmez veya otomatik baseline'a alınmaz.
- Proje `libghostty` SNAPSHOT ve bazı uzaktaki bağımlılıklar kullanır. Core/Actions
  sabitlemesi, bağımlılıklar kilitlenmeden bit-for-bit tekrarlanabilirlik garantisi
  sağlamaz. Paket deposu/ağ erişimi veya upstream API uyumsuzluğu derlemeyi durdurabilir.
- Mevcut `GitHubUpdateChecker.kt` **SagerNet/sing-box** deposunu sorgular; Actions
  artifact'i yayınlamak kendi forkunuzdan uygulama içi güncelleme sunmaz. Bu mevcut
  davranış değiştirilmedi. İleride fork güncellemeleri için repository hedefi,
  SemVer/tag politikası, `SFA-version-metadata.json`, API 23 legacy seçimi ve ABI
  asset seçimi birlikte ele alınmalıdır.
- Tam APK derlemesi GitHub runner üzerinde doğrulanmalıdır. Python yardımcı
  testleri yerelde çalıştırılabilir; bu testler Android/native derlemesinin veya
  fiziksel cihaz uyumluluğunun yerine geçmez.

Yardımcı testleri yerelde çalıştırmak için Python **3.11+** yeterlidir:

```sh
python3 -m unittest discover -s .github/scripts -p 'test_*.py' -v
```

// app 모듈 빌드 파일
import java.util.Properties

/*
 * Supabase 주소와 anon key는 local.properties(깃에 안 올라감)에서 읽는다.
 *   supabase.url=https://xxxx.supabase.co
 *   supabase.anonKey=eyJ...
 *   ai.serverUrl=http://10.0.2.2:8000
 * 비어 있으면 앱은 Fake 저장소로 동작한다 (서버 없이 화면 확인용).
 * service_role 키는 절대 여기에 넣지 않는다.
 */
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
fun localProp(key: String): String = localProps.getProperty(key, "").trim()

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization) // type-safe 내비게이션 route용
    alias(libs.plugins.ksp)                  // Hilt 코드 생성용
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.moim.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.moim.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SUPABASE_URL", "\"${localProp("supabase.url")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProp("supabase.anonKey")}\"")
        // AI 서버 주소 (예: 에뮬레이터에서 내 컴퓨터 서버 → http://10.0.2.2:8000)
        buildConfigField("String", "AI_SERVER_URL", "\"${localProp("ai.serverUrl")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true // 안 쓰는 리소스도 함께 제거 (릴리스 빌드만)
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true // BuildConfig.SUPABASE_URL 사용
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // DataStore: 자동 로그인 여부 같은 작은 설정값 저장
    implementation(libs.androidx.datastore.preferences)

    // Supabase
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.functions)
    implementation(libs.supabase.realtime)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)

    // 알림 예약 (WorkManager)
    implementation(libs.androidx.work.runtime)

    // 내비게이션 (type-safe route)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    // Hilt (의존성 주입)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

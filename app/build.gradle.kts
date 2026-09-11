apply(plugin = "com.android.application")

android {
    namespace = "it.paolo.allenamentoportieri"
    compileSdk = 35

    defaultConfig {
        applicationId = "it.paolo.allenamentoportieri"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

import com.teammanduk.adego.convention.configureHiltAndroid
import com.teammanduk.adego.convention.configureKotlinAndroid
import com.teammanduk.adego.extentions.implementation

plugins{
    id("adego.android.library")
    id("adego.android.compose")
}

configureKotlinAndroid()
configureHiltAndroid()

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
}
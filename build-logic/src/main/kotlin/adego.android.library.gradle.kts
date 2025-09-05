import com.teammanduk.adego.convention.configureHiltAndroid
import com.teammanduk.adego.convention.configureKotlinAndroid

plugins{
    id("com.android.library")
}

configureKotlinAndroid()
configureHiltAndroid()
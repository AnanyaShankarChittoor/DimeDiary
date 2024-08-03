plugins {
    id("com.android.application") version "8.5.1" apply false
    id("org.jetbrains.kotlin.android") version "1.8.10" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}

buildscript {
    dependencies {
        classpath("com.android.tools.build:gradle:8.5.1")
        classpath("com.google.gms:google-services:4.4.2")
    }
}

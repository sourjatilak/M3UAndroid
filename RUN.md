JAVA_HOME=/Library/Java/JavaVirtualMachines/amazon-corretto-17.jdk/Contents/Home ./gradlew :app:smartphone:assembleDebug && ~/Library/Android/sdk/platform-tools/adb -s 192.168.0.9:5555 install -r /Users/sourja/AndroidStudioProjects/M3UAndroid/app/smartphone/build/outputs/apk/debug/1.15.1_universal.apk && ~/Library/Android/sdk/platform-tools/adb -s 192.168.0.9:5555 shell am start -n com.m3u.smartphone/.MainActivity


JAVA_HOME=/Library/Java/JavaVirtualMachines/amazon-corretto-17.jdk/Contents/Home ./gradlew :app:smartphone:assembleDebug && ~/Library/Android/sdk/platform-tools/adb install -r app/smartphone/build/outputs/apk/debug/1.15.1_universal.apk && ~/Library/Android/sdk/platform-tools/adb shell am start -n com.m3u.smartphone/.MainActivity



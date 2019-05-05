#include "com_frostwire_android_python_PythonBridge.h"

JNIEXPORT jint JNICALL Java_com_frostwire_android_python_PythonBridge_square(JNIEnv *jniEnv, jclass clazz, jint n) {
  return n*n;
}

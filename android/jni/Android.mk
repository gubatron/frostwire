LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := python_bridge
LOCAL_SRC_FILES := python_bridge.c

include $(BUILD_SHARED_LIBRARY)

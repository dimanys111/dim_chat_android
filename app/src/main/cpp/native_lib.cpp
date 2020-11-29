#include <jni.h>

#include "sha1.hpp"

#define STB_IMAGE_RESIZE_IMPLEMENTATION
#include "stb_image_resize.h"

#include <fstream>
#include <encode.h>

void * memcpy_chk (void *dstpp, const void *srcpp, size_t len, size_t dstlen)
{
    if (dstlen < len)
        std::terminate();
    return memcpy (dstpp, srcpp, len);
}

void* (*__memcpy_chk)(void *dstpp, const void *srcpp, size_t len, size_t dstlen) = memcpy_chk;

std::string compress(jbyte* buf, std::string str, jint width,jint height,jint orient) {
    jint width_new,height_new;
    if(width*height>1500*1500) {
        auto kw = (float)width/1000;
        auto kh = (float)height/1000;
        auto k = kw > kh ? kw : kh;
        width_new = (jint)(width/k);
        height_new = (jint)(height/k);
        while (width_new % 4 != 0){
            width_new++;
        }
        while (height_new % 4 != 0){
            height_new++;
        }
        stbir_resize_uint8(reinterpret_cast<const unsigned char *>(buf), width , height , width * 4,
                           reinterpret_cast<unsigned char *>(buf), width_new, height_new, width_new * 4, 4);
    } else {
        width_new=width;
        height_new=height;
    }
    jbyte* new_buf;

    switch(orient) {
        case 3:{
            auto new_buf_ = new int32_t[width_new*height_new];
            new_buf = reinterpret_cast<jbyte *>(new_buf_);
            auto old_buf = reinterpret_cast<int32_t*>(buf);
            for (jint j = 0; j < height_new; j++) {
                for (jint i = 0; i < width_new; i++) {
                    new_buf_[j * width_new + i] = old_buf[(height_new - j - 1) * width_new + (width_new-i-1)];
                }
            }}
            break;
        case 6:{
            auto new_buf_ = new int32_t[width_new*height_new];
            new_buf = reinterpret_cast<jbyte *>(new_buf_);
            auto old_buf = reinterpret_cast<int32_t*>(buf);
            for (jint i = 0; i < width_new; i++) {
                for (jint j = 0; j < height_new; j++) {
                    new_buf_[i * height_new + j] = old_buf[(height_new - j - 1) * width_new + i];
                }
            }
            auto a = width_new;
            width_new = height_new;
            height_new = a;}
            break;
        default:
            new_buf = buf;
            break;
    }

    auto size = WebPEncodeRGBA(
            reinterpret_cast<const uint8_t *>(new_buf),
            width_new,
            height_new,
            width_new * 4,
            80,
            reinterpret_cast<uint8_t **>(&new_buf)
            );

    char hex[SHA1_HEX_SIZE];
    sha1 sha1_;
    sha1_.add(new_buf,size);
    sha1_.finalize();
    sha1_.print_hex(hex);
    str+="/";
    str+=hex;

    std::ofstream outfile(str, std::ofstream::binary);
    outfile.write(reinterpret_cast<const char *>(new_buf), size);
    outfile.close();

    if(new_buf != buf){
        delete[] new_buf;
    }

    return std::string(hex);
}

extern "C"
JNIEXPORT __unused  jstring JNICALL
Java_com_example_chat_Util_00024Companion_compress_1webp(JNIEnv *env, jobject thiz, jbyteArray data,
                                                         jint width, jint height, jstring jstr,
                                                         jint orient) {
    jboolean isCopy = 0;
    jbyte *buf = env->GetByteArrayElements(data,&isCopy);
    const char *cstr = env->GetStringUTFChars(jstr, NULL);
    std::string str = std::string(cstr);
    str = compress(buf,str,width,height,orient);
    env->ReleaseByteArrayElements(data, buf, JNI_ABORT);
    return env->NewStringUTF(str.c_str());
}
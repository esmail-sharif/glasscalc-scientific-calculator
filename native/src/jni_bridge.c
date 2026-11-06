#include <jni.h>

/* Implemented in native/src/asm_math.asm. */
extern double asm_add(double a, double b);
extern double asm_subtract(double a, double b);
extern double asm_multiply(double a, double b);
extern double asm_divide(double a, double b);
extern double asm_sin(double value);
extern double asm_cos(double value);
extern double asm_tan(double value);
extern double asm_sqrt(double value);
extern double asm_ln(double value);
extern double asm_log10(double value);
extern double asm_log2(double value);
extern double asm_exp(double value);
extern double asm_exp2(double value);
extern double asm_pow(double base, double exponent);
extern double asm_atan(double value);
extern double asm_atan2(double y, double x);
extern double asm_factorial(long long value);

#define JNI_METHOD(name, signature, call) \
    JNIEXPORT jdouble JNICALL \
    Java_com_esmail_calculator_engine_NativeMath_##name signature { \
        (void)env; \
        (void)type; \
        return (call); \
    }

JNI_METHOD(add, (JNIEnv *env, jclass type, jdouble a, jdouble b), asm_add(a, b))
JNI_METHOD(subtract, (JNIEnv *env, jclass type, jdouble a, jdouble b), asm_subtract(a, b))
JNI_METHOD(multiply, (JNIEnv *env, jclass type, jdouble a, jdouble b), asm_multiply(a, b))
JNI_METHOD(divide, (JNIEnv *env, jclass type, jdouble a, jdouble b), asm_divide(a, b))
JNI_METHOD(sin, (JNIEnv *env, jclass type, jdouble x), asm_sin(x))
JNI_METHOD(cos, (JNIEnv *env, jclass type, jdouble x), asm_cos(x))
JNI_METHOD(tan, (JNIEnv *env, jclass type, jdouble x), asm_tan(x))
JNI_METHOD(sqrt, (JNIEnv *env, jclass type, jdouble x), asm_sqrt(x))
JNI_METHOD(ln, (JNIEnv *env, jclass type, jdouble x), asm_ln(x))
JNI_METHOD(log10, (JNIEnv *env, jclass type, jdouble x), asm_log10(x))
JNI_METHOD(log2, (JNIEnv *env, jclass type, jdouble x), asm_log2(x))
JNI_METHOD(exp, (JNIEnv *env, jclass type, jdouble x), asm_exp(x))
JNI_METHOD(exp2, (JNIEnv *env, jclass type, jdouble x), asm_exp2(x))
JNI_METHOD(pow, (JNIEnv *env, jclass type, jdouble base, jdouble exponent), asm_pow(base, exponent))
JNI_METHOD(atan, (JNIEnv *env, jclass type, jdouble x), asm_atan(x))
JNI_METHOD(atan2, (JNIEnv *env, jclass type, jdouble y, jdouble x), asm_atan2(y, x))

JNIEXPORT jdouble JNICALL
Java_com_esmail_calculator_engine_NativeMath_factorial(JNIEnv *env, jclass type, jlong value) {
    (void)env;
    (void)type;
    return asm_factorial((long long)value);
}

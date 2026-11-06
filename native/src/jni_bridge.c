#include <jni.h>
#include <windows.h>
#include <math.h>
#include <stdint.h>
#include <string.h>

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

/* New Casio Assembly Functions */
extern double asm_vector3d_dot(const double *a, const double *b);
extern void asm_vector3d_cross(const double *a, const double *b, double *out);
extern double asm_vector3d_norm(const double *a);
extern double asm_matrix2x2_det(double a, double b, double c, double d);
extern double asm_matrix3x3_det(const double *m);
extern void asm_matrix3x3_multiply(const double *a, const double *b, double *out);
extern void asm_complex_multiply(const double *a, const double *b, double *out);
extern void asm_complex_divide(const double *a, const double *b, double *out);
extern int64_t asm_base_and(int64_t a, int64_t b);
extern int64_t asm_base_or(int64_t a, int64_t b);
extern int64_t asm_base_xor(int64_t a, int64_t b);
extern int64_t asm_base_xnor(int64_t a, int64_t b);
extern int64_t asm_base_not(int64_t a);
extern int64_t asm_base_neg(int64_t a);
extern double asm_quadratic_discriminant(double a, double b, double c);
extern double asm_stat_sum(const double *values, uint64_t n);
extern double asm_stat_sum_sq(const double *values, uint64_t n);
extern double asm_stat_mean(const double *values, uint64_t n);
extern double asm_stat_stddev(const double *values, uint64_t n, int isSample);
extern void asm_stat_2var_sums(const double *x, const double *y, uint64_t n, double *outSums5);

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

/*
 * x87 FPATAN has useful quadrant behaviour for ordinary finite non-zero
 * inputs, but C runtime atan2 is the reference for signed-zero and infinity
 * combinations. Keep those IEEE-754 edge cases out of the x87 path.
 */
JNIEXPORT jdouble JNICALL
Java_com_esmail_calculator_engine_NativeMath_atan2(
        JNIEnv *env, jclass type, jdouble y, jdouble x) {
    (void)env;
    (void)type;
    if (!isfinite(y) || !isfinite(x) || y == 0.0 || x == 0.0) {
        return atan2(y, x);
    }
    return asm_atan2(y, x);
}

JNIEXPORT jdouble JNICALL
Java_com_esmail_calculator_engine_NativeMath_factorial(
        JNIEnv *env, jclass type, jlong value) {
    (void)env;
    (void)type;
    return asm_factorial((long long)value);
}

JNIEXPORT jdouble JNICALL
Java_com_esmail_calculator_engine_NativeMath_vector3dDot(
        JNIEnv *env, jclass type, jdoubleArray aArray, jdoubleArray bArray) {
    (void)type;
    jdouble a[3], b[3];
    (*env)->GetDoubleArrayRegion(env, aArray, 0, 3, a);
    (*env)->GetDoubleArrayRegion(env, bArray, 0, 3, b);
    return asm_vector3d_dot(a, b);
}

JNIEXPORT void JNICALL
Java_com_esmail_calculator_engine_NativeMath_vector3dCross(
        JNIEnv *env, jclass type, jdoubleArray aArray, jdoubleArray bArray, jdoubleArray outArray) {
    (void)type;
    jdouble a[3], b[3], out[3];
    (*env)->GetDoubleArrayRegion(env, aArray, 0, 3, a);
    (*env)->GetDoubleArrayRegion(env, bArray, 0, 3, b);
    asm_vector3d_cross(a, b, out);
    (*env)->SetDoubleArrayRegion(env, outArray, 0, 3, out);
}

JNIEXPORT jdouble JNICALL
Java_com_esmail_calculator_engine_NativeMath_vector3dNorm(
        JNIEnv *env, jclass type, jdoubleArray aArray) {
    (void)type;
    jdouble a[3];
    (*env)->GetDoubleArrayRegion(env, aArray, 0, 3, a);
    return asm_vector3d_norm(a);
}

JNIEXPORT jdouble JNICALL
Java_com_esmail_calculator_engine_NativeMath_matrix2x2Det(
        JNIEnv *env, jclass type, jdouble a, jdouble b, jdouble c, jdouble d) {
    (void)env;
    (void)type;
    return asm_matrix2x2_det(a, b, c, d);
}

JNIEXPORT jdouble JNICALL
Java_com_esmail_calculator_engine_NativeMath_matrix3x3Det(
        JNIEnv *env, jclass type, jdoubleArray mArray) {
    (void)type;
    jdouble m[9];
    (*env)->GetDoubleArrayRegion(env, mArray, 0, 9, m);
    return asm_matrix3x3_det(m);
}

JNIEXPORT void JNICALL
Java_com_esmail_calculator_engine_NativeMath_matrix3x3Multiply(
        JNIEnv *env, jclass type, jdoubleArray aArray, jdoubleArray bArray, jdoubleArray outArray) {
    (void)type;
    jdouble a[9], b[9], out[9];
    (*env)->GetDoubleArrayRegion(env, aArray, 0, 9, a);
    (*env)->GetDoubleArrayRegion(env, bArray, 0, 9, b);
    asm_matrix3x3_multiply(a, b, out);
    (*env)->SetDoubleArrayRegion(env, outArray, 0, 9, out);
}

JNIEXPORT void JNICALL
Java_com_esmail_calculator_engine_NativeMath_complexMultiply(
        JNIEnv *env, jclass type, jdouble ar, jdouble ai, jdouble br, jdouble bi, jdoubleArray outArray) {
    (void)type;
    double a[2] = { ar, ai };
    double b[2] = { br, bi };
    double out[2];
    asm_complex_multiply(a, b, out);
    (*env)->SetDoubleArrayRegion(env, outArray, 0, 2, out);
}

JNIEXPORT void JNICALL
Java_com_esmail_calculator_engine_NativeMath_complexDivide(
        JNIEnv *env, jclass type, jdouble ar, jdouble ai, jdouble br, jdouble bi, jdoubleArray outArray) {
    (void)type;
    double a[2] = { ar, ai };
    double b[2] = { br, bi };
    double out[2];
    asm_complex_divide(a, b, out);
    (*env)->SetDoubleArrayRegion(env, outArray, 0, 2, out);
}

JNIEXPORT jlong JNICALL
Java_com_esmail_calculator_engine_NativeMath_baseAnd(JNIEnv *env, jclass type, jlong a, jlong b) {
    (void)env; (void)type;
    return (jlong)asm_base_and((int64_t)a, (int64_t)b);
}

JNIEXPORT jlong JNICALL
Java_com_esmail_calculator_engine_NativeMath_baseOr(JNIEnv *env, jclass type, jlong a, jlong b) {
    (void)env; (void)type;
    return (jlong)asm_base_or((int64_t)a, (int64_t)b);
}

JNIEXPORT jlong JNICALL
Java_com_esmail_calculator_engine_NativeMath_baseXor(JNIEnv *env, jclass type, jlong a, jlong b) {
    (void)env; (void)type;
    return (jlong)asm_base_xor((int64_t)a, (int64_t)b);
}

JNIEXPORT jlong JNICALL
Java_com_esmail_calculator_engine_NativeMath_baseXnor(JNIEnv *env, jclass type, jlong a, jlong b) {
    (void)env; (void)type;
    return (jlong)asm_base_xnor((int64_t)a, (int64_t)b);
}

JNIEXPORT jlong JNICALL
Java_com_esmail_calculator_engine_NativeMath_baseNot(JNIEnv *env, jclass type, jlong a) {
    (void)env; (void)type;
    return (jlong)asm_base_not((int64_t)a);
}

JNIEXPORT jlong JNICALL
Java_com_esmail_calculator_engine_NativeMath_baseNeg(JNIEnv *env, jclass type, jlong a) {
    (void)env; (void)type;
    return (jlong)asm_base_neg((int64_t)a);
}

JNIEXPORT jdouble JNICALL
Java_com_esmail_calculator_engine_NativeMath_quadraticDiscriminant(
        JNIEnv *env, jclass type, jdouble a, jdouble b, jdouble c) {
    (void)env; (void)type;
    return asm_quadratic_discriminant(a, b, c);
}

JNIEXPORT jdouble JNICALL
Java_com_esmail_calculator_engine_NativeMath_statSum(
        JNIEnv *env, jclass type, jdoubleArray valuesArray) {
    (void)type;
    jsize n = (*env)->GetArrayLength(env, valuesArray);
    if (n <= 0) return 0.0;
    jdouble *buf = (*env)->GetDoubleArrayElements(env, valuesArray, NULL);
    double res = asm_stat_sum(buf, (uint64_t)n);
    (*env)->ReleaseDoubleArrayElements(env, valuesArray, buf, JNI_ABORT);
    return res;
}

JNIEXPORT jdouble JNICALL
Java_com_esmail_calculator_engine_NativeMath_statSumSq(
        JNIEnv *env, jclass type, jdoubleArray valuesArray) {
    (void)type;
    jsize n = (*env)->GetArrayLength(env, valuesArray);
    if (n <= 0) return 0.0;
    jdouble *buf = (*env)->GetDoubleArrayElements(env, valuesArray, NULL);
    double res = asm_stat_sum_sq(buf, (uint64_t)n);
    (*env)->ReleaseDoubleArrayElements(env, valuesArray, buf, JNI_ABORT);
    return res;
}

JNIEXPORT jdouble JNICALL
Java_com_esmail_calculator_engine_NativeMath_statMean(
        JNIEnv *env, jclass type, jdoubleArray valuesArray) {
    (void)type;
    jsize n = (*env)->GetArrayLength(env, valuesArray);
    if (n <= 0) return 0.0;
    jdouble *buf = (*env)->GetDoubleArrayElements(env, valuesArray, NULL);
    double res = asm_stat_mean(buf, (uint64_t)n);
    (*env)->ReleaseDoubleArrayElements(env, valuesArray, buf, JNI_ABORT);
    return res;
}

JNIEXPORT jdouble JNICALL
Java_com_esmail_calculator_engine_NativeMath_statStdDev(
        JNIEnv *env, jclass type, jdoubleArray valuesArray, jboolean isSample) {
    (void)type;
    jsize n = (*env)->GetArrayLength(env, valuesArray);
    if (n <= 0) return 0.0;
    jdouble *buf = (*env)->GetDoubleArrayElements(env, valuesArray, NULL);
    double res = asm_stat_stddev(buf, (uint64_t)n, isSample ? 1 : 0);
    (*env)->ReleaseDoubleArrayElements(env, valuesArray, buf, JNI_ABORT);
    return res;
}

JNIEXPORT jdoubleArray JNICALL
Java_com_esmail_calculator_engine_NativeMath_statLinearReg(
        JNIEnv *env, jclass type, jdoubleArray xArray, jdoubleArray yArray) {
    (void)type;
    jsize nx = (*env)->GetArrayLength(env, xArray);
    jsize ny = (*env)->GetArrayLength(env, yArray);
    uint64_t n = (uint64_t)(nx < ny ? nx : ny);
    if (n < 2) return NULL;

    jdouble *xBuf = (*env)->GetDoubleArrayElements(env, xArray, NULL);
    jdouble *yBuf = (*env)->GetDoubleArrayElements(env, yArray, NULL);

    double sums[5];
    asm_stat_2var_sums(xBuf, yBuf, n, sums);

    (*env)->ReleaseDoubleArrayElements(env, xArray, xBuf, JNI_ABORT);
    (*env)->ReleaseDoubleArrayElements(env, yArray, yBuf, JNI_ABORT);

    double Sx = sums[0], Sy = sums[1], Sxx = sums[2], Syy = sums[3], Sxy = sums[4];
    double dn = (double)n;
    double deltaX = dn * Sxx - Sx * Sx;
    double deltaY = dn * Syy - Sy * Sy;
    double cov = dn * Sxy - Sx * Sy;

    double B = (fabs(deltaX) > 1e-15) ? (cov / deltaX) : 0.0;
    double A = (Sy - B * Sx) / dn;
    double denomR = sqrt(fabs(deltaX * deltaY));
    double r = (denomR > 1e-15) ? (cov / denomR) : 0.0;

    jdoubleArray res = (*env)->NewDoubleArray(env, 3);
    jdouble out[3] = { A, B, r };
    (*env)->SetDoubleArrayRegion(env, res, 0, 3, out);
    return res;
}

/*
 * Validate the linked PE runtime-function table and execute the platform
 * unwinder against a synthetic stack for every stack-allocating NASM routine.
 * This catches bad .pdata/.xdata RVAs, prologue lengths, code counts, opcodes,
 * allocation sizes and return-address restoration without inducing an exception.
 */
typedef struct GLASS_UNWIND_CODE {
    BYTE CodeOffset;
    BYTE OpInfoAndOperation;
} GLASS_UNWIND_CODE;

typedef struct GLASS_UNWIND_INFO {
    BYTE VersionAndFlags;
    BYTE SizeOfProlog;
    BYTE CountOfCodes;
    BYTE FrameRegisterAndOffset;
    GLASS_UNWIND_CODE UnwindCode[1];
} GLASS_UNWIND_INFO;

static int verify_one_unwind(void *function, DWORD allocation_bytes) {
    DWORD64 image_base = 0;
    DWORD64 control_pc;
    DWORD64 synthetic_return = 0x0000000123456789ULL;
    DWORD64 synthetic_stack[16] = {0};
    DWORD64 establisher_frame = 0;
    PVOID handler_data = NULL;
    CONTEXT context;
    PRUNTIME_FUNCTION runtime_function;
    GLASS_UNWIND_INFO *unwind_info;
    unsigned int expected_op_info;

    runtime_function = RtlLookupFunctionEntry(
            (DWORD64)(ULONG_PTR)function, &image_base, NULL);
    if (runtime_function == NULL || image_base == 0) return 0;
    if (runtime_function->BeginAddress >= runtime_function->EndAddress) return 0;
    if (runtime_function->UnwindData == 0) return 0;

    /* UNWIND_INFO is a PE wire format: byte fields followed by 2-byte codes. */
    unwind_info = (GLASS_UNWIND_INFO *)(ULONG_PTR)(image_base + runtime_function->UnwindData);
    if ((unwind_info->VersionAndFlags & 0x07U) != 1U) return 0;
    if ((unwind_info->VersionAndFlags >> 3) != 0U) return 0;
    if (unwind_info->SizeOfProlog != 4 || unwind_info->CountOfCodes != 1) return 0;

    expected_op_info = (allocation_bytes / 8U) - 1U;
    if (unwind_info->UnwindCode[0].CodeOffset != 4) return 0;
    if ((unwind_info->UnwindCode[0].OpInfoAndOperation & 0x0FU) != 2U) return 0; /* UWOP_ALLOC_SMALL */
    if ((unwind_info->UnwindCode[0].OpInfoAndOperation >> 4) != expected_op_info) return 0;

    synthetic_stack[allocation_bytes / sizeof(DWORD64)] = synthetic_return;
    memset(&context, 0, sizeof(context));
    context.ContextFlags = CONTEXT_CONTROL;
    context.Rsp = (DWORD64)(ULONG_PTR)&synthetic_stack[0];
    control_pc = image_base + runtime_function->BeginAddress + unwind_info->SizeOfProlog;
    context.Rip = control_pc;

    (void)RtlVirtualUnwind(
            0,
            image_base,
            control_pc,
            runtime_function,
            &context,
            &handler_data,
            &establisher_frame,
            NULL);

    return context.Rip == synthetic_return
            && context.Rsp == (DWORD64)(ULONG_PTR)&synthetic_stack[allocation_bytes / 8U + 1U];
}

JNIEXPORT jint JNICALL
Java_com_esmail_calculator_engine_NativeMath_verifyUnwindMetadata(
        JNIEnv *env, jclass type) {
    int valid = 0;
    (void)env;
    (void)type;

    valid += verify_one_unwind((void *)(ULONG_PTR)&asm_sin, 8);
    valid += verify_one_unwind((void *)(ULONG_PTR)&asm_cos, 8);
    valid += verify_one_unwind((void *)(ULONG_PTR)&asm_tan, 8);
    valid += verify_one_unwind((void *)(ULONG_PTR)&asm_sqrt, 8);
    valid += verify_one_unwind((void *)(ULONG_PTR)&asm_ln, 8);
    valid += verify_one_unwind((void *)(ULONG_PTR)&asm_log10, 8);
    valid += verify_one_unwind((void *)(ULONG_PTR)&asm_log2, 8);
    valid += verify_one_unwind((void *)(ULONG_PTR)&asm_exp2, 8);
    valid += verify_one_unwind((void *)(ULONG_PTR)&asm_atan, 8);
    valid += verify_one_unwind((void *)(ULONG_PTR)&asm_atan2, 24);
    valid += verify_one_unwind((void *)(ULONG_PTR)&asm_pow, 24);
    return (jint)valid;
}

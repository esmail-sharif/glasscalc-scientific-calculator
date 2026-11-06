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

/*
 * Validate the linked PE runtime-function table and execute the platform
 * unwinder against a synthetic stack for every stack-allocating NASM routine.
 * This catches bad .pdata/.xdata RVAs, prologue lengths, code counts, opcodes,
 * allocation sizes and return-address restoration without inducing an exception.
 */
static int verify_one_unwind(void *function, DWORD allocation_bytes) {
    DWORD64 image_base = 0;
    DWORD64 control_pc;
    DWORD64 synthetic_return = 0x0000000123456789ULL;
    DWORD64 synthetic_stack[16] = {0};
    DWORD64 establisher_frame = 0;
    PVOID handler_data = NULL;
    CONTEXT context;
    PRUNTIME_FUNCTION runtime_function;
    UNWIND_INFO *unwind_info;
    unsigned int code_index;
    unsigned int expected_op_info;

    runtime_function = RtlLookupFunctionEntry(
            (DWORD64)(ULONG_PTR)function, &image_base, NULL);
    if (runtime_function == NULL || image_base == 0) return 0;
    if (runtime_function->BeginAddress >= runtime_function->EndAddress) return 0;
    if (runtime_function->UnwindData == 0) return 0;

    unwind_info = (UNWIND_INFO *)(ULONG_PTR)(image_base + runtime_function->UnwindData);
    if (unwind_info->Version != 1 || unwind_info->Flags != 0) return 0;
    if (unwind_info->SizeOfProlog != 4 || unwind_info->CountOfCodes != 1) return 0;

    code_index = 0;
    expected_op_info = (allocation_bytes / 8U) - 1U;
    if (unwind_info->UnwindCode[code_index].CodeOffset != 4) return 0;
    if (unwind_info->UnwindCode[code_index].UnwindOp != UWOP_ALLOC_SMALL) return 0;
    if (unwind_info->UnwindCode[code_index].OpInfo != expected_op_info) return 0;

    synthetic_stack[allocation_bytes / sizeof(DWORD64)] = synthetic_return;
    memset(&context, 0, sizeof(context));
    context.ContextFlags = CONTEXT_CONTROL;
    context.Rsp = (DWORD64)(ULONG_PTR)&synthetic_stack[0];
    control_pc = image_base + runtime_function->BeginAddress + unwind_info->SizeOfProlog;
    context.Rip = control_pc;

    (void)RtlVirtualUnwind(
            UNW_FLAG_NHANDLER,
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

; GlassCalc x86-64 math primitives for the Windows x64 ABI.
; Floating-point arguments/results use XMM0/XMM1. Integer factorial uses RCX.
; NASM win64 object format. Stack-allocating routines carry PE unwind metadata.

bits 64
default rel

section .text
global asm_add
global asm_subtract
global asm_multiply
global asm_divide
global asm_sin
global asm_cos
global asm_tan
global asm_sqrt
global asm_ln
global asm_log10
global asm_log2
global asm_exp
global asm_exp2
global asm_pow
global asm_atan
global asm_atan2
global asm_factorial
global asm_vector3d_dot
global asm_vector3d_cross
global asm_vector3d_norm
global asm_matrix2x2_det
global asm_matrix3x3_det
global asm_matrix3x3_multiply
global asm_complex_multiply
global asm_complex_divide
global asm_base_and
global asm_base_or
global asm_base_xor
global asm_base_xnor
global asm_base_not
global asm_base_neg
global asm_quadratic_discriminant

%macro EMIT_UNWIND 5
section .xdata rdata align=8
%3:
    db 1, %5, 1, 0
    db %5, (((%4 / 8) - 1) << 4) | 2
    dw 0
section .pdata rdata align=4
    dd %1 wrt ..imagebase
    dd %2 wrt ..imagebase
    dd %3 wrt ..imagebase
section .text
%endmacro

section .text
asm_add:
    addsd xmm0, xmm1
    ret

asm_subtract:
    subsd xmm0, xmm1
    ret

asm_multiply:
    mulsd xmm0, xmm1
    ret

asm_divide:
    divsd xmm0, xmm1
    ret

asm_sin:
    sub rsp, 8
    movsd [rsp], xmm0
    fld qword [rsp]
    fsin
    fnstsw ax
    test ah, 4
    jnz .sin_range
    fstp qword [rsp]
    movsd xmm0, [rsp]
    add rsp, 8
    ret
.sin_range:
    fstp st0
    mov rax, 0x7ff8000000000000
    movq xmm0, rax
    add rsp, 8
    ret
asm_sin_end:
EMIT_UNWIND asm_sin, asm_sin_end, asm_sin_unwind, 8, 4

asm_cos:
    sub rsp, 8
    movsd [rsp], xmm0
    fld qword [rsp]
    fcos
    fnstsw ax
    test ah, 4
    jnz .cos_range
    fstp qword [rsp]
    movsd xmm0, [rsp]
    add rsp, 8
    ret
.cos_range:
    fstp st0
    mov rax, 0x7ff8000000000000
    movq xmm0, rax
    add rsp, 8
    ret
asm_cos_end:
EMIT_UNWIND asm_cos, asm_cos_end, asm_cos_unwind, 8, 4

asm_tan:
    sub rsp, 8
    movsd [rsp], xmm0
    fld qword [rsp]
    fptan
    fnstsw ax
    test ah, 4
    jnz .tan_range
    fstp st0
    fstp qword [rsp]
    movsd xmm0, [rsp]
    add rsp, 8
    ret
.tan_range:
    fstp st0
    mov rax, 0x7ff8000000000000
    movq xmm0, rax
    add rsp, 8
    ret
asm_tan_end:
EMIT_UNWIND asm_tan, asm_tan_end, asm_tan_unwind, 8, 4

asm_sqrt:
    sub rsp, 8
    movsd [rsp], xmm0
    fld qword [rsp]
    fsqrt
    fstp qword [rsp]
    movsd xmm0, [rsp]
    add rsp, 8
    ret
asm_sqrt_end:
EMIT_UNWIND asm_sqrt, asm_sqrt_end, asm_sqrt_unwind, 8, 4

asm_ln:
    sub rsp, 8
    movsd [rsp], xmm0
    fldln2
    fld qword [rsp]
    fyl2x
    fstp qword [rsp]
    movsd xmm0, [rsp]
    add rsp, 8
    ret
asm_ln_end:
EMIT_UNWIND asm_ln, asm_ln_end, asm_ln_unwind, 8, 4

asm_log10:
    sub rsp, 8
    movsd [rsp], xmm0
    fldlg2
    fld qword [rsp]
    fyl2x
    fstp qword [rsp]
    movsd xmm0, [rsp]
    add rsp, 8
    ret
asm_log10_end:
EMIT_UNWIND asm_log10, asm_log10_end, asm_log10_unwind, 8, 4

; log2(x) = 1 * log2(x), with the x87 argument in ST0.
asm_log2:
    sub rsp, 8
    movsd [rsp], xmm0
    fld1
    fld qword [rsp]
    fyl2x
    fstp qword [rsp]
    movsd xmm0, [rsp]
    add rsp, 8
    ret
asm_log2_end:
EMIT_UNWIND asm_log2, asm_log2_end, asm_log2_unwind, 8, 4

; exp2(x): handle IEEE special values and binary64 range before x87 reduction.
; The finite core is decomposed into an integer scale and a fractional exponent.
; FRNDINT may follow any x87 rounding mode; the resulting fraction remains in
; [-1,1], the documented F2XM1 input interval.
asm_exp2:
    sub rsp, 8
    movsd [rsp], xmm0

    mov rax, [rsp]
    mov rdx, rax
    btr rdx, 63
    mov rcx, 0x7ff0000000000000
    cmp rdx, rcx
    ja .exp2_nan
    je .exp2_infinity
    test rdx, rdx
    jz .exp2_one

    mov rax, 1024
    cvtsi2sd xmm2, rax
    ucomisd xmm0, xmm2
    jae .exp2_overflow

    mov rax, -1075
    cvtsi2sd xmm2, rax
    ucomisd xmm0, xmm2
    jbe .exp2_underflow

    fld qword [rsp]
    fld st0
    frndint
    fxch st1
    fsub st0, st1
    f2xm1
    fld1
    faddp st1, st0
    fscale
    fstp st1
    fstp qword [rsp]
    movsd xmm0, [rsp]
    add rsp, 8
    ret

.exp2_one:
    mov rax, 0x3ff0000000000000
    movq xmm0, rax
    add rsp, 8
    ret
.exp2_infinity:
    bt qword [rsp], 63
    jc .exp2_underflow
.exp2_overflow:
    mov rax, 0x7ff0000000000000
    movq xmm0, rax
    add rsp, 8
    ret
.exp2_underflow:
    xorpd xmm0, xmm0
    add rsp, 8
    ret
.exp2_nan:
    mov rax, 0x7ff8000000000000
    movq xmm0, rax
    add rsp, 8
    ret
asm_exp2_end:
EMIT_UNWIND asm_exp2, asm_exp2_end, asm_exp2_unwind, 8, 4

; exp(x) = 2^(x * log2(e)).
asm_exp:
    mulsd xmm0, [rel log2e]
    jmp asm_exp2
asm_exp_end:

asm_atan:
    sub rsp, 8
    movsd [rsp], xmm0
    fld qword [rsp]
    fld1
    fpatan
    fstp qword [rsp]
    movsd xmm0, [rsp]
    add rsp, 8
    ret
asm_atan_end:
EMIT_UNWIND asm_atan, asm_atan_end, asm_atan_unwind, 8, 4

; atan2(y,x): FPATAN computes atan(ST1/ST0), retaining quadrant information.
asm_atan2:
    sub rsp, 24
    movsd [rsp], xmm0
    movsd [rsp + 8], xmm1
    fld qword [rsp]
    fld qword [rsp + 8]
    fpatan
    fstp qword [rsp]
    movsd xmm0, [rsp]
    add rsp, 24
    ret
asm_atan2_end:
EMIT_UNWIND asm_atan2, asm_atan2_end, asm_atan2_unwind, 24, 4

; x87 pow core. The Java facade handles IEEE special cases and routes negative
; bases through java.lang.Math; this native entry also supports ordinary integer
; powers of negative bases. Values >= 2^63 are representable doubles whose
; integral values are necessarily even, so no int64 conversion is attempted.
asm_pow:
    sub rsp, 24
    movsd [rsp], xmm0
    movsd [rsp + 8], xmm1

    ; Work with absolute bit patterns while retaining the original signs.
    mov rax, [rsp]
    mov rdx, rax
    mov r11, 0x7fffffffffffffff
    and rdx, r11                  ; |base| bits
    mov r10, [rsp + 8]
    mov r8, r10
    and r8, r11                   ; |exponent| bits

    ; IEEE pow: x^+/-0 is +1, including NaN bases.
    test r8, r8
    jz .pow_one

    ; IEEE pow: (+1)^y is +1, including NaN exponents.
    mov rax, 0x3ff0000000000000
    cmp qword [rsp], rax
    je .pow_one

    ; NaN in either operand (after the zero-exponent and +1 identities).
    mov rax, 0x7ff0000000000000
    cmp r8, rax
    ja .pow_nan
    cmp rdx, rax
    ja .pow_nan

    ; Infinite exponent: compare |base| with one.
    cmp r8, rax
    je .pow_infinite_exponent

    ; Zero and infinite bases are handled without entering FYL2X.
    test rdx, rdx
    jz .pow_special_base
    cmp rdx, rax
    je .pow_special_base

    ; For a finite negative base, the exponent must be integral.
    xor r9d, r9d                  ; r9d = 1 iff exponent is an odd integer
    bt qword [rsp], 63
    jnc .pow_calculate
    mov rax, 0x4340000000000000  ; 2^53; all larger finite doubles are even integers
    cmp r8, rax
    jae .pow_calculate
    movsd xmm1, [rsp + 8]
    cvttsd2si rax, xmm1
    cvtsi2sd xmm2, rax
    ucomisd xmm2, xmm1
    jp .pow_nan
    jne .pow_nan
    test rax, 1
    jz .pow_calculate
    mov r9d, 1

.pow_calculate:
    ; The x87 core works on |base|. A negative sign is applied after magnitude.
    btr qword [rsp], 63
    fld qword [rsp + 8]
    fld qword [rsp]
    fyl2x
    fst qword [rsp + 16]          ; retain extended ST0, store exponent for range test
    movsd xmm1, [rsp + 16]

    mov rax, 1024
    cvtsi2sd xmm2, rax
    ucomisd xmm1, xmm2
    jae .pow_overflow
    mov rax, -1075
    cvtsi2sd xmm2, rax
    ucomisd xmm1, xmm2
    jbe .pow_underflow

    fld st0
    frndint
    fxch st1
    fsub st0, st1
    f2xm1
    fld1
    faddp st1, st0
    fscale
    fstp st1
    fstp qword [rsp]
    movsd xmm0, [rsp]
    test r9d, r9d
    jz .pow_return
    movq rax, xmm0
    btc rax, 63
    movq xmm0, rax
    jmp .pow_return

.pow_overflow:
    fstp st0
    mov rax, 0x7ff0000000000000
    jmp .pow_apply_sign
.pow_underflow:
    fstp st0
    xor eax, eax
.pow_apply_sign:
    test r9d, r9d
    jz .pow_emit_bits
    btc rax, 63
.pow_emit_bits:
    movq xmm0, rax
    jmp .pow_return

.pow_special_base:
    ; Determine odd-integral exponent parity for signed zero/infinity.
    xor r9d, r9d
    mov rax, 0x4340000000000000
    cmp r8, rax
    jae .pow_special_magnitude
    movsd xmm1, [rsp + 8]
    cvttsd2si rax, xmm1
    cvtsi2sd xmm2, rax
    ucomisd xmm2, xmm1
    jp .pow_special_magnitude
    jne .pow_special_magnitude
    test rax, 1
    jz .pow_special_magnitude
    mov r9d, 1

.pow_special_magnitude:
    cmp rdx, 0
    je .pow_zero_base_magnitude
    ; Infinite base: positive exponent -> infinity; negative -> signed zero.
    bt qword [rsp + 8], 63
    jc .pow_positive_zero
    mov rax, 0x7ff0000000000000
    jmp .pow_special_sign

.pow_zero_base_magnitude:
    ; Zero base: positive exponent -> zero; negative -> infinity.
    bt qword [rsp + 8], 63
    jc .pow_positive_infinity
.pow_positive_zero:
    xor eax, eax
    jmp .pow_special_sign
.pow_positive_infinity:
    mov rax, 0x7ff0000000000000

.pow_special_sign:
    bt qword [rsp], 63
    jnc .pow_emit_bits
    test r9d, r9d
    jz .pow_emit_bits
    btc rax, 63
    jmp .pow_emit_bits

.pow_infinite_exponent:
    mov rax, 0x3ff0000000000000
    cmp rdx, rax
    je .pow_one
    ja .pow_abs_gt_one
    ; |base| < 1: +infinity -> 0; -infinity -> +infinity.
    bt qword [rsp + 8], 63
    jc .pow_exp_inf_result
    jmp .pow_exp_zero_result
.pow_abs_gt_one:
    ; |base| > 1: +infinity -> +infinity; -infinity -> 0.
    bt qword [rsp + 8], 63
    jc .pow_exp_zero_result
.pow_exp_inf_result:
    mov rax, 0x7ff0000000000000
    jmp .pow_emit_bits
.pow_exp_zero_result:
    xor eax, eax
    jmp .pow_emit_bits

.pow_one:
    mov rax, 0x3ff0000000000000
    movq xmm0, rax
    jmp .pow_return
.pow_nan:
    mov rax, 0x7ff8000000000000
    movq xmm0, rax
.pow_return:
    add rsp, 24
    ret
asm_pow_end:
EMIT_UNWIND asm_pow, asm_pow_end, asm_pow_unwind, 24, 4

; Signed jlong input in RCX. The public facade rejects invalid arguments;
; the native boundary is defensive as well: negative -> NaN, n > 170 -> +Inf.
asm_factorial:
    test rcx, rcx
    js .factorial_nan
    cmp rcx, 170
    ja .factorial_inf
    mov rax, rcx
    mov r8, 1
    cvtsi2sd xmm0, r8
    cmp rax, 1
    jbe .factorial_done
.factorial_loop:
    cvtsi2sd xmm1, rax
    mulsd xmm0, xmm1
    dec rax
    cmp rax, 1
    ja .factorial_loop
.factorial_done:
    ret
.factorial_inf:
    mov rax, 0x7ff0000000000000
    movq xmm0, rax
    ret
.factorial_nan:
    mov rax, 0x7ff8000000000000
    movq xmm0, rax
    ret

; =============================================================================
; New Casio x86-64 NASM Primitives: Vectors, Matrices, Complex, Base-N, EQN
; =============================================================================

; asm_vector3d_dot(RCX = const double *a, RDX = const double *b) -> XMM0
asm_vector3d_dot:
    movsd xmm0, [rcx]
    mulsd xmm0, [rdx]
    movsd xmm1, [rcx + 8]
    mulsd xmm1, [rdx + 8]
    addsd xmm0, xmm1
    movsd xmm2, [rcx + 16]
    mulsd xmm2, [rdx + 16]
    addsd xmm0, xmm2
    ret

; asm_vector3d_cross(RCX = const double *a, RDX = const double *b, R8 = double *out)
asm_vector3d_cross:
    ; out[0] = a[1]*b[2] - a[2]*b[1]
    movsd xmm0, [rcx + 8]
    mulsd xmm0, [rdx + 16]
    movsd xmm1, [rcx + 16]
    mulsd xmm1, [rdx + 8]
    subsd xmm0, xmm1
    movsd [r8], xmm0

    ; out[1] = a[2]*b[0] - a[0]*b[2]
    movsd xmm0, [rcx + 16]
    mulsd xmm0, [rdx]
    movsd xmm1, [rcx]
    mulsd xmm1, [rdx + 16]
    subsd xmm0, xmm1
    movsd [r8 + 8], xmm0

    ; out[2] = a[0]*b[1] - a[1]*b[0]
    movsd xmm0, [rcx]
    mulsd xmm0, [rdx + 8]
    movsd xmm1, [rcx + 8]
    mulsd xmm1, [rdx]
    subsd xmm0, xmm1
    movsd [r8 + 16], xmm0
    ret

; asm_vector3d_norm(RCX = const double *a) -> XMM0
asm_vector3d_norm:
    movsd xmm0, [rcx]
    mulsd xmm0, xmm0
    movsd xmm1, [rcx + 8]
    mulsd xmm1, xmm1
    addsd xmm0, xmm1
    movsd xmm2, [rcx + 16]
    mulsd xmm2, xmm2
    addsd xmm0, xmm2
    sqrtsd xmm0, xmm0
    ret

; asm_matrix2x2_det(XMM0=a, XMM1=b, XMM2=c, XMM3=d) -> XMM0 (ad - bc)
asm_matrix2x2_det:
    mulsd xmm0, xmm3
    mulsd xmm1, xmm2
    subsd xmm0, xmm1
    ret

; asm_matrix3x3_det(RCX = const double *m) -> XMM0
asm_matrix3x3_det:
    ; Term 0: m[4]*m[8] - m[5]*m[7]
    movsd xmm1, [rcx + 32]
    mulsd xmm1, [rcx + 64]
    movsd xmm2, [rcx + 40]
    mulsd xmm2, [rcx + 56]
    subsd xmm1, xmm2
    mulsd xmm1, [rcx]

    ; Term 1: m[3]*m[8] - m[5]*m[6]
    movsd xmm3, [rcx + 24]
    mulsd xmm3, [rcx + 64]
    movsd xmm4, [rcx + 40]
    mulsd xmm4, [rcx + 48]
    subsd xmm3, xmm4
    mulsd xmm3, [rcx + 8]
    subsd xmm1, xmm3

    ; Term 2: m[3]*m[7] - m[4]*m[6]
    movsd xmm5, [rcx + 24]
    mulsd xmm5, [rcx + 56]
    movsd xmm6, [rcx + 32]
    mulsd xmm6, [rcx + 48]
    subsd xmm5, xmm6
    mulsd xmm5, [rcx + 16]
    addsd xmm1, xmm5

    movsd xmm0, xmm1
    ret

; asm_matrix3x3_multiply(RCX = const double *a, RDX = const double *b, R8 = double *out)
asm_matrix3x3_multiply:
    push rbx
    push rsi
    push rdi

    xor rsi, rsi ; row i = 0, 1, 2
.row_loop:
    xor rdi, rdi ; col j = 0, 1, 2
.col_loop:
    ; dot product of row i of a and col j of b
    ; out_idx = (i * 3 + j) * 8
    mov rax, rsi
    imul rax, 3
    add rax, rdi
    shl rax, 3

    ; a[i*3 + 0] * b[0*3 + j]
    mov rbx, rsi
    imul rbx, 3
    shl rbx, 3
    movsd xmm0, [rcx + rbx]
    mov r10, rdi
    shl r10, 3
    mulsd xmm0, [rdx + r10]

    ; + a[i*3 + 1] * b[1*3 + j]
    mov rbx, rsi
    imul rbx, 3
    inc rbx
    shl rbx, 3
    movsd xmm1, [rcx + rbx]
    mov r10, rdi
    add r10, 3
    shl r10, 3
    mulsd xmm1, [rdx + r10]
    addsd xmm0, xmm1

    ; + a[i*3 + 2] * b[2*3 + j]
    mov rbx, rsi
    imul rbx, 3
    add rbx, 2
    shl rbx, 3
    movsd xmm2, [rcx + rbx]
    mov r10, rdi
    add r10, 6
    shl r10, 3
    mulsd xmm2, [rdx + r10]
    addsd xmm0, xmm2

    movsd [r8 + rax], xmm0

    inc rdi
    cmp rdi, 3
    jl .col_loop

    inc rsi
    cmp rsi, 3
    jl .row_loop

    pop rdi
    pop rsi
    pop rbx
    ret

; asm_complex_multiply(RCX = const double *a, RDX = const double *b, R8 = double *out)
asm_complex_multiply:
    ; out_real = a[0]*b[0] - a[1]*b[1]
    movsd xmm0, [rcx]
    mulsd xmm0, [rdx]
    movsd xmm1, [rcx + 8]
    mulsd xmm1, [rdx + 8]
    subsd xmm0, xmm1
    movsd [r8], xmm0

    ; out_imag = a[0]*b[1] + a[1]*b[0]
    movsd xmm2, [rcx]
    mulsd xmm2, [rdx + 8]
    movsd xmm3, [rcx + 8]
    mulsd xmm3, [rdx]
    addsd xmm2, xmm3
    movsd [r8 + 8], xmm2
    ret

; asm_complex_divide(RCX = const double *a, RDX = const double *b, R8 = double *out)
asm_complex_divide:
    ; denom = b[0]*b[0] + b[1]*b[1]
    movsd xmm4, [rdx]
    mulsd xmm4, xmm4
    movsd xmm5, [rdx + 8]
    mulsd xmm5, xmm5
    addsd xmm4, xmm5

    ; out_real = (a[0]*b[0] + a[1]*b[1]) / denom
    movsd xmm0, [rcx]
    mulsd xmm0, [rdx]
    movsd xmm1, [rcx + 8]
    mulsd xmm1, [rdx + 8]
    addsd xmm0, xmm1
    divsd xmm0, xmm4
    movsd [r8], xmm0

    ; out_imag = (a[1]*b[0] - a[0]*b[1]) / denom
    movsd xmm2, [rcx + 8]
    mulsd xmm2, [rdx]
    movsd xmm3, [rcx]
    mulsd xmm3, [rdx + 8]
    subsd xmm2, xmm3
    divsd xmm2, xmm4
    movsd [r8 + 8], xmm2
    ret

; Base-N bitwise logic: RCX = a, RDX = b -> RAX
asm_base_and:
    mov rax, rcx
    and rax, rdx
    ret

asm_base_or:
    mov rax, rcx
    or rax, rdx
    ret

asm_base_xor:
    mov rax, rcx
    xor rax, rdx
    ret

asm_base_xnor:
    mov rax, rcx
    xor rax, rdx
    not rax
    ret

asm_base_not:
    mov rax, rcx
    not rax
    ret

asm_base_neg:
    mov rax, rcx
    neg rax
    ret

; asm_quadratic_discriminant(XMM0=a, XMM1=b, XMM2=c) -> XMM0 = b^2 - 4ac
asm_quadratic_discriminant:
    mulsd xmm1, xmm1
    mulsd xmm0, xmm2
    addsd xmm0, xmm0
    addsd xmm0, xmm0
    subsd xmm1, xmm0
    movsd xmm0, xmm1
    ret

section .rdata align=8
log2e: dq 1.4426950408889634074

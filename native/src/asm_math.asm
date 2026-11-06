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

; exp2(x): split x into integer and fractional parts, then use F2XM1/FSCALE.
asm_exp2:
    sub rsp, 8
    movsd [rsp], xmm0
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

    mov rax, [rsp]
    btr rax, 63
    test rax, rax
    jnz .pow_nonzero_base

    movsd xmm1, [rsp + 8]
    xorpd xmm2, xmm2
    ucomisd xmm1, xmm2
    jp .pow_nan
    je .pow_one
    ja .pow_zero
    mov rax, 0x7ff0000000000000
    movq xmm0, rax
    jmp .pow_return

.pow_nonzero_base:
    xor r9d, r9d
    bt qword [rsp], 63
    jnc .pow_calculate

    movsd xmm1, [rsp + 8]
    movq rax, xmm1
    btr rax, 63
    movq xmm2, rax
    mov rax, 0x43e0000000000000
    movq xmm3, rax
    ucomisd xmm2, xmm3
    jae .pow_calculate              ; large representable integer => even

    cvttsd2si rax, xmm1
    cvtsi2sd xmm2, rax
    ucomisd xmm2, xmm1
    jp .pow_nan
    jne .pow_nan
    test rax, 1
    jz .pow_calculate
    mov r9d, 1

.pow_calculate:
    btr qword [rsp], 63
    fld qword [rsp + 8]
    fld qword [rsp]
    fyl2x
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

.pow_one:
    mov rax, 0x3ff0000000000000
    movq xmm0, rax
    jmp .pow_return
.pow_zero:
    xorpd xmm0, xmm0
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

section .rdata align=8
log2e: dq 1.4426950408889634074

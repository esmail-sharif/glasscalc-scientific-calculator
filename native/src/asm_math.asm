; GlassCalc x86-64 math primitives for the Windows x64 ABI.
; Floating-point arguments and return values are passed in XMM0/XMM1.
; NASM syntax, win64 object format.

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
global asm_pow
global asm_atan
global asm_factorial

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

%macro X87_UNARY 2
%1:
    sub rsp, 8
    movsd [rsp], xmm0
    fld qword [rsp]
    %2
    fstp qword [rsp]
    movsd xmm0, [rsp]
    add rsp, 8
    ret
%endmacro

X87_UNARY asm_sin, fsin
X87_UNARY asm_cos, fcos

asm_tan:
    sub rsp, 8
    movsd [rsp], xmm0
    fld qword [rsp]
    fptan
    fstp st0
    fstp qword [rsp]
    movsd xmm0, [rsp]
    add rsp, 8
    ret

X87_UNARY asm_sqrt, fsqrt

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

asm_pow:
    sub rsp, 16
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
    add rsp, 16
    ret

asm_factorial:
    mov rax, rcx
    mov r8, 1
    cvtsi2sd xmm0, r8
    test rax, rax
    jz .factorial_done
.factorial_loop:
    cvtsi2sd xmm1, rax
    mulsd xmm0, xmm1
    dec rax
    jnz .factorial_loop
.factorial_done:
    ret

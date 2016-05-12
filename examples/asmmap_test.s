
ADD $iptr, $offs, #image
ADD $optr, $offs, #out
ADD $optr, $optr, #1024
ADD $optr, $optr, #4
MOV $span, $span, LSL #8

Pixel:
MOV $cptr, #0
MOV $d, #0
MOV $dy, #3
MOV $res, #0

LoopDy:
MOV $dx, #3

LoopDx:
LDR $a, [$cptr], #4
LDR $b, [$iptr, $d, LSL #2]
MLA $res, $a, $b, $res

SUBS $dx, $dx, #1
ADDNE $d, $d, #1
BNE LoopDx

ADD $d, $d, #256
SUB $d, $d, #2

SUBS $dy, $dy, #1
BNE LoopDy

STR $res, [$optr], #4
ADD $iptr, $iptr, #4
SUBS $span, $span, #1
BNE Pixel

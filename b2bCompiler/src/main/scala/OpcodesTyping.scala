package b2bCompiler

object OpcodesTyping {

  type NOP = 0
  type ACONST_NULL = 1
  type ICONST_M1 = 2
  type ICONST_0 = 3
  type ICONST_1 = 4
  type ICONST_2 = 5
  type ICONST_3 = 6
  type ICONST_4 = 7
  type ICONST_5 = 8
  type LCONST_0 = 9
  type LCONST_1 = 10
  type FCONST_0 = 11
  type FCONST_1 = 12
  type FCONST_2 = 13
  type DCONST_0 = 14
  type DCONST_1 = 15
  type BIPUSH = 16
  type SIPUSH = 17
  type LDC = 18
  type ILOAD = 21
  type LLOAD = 22
  type FLOAD = 23
  type DLOAD = 24
  type ALOAD = 25
  type IALOAD = 46
  type LALOAD = 47
  type FALOAD = 48
  type DALOAD = 49
  type AALOAD = 50
  type BALOAD = 51
  type CALOAD = 52
  type SALOAD = 53
  type ISTORE = 54
  type LSTORE = 55
  type FSTORE = 56
  type DSTORE = 57
  type ASTORE = 58
  type IASTORE = 79
  type LASTORE = 80
  type FASTORE = 81
  type DASTORE = 82
  type AASTORE = 83
  type BASTORE = 84
  type CASTORE = 85
  type SASTORE = 86
  type POP = 87
  type POP2 = 88
  type DUP = 89
  type DUP_X1 = 90
  type DUP_X2 = 91
  type DUP2 = 92
  type DUP2_X1 = 93
  type DUP2_X2 = 94
  type SWAP = 95
  type IADD = 96
  type LADD = 97
  type FADD = 98
  type DADD = 99
  type ISUB = 100
  type LSUB = 101
  type FSUB = 102
  type DSUB = 103
  type IMUL = 104
  type LMUL = 105
  type FMUL = 106
  type DMUL = 107
  type IDIV = 108
  type LDIV = 109
  type FDIV = 110
  type DDIV = 111
  type IREM = 112
  type LREM = 113
  type FREM = 114
  type DREM = 115
  type INEG = 116
  type LNEG = 117
  type FNEG = 118
  type DNEG = 119
  type ISHL = 120
  type LSHL = 121
  type ISHR = 122
  type LSHR = 123
  type IUSHR = 124
  type LUSHR = 125
  type IAND = 126
  type LAND = 127
  type IOR = 128
  type LOR = 129
  type IXOR = 130
  type LXOR = 131
  type IINC = 132
  type I2L = 133
  type I2F = 134
  type I2D = 135
  type L2I = 136
  type L2F = 137
  type L2D = 138
  type F2I = 139
  type F2L = 140
  type F2D = 141
  type D2I = 142
  type D2L = 143
  type D2F = 144
  type I2B = 145
  type I2C = 146
  type I2S = 147
  type LCMP = 148
  type FCMPL = 149
  type FCMPG = 150
  type DCMPL = 151
  type DCMPG = 152
  type IFEQ = 153
  type IFNE = 154
  type IFLT = 155
  type IFGE = 156
  type IFGT = 157
  type IFLE = 158
  type IF_ICMPEQ = 159
  type IF_ICMPNE = 160
  type IF_ICMPLT = 161
  type IF_ICMPGE = 162
  type IF_ICMPGT = 163
  type IF_ICMPLE = 164
  type IF_ACMPEQ = 165
  type IF_ACMPNE = 166
  type GOTO = 167
  type JSR = 168
  type RET = 169
  type TABLESWITCH = 170
  type LOOKUPSWITCH = 171
  type IRETURN = 172
  type LRETURN = 173
  type FRETURN = 174
  type DRETURN = 175
  type ARETURN = 176
  type RETURN = 177
  type GETSTATIC = 178
  type PUTSTATIC = 179
  type GETFIELD = 180
  type PUTFIELD = 181
  type INVOKEVIRTUAL = 182
  type INVOKESPECIAL = 183
  type INVOKESTATIC = 184
  type INVOKEINTERFACE = 185
  type INVOKEDYNAMIC = 186
  type NEW = 187
  type NEWARRAY = 188
  type ANEWARRAY = 189
  type ARRAYLENGTH = 190
  type ATHROW = 191
  type CHECKCAST = 192
  type INSTANCEOF = 193
  type MONITORENTER = 194
  type MONITOREXIT = 195
  type MULTIANEWARRAY = 197
  type IFNULL = 198
  type IFNONNULL = 199

  type InsnOpcode =
    NOP
      | ACONST_NULL
      | ICONST_M1
      | ICONST_0
      | ICONST_1
      | ICONST_2
      | ICONST_3
      | ICONST_4
      | ICONST_5
      | LCONST_0
      | LCONST_1
      | FCONST_0
      | FCONST_1
      | FCONST_2
      | DCONST_0
      | DCONST_1
      | IALOAD
      | LALOAD
      | FALOAD
      | DALOAD
      | AALOAD
      | BALOAD
      | CALOAD
      | SALOAD
      | IASTORE
      | LASTORE
      | FASTORE
      | DASTORE
      | AASTORE
      | BASTORE
      | CASTORE
      | SASTORE
      | POP
      | POP2
      | DUP
      | DUP_X1
      | DUP_X2
      | DUP2
      | DUP2_X1
      | DUP2_X2
      | SWAP
      | IADD
      | LADD
      | FADD
      | DADD
      | ISUB
      | LSUB
      | FSUB
      | DSUB
      | IMUL
      | LMUL
      | FMUL
      | DMUL
      | IDIV
      | LDIV
      | FDIV
      | DDIV
      | IREM
      | LREM
      | FREM
      | DREM
      | INEG
      | LNEG
      | FNEG
      | DNEG
      | ISHL
      | LSHL
      | ISHR
      | LSHR
      | IUSHR
      | LUSHR
      | IAND
      | LAND
      | IOR
      | LOR
      | IXOR
      | LXOR
      | I2L
      | I2F
      | I2D
      | L2I
      | L2F
      | L2D
      | F2I
      | F2L
      | F2D
      | D2I
      | D2L
      | D2F
      | I2B
      | I2C
      | I2S
      | LCMP
      | FCMPL
      | FCMPG
      | DCMPL
      | DCMPG
      | IRETURN
      | LRETURN
      | FRETURN
      | DRETURN
      | ARETURN
      | RETURN
      | ARRAYLENGTH
      | ATHROW
      | MONITORENTER
      | MONITOREXIT

  type IntInsnOpcode = BIPUSH | SIPUSH | NEWARRAY

  type VarInsnOpcode =
    ILOAD
      | LLOAD
      | FLOAD
      | DLOAD
      | ALOAD
      | ISTORE
      | LSTORE
      | FSTORE
      | DSTORE
      | ASTORE
      | RET

  type TypeInsnOpcode =
    NEW
      | ANEWARRAY
      | CHECKCAST
      | INSTANCEOF

  type FieldInsnOpcode =
    GETSTATIC
      | PUTSTATIC
      | GETFIELD
      | PUTFIELD

  type MethodInsnOpcode =
    INVOKEVIRTUAL
      | INVOKESPECIAL
      | INVOKESTATIC
      | INVOKEINTERFACE


  type JumpInsnOpcode =
    IFEQ
      | IFNE
      | IFLT
      | IFGE
      | IFGT
      | IFLE
      | IF_ICMPEQ
      | IF_ICMPNE
      | IF_ICMPLT
      | IF_ICMPGE
      | IF_ICMPGT
      | IF_ICMPLE
      | IF_ACMPEQ
      | IF_ACMPNE
      | GOTO
      | JSR
      | IFNULL
      | IFNONNULL

}

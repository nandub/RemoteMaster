package com.hifiremote.jp1;

public class DigitMaps
{
  public final static short[][] data =
  {
    { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 }, // 0
    { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09 }, // 1
    { 0x00, 0x01, 0x03, 0x02, 0x06, 0x07, 0x05, 0x04, 0x0C, 0x0D }, // 2
    { 0x00, 0x80, 0x40, 0xC0, 0x20, 0xA0, 0x60, 0xE0, 0x10, 0x90 }, // 3
    { 0x00, 0xF4, 0x74, 0xB4, 0x34, 0xD4, 0x54, 0x94, 0x14, 0xE4 }, // 4
    { 0x04, 0x00, 0x80, 0x40, 0xC0, 0x20, 0xA0, 0x60, 0xE0, 0x10 }, // 5
    { 0x04, 0x44, 0x24, 0x64, 0x14, 0x54, 0x34, 0x74, 0x0C, 0x4C }, // 6
    { 0x09, 0x89, 0x49, 0xC9, 0x29, 0xA9, 0x69, 0xE9, 0x19, 0x99 }, // 7
    { 0x0F, 0x3F, 0xBF, 0x7F, 0x1F, 0x9F, 0x5F, 0x2F, 0xAF, 0x6F }, // 8
    { 0x17, 0x16, 0x1B, 0x1A, 0x23, 0x22, 0x2F, 0x2E, 0x27, 0x26 }, // 9
    { 0x17, 0x8F, 0x0F, 0xF7, 0x77, 0xB7, 0x37, 0xD7, 0x57, 0x97 }, // 10
    { 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39 }, // 11
    { 0x31, 0x79, 0x39, 0x59, 0x19, 0x69, 0x29, 0x49, 0x09, 0x71 }, // 12
    { 0x33, 0x7B, 0xBB, 0x3B, 0xDB, 0x5B, 0x9B, 0x1B, 0xEB, 0x6B }, // 13
    { 0x33, 0xF3, 0x6B, 0x63, 0xEB, 0xFB, 0xE3, 0xB3, 0xAB, 0xBB }, // 14
    { 0x40, 0xF0, 0x70, 0xB0, 0xD0, 0x50, 0x90, 0xE0, 0x60, 0xA0 }, // 15
    { 0x44, 0x10, 0x14, 0x18, 0x20, 0x24, 0x28, 0x30, 0x34, 0x38 }, // 16
    { 0x47, 0xDF, 0x5F, 0x9F, 0x1F, 0xCF, 0x4F, 0x8F, 0x0F, 0xC7 }, // 17
    { 0x4F, 0x8F, 0x0F, 0xC7, 0x47, 0x87, 0x07, 0xDF, 0x5F, 0xCF }, // 18
    { 0x4F, 0xE7, 0x67, 0xA7, 0xD7, 0x57, 0x97, 0xF7, 0x77, 0xB7 }, // 19
    { 0x4F, 0xFF, 0x7F, 0xBF, 0xDF, 0x5F, 0x9F, 0xEF, 0x6F, 0xAF }, // 20
    { 0x50, 0x80, 0x40, 0xC0, 0x20, 0xA0, 0x60, 0xE0, 0x10, 0x90 }, // 21
    { 0x5A, 0xDE, 0x5C, 0x9C, 0x1E, 0xD4, 0x56, 0x96, 0x14, 0xD8 }, // 22
    { 0x5D, 0x8F, 0x4F, 0xCF, 0x95, 0x55, 0xD5, 0x8D, 0x4D, 0xCD }, // 23
    { 0x5F, 0xF7, 0x77, 0xB7, 0xCF, 0x4F, 0x8F, 0xEF, 0x6F, 0xAF }, // 24
    { 0x64, 0x68, 0x6C, 0x70, 0x48, 0x4C, 0x50, 0x28, 0x2C, 0x30 }, // 25
    { 0x64, 0xF4, 0x74, 0xB4, 0x34, 0xD4, 0x54, 0x94, 0x14, 0xE4 }, // 26
    { 0x66, 0xF6, 0x76, 0xB6, 0x36, 0xD6, 0x56, 0x96, 0x16, 0xE6 }, // 27
    { 0x67, 0xCF, 0x4F, 0x8F, 0xF7, 0x77, 0xB7, 0xD7, 0x57, 0x97 }, // 28
    { 0x67, 0xF7, 0x77, 0xB7, 0x37, 0xD7, 0x57, 0x97, 0x17, 0xE7 }, // 29
    { 0x68, 0x04, 0x78, 0xB8, 0x38, 0xD8, 0x58, 0x98, 0x18, 0xE8 }, // 30
    { 0x6D, 0xFD, 0x7D, 0xBD, 0x3D, 0xDD, 0x5D, 0x9D, 0x1D, 0xED }, // 31
    { 0x6F, 0xFF, 0x7F, 0xBF, 0x3F, 0xDF, 0x5F, 0x9F, 0x1F, 0xEF }, // 32
    { 0x6F, 0xFF, 0xEF, 0xF7, 0xE7, 0xFB, 0xEB, 0xF3, 0xE3, 0x7F }, // 33
    { 0x77, 0xDF, 0x5F, 0x9F, 0xEF, 0x6F, 0xAF, 0xCF, 0x4F, 0x8F }, // 34
    { 0x77, 0xFF, 0x5F, 0x9F, 0xDF, 0x6F, 0xAF, 0xEF, 0x4F, 0x8F }, // 35
    { 0x80, 0x81, 0x83, 0x82, 0x86, 0x87, 0x85, 0x84, 0x8C, 0x8D }, // 36
    { 0x80, 0xC0, 0xA0, 0xE0, 0x90, 0xD0, 0xB0, 0xF0, 0x88, 0xC8 }, // 37
    { 0x84, 0xC4, 0xA4, 0xE4, 0x94, 0xD4, 0xB4, 0xF4, 0x8C, 0xCC }, // 38
    { 0x86, 0xFA, 0xBA, 0xDA, 0x9A, 0xEA, 0xAA, 0xCA, 0x8A, 0xF2 }, // 39
    { 0x87, 0x37, 0xB7, 0x77, 0x17, 0x97, 0x57, 0x27, 0xA7, 0x67 }, // 40
    { 0x87, 0x6F, 0xE7, 0xF7, 0x27, 0x57, 0x77, 0x97, 0x67, 0x17 }, // 41
    { 0x88, 0x20, 0x28, 0x30, 0x40, 0x48, 0x50, 0x60, 0x68, 0x70 }, // 42
    { 0x8F, 0x3F, 0xBF, 0x7F, 0x1F, 0x9F, 0x5F, 0x2F, 0xAF, 0x6F }, // 43
    { 0x8F, 0xBF, 0x37, 0xCF, 0x97, 0xD7, 0x17, 0xE7, 0x4F, 0x67 }, // 44
    { 0x8F, 0xDF, 0x5F, 0x9F, 0x1F, 0xEF, 0x6F, 0xAF, 0x2F, 0xCF }, // 45
    { 0x90, 0x00, 0x80, 0x40, 0xC0, 0x20, 0xA0, 0x60, 0xE0, 0x10 }, // 46
    { 0x90, 0x20, 0x28, 0x30, 0x40, 0x48, 0x50, 0x60, 0x68, 0x70 }, // 47
    { 0x90, 0xB8, 0xB0, 0xA8, 0xD8, 0xD0, 0xC8, 0xF8, 0xF0, 0xE8 }, // 48
    { 0x97, 0x17, 0xE7, 0x67, 0xA7, 0x27, 0xC7, 0x47, 0x87, 0x07 }, // 49
    { 0x97, 0xB7, 0x37, 0x77, 0xF7, 0x0F, 0xE7, 0x17, 0xD7, 0x57 }, // 50
    { 0x98, 0xBC, 0xB8, 0xB4, 0xB0, 0xAC, 0xA8, 0xA4, 0xA0, 0x9C }, // 51
    { 0x98, 0xDC, 0x5E, 0x9E, 0x1C, 0xD6, 0x54, 0x94, 0x16, 0xDA }, // 52
    { 0x9B, 0x0B, 0x8B, 0x4B, 0xCB, 0x2B, 0xAB, 0x6B, 0xEB, 0x1B }, // 53
    { 0x9C, 0x92, 0xA2, 0x82, 0x9A, 0xAA, 0x8A, 0x96, 0xA6, 0x86 }, // 54
    { 0x9F, 0x07, 0x87, 0x47, 0xC7, 0x0F, 0x8F, 0x4F, 0xCF, 0x1F }, // 55
    { 0x9F, 0x4F, 0x8F, 0x0F, 0xC7, 0x47, 0x87, 0x07, 0xDF, 0x5F }, // 56
    { 0xA4, 0x74, 0xB4, 0x34, 0xD4, 0x54, 0x94, 0x14, 0xE4, 0x64 }, // 57
    { 0xA7, 0x77, 0xB7, 0x37, 0xD7, 0x57, 0x97, 0x17, 0xE7, 0x67 }, // 58
    { 0xA7, 0xF7, 0x77, 0xB7, 0x37, 0xD7, 0x57, 0x97, 0x17, 0xE7 }, // 59
    { 0xAC, 0x40, 0xA0, 0xC0, 0x44, 0xA4, 0xC4, 0x48, 0xA8, 0xC8 }, // 60
    { 0xAC, 0x98, 0x88, 0x90, 0xB8, 0xA8, 0xB0, 0x9C, 0x8C, 0x94 }, // 61
    { 0xAC, 0xD8, 0xB4, 0x70, 0xD4, 0xB8, 0x8C, 0xF0, 0xA8, 0x94 }, // 62
    { 0xAE, 0x7E, 0xBE, 0x3E, 0xDE, 0x5E, 0x9E, 0x1E, 0xEE, 0x6E }, // 63
    { 0xAF, 0x7F, 0xBF, 0x3F, 0xDF, 0x5F, 0x9F, 0x1F, 0xEF, 0x6F }, // 64
    { 0xAF, 0xFF, 0x7F, 0xBF, 0x3F, 0xDF, 0x5F, 0x9F, 0x1F, 0xEF }, // 65
    { 0xB0, 0x20, 0xA0, 0x60, 0xE0, 0x10, 0x90, 0x50, 0xD0, 0x30 }, // 66
    { 0xB4, 0xDC, 0xD8, 0xC0, 0xBC, 0xB8, 0xD4, 0xCC, 0xC8, 0xC4 }, // 67
    { 0xB6, 0x1E, 0x9E, 0x5E, 0x2E, 0xAE, 0x6E, 0x0E, 0x8E, 0x4E }, // 68
    { 0xB7, 0x6F, 0x47, 0x07, 0x4F, 0x67, 0x27, 0x77, 0x57, 0x17 }, // 69
    { 0xB7, 0x7F, 0x9F, 0x1F, 0x5F, 0xAF, 0x2F, 0x6F, 0x8F, 0x0F }, // 70
    { 0xBC, 0xB8, 0xB4, 0xB0, 0xAC, 0xA8, 0xA4, 0xA0, 0x9C, 0x98 }, // 71
    { 0xC0, 0xC4, 0xC8, 0xCC, 0xD0, 0xD4, 0xD8, 0xDC, 0xE0, 0xE4 }, // 72
    { 0xC4, 0xA0, 0xA2, 0xA4, 0xA8, 0xAA, 0xB0, 0xB2, 0xB4, 0xC0 }, // 73
    { 0xC4, 0xFC, 0xDC, 0xBC, 0xF8, 0xD8, 0xB8, 0xF4, 0xD4, 0xB4 }, // 74
    { 0xCF, 0x4F, 0x8F, 0x0F, 0xC7, 0x47, 0x87, 0x07, 0xDF, 0x5F }, // 75
    { 0xCF, 0xCE, 0xCD, 0xCC, 0xCB, 0xCA, 0xC9, 0xC8, 0xC7, 0xC6 }, // 76
    { 0xD2, 0x42, 0xC2, 0x22, 0xA2, 0x62, 0xE2, 0x12, 0x92, 0x52 }, // 77
    { 0xD4, 0xF8, 0xF4, 0xF0, 0xEC, 0xE8, 0xE4, 0xE0, 0xDC, 0xD8 }, // 78
    { 0xD8, 0xE8, 0xEC, 0xF0, 0xF4, 0xF8, 0xC8, 0xCC, 0xD0, 0xD4 }, // 79
    { 0xDF, 0x5F, 0x9F, 0x1F, 0xCF, 0x4F, 0x8F, 0x0F, 0xC7, 0x47 }, // 80
    { 0xDF, 0x5F, 0x9F, 0x1F, 0xEF, 0x6F, 0xAF, 0x2F, 0xCF, 0x4F }, // 81
    { 0xE4, 0xEC, 0xE4, 0xCC, 0xF0, 0xD0, 0xC8, 0xD0, 0xF8, 0xDC }, // 82
    { 0xEF, 0xFF, 0xBF, 0x3F, 0x57, 0x67, 0x2F, 0xD7, 0xE7, 0x37 }, // 83
    { 0xF4, 0x74, 0xB4, 0x34, 0xD4, 0x54, 0x94, 0x14, 0xE4, 0x64 }, // 84
    { 0xF6, 0x76, 0xB6, 0x36, 0xD6, 0x56, 0x96, 0x16, 0xE6, 0x66 }, // 85
    { 0xF7, 0x77, 0xB7, 0x37, 0xD7, 0x57, 0x97, 0x17, 0xE7, 0x67 }, // 86
    { 0xF9, 0x79, 0xB9, 0x39, 0xD9, 0x59, 0x99, 0x19, 0xE9, 0x69 }, // 87
    { 0xFA, 0x7A, 0xBA, 0x3A, 0xDA, 0x5A, 0x9A, 0x1A, 0xEA, 0x6A }, // 88
    { 0xFA, 0xBA, 0xDA, 0x9A, 0xEA, 0xAA, 0xCA, 0x8A, 0xF2, 0xB2 }, // 89
    { 0xFB, 0x7B, 0xBB, 0x3B, 0xDB, 0x5B, 0x9B, 0x1B, 0xEB, 0x6B }, // 90
    { 0xFC, 0x7C, 0xBC, 0x3C, 0xDC, 0x5C, 0x9C, 0x1C, 0xEC, 0x6C }, // 91
    { 0xFC, 0xF8, 0xF4, 0xF0, 0xEC, 0xE8, 0xE4, 0xE0, 0xDC, 0xD8 }, // 92
    { 0xFD, 0x7D, 0xBD, 0x3D, 0xDD, 0x5D, 0x9D, 0x1D, 0xED, 0x6D }, // 93
    { 0xFF, 0x7F, 0xBF, 0x3F, 0xDF, 0x5F, 0x9F, 0x1F, 0xEF, 0x6F }, // 94
    { 0xFF, 0xFE, 0xFD, 0xFC, 0xFB, 0xFA, 0xF9, 0xF8, 0xF7, 0xF6 }, // 95
    { 0x07, 0x0F, 0x17, 0x1F, 0x27, 0x2F, 0x37, 0x3F, 0x47, 0x4F }, // 96
    { 0x00, 0x20, 0x40, 0x60, 0x80, 0x88, 0x68, 0x48, 0x28, 0x08 }, // 97
    { 0xB4, 0xF4, 0x8C, 0xCC, 0xAC, 0xEC, 0x9C, 0xDC, 0xBC, 0xFC }, // 98
    { 0x96, 0xCE, 0x4E, 0x8E, 0x0E, 0xF6, 0x76, 0xB6, 0x36, 0x56 }, // 99
    { 0x17, 0xE7, 0xD7, 0xF7, 0x67, 0x57, 0x77, 0xA7, 0x97, 0xB7 }, // 100
    { 0xFF, 0x7F, 0xBF, 0x3F, 0xEF, 0x6F, 0xAF, 0x2F, 0xF7, 0x77 }, // 101
    { 0x17, 0xE7, 0xD7, 0xF7, 0x67, 0x57, 0x77, 0xA7, 0x97, 0xB7 }, // 102
    { 0xEC, 0x48, 0x4C, 0xC8, 0x88, 0xF0, 0x68, 0x28, 0xCC, 0x8C }, // 103
    { 0xC4, 0xFC, 0xDC, 0xBC, 0xEC, 0xCC, 0xAC, 0xE8, 0xC8, 0xA8 }, // 104
    { 0x58, 0xDC, 0x5E, 0x9E, 0x1C, 0xD6, 0x54, 0x94, 0x16, 0xDA }, // 105
    { 0x4F, 0xDF, 0x5F, 0x9F, 0x1F, 0xEF, 0x6F, 0xAF, 0x2F, 0xCF }, // 106
    { 0x08, 0x60, 0x68, 0x70, 0x40, 0x48, 0x50, 0x20, 0x28, 0x30 }, // 107
    { 0x60, 0x90, 0x80, 0x88, 0xB0, 0xA0, 0xA8, 0x30, 0x20, 0x28 }, // 108
    { 0x40, 0x44, 0x48, 0x4C, 0x50, 0x54, 0x58, 0x5C, 0x60, 0x64 }, // 109
    { 0x20, 0xC0, 0xC8, 0xD0, 0x60, 0x68, 0x70, 0x40, 0x48, 0x50 }, // 110
    { 0x20, 0x40, 0x48, 0x50, 0x60, 0x68, 0x70, 0x80, 0x88, 0x90 }, // 111
    { 0x2F, 0x7F, 0xBF, 0x3F, 0xDF, 0x5F, 0x9F, 0x1F, 0xEF, 0x6F }, // 112
    { 0x8C, 0xB4, 0x74, 0xF4, 0x94, 0x54, 0xD4, 0xA4, 0x64, 0xE4 }, // 113
    { 0xFE, 0x7E, 0xBE, 0x3E, 0xDE, 0x5E, 0x9E, 0x1E, 0xEE, 0x6E }, // 114
    { 0xFB, 0xBB, 0xDB, 0x9B, 0xEB, 0xAB, 0xCB, 0x8B, 0xF3, 0xB3 }, // 115
    { 0x00, 0xF8, 0xF4, 0xF0, 0xEC, 0xE8, 0xE4, 0x00, 0x00, 0x00 }, // 116
    { 0x26, 0x42, 0xC2, 0x22, 0xA2, 0x62, 0xE2, 0x12, 0x92, 0xC6 }, // 117
    { 0x6E, 0xFE, 0x7E, 0xBE, 0x3E, 0xDE, 0x5E, 0x9E, 0x1E, 0xEE }, // 118
    { 0x17, 0xE7, 0xD7, 0xF7, 0x67, 0x57, 0x77, 0xA7, 0x97, 0xB7 }, // 119
    { 0xED, 0x65, 0x75, 0xE5, 0x7D, 0xBD, 0x3D, 0xDD, 0x5D, 0x9D }, // 120
    { 0xFF, 0x7F, 0xBF, 0x3F, 0xEF, 0x6F, 0xAF, 0x2F, 0xF7, 0x77 }, // 121
    { 0x65, 0xF5, 0x75, 0xB5, 0x35, 0xD5, 0x55, 0x95, 0x15, 0xE5 }, // 122
    { 0x00, 0x75, 0xB5, 0x35, 0xD5, 0x55, 0x95, 0x00, 0x00, 0x00 }, // 123
    { 0x08, 0x88, 0x48, 0xC8, 0x28, 0xA8, 0x68, 0xE8, 0x18, 0x98 }, // 124
    { 0x50, 0x28, 0x48, 0x68, 0x88, 0x2C, 0x4C, 0x6C, 0x8C, 0x30 }, // 125
    { 0x6A, 0xFA, 0x7A, 0xBA, 0x3A, 0xDA, 0x5A, 0x9A, 0x1A, 0xEA }, // 126
    { 0x73, 0xEB, 0x6B, 0xAB, 0x2B, 0xCB, 0x4B, 0x8B, 0x0B, 0xF3 }, // 127
    { 0xB5, 0x6D, 0xAD, 0x2D, 0xCD, 0x4D, 0x8D, 0x0D, 0xF5, 0x75 }, // 128
    { 0x87, 0x5F, 0x9F, 0x1F, 0xDF, 0x9D, 0x5D, 0xE7, 0x67, 0xA7 }, // 129
    { 0x83, 0x73, 0xB3, 0x33, 0x53, 0x93, 0x13, 0x63, 0xA3, 0x23 }, // 130
    { 0xA7, 0x77, 0x8F, 0x0F, 0x57, 0xB7, 0x37, 0x67, 0x97, 0x17 }, // 131
    { 0x57, 0x49, 0x51, 0x41, 0x4D, 0x55, 0x45, 0x4B, 0x53, 0x43 }, // 132
    { 0x5C, 0x54, 0x5C, 0x74, 0x48, 0x68, 0x70, 0x68, 0x40, 0x64 }, // 133
    { 0xB8, 0xB0, 0xB8, 0x90, 0xAC, 0x8C, 0x94, 0x8C, 0xA4, 0x80 }, // 134
    { 0x00, 0x08, 0x00, 0x28, 0x14, 0x34, 0x2C, 0x34, 0x1C, 0x38 }, // 135
    { 0x22, 0xFF, 0xEE, 0xDD, 0xBB, 0xAA, 0x99, 0x77, 0x66, 0x55 }, // 136
    { 0xFB, 0xBF, 0xDF, 0x9F, 0xEF, 0xAF, 0xCF, 0x8F, 0xF7, 0xB7 }, // 137
    { 0xF0, 0xE1, 0xD2, 0xC3, 0xB4, 0xA5, 0x96, 0x87, 0x78, 0x69 }, // 138
    { 0x6C, 0xFC, 0x7C, 0xBC, 0x3C, 0xDC, 0x5C, 0x9C, 0x1C, 0xEC }, // 139
    { 0xE2, 0xFE, 0xEE, 0xDE, 0xFC, 0xEC, 0xDC, 0xFA, 0xEA, 0xDA }, // 140
    { 0x6D, 0x37, 0xF7, 0x77, 0x0F, 0xCF, 0x4F, 0x2F, 0xEF, 0x6F }, // 141
    { 0x00, 0x00, 0x84, 0x8C, 0x94, 0x9C, 0x00, 0xA4, 0xAC, 0xB4 }, // 142
    { 0x80, 0x80, 0xA0, 0xB0, 0xA8, 0xB8, 0xA4, 0xB4, 0xAC, 0xBC }, // 143
    { 0xA0, 0x90, 0x88, 0x84, 0x82, 0x81, 0x60, 0x50, 0x48, 0x44 }, // 144
    { 0x36, 0xD6, 0x56, 0x96, 0x16, 0xE6, 0x66, 0xA6, 0x26, 0xC6 }, // 145
    { 0xF4, 0xD4, 0xB4, 0x94, 0x74, 0x54, 0x34, 0x14, 0x38, 0x18 }, // 146
    { 0x00, 0xB5, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 }, // 147
    { 0xDE, 0x5E, 0x9E, 0x1E, 0xEE, 0x6E, 0xAE, 0x2E, 0xCE, 0x4E }, // 148
    { 0xF8, 0xB0, 0xF0, 0x90, 0xD0, 0x88, 0xC8, 0xA8, 0xE8, 0xB8 }, // 149
    { 0x1F, 0x07, 0x5F, 0x57, 0x4F, 0x47, 0x3F, 0x37, 0x2F, 0x27 }, // 150
    { 0xCB, 0xFD, 0xC3, 0xA3, 0xE3, 0x93, 0xD3, 0xB3, 0xF3, 0x8B }, // 151
    { 0xC9, 0x81, 0xC1, 0xA1, 0xE1, 0x91, 0xD1, 0xB1, 0xF1, 0x89 }, // 152
    { 0x81, 0xC1, 0xA1, 0xE1, 0x91, 0xD1, 0xB1, 0xF1, 0x89, 0xC9 }, // 153
    { 0x4D, 0x05, 0x45, 0x25, 0x65, 0x15, 0x55, 0x35, 0x75, 0x0D }, // 154
    { 0xCD, 0x85, 0xC5, 0xA5, 0xE5, 0x95, 0xD5, 0xB5, 0xF5, 0x8D }, // 155
    { 0xAE, 0xFE, 0x7E, 0xBE, 0x3E, 0xDE, 0x5E, 0x9E, 0x1E, 0xEE }, // 156
    { 0x00, 0xEF, 0x6F, 0xAF, 0x2F, 0xCF, 0x4F, 0x8F, 0x0F, 0x00 }, // 157
    { 0xEF, 0x6F, 0xAF, 0x2F, 0xCF, 0x4F, 0x00, 0x00, 0x00, 0x00 }, // 158
    { 0x00, 0x88, 0x84, 0x82, 0x81, 0x60, 0x50, 0x00, 0x00, 0x00 }, // 159
    { 0x48, 0x00, 0x08, 0x10, 0x18, 0x20, 0x28, 0x30, 0x38, 0x40 }, // 160
    { 0x70, 0x94, 0x90, 0x8C, 0x88, 0x84, 0x80, 0x7C, 0x78, 0x74 }, // 161
    { 0xF6, 0x74, 0xB4, 0x36, 0xD4, 0x56, 0x96, 0x14, 0xE4, 0x66 }, // 162
    { 0xD9, 0x62, 0x55, 0x3D, 0x31, 0x25, 0x0B, 0xFE, 0xF2, 0xE6 }, // 163
    { 0xDC, 0xD8, 0xD4, 0xD0, 0xCC, 0xC8, 0xC4, 0xC0, 0xBC, 0xB8 }, // 164
    { 0xA6, 0x74, 0xB4, 0x36, 0xD4, 0x56, 0x96, 0x14, 0xE4, 0x66 }, // 165
    { 0x8F, 0xBF, 0x5F, 0x1F, 0x7F, 0xFF, 0x3F, 0xAF, 0x6F, 0xEF }, // 166
    { 0x4F, 0x7F, 0xD7, 0xBF, 0xFF, 0x5F, 0x9F, 0xDF, 0x6F, 0xAF }, // 167
    { 0x8f, 0x7f, 0xbf, 0x9f, 0xdf, 0x5f, 0x1f, 0xef, 0xaf, 0x2f }, // 168
    { 0x00, 0x04, 0x08, 0x0c, 0x10, 0x14, 0x18, 0x1c, 0x20, 0x24 }, // 169
    { 0xdd, 0x23, 0xc3, 0x43, 0x83, 0x03, 0xfd, 0x7d, 0xbd, 0x3d }, // 170
    { 0xD4, 0xBC, 0xB4, 0xAC, 0x9C, 0x94, 0x8C, 0x3C, 0x34, 0x2C }, // 171
    { 0xD4, 0x90, 0x88, 0x98, 0x84, 0x94, 0xD0, 0xC8, 0xD8, 0xC4 }, // 172
    { 0xCC, 0xF0, 0xEC, 0xE8, 0xE4, 0xE0, 0xDC, 0xD8, 0xD4, 0xD0 }, // 173
    { 0xDD, 0x2D, 0x1D, 0x3D, 0xAD, 0x9D, 0xBD, 0x6D, 0x5D, 0x7D }, // 174
    { 0x85, 0xC7, 0xE7, 0x57, 0x97, 0xB7, 0x37, 0xD7, 0x4F, 0x27 }, // 175
    { 0x00, 0x00, 0x01, 0x00, 0x02, 0x00, 0x03, 0x00, 0x04, 0x00 }, // 176
    { 0x05, 0x00, 0x06, 0x00, 0x07, 0x00, 0x08, 0x00, 0x09, 0x00 }, // 177
    { 0xE0, 0xB4, 0xF0, 0xF4, 0xD4, 0xE4, 0xE8, 0xFC, 0xD8, 0xDC }, // 178
    { 0xCF, 0x3F, 0xDF, 0x5F, 0x9F, 0x1F, 0xEF, 0x6F, 0xAF, 0x2F }, // 179
    { 0x9F, 0xC7, 0xE7, 0x57, 0xB7, 0x37, 0xD7, 0x27, 0x85, 0x55 }, // 180
    { 0x51, 0x00, 0x0F, 0x00, 0x01, 0x00, 0x02, 0x00, 0x03, 0x00 }, // 181
    { 0x04, 0x00, 0x05, 0x00, 0x06, 0x00, 0x07, 0x00, 0x08, 0x00 }, // 182
    { 0xFB, 0x67, 0xFB, 0xF7, 0xFB, 0x77, 0xFB, 0xB7, 0xFB, 0x37 }, // 183
    { 0xFB, 0xD7, 0xFB, 0x57, 0xFB, 0x97, 0xFB, 0x17, 0xFB, 0xE7 }, // 184
    { 0xBC, 0xBA, 0xB6, 0xB0, 0xAE, 0xA8, 0xA4, 0xA2, 0x9E, 0x98 }, // 185
    { 0x65, 0x4F, 0x67, 0x27, 0x7D, 0x77, 0x57, 0x17, 0x9D, 0xB7 }, // 186
    { 0xFB, 0x01, 0x7B, 0x01, 0xBB, 0x01, 0x3B, 0x01, 0xDB, 0x01 }, // 187
    { 0x5B, 0x01, 0x9B, 0x01, 0x1B, 0x01, 0xEB, 0x01, 0x6B, 0x01 }, // 188
    { 0x04, 0xF8, 0x78, 0xB8, 0x38, 0xD8, 0x58, 0x98, 0x18, 0xE8 }, // 189
    { 0xEF, 0x7D, 0x4D, 0x5D, 0xBD, 0x8D, 0x9D, 0x3D, 0x0D, 0x1D }, // 190
    { 0x87, 0x3F, 0x37, 0x27, 0xDF, 0xD7, 0xC7, 0x5F, 0x57, 0x47 }, // 191
    { 0x7C, 0xAF, 0x7C, 0x7F, 0x7C, 0xBF, 0x7C, 0x3F, 0x7C, 0xDF }, // 192
    { 0x7C, 0x5F, 0x7C, 0x9F, 0x7C, 0x1F, 0x7C, 0xEF, 0x7C, 0x6F }, // 193
    { 0x1F, 0xEF, 0x6F, 0xAF, 0x2F, 0xCF, 0x4F, 0x8F, 0x0F, 0xF7 }, // 194
    { 0xD7, 0xFF, 0xD7, 0x7F, 0xD7, 0xBF, 0xD7, 0x3F, 0xD7, 0xDF }, // 195
    { 0xD7, 0x5F, 0xD7, 0x9F, 0xD7, 0x1F, 0xD7, 0xEF, 0xD7, 0x6F }, // 196
    { 0xFF, 0x7D, 0x4D, 0x5D, 0xBD, 0x8D, 0x9D, 0x3D, 0x0D, 0x1D }, // 197
    { 0x6F, 0xF7, 0x0D, 0xF5, 0x8D, 0xFD, 0x7F, 0xAF, 0xFF, 0xBF }, // 198
    { 0x67, 0xE8, 0xF7, 0xE8, 0x77, 0xE8, 0xB7, 0xE8, 0x37, 0xE8 }, // 199
    { 0xD7, 0xE8, 0x57, 0xE8, 0x97, 0xE8, 0x17, 0xE8, 0xE7, 0xE8 }, // 200
    { 0x00, 0x6F, 0x00, 0x00, 0x00, 0x00, 0x77, 0x00, 0x67, 0x00 }, // 201
    { 0x87, 0xB7, 0xF5, 0x75, 0xAF, 0xED, 0x6D, 0x97, 0xD5, 0x55 }, // 202
    { 0x9F, 0xB7, 0x37, 0xCD, 0x8F, 0x0F, 0xED, 0xAF, 0x2F, 0xDD }, // 203
    { 0xA0, 0x8C, 0xF4, 0xB4, 0xCC, 0x94, 0xD0, 0xC0, 0xD4, 0xB0 }, // 204
    { 0xE7, 0xC5, 0x87, 0x85, 0xE5, 0xA7, 0x25, 0x27, 0x65, 0xA5 }, // 205
    { 0x90, 0x80, 0x00, 0x80, 0x80, 0x80, 0x40, 0x80, 0xC0, 0x80 }, // 206
    { 0x20, 0x80, 0xA0, 0x80, 0x60, 0x80, 0xE0, 0x80, 0x10, 0x80 }, // 207
    { 0x7D, 0x6F, 0x5F, 0x7F, 0x2D, 0x1D, 0x3D, 0xAD, 0x9D, 0xBD }, // 208
    { 0xFA, 0x01, 0x7A, 0x01, 0xBA, 0x01, 0x3A, 0x01, 0xDA, 0x01 }, // 209
    { 0x5A, 0x01, 0x9A, 0x01, 0x1A, 0x01, 0xEA, 0x01, 0x6A, 0x01 }, // 210
    { 0x1D, 0xC7, 0x47, 0x87, 0xFD, 0x7D, 0xBD, 0xDD, 0x5D, 0x9D }, // 211
    { 0x6D, 0x5D, 0x9F, 0xDF, 0xFD, 0xBF, 0x7F, 0x7D, 0x3F, 0xF7 }, // 212
    { 0x6D, 0x6F, 0x5F, 0x7F, 0x2D, 0x1D, 0x3D, 0xAD, 0x9D, 0xBD }, // 213
    { 0xFF, 0x5F, 0x5D, 0xDD, 0x1F, 0x9F, 0x9D, 0xDF, 0x0D, 0x4F }, // 214
    { 0x4F, 0xA7, 0x27, 0xD7, 0x97, 0x57, 0x17, 0xB7, 0x77, 0xF7 }, // 215
    { 0x88, 0x68, 0x64, 0x60, 0x98, 0x08, 0xA4, 0xA0, 0x9C, 0x50 }, // 216
    { 0x67, 0xF0, 0xF7, 0xF0, 0x77, 0xF0, 0xB7, 0xF0, 0x37, 0xF0 }, // 217
    { 0xD7, 0xF0, 0x57, 0xF0, 0x97, 0xF0, 0x17, 0xF0, 0xE7, 0xF0 }, // 218
    { 0xC7, 0x67, 0xC7, 0xF7, 0xC7, 0x77, 0xC7, 0xB7, 0xC7, 0x37 }, // 219
    { 0xC7, 0xD7, 0xC7, 0x57, 0xC7, 0x97, 0xC7, 0x17, 0xC7, 0xE7 }, // 220
    { 0x17, 0x03, 0x8F, 0x03, 0x0F, 0x03, 0xF7, 0x03, 0x77, 0x03 }, // 221
    { 0xB7, 0x03, 0x37, 0x03, 0xD7, 0x03, 0x57, 0x03, 0x97, 0x03 }, // 222
    { 0xD8, 0xE4, 0x0C, 0xEC, 0x74, 0xE4, 0xB4, 0xCC, 0x14, 0xF0 }, // 223
    { 0x64, 0xD0, 0x24, 0xC8, 0xAC, 0xD0, 0x6C, 0xF8, 0x8C, 0xDC }, // 224
    { 0x1A, 0xFC, 0x1A, 0xF8, 0x1A, 0xF4, 0x1A, 0xF0, 0x1A, 0xEC }, // 225
    { 0x1A, 0xE8, 0x1A, 0xE4, 0x1A, 0xE0, 0x1A, 0xDC, 0x1A, 0xD8 }, // 226
    { 0x90, 0x59, 0x00, 0x59, 0x80, 0x59, 0x40, 0x59, 0xC0, 0x59 }, // 227
    { 0x20, 0x59, 0xA0, 0x59, 0x60, 0x59, 0xE0, 0x59, 0x10, 0x59 }, // 228
    { 0xCF, 0x8D, 0xAD, 0x9D, 0x4D, 0x6D, 0x5D, 0xCD, 0xED, 0xDD }, // 229
    { 0xB7, 0x60, 0x5F, 0x60, 0x9F, 0x60, 0x1F, 0x60, 0x6F, 0x60 }, // 230
    { 0xAF, 0x60, 0x2F, 0x60, 0x4F, 0x60, 0x8F, 0x60, 0x0F, 0x60 }, // 231
    { 0x2B, 0x17, 0x2B, 0x8F, 0x2B, 0x0F, 0x2B, 0xF7, 0x2B, 0x77 }, // 232
    { 0x2B, 0xB7, 0x2B, 0x37, 0x2B, 0xD7, 0x2B, 0x57, 0x2B, 0x97 }, // 233
    { 0x6D, 0x4F, 0x6F, 0x5F, 0x0D, 0x2D, 0x1D, 0x8D, 0xAD, 0x9D }, // 234
    { 0xDC, 0x64, 0xDC, 0xF4, 0xDC, 0x74, 0xDC, 0xB4, 0xDC, 0x34 }, // 235
    { 0xDC, 0xD4, 0xDC, 0x54, 0xDC, 0x94, 0xDC, 0x14, 0xDC, 0xE4 }, // 236
    { 0x3C, 0x6E, 0x3C, 0xFE, 0x3C, 0x7E, 0x3C, 0xBE, 0x3C, 0x3E }, // 237
    { 0x3C, 0xDE, 0x3C, 0x5E, 0x3C, 0x9E, 0x3C, 0x1E, 0x3C, 0xEE }, // 238
    { 0xE2, 0x00, 0x1D, 0x00, 0x24, 0x00, 0x2B, 0x00, 0x71, 0x00 }, // 239
    { 0x7E, 0x00, 0x47, 0x00, 0x48, 0x00, 0xD4, 0x00, 0xDB, 0x00 }, // 240
    { 0x00, 0xCF, 0x00, 0xCE, 0x00, 0xCD, 0x00, 0xCC, 0x00, 0xCB }, // 241
    { 0x00, 0xCA, 0x00, 0xC9, 0x00, 0xC8, 0x00, 0xC7, 0x00, 0xC6 }, // 242
    { 0x6F, 0xC7, 0xE7, 0x57, 0xB7, 0x37, 0xD7, 0x27, 0x85, 0x55 }, // 243
    { 0xE7, 0x7F, 0xBF, 0x3F, 0x5F, 0x9F, 0x1F, 0x6F, 0xAF, 0x2F }, // 244
    { 0x67, 0xD4, 0xF7, 0xD4, 0x77, 0xD4, 0xB7, 0xD4, 0x37, 0xD4 }, // 245
    { 0xD7, 0xD4, 0x57, 0xD4, 0x97, 0xD4, 0x17, 0xD4, 0xE7, 0xD4 }, // 246
    { 0x1F, 0xFC, 0x1F, 0xF8, 0x1F, 0xF4, 0x1F, 0xF0, 0x1F, 0xEC }, // 247
    { 0x1F, 0xE8, 0x1F, 0xE4, 0x1F, 0xE0, 0x1F, 0xDC, 0x1F, 0xD8 }, // 248
    { 0x11, 0xAF, 0x11, 0x7F, 0x11, 0xBF, 0x11, 0x3F, 0x11, 0xDF }, // 249
    { 0x11, 0x5F, 0x11, 0x9F, 0x11, 0x1F, 0x11, 0xEF, 0x11, 0x6F }, // 250
    { 0xAF, 0x7F, 0x7F, 0x72, 0xBF, 0x7E, 0x3F, 0x76, 0xDF, 0x78 }, // 251
    { 0x5F, 0x70, 0x9F, 0x7C, 0x1F, 0x74, 0xEF, 0x7B, 0x6F, 0x73 }, // 252
    { 0x6F, 0x30, 0xFF, 0x30, 0x7F, 0x30, 0xBF, 0x30, 0x3F, 0x30 }, // 253
    { 0xDF, 0x30, 0x5F, 0x30, 0x9F, 0x30, 0x1F, 0x30, 0xEF, 0x30 }, // 254
    { 0x36, 0x40, 0xD6, 0x40, 0x56, 0x40, 0x96, 0x40, 0x16, 0x40 }, // 255
    { 0xE6, 0x40, 0x66, 0x40, 0xA6, 0x40, 0x26, 0x40, 0xC6, 0x40 }, // 256
    { 0x90, 0x0B, 0x00, 0x0B, 0x80, 0x0B, 0x40, 0x0B, 0xC0, 0x0B }, // 257
    { 0x20, 0x0B, 0xA0, 0x0B, 0x60, 0x0B, 0xE0, 0x0B, 0x10, 0x0B }, // 258
    { 0x6F, 0x00, 0xFF, 0x00, 0x7F, 0x00, 0xBF, 0x00, 0x3F, 0x00 }, // 259
    { 0xDF, 0x00, 0x5F, 0x00, 0x9F, 0x00, 0x1F, 0x00, 0xEF, 0x00 }, // 260
    { 0xDF, 0x00, 0x5F, 0x00, 0x9F, 0x00, 0x1F, 0x00, 0xCF, 0x00 }, // 261
    { 0x4F, 0x00, 0x8F, 0x00, 0x0F, 0x00, 0xC7, 0x00, 0x47, 0x00 }, // 262
    { 0xFF, 0x21, 0x7F, 0x21, 0xBF, 0x21, 0x3F, 0x21, 0xDF, 0x21 }, // 263
    { 0x5F, 0x21, 0x9F, 0x21, 0x1F, 0x21, 0xEF, 0x21, 0x6F, 0x21 }, // 264
    { 0x47, 0x20, 0xDF, 0x20, 0x5F, 0x20, 0x9F, 0x20, 0x1F, 0x20 }, // 265
    { 0xCF, 0x20, 0x4F, 0x20, 0x8F, 0x20, 0x0F, 0x20, 0xC7, 0x20 }, // 266
    { 0x7D, 0xEF, 0x6F, 0xAF, 0xF7, 0x77, 0xB7, 0xE7, 0x67, 0xA7 }, // 267
    { 0x91, 0x0C, 0x01, 0x0C, 0x81, 0x0C, 0x41, 0x0C, 0xC1, 0x0C }, // 268
    { 0x21, 0x0C, 0xA1, 0x0C, 0x61, 0x0C, 0xE1, 0x0C, 0x11, 0x0C }, // 269
    { 0xFF, 0x40, 0x7F, 0x40, 0xBF, 0x40, 0x3F, 0x40, 0xDF, 0x40 }, // 270
    { 0x5F, 0x40, 0x9F, 0x40, 0x1F, 0x40, 0xEF, 0x40, 0x6F, 0x40 }, // 271
    { 0xED, 0x9F, 0x5F, 0xDF, 0x5D, 0xDD, 0x1D, 0xAF, 0x6F, 0xEF }, // 272
    { 0x77, 0xB7, 0x37, 0xD7, 0x57, 0x97, 0x17, 0xE7, 0x67, 0xA7 }, // 273
    { 0x1C, 0x3E, 0xBE, 0x7E, 0x7C, 0xFC, 0x3C, 0x9E, 0x5E, 0xDE }, // 274
    { 0x10, 0xCF, 0x10, 0xCE, 0x10, 0xCD, 0x10, 0xCC, 0x10, 0xCB }, // 275
    { 0x10, 0xCA, 0x10, 0xC9, 0x10, 0xC8, 0x10, 0xC7, 0x10, 0xC6 }, // 276
    { 0x17, 0xB7, 0x8F, 0xAF, 0x77, 0x4F, 0x6F, 0xF7, 0xCF, 0xEF }, // 277
    { 0x67, 0xE4, 0xF7, 0xE4, 0x77, 0xE4, 0xB7, 0xE4, 0x37, 0xE4 }, // 278
    { 0xD7, 0xE4, 0x57, 0xE4, 0x97, 0xE4, 0x17, 0xE4, 0xE7, 0xE4 }, // 279
    { 0x2D, 0x7D, 0xBD, 0x3D, 0xDD, 0x5D, 0x9D, 0x1D, 0xED, 0x6D }, // 280
    { 0xDD, 0x30, 0x23, 0x30, 0xC3, 0x30, 0x43, 0x30, 0x83, 0x30 }, // 281
    { 0x03, 0x30, 0xFD, 0x30, 0x7D, 0x30, 0xBD, 0x30, 0x3D, 0x30 }, // 282
    { 0x90, 0xD0, 0x00, 0xD0, 0x80, 0xD0, 0x40, 0xD0, 0xC0, 0xD0 }, // 283
    { 0x20, 0xD0, 0xA0, 0xD0, 0x60, 0xD0, 0xE0, 0xD0, 0x10, 0xD0 }, // 284
    { 0x6C, 0xFE, 0xFC, 0x1C, 0x1E, 0x5C, 0x9C, 0xDE, 0xDC, 0x2C }, // 285
    { 0x2D, 0x37, 0xF7, 0x77, 0x0F, 0xCF, 0x4F, 0x2F, 0xEF, 0x6F }, // 286
    { 0xA7, 0x6F, 0x4F, 0x77, 0x67, 0x47, 0xBF, 0xAF, 0x8F, 0xB7 }, // 287
    { 0x9D, 0xC7, 0x47, 0x87, 0x07, 0xFD, 0x7D, 0xBD, 0x3D, 0xDD }, // 288
    { 0x7D, 0x7F, 0x6F, 0x4F, 0xBF, 0xAF, 0x8F, 0x3F, 0x2F, 0x0F }, // 289
    { 0xE7, 0x7D, 0x4D, 0x5D, 0xBD, 0x8D, 0x9D, 0x3D, 0x0D, 0x1D }, // 290
    { 0xB7, 0x5F, 0x9F, 0x1F, 0x6F, 0xAF, 0x2F, 0x4F, 0x8F, 0x0F }, // 291
    { 0xEF, 0x7D, 0xFD, 0x3D, 0x9F, 0x5F, 0xDF, 0x5D, 0xDD, 0x1D }, // 292
    { 0xF5, 0x5D, 0x7D, 0x85, 0x6D, 0x4D, 0x75, 0xE5, 0xC5, 0x45 }, // 293
    { 0x0B, 0x7D, 0xBD, 0x3D, 0xDD, 0x5D, 0x9D, 0x1D, 0x03, 0x13 }, // 294
    { 0x2B, 0xBB, 0x3B, 0xDB, 0x5B, 0x9B, 0x1B, 0xEB, 0x6B, 0xAB }, // 295
    { 0x65, 0xC5, 0x87, 0x85, 0x47, 0xE5, 0xA7, 0x25, 0x67, 0x27 }, // 296
    { 0xFF, 0x00, 0x7F, 0x00, 0xBF, 0x00, 0x3F, 0x00, 0xDF, 0x00 }, // 297
    { 0x5F, 0x00, 0x9F, 0x00, 0x1F, 0x00, 0xEF, 0x00, 0x6F, 0x00 }, // 298
    { 0xF5, 0xA5, 0x85, 0x8D, 0x45, 0x75, 0x6D, 0x65, 0x55, 0x4D }, // 299
    { 0xF0, 0x70, 0xB0, 0x30, 0xD0, 0x50, 0x90, 0x10, 0xE0, 0x60 }, // 300
    { 0x5D, 0x9F, 0xDF, 0x55, 0x05, 0xC5, 0xF7, 0xAD, 0x37, 0x45 }, // 301
    { 0x55, 0x55, 0x55, 0x56, 0x55, 0x59, 0x55, 0x5A, 0x55, 0x65 }, // 302
    { 0x55, 0x66, 0x55, 0x69, 0x55, 0x6A, 0x55, 0x95, 0x55, 0x96 }, // 303
    { 0xAA, 0xA0, 0xB0, 0xA8, 0xB8, 0xA4, 0xB4, 0xAC, 0xBC, 0xA2 }, // 304
    { 0xAD, 0xFD, 0x7D, 0xBD, 0x3D, 0xDD, 0x5D, 0x9D, 0x1D, 0xED }, // 305
    { 0xF4, 0x76, 0xB6, 0x34, 0xD6, 0x54, 0x94, 0x16, 0xE6, 0x64 }, // 306
    { 0xF4, 0xC4, 0xA4, 0xE4, 0x94, 0xD4, 0xB4, 0x84, 0xFC, 0xCC }, // 307
    { 0x3D, 0x33, 0x3D, 0x7B, 0x3D, 0xBB, 0x3D, 0x3B, 0x3D, 0xDB }, // 308
    { 0x3D, 0x5B, 0x3D, 0x9B, 0x3D, 0x1B, 0x3D, 0xEB, 0x3D, 0x6B }, // 309
    { 0xEC, 0xC4, 0xA4, 0xE4, 0x94, 0xD4, 0xB4, 0xF4, 0x8C, 0xCC }, // 310
    { 0xCD, 0x97, 0x17, 0x07, 0xA7, 0x27, 0xE7, 0xFD, 0xDD, 0xC7 }, // 311
    { 0xCE, 0x7E, 0x3E, 0x7C, 0xBE, 0xFE, 0xFC, 0x4E, 0x0E, 0x4C }, // 312
    { 0x4C, 0x04, 0x44, 0x24, 0x64, 0x14, 0x54, 0x34, 0x74, 0x0C }, // 313
    { 0x3F, 0xFB, 0x3F, 0x7B, 0x3F, 0xBB, 0x3F, 0x3B, 0x3F, 0xDB }, // 314
    { 0x3F, 0x5B, 0x3F, 0x9B, 0x3F, 0x1B, 0x3F, 0xEB, 0x3F, 0x6B }, // 315
    { 0xBF, 0x67, 0xBF, 0xF7, 0xBF, 0x77, 0xBF, 0xB7, 0xBF, 0x37 }, // 316
    { 0xBF, 0xD7, 0xBF, 0x57, 0xBF, 0x97, 0xBF, 0x17, 0xBF, 0xE7 }, // 317
    { 0x2F, 0xFF, 0x7F, 0xBF, 0x3F, 0xDF, 0x5F, 0x9F, 0x1F, 0xEF }, // 318
    { 0x77, 0x5F, 0x9F, 0x1F, 0x6F, 0xAF, 0x2F, 0x4F, 0x8F, 0x0F }, // 319
    { 0x8F, 0x35, 0x2F, 0x87, 0x3F, 0x75, 0xDF, 0xD5, 0x7F, 0x27 }, // 320
    { 0xF8, 0xF4, 0xF0, 0xEC, 0xE8, 0xE4, 0xE0, 0xDC, 0xD8, 0xD4 }, // 321
    { 0x1F, 0xBD, 0x3F, 0xBF, 0x7F, 0xFD, 0x3D, 0xDF, 0x5F, 0x5D }, // 322
    { 0xFF, 0x67, 0xFF, 0xF7, 0xFF, 0x77, 0xFF, 0xB7, 0xFF, 0x37 }, // 323
    { 0xFF, 0xD7, 0xFF, 0x57, 0xFF, 0x97, 0xFF, 0x17, 0xFF, 0xE7 }, // 324
    { 0x74, 0xEC, 0x6C, 0xAC, 0x2C, 0xCC, 0x4C, 0x8C, 0x0C, 0xF4 }, // 325
    { 0x5D, 0x6F, 0x5F, 0x7F, 0x2D, 0x1D, 0x3D, 0xAD, 0x9D, 0xBD }, // 326
    { 0x50, 0x00, 0x08, 0x10, 0x18, 0x20, 0x28, 0x30, 0x38, 0x40 }, // 327
    { 0xF8, 0xDF, 0x6F, 0x77, 0x47, 0xBF, 0xAF, 0x97, 0xA7, 0x5F }, // 328
    { 0x9C, 0xDC, 0xBC, 0xF8, 0xD8, 0xB8, 0xF4, 0xD4, 0xB4, 0xC4 }, // 329
    { 0x65, 0x6F, 0x47, 0x07, 0x4F, 0x67, 0x27, 0x77, 0x57, 0x17 }, // 330
    { 0x4F, 0x3F, 0x2F, 0x37, 0x9F, 0x8F, 0x97, 0xBF, 0xAF, 0xB7 }, // 331
    { 0x90, 0xB8, 0xB4, 0xB0, 0xAC, 0xA8, 0xA4, 0xA0, 0x9C, 0x98 }  // 332
  };
}

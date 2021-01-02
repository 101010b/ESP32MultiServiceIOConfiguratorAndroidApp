package com.alphadraco.bleconfigurator;

import java.nio.ByteBuffer;
import java.util.UUID;

public class ConfigVariable {
    public enum VARTYPE {
        ui32,
        i32,
        f32,
        str
    }
    public boolean invalid=true;
    public String name = null;
    public UUID uuid = null;
    public VARTYPE vartype = VARTYPE.ui32;
    public long lmin = 0;
    public long lmax = 0;
    public long lvalue = 0;
    public float fmin = 0.0f;
    public float fmax = 0.0f;
    public float fvalue = 0.0f;
    public String svalue = null;

    public long resignUInt32(long val) {
        if (val < 0)
            val = val + 4294967296L;
        return val;
    }

    public int setData(ByteBuffer list, int offset) {
        byte namelen = list.get(offset);offset++;
        if (namelen <= 0) return -1; // Error
        name = "";
        for (int i=0;i<namelen;i++) {
            name = name + (char) list.get(offset);offset++;
        }
        byte vt = list.get(offset);offset++;
        if ((vt < 0) || (vt > 3)) return -1;
        switch (vt) {
            case 0: // UINT32_t
                vartype = VARTYPE.ui32;
                lmin = Integer.reverseBytes(list.getInt(offset)); offset += 4;
                lmax = Integer.reverseBytes(list.getInt(offset)); offset += 4;
                lvalue = Integer.reverseBytes(list.getInt(offset)); offset += 4;
                lmin = resignUInt32(lmin);
                lmax = resignUInt32(lmax);
                lvalue = resignUInt32(lvalue);
                break;
            case 1:
                vartype = VARTYPE.i32;
                lmin = Integer.reverseBytes(list.getInt(offset)); offset += 4;
                lmax = Integer.reverseBytes(list.getInt(offset)); offset += 4;
                lvalue = Integer.reverseBytes(list.getInt(offset)); offset += 4;
                break;
            case 2:
                vartype = VARTYPE.f32;
                fmin = Float.intBitsToFloat(Integer.reverseBytes(list.getInt(offset))); offset += 4;
                fmax = Float.intBitsToFloat(Integer.reverseBytes(list.getInt(offset))); offset += 4;
                fvalue = Float.intBitsToFloat(Integer.reverseBytes(list.getInt(offset))); offset += 4;
                break;
            case 3:
                vartype = VARTYPE.str;
                byte slen = list.get(offset); offset++;
                if ((slen < 0) || (slen > 100)) return -1;
                svalue = "";
                for (int i=0;i<slen;i++) {
                    svalue = svalue + (char) list.get(offset); offset++;
                }
                break;
        }
        return offset;
    }

    public ConfigVariable() {
    }
}

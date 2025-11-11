package com.clinic.c46.CommonService.type;

public enum Shift {
    AFTERNOON(1),
    MORNING(0);

    private final int code;

    Shift(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static Shift fromCode(int code) {
        for (Shift shift : Shift.values()) {
            if (shift.code == code) {
                return shift;
            }
        }
        throw new IllegalArgumentException("Invalid shift code: " + code);
    }
}

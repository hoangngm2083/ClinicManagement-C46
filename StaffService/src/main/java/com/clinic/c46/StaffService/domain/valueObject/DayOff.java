package com.clinic.c46.StaffService.domain.valueObject;

import com.clinic.c46.CommonService.type.Shift;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@Embeddable
@NoArgsConstructor // JPA require
@Getter
public class DayOff implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private LocalDate date;
    private Shift shift;
    private String reason;


    public DayOff(LocalDate date, Shift shift, String reason) {
        // Có thể thêm validate ở đây (ví dụ: reason không được null)
        this.date = date;
        this.shift = shift;
        this.reason = reason;
    }

    // Implement equals() và hashCode() cho Value Object
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DayOff dayOff = (DayOff) o;
        return Objects.equals(date, dayOff.date) && shift == dayOff.shift;
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, shift);
    }
}

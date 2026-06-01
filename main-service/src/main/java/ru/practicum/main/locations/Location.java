package ru.practicum.main.locations;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Location {
    private Double lat;
    private Double lon;
}

package com.url.shortener.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ClickEventDTO {
    private LocalDate clickDate;
    private Long count;
}
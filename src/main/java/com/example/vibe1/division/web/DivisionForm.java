package com.example.vibe1.division.web;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DivisionForm {

    @NotBlank(message = "Nama divisi wajib diisi")
    private String name;
}

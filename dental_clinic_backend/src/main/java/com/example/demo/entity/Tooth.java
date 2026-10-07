package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "tooth")
public class Tooth {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "fdi_number", nullable = false, unique = true)
    private Integer fdiNumber;
    @Column(nullable = false)
    private Integer quadrant;
    @Column(nullable = false)
    private Integer position;
    @Column(nullable = false, length = 10)
    private String jaw;
    @Column(nullable = false, length = 40)
    private String label;
    public Long getId() { return id; }
    public Integer getFdiNumber() { return fdiNumber; }
    public void setFdiNumber(Integer fdiNumber) { this.fdiNumber = fdiNumber; }
    public String getJaw() { return jaw; }
    public void setJaw(String jaw) { this.jaw = jaw; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
}

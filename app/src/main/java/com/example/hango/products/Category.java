package com.example.hango.products;

import com.google.gson.annotations.SerializedName;

public class Category {
    @SerializedName("description")
    private String description;

    @SerializedName("id")
    private int id;

    @SerializedName("label")
    private String label;

    @SerializedName("name")
    private String name;

    // Constructor
    public Category(String description, int id, String label, String name) {
        this.description = description;
        this.id = id;
        this.label = label;
        this.name = name;
    }

    // Getters và Setters
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name; // Để hiển thị trong Spinner
    }
}
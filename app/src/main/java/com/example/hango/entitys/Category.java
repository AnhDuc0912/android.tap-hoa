package com.example.hango.entitys;

import com.google.gson.annotations.SerializedName;

public class Category {
    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("label")
    private String label;

    @SerializedName("description")
    private String description;

    // Getter
    public int getId() { return id; }
    public String getName() { return name; }
    public String getLabel() { return label; }
    public String getDescription() { return description; }

    // Setter
    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setLabel(String label) { this.label = label; }
    public void setDescription(String description) { this.description = description; }
}

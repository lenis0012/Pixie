package com.lenis0012.chatango.pixie.entities;

import com.lenis0012.chatango.pixie.misc.database.Model;
import com.lenis0012.chatango.pixie.misc.database.Storable;

@Model(name = "definitions")
public class Definition {
    @Storable
    private String name;

    @Storable
    private String meaning;

    @Storable
    private String definedBy;

    @Storable
    private String example = "";

    public Definition() {}

    public Definition(String name, String meaning, String definedBy, String example) {
        this.name = name;
        this.meaning = meaning;
        this.definedBy = definedBy;
        this.example = example;
    }

    public String getExample() {
        return example;
    }

    public String getName() {
        return name;
    }

    public String getMeaning() {
        return meaning;
    }

    public String getDefinedBy() {
        return definedBy;
    }
}

package com.lenis0012.chatango.pixie.entities;

import com.lenis0012.chatango.pixie.misc.database.Model;
import com.lenis0012.chatango.pixie.misc.database.Storable;

@Model(name = "commands")
public class CommandModel {
    @Storable
    private String name;

    @Storable
    private int delay;

    public CommandModel(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }
}

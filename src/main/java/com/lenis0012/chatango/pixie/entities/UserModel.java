package com.lenis0012.chatango.pixie.entities;

import com.google.common.collect.Lists;
import com.lenis0012.chatango.pixie.misc.database.Model;
import com.lenis0012.chatango.pixie.misc.database.Storable;

import java.util.List;

@Model(name = "users")
public class UserModel {
    @Storable
    private String name;

    @Storable(genericType = String.class)
    private List<String> ipAddresses = Lists.newArrayList();

    @Storable
    private boolean admin;

    public UserModel() {}

    public UserModel(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<String> getIpAddresses() {
        return ipAddresses;
    }

    public void addIpAddress(String address) {
        if(!ipAddresses.contains(address)) {
            ipAddresses.add(address);
        }
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }
}

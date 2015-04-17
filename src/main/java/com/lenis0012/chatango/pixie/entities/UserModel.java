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

    @Storable(genericType = String.class)
    private List<String> puids = Lists.newArrayList();

    @Storable
    private boolean admin;

    @Storable
    private boolean banned = false;

    @Storable
    private String lastIp = "";

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

    public boolean addIpAddress(String address) {
        if(!ipAddresses.contains(address)) {
            this.lastIp = address;
            ipAddresses.add(address);
            return true;
        } else if(lastIp != address) {
            this.lastIp = address;
            return true;
        }
        return false;
    }

    public boolean addUid(String uid) {
        if(!puids.contains(uid)) {
            puids.add(uid);
            return true;
        }
        return false;
    }

    public List<String> getPuids() {
        return puids;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public boolean isBanned() {
        return banned;
    }

    public void setBanned(boolean banned) {
        this.banned = banned;
    }

    public String getLastIp() {
        return lastIp;
    }
}

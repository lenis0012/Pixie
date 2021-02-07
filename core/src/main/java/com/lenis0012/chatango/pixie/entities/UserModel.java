package com.lenis0012.chatango.pixie.entities;

import com.lenis0012.chatango.pixie.misc.database.Model;
import com.lenis0012.chatango.pixie.misc.database.Storable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Model(name = "users")
public class UserModel {
    @Storable
    private String name;

    @Storable(genericType = String.class)
    private List<String> ipAddresses = new ArrayList<>();

    @Storable(genericType = String.class)
    private List<String> puids = new ArrayList<>();

    @Storable
    private boolean admin;

    @Storable
    private boolean banned = false;

    @Storable
    private String lastIp = "";

    @Storable
    private int votes = 0;

    @Storable
    private int lastVote = -1;

    @Storable(genericType = String.class)
    private List<String> stalkers = new ArrayList<>();

    @Storable(genericType = String.class)
    private List<String> claimed = new ArrayList<>();

    public UserModel() {
    }

    public UserModel(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<String> getIpAddresses() {
        return ipAddresses;
    }

    public List<String> getStalkers() {
        return Collections.unmodifiableList(stalkers);
    }

    public void addStalker(String stalker) {
        stalkers.add(stalker);
    }

    public void clearStalkers() {
        stalkers.clear();
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

    public long getLastVote() {
        return lastVote;
    }

    public void setLastVote(int lastVote) {
        this.lastVote = lastVote;
    }

    public int getVotes() {
        return votes;
    }

    public void setVotes(int votes) {
        this.votes = votes;
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

    public List<String> getClaimed() {
        return claimed;
    }

    public void addClaimed(String claimed) {
        this.claimed.add(claimed);
    }

    public void removeClaimed(String claimed) {
        this.claimed.remove(claimed);
    }
}

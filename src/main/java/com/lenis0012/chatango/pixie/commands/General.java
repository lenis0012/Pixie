package com.lenis0012.chatango.pixie.commands;

import com.google.common.base.Joiner;
import com.lenis0012.chatango.bot.api.User;
import com.lenis0012.chatango.bot.engine.Room;
import com.lenis0012.chatango.pixie.Command;
import com.lenis0012.chatango.pixie.Pixie;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class General {
    private final Pixie pixie;

    @Command
    public void say(Room room, User user, String[] args) {
        pixie.msg(room, Joiner.on(" ").join(args));
    }
}

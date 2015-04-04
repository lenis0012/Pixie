package com.lenis0012.chatango.pixie;

import com.lenis0012.chatango.bot.api.Message;
import com.lenis0012.chatango.bot.api.User;
import com.lenis0012.chatango.bot.engine.Room;

public class Pixie {

    public void msgTo(Room room, User user, String msg) {
        msg(room, "@" + user.getName() + " " + msg);
    }

    public void msg(Room room, String msg) {
        room.message(new Message(msg));
    }
}

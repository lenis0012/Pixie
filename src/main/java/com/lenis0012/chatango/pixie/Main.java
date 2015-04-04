package com.lenis0012.chatango.pixie;

import com.google.common.collect.Sets;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import com.lenis0012.chatango.bot.ChatangoAPI;
import com.lenis0012.chatango.bot.api.EventListener;
import com.lenis0012.chatango.bot.api.Message;
import com.lenis0012.chatango.bot.api.User;
import com.lenis0012.chatango.bot.engine.Engine;
import com.lenis0012.chatango.bot.engine.Room;
import com.lenis0012.chatango.bot.events.EventHandler;
import com.lenis0012.chatango.bot.events.MessageReceiveEvent;
import com.lenis0012.chatango.bot.utils.AuthException;
import com.lenis0012.chatango.pixie.entities.CommandModel;
import com.mongodb.BasicDBObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class Main implements EventListener {

    public static void main(String[] args) throws Exception {
        if(args.length < 3) {
            throw new RuntimeException("Missing arguments! [userame] [password] [rooms]");
        }
        String username = args[0];
        String password = args[1];
        List<String> rooms = Arrays.asList(args[2].split(","));
        new Main(username, password, rooms);
    }

    private Set<CommandInfo> commands = Sets.newConcurrentHashSet();
    private final Engine engine;
    private final Pixie pixie;

    public Main(String username, String password, List<String> rooms) throws AuthException {
        this.engine = ChatangoAPI.startBot(username, password, rooms);
        engine.getRooms().forEach(r -> r.getEventManager().addListener(this));
        this.pixie = new Pixie(engine, commands);

        // Scan commands
        try {
            ClassPath classPath = ClassPath.from(getClass().getClassLoader());
            for(ClassInfo info : classPath.getTopLevelClassesRecursive("com.lenis0012.chatango.pixie.commands")) {
                Class<?> clazz = Class.forName(info.getName());
                Object instance = clazz.getConstructor(Pixie.class).newInstance(pixie);
                for(Method method : clazz.getMethods()) {
                    if(method.isAnnotationPresent(Command.class)) {
                        CommandModel model = new CommandModel(method.getName());
                        pixie.database().loadModel(model, new BasicDBObject("Name", method.getName()));
                        commands.add(new CommandInfo(instance, method, model));
                    }
                }
            }
        } catch(Exception e) {
            ChatangoAPI.getLogger().log(Level.SEVERE, "Failed to scan command classes!", e);
        }
    }

    @EventHandler
    public void onMessageReceive(MessageReceiveEvent event) {
        final Room room = event.getRoom();
        final Message message = event.getMessage();
        final User user = message.getUser();

        ChatangoAPI.getLogger().log(Level.INFO, "{0}: {1}", new Object[] {message.getUser().getName(), message.getText()});
        String[] params = message.getText().split(" ");
        if(params.length > 1) {
            String name = params[0];
            String cmd = params[1];
            if(name.equalsIgnoreCase("pixie")) {
                String[] args = new String[params.length - 2];
                System.arraycopy(params, 2, args, 0, args.length);
                for(CommandInfo info : commands) {
                    if(System.currentTimeMillis() < info.getAllowed()) {
                        continue;
                    }
                    Command command = info.getMethod().getAnnotation(Command.class);
                    if(command.adminOnly() && !pixie.getUser(user.getName()).isAdmin()) {
                        continue;
                    }
                    List<String> aliases = command.aliases().length > 0 ? Arrays.asList(command.aliases()) : Arrays.asList(info.getMethod().getName().toLowerCase());
                    if(aliases.contains(cmd.toLowerCase())) {
                        try {
                            info.getMethod().invoke(info.getInstance(), room, user, args);
                            info.delay(info.getModel().getDelay());
                        } catch(Exception e) {
                            ChatangoAPI.getLogger().log(Level.WARNING, "Failed to execute command!", e);
                            pixie.msgTo(room, message.getUser(), "An error occurred while running command, please report to lenis0012!");
                        }
                    }
                }

                return; // Is command.
            }
        }

        // Truth/Dare
        if((params[0].equalsIgnoreCase("t") || params[0].equalsIgnoreCase("d")) && user.hasTag("t/d")) {
            user.removetag("t/d");
            List<String> options = params[0].equalsIgnoreCase("t") ? pixie.getTruths() : pixie.getDares();
            String msg = pixie.replaceWords(options.get(pixie.getRandom().nextInt(options.size())));
            pixie.msgTo(room, user, pixie.pickUsers(room, msg));
        }
    }
}
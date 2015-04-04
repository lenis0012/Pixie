package com.lenis0012.chatango.pixie.misc;

import java.lang.reflect.Constructor;

/**
 * Created by Lenny on 4/4/2015.
 */
public class CommonUtil {
    public static Object instance(Class<?> clazz, Object... args) {
        try {
            for(Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                if(constructor.getParameterTypes().length == args.length) {
                    boolean typeMismatch = false;
                    for(int i = 0; i < args.length; i++) {
                        if(!constructor.getParameterTypes()[i].isInstance(args[i])) {
                            typeMismatch = false;
                        }
                    }

                    if(!typeMismatch) {
                        if(!constructor.isAccessible()) {
                            constructor.setAccessible(true);
                        }
                        try {
                            return constructor.newInstance(args);
                        } catch(Exception e) {
                            try {
                                return clazz.newInstance();
                            } catch(Exception e1) {
                                throw new IllegalArgumentException("Invalid amount of parameters when constructing class!", e1);
                            }
                        }
                    }
                }
            }

            //Attempt to use 0 args
            try {
                return clazz.newInstance();
            } catch(Exception e) {
                throw new IllegalArgumentException("Invalid amount of parameters when constructing class!", e);
            }
        } catch(Exception e) {
            throw new IllegalArgumentException("Class constructing failed", e);
        }
    }
}

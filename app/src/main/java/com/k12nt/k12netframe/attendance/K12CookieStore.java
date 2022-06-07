package com.k12nt.k12netframe.attendance;

import android.content.Context;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import cz.msebera.android.httpclient.client.CookieStore;
import cz.msebera.android.httpclient.cookie.Cookie;

public class K12CookieStore  implements CookieStore {

    private final ConcurrentHashMap<String, Cookie> cookies;

    /**
     * Construct a persistent cookie store.
     *
     * @param context Context to attach cookie store to
     */
    public K12CookieStore() {
        cookies = new ConcurrentHashMap<String, Cookie>();
    }

    @Override
    public void addCookie(Cookie cookie) {
        String name = cookie.getName() + cookie.getDomain();

        // Save cookie into local store, or remove if expired
        if (!cookie.isExpired(new Date())) {
            cookies.put(name, cookie);
        } else {
            cookies.remove(name);
        }
    }

    @Override
    public void clear() {
        cookies.clear();
    }

    @Override
    public boolean clearExpired(Date date) {
        boolean clearedAny = false;

        for (ConcurrentHashMap.Entry<String, Cookie> entry : cookies.entrySet()) {
            String name = entry.getKey();
            Cookie cookie = entry.getValue();
            if (cookie.isExpired(date)) {
                // Clear cookies from local store
                cookies.remove(name);

                // We've cleared at least one
                clearedAny = true;
            }
        }

        return clearedAny;
    }

    @Override
    public List<Cookie> getCookies() {
        return new ArrayList<Cookie>(cookies.values());
    }

}

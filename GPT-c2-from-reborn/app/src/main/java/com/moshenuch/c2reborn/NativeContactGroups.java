package com.moshenuch.c2reborn;

import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

/**
 * Nokia-style app-local contact groups.
 * References phone numbers, NOT Android Contacts group IDs or mutable list indices.
 * Never writes or deletes a device contact.
 */
final class NativeContactGroups {
    static final class Group {
        final String id;
        String name;
        final ArrayList<String> numbers = new ArrayList<>();
        Group(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    final ArrayList<Group> groups = new ArrayList<>();
    private final SharedPreferences prefs;
    static final int MAX_GROUPS = 30;
    static final int MAX_MEMBERS = 100;

    NativeContactGroups(SharedPreferences prefs) {
        this.prefs = prefs;
        try {
            JSONArray list = new JSONArray(prefs.getString("contact_groups_v1", "[]"));
            for (int i = 0; i < list.length() && groups.size() < MAX_GROUPS; i++) {
                JSONObject entry = list.optJSONObject(i);
                if (entry == null) continue;
                String id = entry.optString("id", "").trim();
                String name = entry.optString("name", "").trim();
                if (id.isEmpty() || name.isEmpty() || byId(id) != null) continue;
                Group group = new Group(id, name);
                JSONArray contacts = entry.optJSONArray("numbers");
                if (contacts != null) {
                    for (int k = 0; k < contacts.length() && group.numbers.size() < MAX_MEMBERS; k++) {
                        String number = contacts.optString(k, "").trim();
                        if (!canonical(number).isEmpty() && !contains(group, number))
                            group.numbers.add(number);
                    }
                }
                groups.add(group);
            }
        } catch (Exception ignored) { }
    }

    Group byId(String id) {
        if (id == null) return null;
        for (Group group : groups)
            if (group.id.equals(id)) return group;
        return null;
    }

    int indexOf(String id) {
        for (int i = 0; i < groups.size(); i++)
            if (groups.get(i).id.equals(id)) return i;
        return -1;
    }

    Group at(int index) {
        return index >= 0 && index < groups.size() ? groups.get(index) : null;
    }

    private static String canonical(String number) {
        if (number == null) return "";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < number.length(); i++) {
            char c = number.charAt(i);
            if (c >= '0' && c <= '9') out.append(c);
        }
        return out.toString();
    }

    private static boolean contains(Group group, String number) {
        String sought = canonical(number);
        for (String value : group.numbers)
            if (canonical(value).equals(sought)) return true;
        return false;
    }

    private boolean nameAvailable(String proposed, String exceptId) {
        for (Group group : groups) {
            if ((exceptId == null || !exceptId.equals(group.id)) &&
                    group.name.equalsIgnoreCase(proposed)) return false;
        }
        return true;
    }

    Group create(String name) {
        if (name == null) return null;
        name = name.trim();
        if (name.isEmpty() || name.length() > 30 ||
                groups.size() >= MAX_GROUPS || !nameAvailable(name, null)) return null;
        Group group = new Group(UUID.randomUUID().toString(), name);
        groups.add(group);
        persist();
        return group;
    }

    boolean rename(String id, String name) {
        Group group = byId(id);
        if (group == null || name == null) return false;
        name = name.trim();
        if (name.isEmpty() || name.length() > 30 || !nameAvailable(name, id)) return false;
        group.name = name;
        persist();
        return true;
    }

    boolean delete(String id) {
        Group group = byId(id);
        if (group == null) return false;
        groups.remove(group);
        persist();
        return true;
    }

    boolean addMember(String id, String number) {
        Group group = byId(id);
        if (group == null || canonical(number).isEmpty() ||
                group.numbers.size() >= MAX_MEMBERS || contains(group, number)) return false;
        group.numbers.add(number.trim());
        persist();
        return true;
    }

    boolean removeMember(String id, String number) {
        Group group = byId(id);
        if (group == null) return false;
        String key = canonical(number);
        for (int i = 0; i < group.numbers.size(); i++) {
            if (canonical(group.numbers.get(i)).equals(key)) {
                group.numbers.remove(i);
                persist();
                return true;
            }
        }
        return false;
    }

    private void persist() {
        JSONArray all = new JSONArray();
        try {
            for (Group group : groups) {
                JSONObject row = new JSONObject();
                row.put("id", group.id);
                row.put("name", group.name);
                JSONArray members = new JSONArray();
                for (String number : group.numbers) members.put(number);
                row.put("numbers", members);
                all.put(row);
            }
        } catch (Exception ignored) { }
        prefs.edit().putString("contact_groups_v1", all.toString()).apply();
    }
}
